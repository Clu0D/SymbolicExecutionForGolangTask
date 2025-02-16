package interpreter.ast

import interpreter.ast.Interpreter.Companion.STATIC_FOR_MAX_LENGTH
import interpreter.ast.Interpreter.Companion.visitAssignStmt
import interpreter.ast.Interpreter.Companion.visitBasicLit
import interpreter.ast.Interpreter.Companion.visitGenDeclaration
import interpreter.ast.Interpreter.Companion.visitIncDec
import interpreter.ast.Interpreter.Companion.visitSelector
import interpreter.ast.Interpreter.Companion.visitType
import interpreter.ssa.SsaInterpreter.Companion.knownFunctions
import interpreter.ssa.SsaInterpreter.Companion.visitOp
import memory.Memory
import io.ksmt.KContext
import memory.*

class StaticInterpreterRecursive(
    val functionDeclarations: Map<String, AstFuncDecl>,
    typeDeclarations: List<AstType>,
    ctx: KContext,
) {
    val mem = Memory(ctx)

    init {
        typeDeclarations.forEach { visit(it) }
    }

    fun startFunction(
        func: AstFuncDecl,
        args: List<Symbolic?>?,
        ctx: KContext
    ): List<SymbolicResult> {
        return visitFunction(func, args) + mem.errors
    }

    fun visitFunction(
        func: AstFuncDecl,
        args: List<Symbolic?>? = null
    ): List<SymbolicReturn> {
        mem.enterFunction()
        mem.addArgsSymbolic(
            (func.params ?: listOf()).map(AstField::toPairs).flatten().toMap(),
            args ?: listOf()
        )

        visit(func.body)

        return mem.exitFunction()
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

                result.combineToSymbolic(mem)
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
        val body = visit(bodyNode)
        mem.removeCond()
        mem.removeVisibilityLevel()

        if (elseNode != null) {
            mem.addVisibilityLevel()
            mem.addCond(condExpr.not(mem), true)
            val elseBody = visit(elseNode)
            mem.removeCond()
            mem.addVisibilityLevel()
        }

        return null
    }

    private fun visit(node: AstNode): Symbolic? {
        return when (node) {
            is AstAssignStmt -> {
                visitAssignStmt(node.lhs, node.token, node.rhs.map { visit(it) }, mem)
                null
            }

            is AstBasicLit -> {
                visitBasicLit(node, mem)
            }

            is AstBinaryExpr -> {
                val x = visit(node.x)!!
                val y = visit(node.y)!!
                visitOp(node.op, listOf(x, y), mem)
            }

            is AstBlockStmt -> {
                node.statements?.map { statement ->
                    visit(statement)
                }
//                todo does it return something?
//                    .last()
                null
            }

            is AstCallExpr -> {
                val argumentResults = node.args.map { argument ->
                    visit(argument)!!
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
                val cond = visit(node.cond)!!
                visitIf(cond.bool(mem), node.body, node.elseBody)
            }

            is AstReturn -> {
                val results = node.result.map { statement ->
                    visit(statement)!!
                }
                mem.addResults(results)
                null
            }

            is AstUnaryExpr -> {
                val x = visit(node.x)!!
                visitOp(node.op, listOf(x), mem)
            }

            is AstGenDecl -> {
                visitGenDeclaration(node.specs, mem)
                null
            }

            is AstFor -> {
                // todo is init ignored?
                var init = visit(node.init)
                var i = 0
                while (true) {
                    val cond = if (i > STATIC_FOR_MAX_LENGTH) {
                        BoolType.`false`(mem)
                    } else {
                        visit(node.cond)!!.bool(mem)
                    }

                    val body = visitIf(cond, node.body, null)
                    if (i > STATIC_FOR_MAX_LENGTH) {
                        break
                    }
                    i++
                }
                visit(node.post)
            }

            is AstIncDec -> {
                visitIncDec(node.x, node.tok, mem)
                null
            }

            is AstIndexExpr -> {
                val x = visit(node.x)
                val index = visit(node.index)
                val xArr = x!!.array(mem)

                xArr.get(index!!.int64(mem), mem)
            }

            is AstSelector ->
                visitSelector(node.selectorName, visit(node.x), mem)

            is AstUnsupported -> error(node.unsupported)

            is AstType -> visitType(node, mem)

            else -> error(node.javaClass.name)
        }
    }
}

