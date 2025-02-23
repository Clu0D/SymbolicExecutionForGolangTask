package interpreter.ssa

import com.jetbrains.rd.util.printlnError
import interpreter.InterpreterQueue
import io.ksmt.KContext
import io.ksmt.sort.KSort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.yield
import memory.*
import memory.ssa.SsaExecutionStatistics
import memory.ssa.SsaState

class SsaDynamicInterpreter(
    val queue: InterpreterQueue<SsaState>,
    val terminalNodes: MutableSet<SsaNode>,
    val distToTerminal: Map<SsaNode, MutableSet<SsaNode>>
) : SsaStaticInterpreter() {
    override val isDynamic = true

    override fun updateDistancesToTerminal(vararg removedNodes: SsaNode) {
        if (removedNodes.any { terminalNodes.contains(it) }) {
            if (print)
                println("terminal nodes found: ${removedNodes.toList().map { it.printItself() }}")
            terminalNodes.removeAll(removedNodes)
            distToTerminal.forEach { (_, map) ->
                map -= removedNodes
            }
            if (print)
                println("remaining terminal nodes: ${terminalNodes.map { it.printItself() }}")
        }
    }

    /**
     * if there is no node in our array, probably that is not node from our function
     */
    override fun isNodeUseful(node: SsaNode, mem: Memory): Boolean {
        return mem.instrOnPathStack.size <= maxExecutionDepth
                && (distToTerminal[node] == null || distToTerminal[node]!!.isNotEmpty())
    }

    var results = mutableSetOf<SymbolicResult<SsaNode>>()
    val globalStatistics = SsaExecutionStatistics()

    override suspend fun startFunction(
        func: FuncSsaNode,
        args: List<Symbolic?>?,
        ctx: KContext,
        initialState: SsaState
    ): Pair<List<SymbolicResult<SsaNode>>, Map<String, KSort>> {
        prepareState(func, args, initialState)

        queue.add(initialState)

        updateDistancesToTerminal()

        interpretLoop()

        return results.toList() to initialState.mem.createdConsts
    }

    private suspend fun interpretLoop() {
        while (queue.size() > 0) {
            try {
                yield()
            } catch (_: CancellationException) {
                while (queue.size() > 0) {
                    val state = queue.get()
                    results.addAll(state.mem.errors)
                }
                printlnError("\tstopped by timeout")
                return
            }
            val bestState = queue.get()

            val (cond, result, size) = interpretStep(bestState)
            when (size) {
//                execution ended
                0 -> {
                    results.add(SymbolicReturn(cond, result, bestState.getVisitedNodes()))
                    results.addAll(bestState.mem.errors)
                }
//                force stop
                -1 -> results.addAll(bestState.mem.errors)
                else -> queue.add(bestState)
            }
        }
    }

    override fun finishDynamicPanicInterpretation(
        state: SsaState,
        node: PanicSsaNode
    ): SsaNode {
        if (print)
            println("${state.stateId}\t\tPanic from Dynamic")

        updateDistancesToTerminal(node)

        return SsaStopNode()
    }

    override fun finishDynamicReturnInterpretation(
        state: SsaState,
        node: ReturnSsaNode,
        args: MutableList<Symbolic?>
    ): SsaNode {
        if (print)
            println("${state.stateId}\t\tReturn from Dynamic")

        updateDistancesToTerminal(node)

        state.mem.addResults(args.requireNoNulls())

        var result: SsaNode = node
        while (true) {
            val startedNode = state.startedNodes.removeLast()
            statisticsEndVisit(startedNode, state)

            while (true) {
                state.waitingNodes.removeFirst().first ?: break
            }
            when (startedNode) {
                is SsaStartFunctionNode -> {
                    result = startedNode
                    break
                }
//                this removes visibility layer and potentially pops solver
//                (so push and pops are consistent)
                is SsaEndBranchNode ->
                    translator(startedNode, state.executionStatistics).second(state.mem, listOf())

                else -> {
                    statisticsEndVisit(startedNode, state)
                    continue
                }
            }
        }
        updateDistancesToTerminal(node)
        return result
    }

    /**
     * uses dynamic variant of controlling branches
     */
    override fun startDynamicNodeInterpretation(
        state: SsaState,
        node: SsaNode,
        args: MutableList<Symbolic?>
    ): Boolean {
        return when (node) {
            is SsaBranchControlNode -> {
                if (print)
                    println("${state.stateId}\t\tSsaBranchControlNode from Dynamic")
                val cond = args[0]!!.bool(state.mem)

                val newState = state.clone()

                startNodeInterpretation(state, node, { _, _ ->
                    listOf(
                        SsaStartBranchNode(true, node.body, cond, SsaStopNode(), false)
                    )
                }, args.map { it }.toMutableList())

                startNodeInterpretation(newState, node, { _, _ ->
                    listOf(
                        SsaStartBranchNode(false, node.elseBody, cond, SsaStopNode(), false)
                    )
                }, args.map { it }.toMutableList())

                queue.add(newState)
                true
            }

            else -> false
        }
    }

    var globalTime = 0
    var globalNewCodeTime = 0

    override fun statisticsStartVisit(node: SsaNode, state: SsaState): Boolean {
        globalTime++
        return if (super.statisticsStartVisit(node, state)) {
            val oldInfo = globalStatistics.startVisit(node)
            if (!oldInfo.visitStarted) {
                globalNewCodeTime = globalTime
            }
            true
        } else
            false
    }

    override fun statisticsEndVisit(node: SsaNode, state: SsaState): Boolean {
        return if (super.statisticsEndVisit(node, state)) {
            globalStatistics.endVisit(node)
            true
        } else
            false
    }
}
