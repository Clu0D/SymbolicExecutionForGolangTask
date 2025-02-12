package interpreter

import memory.Memory
import io.ksmt.KContext
import io.ksmt.sort.KSort
import memory.*

abstract class Interpreter(
    val functionDeclarations: Map<String, AstFuncDecl>,
    val typeDeclarations: List<AstType>
) {
    abstract fun startFunction(
        func: AstFuncDecl,
        args: List<Symbolic?>? = null,
        ctx: KContext,
        initialState: State
    ): Pair<Collection<SymbolicResult>, Map<String, KSort>>

    companion object {
        const val STATIC_FOR_MAX_LENGTH = 10

        fun visitType(node: AstType, mem: Memory) = when (node.typeNode) {
            is AstStruct -> {
                mem.addType(
                    node.name,
                    (node.typeNode.fields ?: listOf()).map(AstField::toPairs).flatten().toMap()
                )
                null
            }

            else -> TODO(node.typeNode.printItself())
        }

        fun visitSelector(selectorName: String, x: Symbolic?, mem: Memory) = when (x) {
            is StarSymbolic -> {
                // todo wrong as mem has no size assertion for now
                val obj = x.get(mem)
                (obj as StructSymbolic).fields[selectorName]
            }

            is StructSymbolic -> x.fields[selectorName]

            else -> error("should be star or declared ${x!!.type}")
        }

        fun visitIncDec(x: AstNode, token: String, mem: Memory) {
            with(mem.ctx) {
                val name = when (x) {
                    is AstIdent -> {
                        x.name
                    }

                    else -> TODO()
                }

                val value = when (token) {
                    "++" -> mkBvAddExpr(mem.readValue(name).intExpr(), mkBv(1))
                    "--" -> mkBvSubExpr(mem.readValue(name).intExpr(), mkBv(1))
                    else -> error("should only be ++ or --")
                }.toSymbolic()
                mem.writeValue(name, value)
                null
            }
        }

        fun visitGenDeclaration(specs: List<AstNode>, mem: Memory) {
            specs.forEach { spec ->
                when (spec) {
                    is AstImportSpec -> {
                        println("import is ignored for now ${spec.path}")
                    }

                    is AstValueSpec -> {
                        mem.addArgsDefault(
                            spec.names.associateWith { Type.fromName(spec.typeName) },
                            (spec.values ?: listOf()).map { value ->
                                visitBasicLit(AstBasicLit(spec.typeName, value), mem)
                            }
                        )
                    }

                    else -> error(spec.printItself())
                }

            }
        }

        fun visitBasicLit(
            node: AstBasicLit,
            mem: Memory
        ) = with(mem.ctx) {
            when (node.kind) {
                "INT" -> node.value.toInt().toBv().toSymbolic()
                "FLOAT" -> node.value.toDouble().expr.toSymbolic()
                "STRING" -> UninterpretedType.fromString(node.value, mem)
                else -> TODO(node.kind)
            }
        }

        fun visitOp(
            op: String,
            args: List<Symbolic?>,
            mem: Memory
        ): Symbolic {
            return with(mem.ctx) {
                if (args.size == 1 && op == "*") {
                    when (val star = args[0]!!) {
                        is StarSymbolic -> star.get(mem)
                        else -> error("only star type allowed")
                    }
                } else when (args[0]) {
                    is IntSymbolic -> {
                        val exprs = args.map { it!!.intExpr() }
                        val a = exprs[0]
                        val b by lazy { exprs[1] }

                        when (op) {
                            "+" -> mkBvAddExpr(a, b)
                            "*" -> mkBvMulExpr(a, b)
                            "-" -> if (args.size == 1)
                                mkBvNegationExpr(a)
                            else
                                mkBvSubExpr(a, b)

                            "/" -> {
                                mem.addError(
                                    (b eq IntType.ZERO(mem)).toSymbolic(),
                                    "divisionByZero"
                                )
                                mkBvSignedDivExpr(a, b)
                            }

                            "%" -> {
                                mem.addError(
                                    (b eq IntType.ZERO(mem)).toSymbolic(),
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
                            "^" -> mkBvXorExpr(a, b)
                            "~" -> mkBvNegationExpr(a)

                            ">>" -> mkBvArithShiftRightExpr(a, b)
                            "<<" -> mkBvArithShiftRightExpr(a, b)

                            else -> TODO(op)
                        }.toSymbolic()
                    }

                    is FloatSymbolic -> {
                        val exprs = args.map { it!!.floatExpr(mem) }
                        val a = exprs[0]
                        val b by lazy { exprs[1] }

                        val roundingMode = mkFpRoundingModeExpr(io.ksmt.expr.KFpRoundingMode.RoundTowardZero)

                        when (op) {
                            "+" -> mkFpAddExpr(roundingMode, a, b)
                            "*" -> mkFpMulExpr(roundingMode, a, b)

//                            that actually can give an error, but only if we use Const
//                            (https://stackoverflow.com/questions/51512496/go-float-zero-division-compiler-error)
//                            don't know can it happen in runtime or not
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

                            else -> error("'${op}'")
                        }.toSymbolic()
                    }

                    is BoolSymbolic -> {
                        val exprs = args.map { it!!.boolExpr() }
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
                            else -> error("'${op}'")
                        }.toSymbolic()
                    }

                    is ComplexSymbolic -> {
                        val real = visitOp(op, args.map { it!!.complex().real }, mem)
                        val img = visitOp(op, args.map { it!!.complex().img }, mem)
                        ComplexSymbolic(real.floatExpr(mem), img.floatExpr(mem))
                    }

                    is AbstractArraySymbolic -> {
                        val a = args[0]!!.arrayFinite()
                        val b = args[1]!!.arrayFinite()

                        when (op) {
                            "==" -> a.eq(b, mem)
                            "!=" -> mkNot(a.eq(b, mem).boolExpr()).toSymbolic()

                            else -> error("'${op}'")
                        }
                    }

                    is StarSymbolic -> {
                        val a = args[0]!!.star()
                        val b = args[1]!!.star()

                        when (op) {
                            "==" -> a.eq(b, mem)
                            "!=" -> mkNot(a.eq(b, mem).boolExpr()).toSymbolic()

                            else -> error("'${op}'")
                        }
                    }

//                    todo
//                    is StarSymbolic -> {
//                        val exprs = args.map { it!!.star() }
//                        val a = exprs[0]
//                        val b by lazy { exprs[1] }
//
//                        when (op) {
//                            "==" -> {
//                                if (a.type != b.type)
//                                    error("types don't match")
//                                mkEq(a.expr, b.expr)
//                            }
//
//                            "!=" -> {
//                                if (a.type != b.type)
//                                    error("types don't match")
//                                mkNot(mkEq(a.expr, b.expr))
//                            }
//
//                            else -> error("'${op}'")
//                        }.toSymbolic()
//                    }

                    else -> error("'$op' '$args'")
                }
            }
        }

        fun visitAssignStmt(lhs: List<AstNode>, token: String, rhsResults: List<Symbolic?>, mem: Memory) {
            val memObjects = rhsResults.map { rhsResult ->
                when (rhsResult) {
                    is ListSymbolic -> rhsResult.list
                    null -> error("rhs can't be null")
                    else -> listOf(rhsResult)
                }
            }.flatten()

            assert(lhs.size == memObjects.size) { "lhs size != rhs size" }

            lhs.zip(memObjects).forEach { (nameAst, value) ->
                val name = (nameAst as AstIdent).name
                mem.writeValue(
                    name, when (token) {
                        "=" -> value
                        ":=" -> value
                        else -> {
                            val token = token.dropLast(1)
                            val oldValue = mem.readValue(name)
                            visitOp(token, listOf(oldValue, value), mem)
                        }
                    }
                )
            }
        }

        val knownFunctions: Map<String, (List<Symbolic>, Memory) -> Symbolic> = mapOf(
            "real" to { args: List<Symbolic>, _: Memory ->
                assert(args.size == 1)
                (args[0] as ComplexSymbolic).real
            },
            "imag" to { args, _ ->
                assert(args.size == 1)
                (args[0] as ComplexSymbolic).img
            },
            "float64" to { args, mem ->
                assert(args.size == 1)
                args[0].floatExpr(mem).toSymbolic()
            },
            "len" to { args, _ ->
                assert(args.size == 1)
                args[0].arrayFinite().length()
            },
            "make" to { args, mem ->
                assert(args.size == 2)
                val type = args[0].type as ArrayType
                val length = args[1].int()
                val arrayWithLenType = ArrayType(type.elementType, length)
                arrayWithLenType.defaultSymbolic(mem, true)
            },
            "external:New" to { args, _ ->
                assert(args.size == 1)
//                actually it should be a wrapper
                args[0]
            }
        )
    }
}