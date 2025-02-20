package interpreter.ast

import interpreter.ast.AstInterpreter.Companion.STATIC_FOR_MAX_LENGTH
import interpreter.ast.AstInterpreter.Companion.visitAssignStmt
import interpreter.ast.AstInterpreter.Companion.visitBasicLit
import interpreter.ast.AstInterpreter.Companion.visitGenDeclaration
import interpreter.ast.AstInterpreter.Companion.visitIncDec
import interpreter.ast.AstInterpreter.Companion.visitSelector
import interpreter.ast.AstInterpreter.Companion.visitType
import interpreter.ssa.SsaInterpreter.Companion.knownFunctions
import interpreter.ssa.SsaInterpreter.Companion.visitOp
import memory.Memory
import io.ksmt.KContext
import memory.*

class StaticAstInterpreterRecursive(
    val functionDeclarations: Map<String, AstFuncDecl>,
    typeDeclarations: List<AstType>,
    ctx: KContext,
) {
    val mem = Memory(ctx, TODO())

    init {
        typeDeclarations.forEach { visit(it, TODO()) }
    }

    fun startFunction(
        func: AstFuncDecl,
        args: List<Symbolic?>?,
        ctx: KContext
    ): List<SymbolicResult<Any>> {
        return visitFunction(func, args) + mem.errors  as List<SymbolicReturn<Any>>
    }

    fun visitFunction(
        func: AstFuncDecl,
        args: List<Symbolic?>? = null
    ): List<SymbolicReturn<Any>> {
        mem.enterFunction()
        mem.addArgsSymbolic(
            (func.params ?: listOf()).map(AstField::toPairs).flatten().toMap(),
            args ?: listOf()
        )

        visit(func.body, TODO())

        return mem.exitFunction() as List<SymbolicReturn<Any>>
    }

    private fun visitCall(
        call: AstIdent,
        args: List<Symbolic>,
        functionDeclarations: Map<String, AstFuncDecl>,
        mem: Memory
    ): Symbolic? {
        return when (call.name) {
            in knownFunctions -> knownFunctions[call.name]!!(args, mem)
            else -> {
                val result = functionDeclarations[call.name]?.let {
                    visitFunction(it, args)
                } ?: error("no function ${call.name}")

                result.combineToSymbolic<Any>(mem)
            }
        }
    }

    private fun visitIf(
        condExpr: BoolSymbolic,
        bodyNode: AstNode,
        elseNode: AstNode?
    ): Symbolic? {

        mem.addVisibilityLevel()
        mem.addCond(condExpr, true)
        val body = visit(bodyNode, TODO())
        mem.removeCond()
        mem.removeVisibilityLevel()

        if (elseNode != null) {
            mem.addVisibilityLevel()
            mem.addCond(condExpr.not(mem), true)
            val elseBody = visit(elseNode, TODO())
            mem.removeCond()
            mem.addVisibilityLevel()
        }

        return null
    }

    private fun visit(node: AstNode, state: State): Symbolic? {
        return when (node) {
            is AstAssignStmt -> {
                visitAssignStmt(node.lhs, node.token, node.rhs.map { visit(it, TODO()) }, TODO())
                null
            }

            is AstBasicLit -> {
                visitBasicLit(node, mem)
            }

            is AstBinaryExpr -> {
                val x = visit(node.x, TODO())!!
                val y = visit(node.y, TODO())!!
                visitOp(node.op, listOf(x, y), TODO())
            }

            is AstBlockStmt -> {
                node.statements?.map { statement ->
                    visit(statement, TODO())
                }
//                todo does it return something?
//                    .last()
                null
            }

            is AstCallExpr -> {
                val argumentResults = node.args.map { argument ->
                    visit(argument, TODO())!!
                }
                when (node.call) {
                    is AstIdent -> {
                        visitCall(node.call, argumentResults, functionDeclarations, mem)
                    }

                    else -> TODO()
                }
            }

            is AstField -> TODO()
            is AstFile -> error("astFile visit is in Main")

            is AstFuncDecl -> TODO("declaration inside another declaration")

            is AstIdent -> {
                mem.readValue(node.name)
            }

            is AstIf -> {
                val cond = visit(node.cond, TODO())!!
                visitIf(cond.bool(mem), node.body, node.elseBody)
            }

            is AstReturn -> {
                val results = node.result.map { statement ->
                    visit(statement, TODO())!!
                }
                mem.addResults(results)
                null
            }

            is AstUnaryExpr -> {
                val x = visit(node.x, TODO())!!
                visitOp(node.op, listOf(x), TODO())
            }

            is AstGenDecl -> {
                visitGenDeclaration(node.specs, mem)
                null
            }

            is AstFor -> {
                // todo is init ignored?
                var init = visit(node.init, TODO())
                var i = 0
                while (true) {
                    val cond = if (i > STATIC_FOR_MAX_LENGTH) {
                        BoolType.`false`(mem)
                    } else {
                        visit(node.cond, TODO())!!.bool(mem)
                    }

                    val body = visitIf(cond, node.body, null)
                    if (i > STATIC_FOR_MAX_LENGTH) {
                        break
                    }
                    i++
                }
                visit(node.post, TODO())
            }

            is AstIncDec -> {
                visitIncDec(node.x, node.tok, mem)
                null
            }

            is AstIndexExpr -> {
                val x = visit(node.x, TODO())
                val index = visit(node.index, TODO())
                val xArr = x!!.array(mem)

                xArr.get(index!!.int64(mem), mem)
            }

            is AstSelector ->
                visitSelector(node.selectorName, visit(node.x, TODO()), mem)

            is AstUnsupported -> error(node.unsupported)

            is AstType -> visitType(node, mem)

            else -> error(node.javaClass.name)
        }
    }
}

