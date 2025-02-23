package memory

import interpreter.UnSatPathException
import interpreter.ssa.*
import io.ksmt.KContext
import io.ksmt.expr.KApp
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.KBv64Sort
import io.ksmt.sort.KSort
import memory.ssa.SsaState
import kotlin.time.Duration.Companion.seconds

data class Memory(
    val ctx: KContext,
    val state: SsaState,
    private val variablesVisibilityLevel: MutableList<MutableSet<String>> = mutableListOf(mutableSetOf()),
    private val localVariablesStack: MutableList<MutableMap<String, Symbolic>> = mutableListOf(mutableMapOf()),
    private val returnsStack: MutableList<MutableList<SymbolicReturn<SsaNode>>> = mutableListOf(mutableListOf()),
    internal val instrOnPathStack: MutableList<MutableList<SsaNode>> = mutableListOf(mutableListOf()),
    val errors: MutableList<SymbolicError<SsaNode>> = mutableListOf(),
    private val globalValues: MutableMap<String, Triple<List<Boolean>, InfAbstractArray, InfAbstractArray>> = mutableMapOf(),
    private var pathCond: MutableList<Pair<BoolSymbolic, Boolean>> = mutableListOf(),
    val createdConsts: MutableMap<String, KSort> = mutableMapOf(),
    internal val solver: KZ3Solver = KZ3Solver(ctx)
) {
    val print = 0
    internal val SOLVER_TIMEOUT = 0.5.seconds

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

    fun readValue(name: String): Symbolic {
        return when (name) {
            "nil" -> Int64Type().zero(this)
            else -> localVariables()[name] ?: error("mem does not have \"$name\"")
        }
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
//                do nothing, error can't happen
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
        if (print > 1) println("adding cond")

        pathCond.add(cond to needToPush)

        if (needToPush)
            solver.push()

        solver.assert(cond.expr)
        val sat = solver.check(SOLVER_TIMEOUT)
        return when (sat) {
            KSolverStatus.SAT -> {
                if (print > 1)
                    println("SAT on path")
                true
            }

            KSolverStatus.UNSAT -> {
                if (print > 1)
                    println("UNSAT sat on path")
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

    fun globalArrayName(field: String, type: Type): String {
        val globalArrayName = "$field:$type"
        if (globalValues[globalArrayName] == null) {
            if (print > 0)
                println(
                    "creating globalArray $globalArrayName"
                )
            val infType = if (type is ArrayType)
                InfArrayType(type.elementType)
            else
                type

            val array1 = InfAbstractArray.create(
                infType,
                SymbolicArrayBehaviour(globalArrayName),
                this
            )
            val array2 = InfAbstractArray.create(
                infType,
                DefaultArrayBehaviour,
                this
            )
            globalValues.put(
                globalArrayName, Triple(
                    listOf(),
                    array1,
                    array2
                )
            )
            if (type is StructType) {
                TODO()
            } else if (type is NamedType) {
                type.underlying.fields.map { (name, fieldType) ->
                    globalArrayName("${type.name}:$name", fieldType)
                }
            }
        }
        return globalArrayName
    }

    fun addNewSymbolicStar(field: String, type: Type, canBeNull: Boolean, name: String): Int64Symbolic {
        val globalArrayName = globalArrayName(field, type)

        val (areSymbolic, arraySymbolic, arrayDefault) = globalValues[globalArrayName]!!

        val newSize = areSymbolic.size + 2L
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
                        mkBvSignedGreaterOrEqualExpr(address, Int64Type().fromInt(1, this@Memory).expr)
                    else
                        mkBvSignedGreaterExpr(address, Int64Type().fromInt(1, this@Memory).expr),
                    mkBvSignedLessOrEqualExpr(address, Int64Type().fromInt(newSize, this@Memory).expr),
                    *areSymbolic.mapIndexed { i, isSymbolic ->
                        if (!isSymbolic)
                            address neq Int64Type().fromInt(newSize, this@Memory).expr
                        else
                            null
                    }.filterNotNull().toTypedArray()
                ).toBoolSymbolic(),
                false
            )
            return Int64Symbolic(address)
        }
    }

    fun addNewDefaultStar(field: String, type: Type): Int64Symbolic {
        val globalArrayName = globalArrayName(field, type)

        val (areSymbolic, arraySymbolic, arrayDefault) = globalValues[globalArrayName]!!

        val newSize = areSymbolic.size + 2L
        if (print > 1) println("NEWDEFAULT ${newSize} $type")
        globalValues[globalArrayName] = Triple(areSymbolic + false, arraySymbolic, arrayDefault)

        return Int64Type().fromInt(newSize, this)
    }

    fun putStar(
        field: String,
        type: Type,
        address: Int64Symbolic,
        value: Symbolic,
        isSymbolic: BoolSymbolic
    ) {
        val globalArrayName = globalArrayName(field, type)

        val (areSymbolic, arraySymbolic, arrayDefault) = globalValues[globalArrayName]!!

        val oldSymbolic = arraySymbolic.get(address, this@Memory)
            .toSymbolic(SymbolicArrayBehaviour(globalArrayName), this)
        val oldDefault = arrayDefault.get(address, this@Memory)
            .toSymbolic(DefaultArrayBehaviour, this)

        if (oldSymbolic is AbstractArray && oldDefault is AbstractArray && value is StarSymbolic) {
            val valueArray = value.get(this@Memory)
            arraySymbolic.put(address, ite(isSymbolic, valueArray, oldSymbolic), this@Memory)
            arrayDefault.put(address, ite(isSymbolic, oldDefault, valueArray), this@Memory)
        } else {
            arraySymbolic.put(address, ite(isSymbolic, value, oldSymbolic), this@Memory)
            arrayDefault.put(address, ite(isSymbolic, oldDefault, value), this@Memory)
        }
    }

    fun getStar(
        field: String,
        type: Type,
        address: Int64Symbolic,
        isSymbolic: BoolSymbolic
    ): Symbolic {
        val globalArrayName = globalArrayName(field, type)

        val (areSymbolic, arraySymbolic, arrayDefault) = globalValues[globalArrayName]!!

        val oldSymbolic = arraySymbolic.get(address, this@Memory)
            .toSymbolic(SymbolicArrayBehaviour(globalArrayName), this)
        val oldDefault = arrayDefault.get(address, this@Memory)
            .toSymbolic(DefaultArrayBehaviour, this)

        return ite(isSymbolic, oldSymbolic, oldDefault)
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

    fun ite(cond: BoolSymbolic, fromBody: Symbolic, fromElse: Symbolic): Symbolic {
        val result1 = solver.checkWithAssumptions(listOf(cond.expr), SOLVER_TIMEOUT / 5)
        if (result1 == KSolverStatus.UNSAT)
            return fromElse

        val result2 = solver.checkWithAssumptions(listOf(cond.not(this).expr), SOLVER_TIMEOUT / 5)
        if (result2 == KSolverStatus.UNSAT)
            return fromBody

        return with(ctx) {
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
                    val elseArray = fromElse as FiniteArraySymbolic
                    val length = ite(cond, fromBody.length(), elseArray.length()).int64(this@Memory)
                    val combinedArray = InfCombinedArray(
                        fromBody.array, elseArray.array, cond
                    )

                    FiniteArraySymbolic(
                        ArrayType(
                            fromBody.arrayType.elementType,
                            length
                        ),
                        combinedArray,
                        null,
                        this@Memory
                    )
                }

                is StarSymbolic -> {
                    if (fromBody is GlobalStarSymbolic && fromElse is GlobalStarSymbolic) {
                        GlobalStarSymbolic(
                            fromBody.starType,
                            ite(cond, fromBody.address, fromElse.address).int64(this@Memory),
                            ite(cond, fromBody.isSymbolic, fromElse.isSymbolic).bool(this@Memory),
                            fromBody.field
                        )
                    } else {
                        ite(
                            cond,
                            fromBody.toGlobal(this@Memory),
                            (fromElse.star(this@Memory)).toGlobal(this@Memory)
                        )
                    }
                }

                else -> error(fromBody.javaClass.simpleName)
            }
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
