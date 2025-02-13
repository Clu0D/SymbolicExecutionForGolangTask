package interpreter.ssa

import interpreter.Interpreter.Companion.knownFunctions
import interpreter.Interpreter.Companion.visitOp
import io.ksmt.KContext
import io.ksmt.sort.KSort
import memory.*
import memory.ssa.SsaState

class StaticSsaInterpreter(
    functionDeclarations: Map<String, FuncSsaNode>
) : SsaInterpreter(functionDeclarations) {

    private fun createDefaultParam(value: SsaNode, mem: Memory): Symbolic {
        return when (value) {
            is LinkSsaNode -> createDefaultParam(value.deLink(), mem)
            is ParamSsaNode -> {
                val name = value.name
                val type = Type.fromSsa(value.valueType!!, mem, true)
                println("createDefault $name $type")

                return type.defaultSymbolic(mem, false)
            }

            else -> error("should be link or param")
        }
    }

    override fun startFunction(
        func: FuncSsaNode,
        args: List<Symbolic?>?,
        ctx: KContext,
        initialState: SsaState
    ): Pair<Collection<SymbolicResult>, Map<String, KSort>> {

        println("FUNCTION!@#@!\t\t${func.name}")

        val initializedArgs = func.params.mapIndexed { i, node ->
            statisticsStartVisit(node, initialState)
            (args?.getOrNull(i) ?: createDefaultParam(node, initialState.mem)).also {
                statisticsEndVisit(node, initialState)
            }
        }.toMutableList<Symbolic?>()

        initialState.waitingNodes.add(
            SsaStartFunctionNode(func) to initializedArgs
        )

        return interpretLoop(initialState) to initialState.mem.createdConsts
    }

    private fun interpretLoop(state: SsaState): Collection<SymbolicResult> {
        while (state.waitingNodes.size > 0) {
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

    protected open fun statisticsStartVisit(node: SsaNode, state: SsaState) {
        state.time++
        state.mem.addInstrToPath(node)
        val oldInfo = state.executionStatistics.startVisit(node)
        if (!oldInfo.visitStarted) {
            state.newCodeTime = state.time
        }
    }

    protected open fun statisticsEndVisit(node: SsaNode, state: SsaState) {
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

        println("\t+\t ${childNodes.dropLast(1).map { it.first?.printItself() }}")
        state.waitingNodes.addAll(0, childNodes)
        state.startedNodes.add(node)
    }

    fun interpretStep(state: SsaState): Triple<BoolSymbolic, Symbolic?, Int> {
        val waitingNodes = state.waitingNodes
        val startedNodes = state.startedNodes
        val mem = state.mem

        val pathCond = mem.fullPathCond()

        val (childNode, childrenResults) = waitingNodes.removeFirst()

        if (childNode == null) {
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
            println("finishing \t${startedNode.printItself()}")

            val result = translator(startedNode).second(mem, childrenResults)

            print("\t\t${result} \t\t\t:${result?.type}")

            if (result != null && startedNode.id != 0) {
                if (mem.readValue(startedNode) != result) {
                    mem.writeValue(startedNode, result)
                }
            }

            println()

            if (waitingNodes.isEmpty()) {
                return Triple(pathCond, result, startedNodes.size)
            }

            val (nextWaiting, nextResults) = waitingNodes.first()
            nextResults.add(result)
        } else if (childNode is SsaStopNode) {
            println("stop")
            return Triple(pathCond, null, -1)
        } else {
            println("starting \t${childNode.printItself()}")

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
    protected fun translator(node: SsaNode): Pair<(Memory, List<Symbolic?>) -> List<SsaNode>, (Memory, List<Symbolic?>) -> Symbolic?> =
        when (node) {
            is UnOpSsaNode -> Pair(
                { _, _ ->
                    listOf(node.x)
                }, { mem, args ->
                    visitOp(node.op, args, mem)
                }
            )

            is BinOpSsaNode -> Pair(
                { _, _ ->
                    listOf(node.x, node.y)
                }, { mem, args ->
                    visitOp(node.op, args, mem)
                }
            )

            is BlockSsaNode -> Pair(
                { _, _ ->
//                    todo we can use Phi nodes lazily?
                    node.instr?.filter {
                        true
//                        when (it) {
//                            is PhiSsaNode -> false
//                            is LinkSsaNode -> it.deLink() !is PhiSsaNode
//                            else -> true
//                        }
                    } ?: listOf()
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
                            else -> TODO()
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

                        mem.enterFunction()
//                    mem.addArgsSymbolic(
//                        deLinkedParams.map { it.name }.associateWith { null }.toMap(),
//                        args
//                    )
                        deLinkedParams.zip(args.requireNoNulls()).forEach { (param, paramValue) ->
                            println("_id:${param.id} $paramValue")
                            mem.writeValue(param, paramValue)
                        }

                        listOf(node.functionNode.body)
                    }
                }, { mem, args ->
                    if (node.functionNode.body == null)
                    // external function
                        args[0]
                    else
                        mem.exitFunction().combineToSymbolic(mem)
                }
            )

            is FuncSsaNode -> TODO()
//            is FuncSsaNode -> Pair(
//                { _, _ ->
//                    println("FUNC__")
//                    listOf()
//                }, { _, _ ->
//                    Symbolic(FunctionType())
//                }
//            )

            is IfSsaNode -> Pair(
                { _, _ ->
                    listOf(
                        node.cond,
                        SsaBranchControlNode(node.body, node.elseBody)
                    )
                }, { _, _ ->
//                    todo return something?
                    null
                }
            )

            is SsaBranchControlNode -> Pair(
                { _, args ->
                    val cond = args[0]!!.bool()
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
                    println("jump")
                    listOf(node.successor)
                }, { _, _ ->
//                    todo args.last()?
                    null
                }
            )

            is PanicSsaNode -> Pair(
                { mem, _ ->
                    mem.addError(
                        (BoolType.TRUE(mem)),
                        node.x
                    )
                    listOf()
                }, { _, _ ->
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
                    println("args[0] ${args[0]}")
                    println("args[1] ${args[1]}")
                    val address = args[0]!! as StarSymbolic
                    val value = args[1]!!

                    address.put(value, mem)
                    null
                }
            )

            is AllocSsaNode -> Pair(
                { _, _ -> listOf() },
                { mem, _ ->
                    println("alloc ${Type.fromSsa(node.valueType!!, mem, true)}")
                    Type.fromSsa(node.valueType!!, mem, true).createSymbolic(node.name, mem, true)
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
                        is FunctionType -> Symbolic(FunctionType)

                        is BoolType -> BoolType.fromBool(value.toBoolean(), mem)
                        is IntType -> IntType.fromInt(value.toInt(), mem)
                        is FloatType -> FloatType.fromDouble(value.toDouble(), mem)

                        is ArrayType -> if (value == "nil") {
                            type.defaultSymbolic(mem, true)
                        } else {
                            TODO()
                        }
//                        is ArraySimpleType -> if (value == "nil") {
//                            type.defaultSymbolic(mem, true)
//                        } else {
//                            TODO()
//                        }

                        is StructType -> TODO()

                        is ComplexType -> TODO()
                        is NamedType -> TODO()
                        is ListType -> TODO()
                        is StarType -> {
                            if (value != "nil")
                                error("should be nil")
                            NilLocalStarSymbolic(type)
                        }

                        is UninterpretedType -> UninterpretedType.fromString(value, mem)
//                        is SignatureTypeNode ->
//                            Symbolic(FunctionType(), node.name)
//
//                        else -> {
//                            val nameParts = node.name.split(":")
//                            when (nameParts.getOrNull(1)) {
//                                "int" -> IntType.fromInt(nameParts[0].toInt(), mem)
//                                "bool" -> BoolType.fromBool(nameParts[0].toBoolean(), mem)
//                                "float64" -> FloatType.fromDouble(nameParts[0].toDouble(), mem)
//                                "string" -> UninterpretedSymbolic.create(nameParts[0], mem)
//                                null -> TODO()
////                                    mem.readValue(nameParts[0])
//                                else -> TODO(nameParts.getOrNull(1)!!)
//                            }
//                        }
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
                        fromType == IntType() && toType == FloatType() ->
                            value.floatExpr(mem).toSymbolic()

                        else -> TODO("conversion $fromType -> $toType")
                    }
                }
            )

            is ExtractSsaNode -> TODO()

            is FieldAddrSsaNode -> Pair(
                { _, _ ->
                    listOf(node.x)
                },
                { mem, args ->
//                    node.index
                    println("FIELD ${args[0]}")
                    val x = args[0]!!.star()
                    x.findField(node.field)
                }
            )

            is GlobalSsaNode -> TODO()
            is IndexAddrSsaNode -> Pair(
                { _, _ ->
                    listOf(node.x, node.index)
                },
                { mem, args ->
                    val x = args[0]!!.arrayFinite()
                    val index = args[1]!!.int()
                    println("indexAddr ${x.type}")
                    x.get(index, mem)
                }
            )

            is MakeInterfaceSsaNode -> TODO()
            is MakeSliceSsaNode -> TODO()
            is ParamSsaNode -> Pair(
                { _, _ -> listOf() },
                { mem, _ -> mem.readValue(node) }
            )

//            TODO can be done better for dynamic
            is PhiSsaNode -> Pair(
                { mem, _ ->
                    var chosenBlock: SsaNode? = null
                    for (block in mem.instrOnPath()) {
                        chosenBlock = node.edgesMap()[block]
                        if (chosenBlock != null)
                            break
                    }
                    println("ins${mem.instrOnPath().map { it.printItself() }}")
//                    println("del${node.chosenBlock.map { it.printItself() }}")
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

                    when {
                        high == null -> x
                        x is StarSymbolic -> {
                            when (val obj = x.get(mem)) {
                                is FiniteArraySymbolic ->
                                    FiniteArraySymbolic(
                                        high as IntSymbolic,
                                        mem,
                                        obj
                                    )

                                else -> TODO(obj.type.toString())
                            }
                        }

                        else -> TODO(x.type.toString())
                    }
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

            is UnknownSsaNode -> error("unknown node in translator: ${node.printItself()}")
        }
}