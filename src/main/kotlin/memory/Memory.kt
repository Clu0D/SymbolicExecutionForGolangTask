package memory

import interpreter.ssa.*
import io.ksmt.KContext
import io.ksmt.expr.KApp
import io.ksmt.expr.KArrayConst
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KArraySort
import io.ksmt.sort.KBv64Sort
import io.ksmt.sort.KSort
import kotlin.time.Duration.Companion.seconds

// todo typealias (?)
data class Memory(
    val ctx: KContext,
    private val variablesVisibilityLevel: MutableList<MutableSet<String>> = mutableListOf(mutableSetOf()),
    private val localVariablesStack: MutableList<MutableMap<String, Symbolic>> = mutableListOf(mutableMapOf()),
    private val returnsStack: MutableList<MutableList<SymbolicReturn>> = mutableListOf(mutableListOf()),
    private val instrOnPathStack: MutableList<MutableList<SsaNode>> = mutableListOf(mutableListOf()),
    val errors: MutableList<SymbolicError> = mutableListOf(),
    private val globalValues: MutableMap<String, Pair<Long, InfiniteArray>> = mutableMapOf(),
    private val declaredTypeFields: MutableMap<String, Map<String, Type>> = mutableMapOf(),
    private var pathCond: MutableList<Pair<BoolSymbolic, Boolean>> = mutableListOf(),
    val createdConsts: MutableMap<String, KSort> = mutableMapOf(),
    private val solver: KZ3Solver = KZ3Solver(ctx)
) {
    val print = true
    private val SOLVER_TIMEOUT = 1.seconds

    private val uniqueCounter: GlobalUniqueCounter = globalUniqueCounter

    private var fullErrorsCond = BoolType.`false`(this)

    private fun localVariables() = localVariablesStack.first()

    private fun returns() = returnsStack.first()

    private fun fullReturnCond(): BoolSymbolic = with(ctx) {
        mkOr(returnsStack.flatten().map { it.cond.boolExpr(this@Memory) })
    }.toBoolSymbolic()

    fun fullPathCond() = with(ctx) {
        mkAnd(
            mkAnd(pathCond.map { it.first.boolExpr(this@Memory) }),
            mkNot(fullErrorsCond.boolExpr(this@Memory)),
            mkNot(fullReturnCond().boolExpr(this@Memory))
        ).toBoolSymbolic()
    }

    fun enterFunction() {
        localVariablesStack.add(0, mutableMapOf())
        returnsStack.add(0, mutableListOf())
        instrOnPathStack.add(0, mutableListOf())
        addVisibilityLevel()
    }

    fun exitFunction(): List<SymbolicReturn> {
        removeVisibilityLevel()
        localVariablesStack.removeFirst()
        val results = returns()
        addCond(with(ctx) {
            mkOr(results.map { it.cond.boolExpr(this@Memory) })
        }.toBoolSymbolic(), false)
        returnsStack.removeFirst()
        instrOnPathStack.removeFirst()
        return results
    }

    override fun toString() = """
        "${localVariables()}"
        "$errors"
    """.trimIndent()

    fun hasValue(node: SsaNode): Boolean = when (node) {
        is ValueSsaNode -> localVariables().containsKey(node.name)
        is LinkToSsaNode -> hasValue(node.deLink())
        else -> error("only ValueNodes allowed ${node.printItself()}")
    }

    fun readValue(name: String): Symbolic = when (name) {
////        todo should int be here?????
//        "int" -> Symbolic(IntType())
        "nil" -> AddressType().zero(this)
        else -> localVariables()[name] ?: error("mem does not have \"$name\"")
    }

    fun readValue(node: SsaNode): Symbolic? = when (node) {
        is ValueSsaNode ->
            if (hasValue(node))
                readValue(node.name)
            else
                null

        is LinkToSsaNode -> readValue(node.deLink())
        else -> error("only ValueNodes allowed ${node.printItself()}")
    }

    fun writeValue(name: String, value: Symbolic) {
        val oldValue = localVariables()[name]
        localVariables()[name] =
            if (oldValue == null) {
                value
            } else {
                ite(fullPathCond(), value, oldValue)
            }
    }

    fun writeValue(node: SsaNode, value: Symbolic): Unit = when (node) {
        is ValueSsaNode -> writeValue(node.name, value)
        is LinkToSsaNode -> writeValue(node.deLink(), value)
        else -> error("only ValueNodes allowed ${node.printItself()}")
    }

    fun writeAll(vararg es: Pair<String, Symbolic>) {
        es.forEach {
            writeValue(it.first, it.second)
        }
    }

    fun addError(errorCond: BoolSymbolic, error: String) = with(ctx) {
        val sat = solver.checkWithAssumptions(listOf(fullPathCond().expr, errorCond.expr), SOLVER_TIMEOUT)
        when (sat) {
            KSolverStatus.SAT -> {
                val fullErrorCond = mkAnd(errorCond.expr, fullPathCond().boolExpr(this@Memory))
                errors += SymbolicError(fullErrorCond.toBoolSymbolic(), error)
                fullErrorsCond = mkOr(errorCond.expr, fullErrorCond).toBoolSymbolic()
            }

            KSolverStatus.UNSAT -> {
//                do nothing, this error can't happen
            }

            KSolverStatus.UNKNOWN -> {
                println("UNKNOWN sat on error (saving as an error, but not removing from path):\n\treason:${solver.reasonOfUnknown()}")

                val fullErrorCond = mkAnd(errorCond.expr, fullPathCond().boolExpr(this@Memory))
                errors += SymbolicError(fullErrorCond.toBoolSymbolic(), error)
            }
        }
    }

    fun addError(error: SymbolicError) = addError(error.cond, error.error)

    fun addResults(returns: List<Symbolic>) {
        returns() += SymbolicReturn(fullPathCond(), returns)
    }

    fun addCond(cond: BoolSymbolic, needToPush: Boolean): Boolean {
        pathCond.add(cond to needToPush)

        if (needToPush)
            solver.push()

        solver.assert(cond.expr)
        val sat = solver.check(SOLVER_TIMEOUT)
        return when (sat) {
            KSolverStatus.SAT -> {
                if (print) println("SAT on path")
                true
            }

            KSolverStatus.UNSAT -> {
                if (print) println("UNSAT sat on path")
                false
            }

            KSolverStatus.UNKNOWN -> {
                println("UNKNOWN sat on path (continuing this branch), reason: ${solver.reasonOfUnknown()}")
                true
            }
        }
    }

    fun removeCond() {
        solver.pop()
        pathCond.removeLast()
    }

    /** adds new local variables
     * using arg values
     * or creating new symbolic if there is null
     */
    fun addArgsSymbolic(
        fields: Map<String, Type?>,
        args: List<Symbolic?>,
    ) {
        var i = 0
        val newArgs = fields.map { (name, type) ->
            name to (args.getOrNull(i) ?: type!!.createSymbolic(name, this)).also { i++ }
        }.toTypedArray()
        writeAll(*newArgs)
    }

    /** adds new local variables
     * using arg values
     * or creating new default if there is null
     */
    fun addArgsDefault(
        fields: Map<String, Type>,
        args: List<Symbolic>
    ) {
        var i = 1
        val newArgs = fields.map { (name, type) ->
            if (print) println("GEN name $name ${args.getOrNull(i)}")
            name to (args.getOrNull(i) ?: type.defaultSymbolic(this)).also { i++ }
        }.toTypedArray()
        writeAll(*newArgs)
    }

    fun globalArrayName(ctxName: String, type: Type): String {
        val arrayType = if (type is StarType && type.fake)
            type.elementType
        else
            type

        val globalArrayName = "$ctxName:$arrayType"
        if (globalValues[globalArrayName] == null) {
            if (print) println("creating global $globalArrayName")
//            todo null?
            globalValues += globalArrayName to (1L to InfiniteArray.create(arrayType, this, false))
        }
        return globalArrayName
    }

    fun addNewStarObject(prefix: String, value: Symbolic): Int64Symbolic {
        val globalArrayName = globalArrayName(prefix, value.type)
        val (size, array) = globalValues[globalArrayName]!!
        array.put(size, value, this)
        globalValues[globalArrayName] = (size + 1) to array
        return AddressType().fromInt(size, this)
    }

    fun putStarObject(prefix: String, value: Symbolic, address: Int64Symbolic) {
        val globalArrayName = globalArrayName(prefix, value.type)
        val (_, array) = globalValues[globalArrayName]!!
        array.put(address, value, this)
    }

    fun getStarObject(prefix: String, type: Type, address: Int64Symbolic): Symbolic {
        val globalArrayName = globalArrayName(prefix, type)
        val (_, array) = globalValues[globalArrayName]!!
        return array.get(address, this)
    }

    fun addType(name: String, fields: Map<String, Type>) {
        declaredTypeFields += mapOf(name to fields)
    }

    fun addVisibilityLevel() {
        instrOnPathStack.add(0, mutableListOf())
        variablesVisibilityLevel.add(0, mutableSetOf())
    }

    fun removeVisibilityLevel() {
        variablesVisibilityLevel.first().forEach { name ->
            localVariables().remove(name)
        }
        variablesVisibilityLevel.removeFirst()
        instrOnPathStack.removeFirst()
    }

    fun ite(cond: BoolSymbolic, fromBody: Symbolic, fromElse: Symbolic): Symbolic =
        with(ctx) {
            val condExpr = cond.boolExpr(this@Memory)
            when (fromBody) {
                is ComplexSymbolic -> {
                    val bodyReal = fromBody.complex(this@Memory).real.expr
                    val bodyImg = fromBody.complex(this@Memory).img.expr
                    val elseReal = fromBody.complex(this@Memory).real.expr
                    val elseImg = fromBody.complex(this@Memory).img.expr

                    ComplexSymbolic(
                        mkIte(condExpr, bodyReal, elseReal),
                        mkIte(condExpr, bodyImg, elseImg)
                    )
                }

                is BoolSymbolic -> mkIte(
                    condExpr, fromBody.boolExpr(this@Memory), fromElse.boolExpr(this@Memory)
                ).toBoolSymbolic()

                is IntSymbolic -> mkIte(
                    condExpr,
                    fromBody.expr as KExpr<KSort>,
                    fromElse.intExpr(this@Memory) as KExpr<KSort>
                ).let { Type.toSymbolic(it) }

                is FloatSymbolic -> mkIte(
                    condExpr, fromBody.floatExpr(this@Memory), fromElse.floatExpr(this@Memory)
                ).let { Type.toSymbolic(it) }

                is UninterpretedSymbolic -> mkIte(
                    condExpr,
                    fromBody.uninterpretedExpr(this@Memory),
                    fromElse.uninterpretedExpr(this@Memory)
                ).let { Type.toSymbolic(it) }

                is FiniteArraySymbolic ->
                    FiniteArraySymbolic(
                        ite(cond, fromBody.length, fromElse.array(this@Memory).length).int64(this@Memory),
                        this@Memory,
                        ite(
                            cond,
                            fromBody.innerArray.toSymbolic(),
                            fromElse.array(this@Memory).innerArray.toSymbolic()
                        ) as InfiniteArraySymbolic
                    )

                is InfiniteArraySymbolic if(fromBody.elementType is StarType) ->
                    InfiniteStarArray(
                        fromBody.elementType,
                        mkIte(condExpr, fromBody.arrayExpr, (fromElse as InfiniteArraySymbolic).arrayExpr)
                                as KArrayConst<KArraySort<KBv64Sort, KSort>, KSort>
                    ).toSymbolic()

                is InfiniteArraySymbolic ->
                    InfiniteSimpleArray(
                        fromBody.elementType,
                        mkIte(condExpr, fromBody.arrayExpr, (fromElse as InfiniteArraySymbolic).arrayExpr)
                                as KArrayConst<KArraySort<KBv64Sort, KSort>, KSort>
                    ).toSymbolic()

                is ArrayStarSymbolic -> {
                    when (fromElse) {
                        is ArrayStarSymbolic -> {
                            ArrayStarSymbolic(
                                ite(cond, fromBody.address, fromElse.address).int64(this@Memory),
                                ite(cond, fromBody.array, fromElse.array).array(this@Memory),
                                fromBody.fake
                            )
                        }

                        else ->
                            ite(cond, fromBody.toGlobal(this@Memory), fromElse)
                    }
                }

                is StarSymbolic -> {
                    if (fromBody is GlobalStarSymbolic && fromElse is GlobalStarSymbolic) {
                        GlobalStarSymbolic(
                            "",
                            fromBody.elementType,
                            ite(cond, fromBody.address, fromElse.address).int64(this@Memory),
                            fromBody.fake
                        )
                    } else {
                        ite(
                            cond,
                            fromBody.toGlobal(this@Memory),
                            (fromElse.star(this@Memory)).toGlobal(this@Memory)
                        )
                    }
                }

                is InfiniteArraySymbolic -> TODO()

                else -> error(fromBody.javaClass.simpleName)
            }
        }

    fun getTypeFields(elementType: StructType): Map<String, Type> {
//        return declaredTypeFields[elementType]!!
        TODO()
    }

    companion object {

        class GlobalUniqueCounter {

            private var uniqueSolverNameCounter = 0
            fun getNext(): Int {
                return uniqueSolverNameCounter++
            }
        }

        val globalUniqueCounter = GlobalUniqueCounter()
    }

    fun clone() = Memory(
        ctx,
        variablesVisibilityLevel.map { a -> a.map { it }.toMutableSet() }.toMutableList(),
        localVariablesStack.map { it.map { (a, b) -> a to b }.toMap().toMutableMap() }.toMutableList(),
        returnsStack.map { a -> a.map { it }.toMutableList() }.toMutableList(),
        instrOnPathStack.map { a -> a.map { it }.toMutableList() }.toMutableList(),
        errors.map { it }.toMutableList(),
        globalValues.map { (a, b) -> a to b }.toMap().toMutableMap(),
        declaredTypeFields.map { (a, b) -> a to b }.toMap().toMutableMap(),
        pathCond.map { it }.toMutableList(),
        createdConsts,
        KZ3Solver(ctx).apply {
            pathCond.forEach { (cond, push) ->
                this.assert(cond.expr)
                if (push)
                    this.push()
            }
        }
    )

    fun addConst(name: String, sort: KSort): KApp<KSort, *> {
        val uniqueName = "${uniqueCounter.getNext()}@$name"
        createdConsts[uniqueName] = sort
        return ctx.mkConst(uniqueName, sort)
    }

    fun addInstrToPath(instr: SsaNode) {
        val deLinkedInstr = when (instr) {
            is LinkToSsaNode -> instr.deLink()
            else -> instr
        }
        instrOnPathStack.first().add(0, deLinkedInstr)
    }

    fun instrOnPath(): List<SsaNode> {
        return instrOnPathStack.flatten()
    }
}
