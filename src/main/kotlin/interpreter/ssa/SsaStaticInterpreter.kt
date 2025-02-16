package interpreter.ssa

import io.ksmt.KContext
import io.ksmt.sort.KSort
import kotlinx.coroutines.yield
import memory.*
import memory.ssa.SsaExecutionStatistics
import memory.ssa.SsaState

open class SsaStaticInterpreter(
    functionDeclarations: Map<String, FuncSsaNode>
) : SsaInterpreter() {

    open fun updateDistancesToTerminal(vararg removedNodes: SsaNode) {
        // static does nothing
    }

    open fun isNodeUseful(node: BlockSsaNode): Boolean {
        // static does nothing
        return true
    }

    override suspend fun startFunction(
        func: FuncSsaNode,
        args: List<Symbolic?>?,
        ctx: KContext,
        initialState: SsaState
    ): Pair<Collection<SymbolicResult>, Map<String, KSort>> {
        prepareState(func, args, initialState)

        return interpretLoop(initialState) to initialState.mem.createdConsts
    }

    private suspend fun interpretLoop(state: SsaState): Collection<SymbolicResult> {
        while (state.waitingNodes.isNotEmpty()) {
            yield()
            val (cond, result, size) = interpretStep(state)
//            execution ended
            if (size == 0) return listOf(
                SymbolicReturn(cond, result)
            ) + state.mem.errors
//            force stop
            if (size == -1) return state.mem.errors
        }
        error("should not happen")
    }

    override fun statisticsStartVisit(node: SsaNode, state: SsaState) {
        state.time++
        state.mem.addInstrToPath(node)
        val oldInfo = state.executionStatistics.startVisit(node)
        if (!oldInfo.visitStarted) {
            state.newCodeTime = state.time
        }
    }

    override fun statisticsEndVisit(node: SsaNode, state: SsaState) {
        state.executionStatistics.endVisit(node)
    }

    protected open fun startDynamicNodeInterpretation(
        state: SsaState,
        node: SsaNode,
        args: MutableList<Symbolic?>
    ): Boolean {
        // does nothing in static
        return false
    }

    protected open fun finishDynamicReturnInterpretation(
        state: SsaState,
        node: ReturnSsaNode,
        args: MutableList<Symbolic?>
    ): SsaNode {
        return node
    }

    protected fun startNodeInterpretation(
        state: SsaState,
        node: SsaNode,
        function: (Memory, List<Symbolic?>) -> List<SsaNode>,
        args: MutableList<Symbolic?>
    ) {
        val newResults = mutableListOf<Symbolic?>()
        val childNodes = (function(state.mem, args) + null)
            .map { Pair(it, newResults) }
        state.waitingNodes.addAll(0, childNodes)
        state.startedNodes.add(node)
    }

    fun interpretStep(state: SsaState): Triple<BoolSymbolic, Symbolic?, Int> {
        val waitingNodes = state.waitingNodes
        val startedNodes = state.startedNodes
        val mem = state.mem

        val pathCond = mem.fullPathCond()

        val (childNode, childrenResults) = waitingNodes.removeFirst()
        if (childNode is SsaStopNode ||
            childrenResults.filterNotNull().any { it is StopSymbolic }
        ) {
            if (print) println("stop")
            return Triple(pathCond, null, -1)
        } else if (childNode == null) {
            // end of started node
            var startedNode = startedNodes.removeLast()
            statisticsEndVisit(startedNode, state)

            if (startedNode is ReturnSsaNode) {
                startedNode = finishDynamicReturnInterpretation(
                    state,
                    startedNode,
                    childrenResults
                )
            }
            if (print && startedNode !is LinkToSsaNode)
                println("finishing \t${startedNode.printItself()}")

            val result = translator(startedNode, state.executionStatistics).second(mem, childrenResults)
                ?.getDereferenced(mem)

            if (print && startedNode !is LinkToSsaNode)
                println("\t\t\t\t${result?.type}")

            if (result == StopSymbolic) {
                if (print)
                    println("stop")
                return Triple(pathCond, null, -1)
            }

            if (result != null && startedNode.id != 0) {
                if (mem.readValue(startedNode) != result) {
                    mem.writeValue(startedNode, result)
                }
            }

            if (waitingNodes.isEmpty()) {
                return Triple(pathCond, result, startedNodes.size)
            }

            val (nextWaiting, nextResults) = waitingNodes.first()
            nextResults.add(result)
        } else {
            statisticsStartVisit(childNode, state)

            val wasDoneDynamically = startDynamicNodeInterpretation(state, childNode, childrenResults)
            if (!wasDoneDynamically) {
                startNodeInterpretation(
                    state,
                    childNode,
                    translator(childNode, state.executionStatistics).first,
                    childrenResults
                )
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
    protected fun translator(
        node: SsaNode,
        executionStatistics: SsaExecutionStatistics
    ): Pair<(Memory, List<Symbolic?>) -> List<SsaNode>, (Memory, List<Symbolic?>) -> Symbolic?> =
        when (node) {
            is UnOpSsaNode -> Pair(
                { _, _ ->
                    listOf(node.x)
                }, { mem, args ->
                    visitOp(node.op, args.requireNoNulls(), mem)
                }
            )

            is BinOpSsaNode -> Pair(
                { _, _ ->
                    listOf(node.x, node.y)
                }, { mem, args ->
                    visitOp(node.op, args.requireNoNulls(), mem)
                }
            )

            is BlockSsaNode -> Pair(
                { _, _ ->
                    if (isNodeUseful(node)) {
                        node.instr ?: listOf()
                    } else {
                        listOf(SsaStopNode())
                    }
                }, { _, _ ->
                    null
                }
            )

            is CallSsaNode -> Pair(
                { _, _ ->
                    when (node.value) {
                        is BuiltInSsaNode -> listOf(*node.args.toTypedArray(), node.value)
                        is LinkSsaNode -> listOf(
                            *node.args.toTypedArray(),
                            SsaStartFunctionNode(node.value.deLink() as FuncSsaNode)
                        )

                        else -> listOf(*node.args.toTypedArray(), SsaStartFunctionNode(node.value as FuncSsaNode))
                    }
                }, { _, args ->
                    when (val result = args.last()) {
                        is ListSymbolic -> when (result.list.size) {
                            0 -> null
                            1 -> result.list[0]
                            else -> result
                        }

                        else -> result
                    }
                }
            )

            is InvokeSsaNode -> TODO()

            is SsaStartFunctionNode -> Pair(
                { mem, args ->
                    if (node.functionNode.body == null) {
                        // external function
                        val funcName = "external:${node.functionNode.name}"
                        if (knownFunctions.containsKey(funcName)) {
                            listOf(SsaKeepResult(knownFunctions[funcName]!!(args.requireNoNulls(), mem)))
                        } else {
                            error("function is not implemented $funcName")
                        }
                    } else {
                        val deLinkedParams = node.functionNode.params.map {
                            when (it) {
                                is ParamSsaNode -> it
                                is LinkSsaNode -> (it.deLink() as ParamSsaNode)
                                else -> error("should be param or link to param only")
                            }
                        }

                        executionStatistics.pushToStack()
                        mem.enterFunction()

                        deLinkedParams.zip(args.requireNoNulls()).forEach { (param, paramValue) ->
                            mem.writeValue(param, paramValue)
                        }

                        listOf(node.functionNode.body)
                    }
                }, { mem, args ->
                    if (node.functionNode.body == null)
                    // external function
                        args[0]
                    else {
                        executionStatistics.popFromStack()
                        mem.exitFunction().combineToSymbolic(mem)
                    }
                }
            )

            is FuncSsaNode -> TODO()

            is IfSsaNode -> Pair(
                { mem, _ ->
                    listOf(
                        node.cond,
                        SsaBranchControlNode(node.body, node.elseBody)
                    )
                }, { _, _ -> null }
            )

            is SsaBranchControlNode -> Pair(
                { mem, args ->
                    val cond = args[0]!!.bool(mem)
                    listOf(
                        SsaStartBranchNode(true, node.body, cond, null),
                        SsaEndBranchNode(true),
                        SsaStartBranchNode(false, node.elseBody, cond, null),
                        SsaEndBranchNode(true)
                    )
                },
                { _, _ -> null }
            )

            is SsaStartBranchNode -> Pair(
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

            is SsaEndBranchNode -> Pair(
                { _, _ -> listOf() },
                { mem, _ ->
                    if (node.removesCond)
                        mem.removeCond()
                    mem.removeVisibilityLevel()
                    null
                }
            )

            is SsaStopNode -> Pair(
                { _, _ ->
                    error("stop")
                },
                { _, _ -> null }
            )

            is JumpSsaNode -> Pair(
                { _, _ ->
                    listOf(node.successor)
                }, { _, _ ->
//                    todo args.last()?
                    null
                }
            )

            is PanicSsaNode -> Pair(
                { mem, _ ->
                    mem.addError(
                        (BoolType.`true`(mem)),
                        node.x
                    )
                    listOf()
                }, { _, _ ->
                    updateDistancesToTerminal(node)
                    null
                }
            )

            is ReturnSsaNode -> Pair(
                { _, _ -> node.results },
                { mem, args ->
                    mem.addResults(args.requireNoNulls())
                    mem.addCond(mem.fullPathCond().not(mem), false)
                    null
                }
            )

            is StoreSsaNode -> Pair(
                { _, _ -> listOf(node.addr, node.value) },
                { mem, args ->
                    when (val address = args[0]!!) {
                        is StarSymbolic -> {
                            val value = args[1]!!
                            node.value.parentF
                            if (address.put(value, mem)) {
                                null
                            } else {
                                StopSymbolic
                            }
                        }

                        else -> error("store allows only to use *")
                    }

                }
            )

            is AllocSsaNode -> Pair(
                { _, _ -> listOf() },
                { mem, _ ->
                    // todo true!
                    val type = Type.fromSsa(node.valueType!!, mem, false) as StarType
                    visitAlloc(type.elementType, mem)
                }
            )

            is BuiltInSsaNode -> Pair(
                { mem, args ->
                    val funcName = node.name
                    if (knownFunctions.containsKey(funcName)) {
                        listOf(SsaKeepResult(knownFunctions[funcName]!!(args.requireNoNulls(), mem)))
                    } else {
                        error("function is not implemented $funcName")
                    }
                },
                { _, args ->
                    args.last()
                }
            )

            is SsaKeepResult -> Pair(
                { _, _ -> listOf() },
                { _, _ -> node.result }
            )

            is ConstSsaNode -> Pair(
                { _, _ -> listOf() },
                { mem, _ ->
                    val nameParts = node.name.split(":")
                    val value = nameParts[0]

                    when (val type = Type.fromSsa(node.valueType!!, mem, false)) {
//                        is FunctionType -> Symbolic(FunctionType)

                        is BoolType -> BoolType.fromBool(value.toBoolean(), mem)

                        is Int8Type -> Int8Type().fromInt(value.toLong(), mem)
                        is Int16Type -> Int16Type().fromInt(value.toLong(), mem)
                        is Int32Type -> Int32Type().fromInt(value.toLong(), mem)
                        is Int64Type -> Int64Type().fromInt(value.toLong(), mem)

                        is Float32Type -> Float32Type().fromDouble(value.toDouble(), mem)
                        is Float64Type -> Float64Type().fromDouble(value.toDouble(), mem)

                        is ArrayType if (value == "nil") ->
                            NilLocalStarSymbolic(type, true)

                        is StarType  if (value == "nil") ->
                            NilLocalStarSymbolic(type, true)

                        is UninterpretedType -> UninterpretedType.fromString(value, mem)

                        UnknownType -> UninterpretedType.fromString("error", mem)
                        else -> TODO("$type")
                    }
                }
            )

            is ConvertSsaNode -> Pair(
                { _, _ ->
                    listOf(node.x)
                },
                { mem, args ->
                    val value = args[0]!!
                    val fromType = value.type
                    val toType = Type.fromSsa(node.valueType!!, mem, false)

                    if (fromType == toType)
                        value
                    else when {
                        fromType is IntType && toType is Float64Type ->
                            Float64Type().round(value.int(mem), mem)

                        fromType is IntType && toType is Float32Type ->
                            Float32Type().round(value.int(mem), mem)

                        // bv2int works in ksmt, while int2bv is not supported
                        // it is possible to do that with some bit magic
//                        fromType is IntType && toType is Int64Type ->
                        else -> TODO("conversion $fromType -> $toType")
                    }
                }
            )

            is ExtractSsaNode -> Pair(
                { _, args ->
                    listOf(SsaKeepResult(ListSymbolic(args.requireNoNulls())))
                },
                { mem, args ->
                    (args[0]!!.list(mem)).list[node.index]
                }
            )

            is FieldAddrSsaNode -> Pair(
                { _, _ ->
                    listOf(node.x)
                },
                { mem, args ->
                    val x = args[0]!!.star(mem)

                    x.findField(node.field, mem)
                }
            )

            is IndexAddrSsaNode -> Pair(
                { _, _ ->
                    listOf(node.x, node.index)
                },
                { mem, args ->
                    val address = args[1]!!.int64(mem)
                    when (val x = args[0]!!.getDereferenced(mem)) {
                        is FiniteArraySymbolic -> ArrayStarSymbolic(address, x, false)
                        is StarSymbolic -> ArrayStarSymbolic(address, x.dereference().get(mem).array(mem), false)
                        else -> error("only [] or *[]")
                    }
                }
            )

            /**
             * that's just a stub for tests to work,
             * interfaces are not supported
             */
            is MakeInterfaceSsaNode -> Pair(
                { _, _ ->
                    listOf(node.x)
                },
                { mem, args ->
                    val i = args.last()
                    LocalStarSymbolic(UninterpretedType.fromString("$i", mem), false)
                }
            )

            is MakeSliceSsaNode -> Pair(
                { _, _ -> listOf(node.len) },
                { mem, args ->
                    val len = args[0]!!

                    val type = Type.fromSsa(node.valueType!!, mem, true) as ArrayType
                    val array = visitAlloc(type, mem)

                    visitSlice(len, array, mem)
                }
            )

            is ParamSsaNode -> Pair(
                { _, _ -> listOf() },
                { mem, _ -> mem.readValue(node) }
            )

            is PhiSsaNode -> Pair(
                { mem, _ ->
                    var chosenBlock: SsaNode? = null
                    for (block in mem.instrOnPath()) {
                        chosenBlock = node.edgesMap()[block]
                        if (chosenBlock != null)
                            break
                    }
                    if (chosenBlock == null) {
                        error("unreachable phi")
                    }
                    listOf(chosenBlock)
                },
                { _, args ->
                    args.last()
                }
            )

            is SliceSsaNode -> Pair(
                { _, _ ->
                    listOfNotNull(node.x, node.high)
                },
                { mem, args ->
                    val x = args[0]!!
                    val high = args.getOrNull(1)

                    visitSlice(high, x, mem)
                }
            )

            is LinkSsaNode -> Pair(
                { mem, _ ->
                    if (mem.hasValue(node))
                        listOf()
                    else
                        listOf(node.deLink())
                },
                { mem, args ->
                    if (mem.hasValue(node)) {
                        mem.readValue(node)
                    } else {
                        args.last()
                    }
                }
            )

            is LinkParamSsaNode -> Pair(
                { _, _ -> listOf() },
                { mem, _ -> mem.readValue(node) }
            )

            is LinkBlockSsaNode -> Pair(
                { _, _ -> listOf(node.deLink()) },
                { _, _ -> null }
            )

            is UnknownSsaNode -> Pair(
                { _, _ -> listOf() },
                { _, _ -> null }
            )

            is GlobalSsaNode -> TODO()
        }
}