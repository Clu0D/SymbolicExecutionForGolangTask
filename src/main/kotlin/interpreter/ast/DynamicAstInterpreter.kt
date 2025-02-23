package interpreter.ast

import interpreter.InterpreterQueue
import interpreter.ssa.SsaNode
import io.ksmt.KContext
import io.ksmt.sort.KSort
import memory.*
import memory.ast.AstState
import kotlin.collections.forEach

class DynamicAstInterpreter(
    functionDeclarations: Map<String, AstFuncDecl>,
    typeDeclarations: List<AstType>,
    val interpreterQueue: InterpreterQueue<AstState>
) : StaticAstInterpreter(functionDeclarations, typeDeclarations) {

    var results = mutableSetOf<SymbolicResult<Any>>()
    val globalStatistics = ExecutionStatistics()

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

        interpreterQueue.add(initialState)

        interpretLoop()

        return results.also { results = mutableSetOf() } to initialState.mem.createdConsts
    }

    private fun interpretLoop() {
        while (interpreterQueue.size() > 0) {
            val bestState = interpreterQueue.get()

//            println(
//                "${bestState.stateId}/${queue.size() + 1}\t  " +
//                        "${bestState.startedNodes.lastOrNull()?.printItself()}:\t " +
//                        "${bestState.waitingNodes.first().first?.printItself()}"
//            )

            val (cond, result, size) = interpretState(bestState)
            when (size) {
//                execution ended
                0 -> {
                    results.add(SymbolicReturn(cond, result, bestState.getVisitedNodes()))
                    results.addAll(bestState.mem.errors as List<SymbolicResult<Any>>)
                }

//                force stop
                -1 -> results.addAll(bestState.mem.errors as List<SymbolicResult<Any>>)
                else -> interpreterQueue.add(bestState)
            }
        }
    }

    override fun finishDynamicReturnInterpretation(
        state: AstState,
        node: AstReturn,
        args: MutableList<Symbolic?>
    ): StartFunctionNode {
        state.mem.addResults(args.requireNoNulls())

        while (true) {
            val startedNode = state.startedNodes.removeLast()
            statisticsEndVisit(startedNode, state)
            while (true) {
                state.waitingNodes.removeFirst().first ?: break
            }
            when (startedNode) {
                is StartFunctionNode -> return startedNode
//                this removes visibility layer
                is AstFor -> translator(startedNode).second(state.mem, listOf())
//                this removes visibility layer and potentially pops solver (so push and pops are consistent)
                is EndBranchNode -> translator(startedNode).second(state.mem, listOf())

                else -> continue
            }
        }
    }

    override fun startDynamicNodeInterpretation(
        state: AstState,
        node: AstNode,
        args: MutableList<Symbolic?>
    ): Boolean {
        return when (node) {
            is BranchControlNode -> {
                val cond = args[0]!!.bool(state.mem)

                val newState = state.clone()

                startNodeInterpretation(state, node, { _, _ ->
                    listOf(
                        StartBranchNode(true, node.body, cond, StopNode()),
                        EndBranchNode(false)
                    )
                }, args.map { it }.toMutableList())

                startNodeInterpretation(newState, node, { _, _ ->
                    listOf(
                        StartBranchNode(false, node.elseBody, cond, StopNode()),
                        EndBranchNode(false)
                    )
                }, args.map { it }.toMutableList())

                interpreterQueue.add(newState)

                true
            }

            else -> false
        }
    }

    override fun statisticsStartVisit(node: AstNode, state: AstState) {
        state.executionStatistics.startVisit(node)
        val oldInfo = globalStatistics.startVisit(node)
        if (!oldInfo.visitStarted) {
            state.newCodeTime = state.time
        }
    }

    override fun statisticsEndVisit(node: AstNode, state: AstState) {
        state.executionStatistics.endVisit(node)
        globalStatistics.endVisit(node)
    }

}
