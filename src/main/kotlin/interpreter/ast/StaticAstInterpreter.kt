package interpreter.ast

import interpreter.ssa.SsaInterpreter.Companion.knownFunctions
import interpreter.ssa.SsaInterpreter.Companion.visitOp
import memory.Memory
import io.ksmt.KContext
import io.ksmt.sort.KSort
import memory.*
import memory.ast.AstState
import kotlin.collections.plus

open class StaticAstInterpreter(
    functionDeclarations: Map<String, AstFuncDecl>,
    typeDeclarations: List<AstType>
) : AstInterpreter(functionDeclarations, typeDeclarations) {

    override fun startFunction(
        func: AstFuncDecl,
        args: List<Symbolic?>?,
        ctx: KContext,
        initialState: AstState
    ): Pair<Collection<SymbolicResult<Any>>, Map<String, KSort>> {
        typeDeclarations.forEach {
            initialState.waitingNodes.add(it to mutableListOf())
        }
        initialState.waitingNodes.add(StartFunctionNode(func.name) to (args ?: listOf()).toMutableList())

        return interpretLoop(initialState) to initialState.mem.createdConsts
    }

    private fun interpretLoop(state: AstState): Collection<SymbolicResult<Any>> {
        while (state.waitingNodes.isNotEmpty()) {
            val (cond, result, size) = interpretState(state)
//            execution ended
            if (size == 0) return listOf(
                SymbolicReturn<Any>(cond, result, state.getVisitedNodes())
            ) + state.mem.errors as List<SymbolicResult<Any>>
//            force stop
            if (size == -1) return state.mem.errors as List<SymbolicResult<Any>>
        }
        error("should not happen (interpret loop ended in unusual way)")
    }

    open fun statisticsStartVisit(node: AstNode, state: AstState) {
        val oldInfo = state.executionStatistics.startVisit(node)
        if (!oldInfo.visitStarted) {
            state.newCodeTime = state.time
        }
    }

    open fun statisticsEndVisit(node: AstNode, state: AstState) {
        state.executionStatistics.endVisit(node)
    }

    open fun startDynamicNodeInterpretation(
        state: AstState,
        node: AstNode,
        args: MutableList<Symbolic?>
    ): Boolean {
        // does nothing in static
        return false
    }

    protected fun startNodeInterpretation(
        state: AstState,
        node: AstNode,
        function: (Memory, List<Symbolic?>) -> List<AstNode>,
        args: MutableList<Symbolic?>
    ) {
        val newResults = mutableListOf<Symbolic?>()
        val childNodes = (function(state.mem, args) + null)
            .map { Pair(it, newResults) }

        state.waitingNodes.addAll(0, childNodes)
        state.startedNodes.add(node)
    }

    protected open fun finishDynamicReturnInterpretation(
        state: AstState,
        node: AstReturn,
        args: MutableList<Symbolic?>
    ): AstNode {
        return node
    }

    fun interpretState(state: AstState): Triple<BoolSymbolic, Symbolic?, Int> {
        val waitingNodes = state.waitingNodes
        val startedNodes = state.startedNodes
        val mem = state.mem
        state.time++

        val pathCond = mem.fullPathCond()

        val (childNode, childrenResults) = waitingNodes.removeFirst()

        if (childNode == null) {
            // end of started node
            var startedNode = startedNodes.removeLast()
            statisticsEndVisit(startedNode, state)

            if (startedNode is AstReturn) {
                startedNode = finishDynamicReturnInterpretation(
                    state,
                    startedNode,
                    childrenResults
                )
            }
            val result = translator(startedNode).second(mem, childrenResults)
                ?.let { StarSymbolic.removeFake(it, mem) }

            if (waitingNodes.isEmpty()) {
                return Triple(pathCond, result, startedNodes.size)
            }

            val (nextWaiting, nextResults) = waitingNodes.first()
            nextResults.add(result)
        } else if (childNode is StopNode) {
            return Triple(pathCond, null, -1)
        } else {
            statisticsStartVisit(childNode, state)

            val wasDoneDynamically = startDynamicNodeInterpretation(state, childNode, childrenResults)
            if (!wasDoneDynamically) {
                startNodeInterpretation(state, childNode, translator(childNode).first, childrenResults)
            }
        }
        return Triple(pathCond, null, waitingNodes.size + startedNodes.size)
    }

    /**
     * two functions for each AstNodes
     *
     * first creates new list of nodes to be executed in order to finish this node
     *
     * second gets the result of those nodes and does something with it
     */
    protected fun translator(node: AstNode): Pair<(Memory, List<Symbolic?>) -> List<AstNode>, (Memory, List<Symbolic?>) -> Symbolic?> =
        when (node) {
            is AstAssignStmt -> Pair(
                { _, _ ->
                    node.rhs
                }, { mem, rhsResults ->
                    visitAssignStmt(node.lhs, node.token, rhsResults, TODO())
                    null
                }
            )

            is AstBasicLit -> Pair(
                { _, _ ->
                    listOf()
                }, { mem, _ ->
                    visitBasicLit(node, mem)
                }
            )

            is AstBinaryExpr -> Pair(
                { _, _ ->
                    listOf(node.x, node.y)
                }, { mem, args ->
                    visitOp(node.op, args.requireNoNulls(), TODO())
                }
            )

            is AstBlockStmt -> Pair(
                { _, _ ->
                    node.statements ?: listOf()
                }, { _, args ->
//                todo does it return something?
//                    args.last()
                    null
                }
            )

            is AstCallExpr -> Pair(
                { _, _ ->
                    val name = (node.call as AstIdent).name
                    if (knownFunctions.containsKey(name)) {
                        node.args
                    } else {
                        node.args + StartFunctionNode(name)
                    }
                }, { mem, args ->
                    val name = (node.call as AstIdent).name
                    if (knownFunctions.containsKey(name)) {
                        knownFunctions[name]!!(args.requireNoNulls(), mem)
                    } else {
                        args.last()
                    }
                }
            )

            is StartFunctionNode -> Pair(
                { mem, args ->
                    val func = functionDeclarations[node.functionName]!!

                    mem.enterFunction()
                    mem.addArgsSymbolic(
                        (func.params ?: listOf()).map(AstField::toPairs).flatten().toMap(),
                        args
                    )

                    listOf(func.body)
                }, { mem, _ ->
                    mem.exitFunction().combineToSymbolic(mem)
                }
            )

            is AstIdent -> Pair(
                { _, _ ->
                    listOf()
                },
                { mem, _ ->
                    mem.readValue(node.name)
                }
            )

            is AstFor -> Pair(
                { mem, _ ->
                    mem.addVisibilityLevel()
                    listOf(
                        node.init,
                        AstIf(
                            node.cond,
                            ContinueForNode(1, node.body, node.post, node.cond),
                            null
                        )
                    )
                },
                { mem, _ ->
                    mem.removeVisibilityLevel()
                    null
                }
            )

            is ContinueForNode -> Pair(
                { _, _ ->
                    if (node.counter > STATIC_FOR_MAX_LENGTH) {
                        listOf()
                    } else {
                        listOf(
                            // todo break or continue
                            node.body,
                            node.post,
                            AstIf(
                                node.cond,
                                ContinueForNode(node.counter + 1, node.body, node.post, node.cond),
                                null
                            )
                        )
                    }
                },
                { _, _ ->
                    null
                }
            )

            is AstFuncDecl -> TODO("declaration inside another declaration")

            is AstGenDecl -> Pair(
                { _, _ -> listOf() },
                { mem, _ ->
                    visitGenDeclaration(node.specs, mem)
                    null
                }
            )

            is AstIf -> Pair(
                { _, _ ->
                    listOf(
                        node.cond,
                        BranchControlNode(node.body, node.elseBody)
                    )
                },
                { _, _ -> null }
            )

            is BranchControlNode -> Pair(
                { mem, args ->
                    val cond = args[0]!!.bool(mem)
                    val elseList = node.elseBody?.let {
                        listOf(
                            StartBranchNode(false, node.elseBody, cond, null),
                            EndBranchNode(true)
                        )
                    } ?: listOf()
                    listOf(
                        StartBranchNode(true, node.body, cond, null),
                        EndBranchNode(true)
                    ) + elseList
                },
                { _, _ -> null }
            )

            is StartBranchNode -> Pair(
                { mem, _ ->
                    mem.addVisibilityLevel()
                    listOfNotNull(
                        if (node.branch) {
                            if (mem.addCond(node.cond, true)) {
                                node.body
                            } else {
                                node.stopOrContinue
                            }
                        } else {
                            if (mem.addCond(node.cond.not(mem), true)) {
                                node.body
                            } else {
                                node.stopOrContinue
                            }
                        }
                    )
                },
                { _, _ -> null }
            )

            is EndBranchNode -> Pair(
                { _, _ -> listOf() },
                { mem, _ ->
                    if (node.removesCond)
                        mem.removeCond()
                    mem.removeVisibilityLevel()
                    null
                }
            )

            is StopNode -> Pair(
                { _, _ ->
                    error("stop")
                },
                { _, _ -> null }
            )

            is AstIncDec -> Pair(
                { _, _ -> listOf() },
                { mem, _ ->
                    visitIncDec(node.x, node.tok, mem)
                    null
                }
            )

            is AstIndexExpr -> Pair(
                { _, _ -> listOf(node.x, node.index) },
                { mem, args ->
                    val x = args[0]!!.array(mem)
                    val index = args[1]!!.int64(mem)
                    x.get(index, mem)
                }
            )

            is AstReturn -> Pair(
                { _, _ -> node.result },
                { mem, args ->
                    mem.addResults(args.requireNoNulls())
                    mem.addCond(mem.fullPathCond().not(mem), false)
                    null
                }
            )

            is AstSelector -> Pair(
                { _, _ -> listOf(node.x) },
                { mem, args ->
                    visitSelector(node.selectorName, args[0], mem)
                }
            )

            is AstType -> Pair(
                { _, _ -> listOf() },
                { mem, _ ->
                    visitType(node, mem)
                }
            )

            is AstArrayType -> Pair(
                { _, _ -> listOfNotNull(node.elementType, node.length) },
                { mem, args ->
                    val elementType = args[0]!!.type as StarType
                    val length = args[1] as Int64Symbolic
                    Symbolic(ArrayType(elementType, length))
                }
            )

            is AstUnaryExpr -> Pair(
                { _, _ ->
                    listOf(node.x)
                },
                { mem, args ->
                    visitOp(node.op, args.requireNoNulls(), TODO())
                }
            )

//            is AstRange -> Pair(
//                { _, _ ->
//                    listOf(node.x)
//                },
//                { _, _ ->
//                    null
//                }
//            )

            is AstUnsupported -> error(node.unsupported)

            else -> error(node.javaClass.name)
        }
}