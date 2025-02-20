package memory

import interpreter.UnSatPathException
import interpreter.ssa.*
import io.ksmt.KContext
import io.ksmt.expr.KApp
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KArraySort
import io.ksmt.sort.KBv64Sort
import io.ksmt.sort.KSort
import memory.ssa.SsaState
import sun.security.util.Length
import kotlin.time.Duration.Companion.seconds

data class Memory(
    val ctx: KContext,
    val state: SsaState,
    private val variablesVisibilityLevel: MutableList<MutableSet<String>> = mutableListOf(mutableSetOf()),
    private val localVariablesStack: MutableList<MutableMap<String, Symbolic>> = mutableListOf(mutableMapOf()),
    private val returnsStack: MutableList<MutableList<SymbolicReturn<SsaNode>>> = mutableListOf(mutableListOf()),
    internal val instrOnPathStack: MutableList<MutableList<SsaNode>> = mutableListOf(mutableListOf()),
    val errors: MutableList<SymbolicError<SsaNode>> = mutableListOf(),
    private val globalValues: MutableMap<String, Triple<List<Boolean>, InfiniteArray, InfiniteArray>> = mutableMapOf(),
    private val declaredTypeFields: MutableMap<String, Map<String, Type>> = mutableMapOf(),
    private var pathCond: MutableList<Pair<BoolSymbolic, Boolean>> = mutableListOf(),
    val createdConsts: MutableMap<String, KSort> = mutableMapOf(),
    private val solver: KZ3Solver = KZ3Solver(ctx)
) {
    val print = false
    private val SOLVER_TIMEOUT = 0.5.seconds

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

    fun exitFunction(): List<SymbolicReturn<SsaNode>> {
        removeVisibilityLevel()
        localVariablesStack.removeFirst()
        val results = returns()
// todo remove
//        addCond(with(ctx) {
//            mkOr(results.map { it.cond.boolExpr(this@Memory) })
//        }.toBoolSymbolic(), false)
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
        "nil" -> Int64Type().zero(this)
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

    fun addError(errorCond: BoolSymbolic, error: String) {
        with(ctx) {
            val sat = solver.checkWithAssumptions(listOf(fullPathCond().expr, errorCond.expr), SOLVER_TIMEOUT)
            when (sat) {
                KSolverStatus.SAT -> {
                    val fullErrorCond = mkAnd(errorCond.expr, fullPathCond().boolExpr(this@Memory))
                    errors += SymbolicError<SsaNode>(fullErrorCond.toBoolSymbolic(), error, state.getVisitedNodes())
                    fullErrorsCond = mkOr(errorCond.expr, fullErrorCond).toBoolSymbolic()
                }

                KSolverStatus.UNSAT -> {
//                do nothing, this error can't happen
                }

                KSolverStatus.UNKNOWN -> {
                    println("UNKNOWN sat on error (saving as an error, but not removing from path):\n\treason:${solver.reasonOfUnknown()}")

                    val fullErrorCond = mkAnd(errorCond.expr, fullPathCond().boolExpr(this@Memory))
                    errors += SymbolicError(fullErrorCond.toBoolSymbolic(), error, state.getVisitedNodes())
                }
            }
            solver.assert(errorCond.not(this@Memory).expr)
            if (solver.check(SOLVER_TIMEOUT) == KSolverStatus.UNSAT)
                throw UnSatPathException()
        }
    }

    fun addError(error: SymbolicError<SsaNode>) =
        addError(error.cond, error.error)

    fun addResults(returns: List<Symbolic>) {
        returns() += SymbolicReturn(fullPathCond(), returns, state.getVisitedNodes())
    }

    fun addCond(cond: BoolSymbolic, needToPush: Boolean): Boolean {
        if (print) println("adding cond ($cond)")

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
                if (print) {
                    println("UNSAT sat on path")

                    println("")
                    println("PATH COND:")
                    println(pathCond.joinToString("\n") { (a, b) -> if (b) "needToPush! $a" else "$a" })
                    println("ERRORS:")
                    println(errors.joinToString("\n") { "${it.cond}\t:\n\t${it.error}" })
                    println("")
                }

                false
            }

            KSolverStatus.UNKNOWN -> {
                println("UNKNOWN sat on path (continuing this branch), reason: ${solver.reasonOfUnknown()}")
                true
            }
        }
    }

    fun removeCond() {
        while (true) {
            val (_, needToPush) = pathCond.removeLast()
            if (needToPush)
                solver.pop()
            break
        }
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

    fun globalArrayName(type: Type, isStarFake: Boolean, length: Int64Symbolic?): String {
        val globalArrayName = "$type"
        if (globalValues[globalArrayName] == null) {
            if (print)
                println(
                    "creating globalArray $globalArrayName isStarFake=$isStarFake"
                )

            val realType = if (type is SimpleType)
                ArrayInfSimpleType(type as SimpleType)
            else
                ArrayInfSimpleType(StarType(type, true))

            globalValues.put(
                globalArrayName, Triple(
                    listOf(),
                    InfiniteArray.create(
                        realType,
                        SymbolicArrayBehaviour(isStarFake, globalArrayName),
                        this
                    ),
                    InfiniteArray.create(
                        realType,
                        DefaultArrayBehaviour(isStarFake, null),
                        this
                    )
                )
            )
        }
        return globalArrayName
    }

    fun addNewSymbolicStar(type: Type, canBeNull: Boolean, isStarFake: Boolean, name: String): Int64Symbolic {
        val globalArrayName =
            globalArrayName(type, isStarFake, Int64Type().createSymbolic("$name:length", this).int64(this))

        val (areSymbolic, arraySymbolic, arrayDefault) = globalValues[globalArrayName]!!

        val size = areSymbolic.size
        globalValues[globalArrayName] = Triple(areSymbolic + true, arraySymbolic, arrayDefault)

        with(ctx) {
            val address = addConst(name, Int64Type().sort(this@Memory)) as KExpr<KBv64Sort>

            /**
             * ensures the global address is:
             *
             * > 0 (for non-null) or >= 0 for (nullable)
             *
             * <= size + 1
             *
             * != i + 1 (for any other elements that are local)
             */
            addCond(
                mkAnd(
                    if (canBeNull)
                        mkBvSignedGreaterOrEqualExpr(address, Int64Type().fromInt(0, this@Memory).expr)
                    else
                        mkBvSignedGreaterExpr(address, Int64Type().fromInt(0, this@Memory).expr),
                    mkBvSignedLessOrEqualExpr(address, Int64Type().fromInt((size + 1).toLong(), this@Memory).expr),
                    *areSymbolic.mapIndexed { i, isSymbolic ->
                        if (!isSymbolic)
                            address neq Int64Type().fromInt((size + 1).toLong(), this@Memory).expr
                        else
                            null
                    }.filterNotNull().toTypedArray()
                ).toBoolSymbolic(),
                false
            )
            return Int64Symbolic(address)
        }
    }

    fun addNewDefaultStar(type: Type, isStarFake: Boolean): Int64Symbolic {
        val globalArrayName = globalArrayName(type, isStarFake, Int64Type().defaultSymbolic(this).int64(this))

        val (areSymbolic, arraySymbolic, arrayDefault) = globalValues[globalArrayName]!!

        val size = areSymbolic.size
        globalValues[globalArrayName] = Triple(areSymbolic + false, arraySymbolic, arrayDefault)

        with(ctx) {
            val address = addConst("$type:address", Int64Type().sort(this@Memory)) as KExpr<KBv64Sort>

            /**
             * ensures the local address is:
             *
             * == size + 1
             */
            addCond(
                (address eq Int64Type().fromInt((size + 1).toLong(), this@Memory).expr).toBoolSymbolic(),
                false
            )
            return Int64Symbolic(address)
        }
    }

    fun putStar(
        address: Int64Symbolic,
        value: Symbolic,
        isSymbolic: BoolSymbolic,
        isStarFake: Boolean
    ) {
        val globalArrayName = globalArrayName(value.type, isStarFake, (value as? FiniteArraySymbolic)?.length)

        val (areSymbolic, arraySymbolic, arrayDefault) = globalValues[globalArrayName]!!

        val oldSymbolic = arraySymbolic.get(address, this@Memory)
        val oldDefault = arrayDefault.get(address, this@Memory)

        arraySymbolic.put(address, ite(isSymbolic, value, oldSymbolic), this@Memory)
        arrayDefault.put(address, ite(isSymbolic, oldDefault, value), this@Memory)
    }

    fun getStar(
        type: Type,
        address: Int64Symbolic,
        isSymbolic: BoolSymbolic,
        isStarFake: Boolean
    ): Symbolic {
        val globalArrayName = globalArrayName(type, isStarFake,  (type as? ArrayType)?.length)

        val (areSymbolic, arraySymbolic, arrayDefault) = globalValues[globalArrayName]!!

        val oldSymbolic = arraySymbolic.get(address, this@Memory)
        val oldDefault = arrayDefault.get(address, this@Memory)

        return ite(isSymbolic, oldSymbolic, oldDefault)
    }

//    fun putLocalStarObject(value: Symbolic, address: Int64Symbolic) {
//        val globalArrayName = globalArrayName(value.type, arrayBehaviour.isStarFake)
//        val (_, array) = globalValues[globalArrayName]!!
//        array.put(address, value, this, arrayBehaviour)
//    }
//
//    fun getGlobalStarObject(type: Type, address: Int64Symbolic): Symbolic {
//        val globalArrayName = globalArrayName(type, arrayBehaviour.isStarFake)
//        val (_, array) = globalValues[globalArrayName]!!
//        return array.get(address, this, arrayBehaviour)
//    }

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

                is FiniteArraySymbolic -> {
                    val elseArray = fromElse.array(this@Memory)
                    val length = ite(cond, fromBody.length, elseArray.length).int64(this@Memory)
                    val combinedArray = CombinedArray(fromBody.array, elseArray.array, cond)

                    FiniteArraySymbolic(
                        length,
                        combinedArray,
                        this@Memory
                    )
                }

                is StarSymbolic -> {
                    if (fromBody is GlobalStarSymbolic && fromElse is GlobalStarSymbolic) {
                        GlobalStarSymbolic(
                            fromBody.starType,
                            ite(cond, fromBody.address, fromElse.address).int64(this@Memory),
                            ite(cond, fromBody.isSymbolic, fromElse.isSymbolic).bool(this@Memory),
                            fromBody.isStarFake
                        )
                    } else {
                        ite(
                            cond,
                            fromBody.toGlobal(this@Memory, false),
                            (fromElse.star(this@Memory)).toGlobal(this@Memory, false)
                        )
                    }
                }

                else -> error(fromBody.javaClass.simpleName)
            }
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

    fun clone(state: SsaState) = Memory(
        ctx,
        state,
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
