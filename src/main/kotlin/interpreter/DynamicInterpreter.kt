package interpreter

import io.ksmt.KContext
import io.ksmt.sort.KSort
import memory.*
import java.util.*
import kotlin.random.Random

class DynamicInterpreter(
    functionDeclarations: Map<String, AstFuncDecl>,
    typeDeclarations: List<AstType>,
    val queue: Queue<State>
) : StaticInterpreter(functionDeclarations, typeDeclarations) {

    var results = mutableSetOf<SymbolicResult>()
    val globalStatistics = ExecutionStatistics()

    override fun startFunction(
        func: AstFuncDecl,
        args: List<Symbolic?>?,
        ctx: KContext,
        initialState: State
    ): Pair<Collection<SymbolicResult>, Map<String, KSort>> {
        typeDeclarations.forEach {
            initialState.waitingNodes.add(it to mutableListOf())
        }
        initialState.waitingNodes.add(StartFunctionNode(func.name) to (args ?: listOf()).toMutableList())

        queue.add(initialState)

        interpretLoop()

        return results.also { results = mutableSetOf() } to initialState.mem.createdConsts
    }

    private fun interpretLoop() {
        while (queue.size() > 0) {
            val bestState = queue.get()

//            println(
//                "${bestState.stateId}/${queue.size() + 1}\t  " +
//                        "${bestState.startedNodes.lastOrNull()?.printItself()}:\t " +
//                        "${bestState.waitingNodes.first().first?.printItself()}"
//            )

            val (cond, result, size) = interpretState(bestState)
            when (size) {
//                execution ended
                0 -> {
                    results.add(SymbolicReturn(cond, result))
                    results.addAll(bestState.mem.errors)
                }

//                force stop
                -1 -> results.addAll(bestState.mem.errors)
                else -> queue.add(bestState)
            }
        }
    }

    override fun finishDynamicReturnInterpretation(
        state: State,
        node: AstReturn,
        args: MutableList<Symbolic?>
    ): StartFunctionNode {
        state.mem.addResults(args.requireNoNulls())

        while (true) {
            val startedNode = state.startedNodes.removeLast()
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
        state: State,
        node: AstNode,
        args: MutableList<Symbolic?>
    ): Boolean {
        return when (node) {
            is BranchControlNode -> {
                val cond = args[0]!!.bool()

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

                queue.add(newState)

                true
            }

            else -> false
        }
    }

    override fun statisticsStartVisit(node: AstNode, state: State) {
        state.executionStatistics.startVisit(node)
        val oldInfo = globalStatistics.startVisit(node)
        if (!oldInfo.visitStarted) {
            state.newCodeTime = state.time
        }
    }

    override fun statisticsEndVisit(node: AstNode, state: State) {
        state.executionStatistics.endVisit(node)
        globalStatistics.endVisit(node)
    }

    companion object {
        fun dfsQueue(): Queue<State> = StandardQueue(::priorityDfs)
        fun bfsQueue(): Queue<State> = StandardQueue(::priorityBfs)
        fun timeToNewCodeQueue(): Queue<State> = StandardQueue(::priorityTimeFromNewCodeFound)
        fun randomQueue(): Queue<State> = WeightedRandomQueue(::priorityRandom)
        fun randomTimeToNewCodeQueue(): Queue<State> =
            WeightedRandomQueue(::priorityTimeFromNewCodeFound)

        interface Queue<T> {
            fun size(): Int

            fun add(e: T)

            fun get(): T
        }

        private var dfsCounter = 1.0
        private fun priorityDfs(state: State): Double {
            dfsCounter--
            return dfsCounter
        }

        private var bfsCounter = 1.0
        private fun priorityBfs(state: State): Double {
            bfsCounter++
            return bfsCounter
        }

        private fun priorityTimeFromNewCodeFound(state: State): Double {
            return (state.time - state.newCodeTime + 1).toDouble()
        }

        private val rnd = Random

        private fun priorityRandom(state: State): Double {
            return rnd.nextDouble() + 0.01
        }

        private class StandardQueue<T>(val priorityF: (T) -> Double) : Queue<T> {
            private val treeSet = TreeSet<Pair<T, Double>>(compareBy { it.second })

            override fun size(): Int =
                treeSet.size

            override fun add(e: T) {
                treeSet.add(e to priorityF(e))
            }

            override fun get(): T =
                treeSet.pollFirst()!!.first
        }

        private class WeightedRandomQueue<T>(val priorityF: (T) -> Double) : Queue<T> {
            private val list = mutableListOf<Pair<T, Double>>()
            private var sumWeight = 0.0

            override fun size(): Int =
                list.size

            override fun add(e: T) {
                val weight = priorityF(e)
                list.add(e to weight)
                sumWeight += weight
            }

            override fun get(): T {
                var randomWeight = rnd.nextDouble(sumWeight)
                while (true) {
                    val (e, w) = list.removeFirst()
                    randomWeight -= w
                    if (randomWeight <= 0) {
                        sumWeight -= w
                        return e
                    }
                    list.add(e to w)
                }
            }

        }
    }
}
