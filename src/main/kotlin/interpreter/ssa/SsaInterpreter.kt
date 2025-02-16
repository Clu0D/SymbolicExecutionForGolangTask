package interpreter.ssa

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFpRoundingMode
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KSort
import memory.*
import memory.ssa.SsaState

abstract class SsaInterpreter {
    var print = true

    private fun createSymbolicParam(value: SsaNode, mem: Memory): Symbolic {
        return when (value) {
            is LinkSsaNode -> createSymbolicParam(value.deLink(), mem)
            is ParamSsaNode -> {
                val name = value.name
                val type = Type.fromSsa(value.valueType!!, mem, true)
                if (print)
                    println("createSymbolic $name $type")

                return type.createSymbolic(value.name, mem)
            }

            else -> error("should be link or param")
        }
    }

    abstract suspend fun startFunction(
        func: FuncSsaNode,
        args: List<Symbolic?>? = null,
        ctx: KContext,
        initialState: SsaState
    ): Pair<Collection<SymbolicResult>, Map<String, KSort>>

    abstract fun statisticsStartVisit(node: SsaNode, state: SsaState)

    abstract fun statisticsEndVisit(node: SsaNode, state: SsaState)

    fun prepareState(
        func: FuncSsaNode,
        args: List<Symbolic?>?,
        state: SsaState
    ) {
        if (print) {
            println()
            println()
            println("FUNCTION!@#@!\t\t${func.name}")
            println()
        }

        val initializedArgs = func.params.mapIndexed { i, node ->
            statisticsStartVisit(node, state)
            (args?.getOrNull(i) ?: createSymbolicParam(node, state.mem)).also {
                statisticsEndVisit(node, state)
            }
        }.toMutableList<Symbolic?>()

        state.waitingNodes.add(
            SsaStartFunctionNode(func) to initializedArgs
        )
    }

    companion object {
        fun visitOp(
            op: String,
            argsReferenced: List<Symbolic>,
            mem: Memory
        ): Symbolic {
            val args = argsReferenced.map { it.getDereferenced(mem) }
            return with(mem.ctx) {
                if (args.size == 1 && op == "*") {
                    when (val star = args[0]) {
                        is StarSymbolic -> star.dereference()
                        else -> error("only star type allowed, not $star")
                    }
                } else when (val arg0 = args[0]) {
                    is IntSymbolic if(arg0.hasSign()) -> {
                        val exprs = args.map { it.intExpr(mem) }
                        val a = exprs[0]
                        val b by lazy { exprs[1] }
                        val zero = (args[0].type as IntType).zeroExpr(mem)

                        when (op) {
                            "+" -> mkBvAddExpr(a, b)
                            "*" -> mkBvMulExpr(a, b)
                            "-" -> if (args.size == 1)
                                mkBvNegationExpr(a)
                            else
                                mkBvSubExpr(a, b)

                            "/" -> {
                                mem.addError(
                                    (b eq zero).toBoolSymbolic(),
                                    "divisionByZero"
                                )
                                mkBvSignedDivExpr(a, b)
                            }

                            "%" -> {
                                mem.addError(
                                    (b eq zero).toBoolSymbolic(),
                                    "divisionByZero"
                                )
                                mkBvSignedModExpr(a, b)
                            }

                            "<" -> mkBvSignedLessExpr(a, b)
                            "<=" -> mkBvSignedLessOrEqualExpr(a, b)
                            ">" -> mkBvSignedGreaterExpr(a, b)
                            ">=" -> mkBvSignedGreaterOrEqualExpr(a, b)

                            "==" -> a eq b
                            "!=" -> a neq b

                            "&" -> mkBvAndExpr(a, b)
                            "|" -> mkBvOrExpr(a, b)
                            "^" -> if (args.size == 1) {
                                mkBvNegationExpr(a)
                            } else {
                                mkBvXorExpr(a, b)
                            }

                            "~" -> mkBvNegationExpr(a)

                            ">>" -> mkBvArithShiftRightExpr(a, b)
                            "<<" -> mkBvArithShiftRightExpr(a, b)

                            else -> error("op: Int'${op}'")
                        }.let { Type.toSymbolic(it) }
                    }

                    is IntSymbolic if(!arg0.hasSign()) -> TODO("unsigned operations")
                    is FloatSymbolic -> {
                        val exprs = args.map { it.floatExpr(mem) }
                        val a = exprs[0]
                        val b by lazy { exprs[1] }

                        val roundingMode = mkFpRoundingModeExpr(KFpRoundingMode.RoundTowardZero)

                        when (op) {
                            "+" -> mkFpAddExpr(roundingMode, a, b)
                            "*" -> mkFpMulExpr(roundingMode, a, b)

                            "/" -> mkFpDivExpr(roundingMode, a, b)

                            "-" -> if (args.size == 1)
                                mkFpNegationExpr(a)
                            else
                                mkFpSubExpr(roundingMode, a, b)


                            "<" -> mkFpLessExpr(a, b)
                            "<=" -> mkFpLessOrEqualExpr(a, b)
                            ">" -> mkFpGreaterExpr(a, b)
                            ">=" -> mkFpGreaterOrEqualExpr(a, b)

                            "==" -> mkFpEqualExpr(a, b)
                            "!=" -> mkNot(mkFpEqualExpr(a, b))

                            else -> error("op: Float'${op}'")
                        }.let { Type.toSymbolic(it) }
                    }

                    is BoolSymbolic -> {
                        val exprs = args.map { it.boolExpr(mem) }
                        val a = exprs[0]
                        val b by lazy { exprs[1] }

                        when (op) {
                            "&&" -> mkAnd(a, b)
                            "||" -> mkOr(a, b)
                            "!" -> mkNot(a)
                            "==" -> mkEq(a, b)
                            "!=" -> mkNot(mkEq(a, b))

                            "<" -> mkAnd(mkNot(a), b)
                            "<=" -> mkOr(mkNot(a), b)
                            ">" -> mkAnd(a, mkNot(b))
                            ">=" -> mkOr(a, mkNot(b))
                            else -> error("op: Bool'${op}'")
                        }.toBoolSymbolic()
                    }

                    is ComplexSymbolic -> {
                        val real = visitOp(op, args.map { it.complex(mem).real }, mem).float64(mem)
                        val img = visitOp(op, args.map { it.complex(mem).img }, mem).float64(mem)
                        ComplexSymbolic(real.expr, img.expr)
                    }

                    is StarSymbolic -> {
                        val a = args[0].star(mem)

                        when (val b = args[1]) {
                            is StarSymbolic -> when (op) {
                                "==" -> a.eq(b, mem)
                                "!=" -> mkNot(a.eq(b, mem).boolExpr(mem)).toBoolSymbolic()

                                else -> error("op: '${op}' $args")
                            }

                            else -> {
                                visitOp(op, listOf(a.get(mem), b), mem)
                            }
                        }
                    }

                    else -> error("op:'$op' '$args'")
                }
            }
        }

        fun visitAlloc(type: Type, mem: Memory): GlobalStarSymbolic {
            val address = mem.addNewStarObject("", type.defaultSymbolic(mem))
            return GlobalStarSymbolic("", type, address, false)
        }

        fun visitSlice(high: Symbolic?, x: Symbolic, mem: Memory): Symbolic = when {
            high == null -> x
            x is StarSymbolic -> {
                when (val obj = x.dereference().get(mem)) {
                    is FiniteArraySymbolic ->
                            FiniteArraySymbolic(
                                high.int64(mem),
                                mem,
                                obj
                            )

                    is InfiniteArraySymbolic ->
                            FiniteArraySymbolic(
                                high.int64(mem),
                                mem,
                                obj
                            )

                    else -> error("should be an array, not ${obj.type}")
                }
            }

            else -> error("should be a *, not ${x.type}")
        }

        val knownFunctions: Map<String, (List<Symbolic>, Memory) -> Symbolic> = mapOf(
            "real" to { args: List<Symbolic>, mem: Memory ->
                assert(args.size == 1)
                (args[0].complex(mem)).real
            },
            "imag" to { args, mem ->
                assert(args.size == 1)
                (args[0].complex(mem)).img
            },
            "float64" to { args, mem ->
                assert(args.size == 1)
                args[0].floatExpr(mem).toFloatSymbolic()
            },
            "len" to { args, mem ->
                assert(args.size == 1)
                when (val a = args[0]) {
                    is StarSymbolic -> a.get(mem).array(mem).length()
                    else -> a.array(mem).length()
                }
            },
            "make" to { args, mem ->
                assert(args.size == 2)
                val type = args[0].type as ArrayType
                val length = args[1].int64(mem)
                val arrayWithLenType = ArrayType(type.elementType, length)
                arrayWithLenType.defaultSymbolic(mem)
            },
            "external:New" to { args, _ ->
                assert(args.size == 1)
//                actually it should be a wrapper
                args[0]
            },
            "external:IsNaN" to { args, mem ->
                assert(args.size == 1)
                mem.ctx.mkFpIsNaNExpr(args[0].floatExpr(mem)).toBoolSymbolic()
            },
            "external:Inf" to { args, mem ->
                assert(args.size == 1)
                with(mem.ctx) {
                    mem.ite(
                        mkBvSignedGreaterExpr(
                            args[0].intExpr(mem),
                            (args[0].type as IntType).zero(mem).expr as KExpr<KBvSort>
                        ).toBoolSymbolic(),
                        mkFpInf(false, Float64Type().sort(mem)).let { Type.toSymbolic(it) },
                        mkFpInf(true, Float64Type().sort(mem)).let { Type.toSymbolic(it) }
                    )
                }
            },
            "external:Sprintf" to { args, mem ->
                assert(args.size > 1)
                println("Sprintf is not emulated properly")
                UninterpretedType.fromString("Sprintf", mem)
            }
        )
    }
}