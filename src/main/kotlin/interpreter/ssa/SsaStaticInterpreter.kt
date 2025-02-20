package interpreter.ssa

import interpreter.UnSatPathException
import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.sort.KBv64Sort
import io.ksmt.sort.KSort
import kotlinx.coroutines.yield
import memory.*
import memory.ssa.SsaExecutionStatistics
import memory.ssa.SsaState

open class SsaStaticInterpreter() : SsaInterpreter() {
    open val isDynamic = false

    open fun updateDistancesToTerminal(vararg removedNodes: SsaNode) {
        // static does nothing
    }

    open fun isNodeUseful(node: SsaNode, mem: Memory): Boolean {
        // static does nothing
        return true
    }

    override suspend fun startFunction(
        func: FuncSsaNode,
        args: List<Symbolic?>?,
        ctx: KContext,
        initialState: SsaState
    ): Pair<List<SymbolicResult<SsaNode>>, Map<String, KSort>> {
        prepareState(func, args, initialState)

        return interpretLoop(initialState).toList() to initialState.mem.createdConsts
    }

    private suspend fun interpretLoop(state: SsaState): Collection<SymbolicResult<SsaNode>> {
        while (state.waitingNodes.isNotEmpty()) {
            yield()
            val (cond, result, size) = interpretStep(state)
//            execution ended
            if (size == 0) return listOf(
                SymbolicReturn(cond, result, state.getVisitedNodes())
            ) + state.mem.errors
//            force stop
            if (size == -1) return state.mem.errors
        }
        error("should not happen (interpret loop ended in unusual way)")
    }

    override fun statisticsStartVisit(node: SsaNode, state: SsaState): Boolean {
        state.time++
        return if (node.id > 0) {
            state.mem.addInstrToPath(node)
            val oldInfo = state.executionStatistics.startVisit(node)
            if (!oldInfo.visitStarted) {
                state.newCodeTime = state.time
            }
            state.visitedNodes += node
            true
        } else
            false
    }

    override fun statisticsEndVisit(node: SsaNode, state: SsaState): Boolean {
        return if (node.id > 0) {
            state.executionStatistics.endVisit(node)
            true
        } else
            false
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

    protected open fun finishDynamicPanicInterpretation(
        state: SsaState,
        node: PanicSsaNode
    ): SsaNode {
        // does nothing in static
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
            if (print)
                println("${state.stateId}\t stop_")
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
            } else if (startedNode is PanicSsaNode) {
                startedNode = finishDynamicPanicInterpretation(
                    state,
                    startedNode
                )
            }
            if (print && startedNode !is LinkToSsaNode)
                println("${state.stateId}\t\t${startedNode.printItself()}")

            val result = try {
                translator(startedNode, state.executionStatistics).second(mem, childrenResults)
                    ?.let { StarSymbolic.removeFake(it, mem) }
            } catch (_: UnSatPathException) {
                StopSymbolic
            }
            if (print && startedNode !is LinkToSsaNode)
                println("${state.stateId}\t \t\t\t\t${result?.type}")

            if (result == StopSymbolic) {
                if (print)
                    println("${state.stateId}\t stop^")
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
            if (print && childNode !is LinkToSsaNode)
                println("${state.stateId}\t\t____${childNode.printItself()}")

            statisticsStartVisit(childNode, state)

            try {
                val wasDoneDynamically = startDynamicNodeInterpretation(state, childNode, childrenResults)
                if (!wasDoneDynamically) {
                    startNodeInterpretation(
                        state,
                        childNode,
                        translator(childNode, state.executionStatistics).first,
                        childrenResults
                    )
                }
            } catch (_: UnSatPathException) {
                if (print)
                    println("${state.stateId}\t stop*")
                return Triple(pathCond, null, -1)
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
                { mem, _ ->
                    if (isNodeUseful(node, mem)) {
                        node.instr?.map {
                            when (it) {
                                is PhiSsaNode -> listOf(SsaForcePhiNode(it))
                                is LinkToSsaNode -> when (val phi = it.deLink()) {
                                    is PhiSsaNode -> listOf(SsaForcePhiNode(phi), it)
                                    else -> listOf(it)
                                }

                                else -> listOf(it)
                            }
                        }?.flatten() ?: listOf()
                    } else {
                        listOf(PanicSsaNode("path stopped by interpreter"))
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

            is InvokeSsaNode -> TODO("is not used in tests")

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

            is FuncSsaNode -> TODO("function inside function declaration")

            is IfSsaNode -> Pair(
                { mem, _ ->
                    if (!isNodeUseful(node, mem))
                        return@Pair listOf(PanicSsaNode("path stopped by interpreter"))

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
                        SsaStartBranchNode(true, node.body, cond, null, true),
                        SsaEndBranchNode(true),
                        SsaStartBranchNode(false, node.elseBody, cond, null, true),
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
                            if (mem.addCond(node.cond, node.needToPush)) {
                                node.body
                            } else {
                                node.stopOrContinue
                            }
                        } else {
                            if (mem.addCond(node.cond.not(mem), node.needToPush)) {
                                node.body
                            } else {
                                node.stopOrContinue
                            }
                        }
                    )
                },
                { _, _ ->
                    if (isDynamic)
                        error("should not happen (going back in control graph)")
                    null
                }
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
                    error("should not happen (stop node started)")
                },
                { _, _ ->
                    StopSymbolic
                }
            )

            is JumpSsaNode -> Pair(
                { mem, _ ->
                    if (!isNodeUseful(node, mem))
                        return@Pair listOf(PanicSsaNode("path stopped by interpreter"))

                    listOf(node.successor)
                }, { _, _ ->
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
                { mem, _ ->
                    if (!isNodeUseful(node, mem))
                        return@Pair listOf(PanicSsaNode("path stopped by interpreter"))
                    node.results
                },
                { mem, args ->
                    mem.addResults(args.requireNoNulls())

                    mem.addCond(mem.fullPathCond().not(mem), false)
                    null
                }
            )

            is StoreSsaNode -> Pair(
                { _, _ -> listOf(node.addr, node.value) },
                { mem, args ->
                    val address = args[0]!! as StarSymbolic
                    val value = args[1]!!

                    address.put(value, mem)
                    null
                }
            )

            is AllocSsaNode -> Pair(
                { _, _ -> listOf() },
                { mem, _ ->
                    val type = Type.fromSsa(node.valueType!!, mem) as StarType

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

                    when (val type = Type.fromSsa(node.valueType!!, mem)) {
//                        is FunctionType -> Symbolic(FunctionType)

                        is BoolType -> BoolType.fromBool(value.toBoolean(), mem)

                        is Int8Type -> Int8Type().fromInt(value.toLong(), mem)
                        is Int16Type -> Int16Type().fromInt(value.toLong(), mem)
                        is Int32Type -> Int32Type().fromInt(value.toLong(), mem)
                        is Int64Type -> Int64Type().fromInt(value.toLong(), mem)

                        is Float32Type -> Float32Type().fromDouble(value.toDouble(), mem)
                        is Float64Type -> Float64Type().fromDouble(value.toDouble(), mem)

                        is ArrayType if (value == "nil") ->
                            NilLocalStarSymbolic(StarType(type, false))

                        is StarType  if (value == "nil") ->
                            NilLocalStarSymbolic(StarType(type, false))

                        is UninterpretedType -> UninterpretedType.fromString(value, mem)

                        UnknownType -> UninterpretedType.fromString("error", mem)
                        else -> error("const of $type")
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
                    val toType = Type.fromSsa(node.valueType!!, mem)

                    if (fromType == toType)
                        value
                    else when {
                        fromType is IntType && toType is Float64Type ->
                            Float64Type().round(value.int(mem), mem)

                        fromType is IntType && toType is Float32Type ->
                            Float32Type().round(value.int(mem), mem)

                        /**
                         * bv2int works in ksmt, while int2bv is not supported
                         *
                         * but it is possible to do that with some bit magic
                         */
                        fromType is IntType && toType is IntType ->
                            IntType.cast(value, toType, mem)

                        fromType is FloatType && toType is IntType ->
                            with(mem.ctx) {
                                IntType.cast(
                                    Int64Symbolic(
                                        mkFpToBvExpr(
                                            Float64Type().roundingMode(mem),
                                            value.floatExpr(mem),
                                            64, true
                                        ) as KExpr<KBv64Sort>
                                    ), toType, mem
                                )
                            }

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
                    val address = IntType.cast(args[1]!!, Int64Type(), mem).int64(mem)

                    ArrayStarSymbolic(
                        address, when (val x = StarSymbolic.removeFake(args[0]!!, mem)) {
                            is FiniteArraySymbolic -> x
                            is StarSymbolic -> (x.dereference(mem) as FiniteArraySymbolic)
                            else -> error("only [] or *[], not ${x.javaClass.name}")
                        },
                        false
                    )
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
                    NilLocalStarSymbolic(StarType(UninterpretedType("$i"), false))
                }
            )

            is MakeSliceSsaNode -> Pair(
                { _, _ -> listOf(node.len) },
                { mem, args ->
                    val len = args[0]!!
                    val type = Type.fromSsa(node.valueType!!, mem) as ArrayType
                    val array = StarSymbolic.removeFake(visitAlloc(type, mem), mem)

                    visitSlice(
                        len,
                        if (array is StarSymbolic)
                            array.dereference(mem)
                        else
                            array,
                        mem
                    )
                }
            )

            is ParamSsaNode -> Pair(
                { _, _ -> listOf() },
                { mem, _ -> mem.readValue(node) }
            )

            is SsaForcePhiNode -> Pair(
                { mem, _ ->
                    var chosenBlock: SsaNode? = null
                    for (block in mem.instrOnPath()) {
                        chosenBlock = node.phi.edgesMap()[block]
                        if (chosenBlock != null)
                            break
                    }
                    if (chosenBlock == null) {
                        error("unreachable phi")
                    }

                    listOf(chosenBlock)
                },
                { mem, args ->
                    args.last()
                }
            )

            is PhiSsaNode -> Pair(
                { mem, _ ->
                    if (!isNodeUseful(node, mem))
                        return@Pair listOf(PanicSsaNode("path stopped by interpreter"))

                    listOf(
                        SsaKeepResult(
                            mem.readValue(node)
                                ?: error("phi wasn't done before read")
                        )
                    )
                },
                { mem, args ->
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

                    visitSlice(
                        high,
                        if (x is StarSymbolic)
                            x.star(mem).dereference(mem)
                        else
                            x,
                        mem
                    )
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