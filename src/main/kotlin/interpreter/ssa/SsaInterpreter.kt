package interpreter.ssa

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFpRoundingMode
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KSort
import memory.*
import memory.ssa.SsaState

abstract class SsaInterpreter {
    var print = false
    val maxExecutionDepth = 150

    private fun createSymbolicParam(value: SsaNode, mem: Memory): Symbolic {
        return when (value) {
            is LinkSsaNode -> createSymbolicParam(value.deLink(), mem)

            is ParamSsaNode -> {
                val type = Type.fromSsa(value.valueType!!, mem)
                val symbolic = type.createSymbolic("param#${value.name}", mem)

                return symbolic
            }

            else -> error("should be link or param")
        }
    }

    abstract suspend fun startFunction(
        func: FuncSsaNode,
        args: List<Symbolic?>? = null,
        ctx: KContext,
        initialState: SsaState
    ): Pair<Collection<SymbolicResult<SsaNode>>, Map<String, KSort>>

    abstract fun statisticsStartVisit(node: SsaNode, state: SsaState): Boolean

    abstract fun statisticsEndVisit(node: SsaNode, state: SsaState): Boolean

    fun prepareState(
        func: FuncSsaNode,
        args: List<Symbolic?>?,
        state: SsaState
    ) {
        if (print)
            println("\nFUNCTION ${func.name}\n")

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
            args: List<Symbolic>,
            mem: Memory
        ): Symbolic {
            return with(mem.ctx) {
                if (args.size == 1 && op == "*") {
                    when (val star = args[0]) {
                        is StarSymbolic -> star.get(mem)
                        else -> star
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

                    else -> {
                        val a = args[0]
                        val b = args[1]
                        when {
                            a is StarSymbolic && b is StarSymbolic && op == "==" -> a.eq(b, mem)
                            a is StarSymbolic && b is StarSymbolic && op == "!=" -> a.eq(b, mem).not(mem)
                            a is StarSymbolic -> visitOp(op, listOf(a, LocalStarSymbolic(b, a.field)), mem)
                            b is StarSymbolic -> visitOp(op, listOf(LocalStarSymbolic(a, b.field), b), mem)
                            else -> error("op: '${op}' $a $b")
                        }
                    }
                }
            }
        }

        fun visitAlloc(starType: StarType, mem: Memory): Symbolic {
            val address = mem.addNewDefaultStar("", starType.elementType)
            return GlobalStarSymbolic(starType, address, BoolType.`false`(mem), "")
        }

        fun visitSlice(high: Symbolic?, x: Symbolic, mem: Memory): Symbolic {
            return when {
                high == null -> x

                x is StarSymbolic -> {
                    val array = x.get(mem) as FiniteArraySymbolic
                    val newArray = FiniteArraySymbolic(
                        ArrayType(
                            array.arrayType.elementType(),
                            IntType.cast(high, Int64Type(), mem).int64(mem)
                        ),
                        array.array,
                        array.arrayBehaviour,
                        mem
                    )
                    x.put(newArray, mem)
                }

                x is StopSymbolic -> x
                else -> error("should be an array, not ${x.javaClass.name}")
            }
        }

        val knownFunctions: Map<String, (List<Symbolic>, Memory) -> Symbolic?> = mapOf(
            "real" to { args: List<Symbolic>, mem: Memory ->
                (args[0].complex(mem)).real
            },
            "imag" to { args, mem ->
                (args[0].complex(mem)).img
            },
            "float64" to { args, mem ->
                args[0].floatExpr(mem).toFloatSymbolic()
            },
            "len" to { args, mem ->
                when (val arr = args[0]) {
                    is FiniteArraySymbolic -> arr
                    is StarSymbolic -> arr.get(mem).array(mem)
                    else -> error("should be [] or *[], not ${arr.javaClass.name}")
                }.length()
            },
            "make" to { args, mem ->
                val type = args[0].type as ArrayType
                val length = args[1].int64(mem)
                val arrayWithLenType = ArrayType(type.elementType, length)
                arrayWithLenType.defaultSymbolic(mem)
            },
            "external:New" to { args, _ ->
//                actually it should be a wrapper
                args[0]
            },
            "external:IsNaN" to { args, mem ->
                mem.ctx.mkFpIsNaNExpr(args[0].floatExpr(mem)).toBoolSymbolic()
            },
            "external:Inf" to { args, mem ->
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
                println("Sprintf is not emulated properly")
                UninterpretedType.fromString("Sprintf", mem)
            },
            "assume" to { args, mem ->
                assume(args, mem)
            },
            "external:assume" to { args, mem ->
                assume(args, mem)
            },
            "makeSymbolic" to { args, mem ->
                makeSymbolic(args, mem)
            },
            "external:makeSymbolic" to { args, mem ->
                makeSymbolic(args, mem)
            }
        )

        private fun assume(args: List<Symbolic>, mem: Memory): Symbolic? =
            if (mem.addCond(args[0].bool(mem), false))
                null
            else
                StopSymbolic

        private fun makeSymbolic(args: List<Symbolic>, mem: Memory): Symbolic =
            args[0].type.createSymbolic("makeSymbolic#", mem)
    }
}