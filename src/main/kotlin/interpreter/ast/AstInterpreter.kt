package interpreter.ast

import com.jetbrains.rd.util.printlnError
import interpreter.ssa.SsaInterpreter.Companion.visitOp
import interpreter.ssa.SsaNode
import memory.Memory
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.sort.KBvSort
import io.ksmt.sort.KSort
import memory.*
import memory.ast.AstState
import memory.ssa.SsaState

abstract class AstInterpreter(
    val functionDeclarations: Map<String, AstFuncDecl>,
    val typeDeclarations: List<AstType>
) {
    abstract fun startFunction(
        func: AstFuncDecl,
        args: List<Symbolic?>? = null,
        ctx: KContext,
        initialState: AstState
    ): Pair<Collection<SymbolicResult<Any>>, Map<String, KSort>>

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
                val obj = x.get(mem)
                (obj.struct(mem)).fields[selectorName]
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

                    else -> error("only AstIdent")
                }
                val x = mem.readValue(name).int(mem)
                val xType = x.type as IntType
                val value = when (token) {
                    "++" -> mkBvAddExpr(x.expr as KExpr<KBvSort>, xType.fromInt(1, mem).expr as KExpr<KBvSort>)
                    "--" -> mkBvSubExpr(x.expr as KExpr<KBvSort>, xType.fromInt(1, mem).expr as KExpr<KBvSort>)
                    else -> error("should only be ++ or --")
                }.toIntSymbolic()
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
                "INT" -> Int64Type().fromInt(node.value.toLong(), mem)
                "FLOAT" -> Float64Type().fromDouble(node.value.toDouble(), mem)
                "STRING" -> UninterpretedType.fromString(node.value, mem)
                else -> error(node.kind)
            }
        }

        fun visitAssignStmt(lhs: List<AstNode>, token: String, rhsResults: List<Symbolic?>, state: SsaState) {
            val mem = state.mem
            val memObjects = rhsResults.map { rhsResult ->
                when (rhsResult) {
                    is ListSymbolic -> rhsResult.list
                    null -> error("rhs can't be null")
                    else -> listOf(rhsResult)
                }
            }.flatten()

            if (lhs.size == memObjects.size)
                printlnError("ERROR lhs size != rhs size")

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
    }
}