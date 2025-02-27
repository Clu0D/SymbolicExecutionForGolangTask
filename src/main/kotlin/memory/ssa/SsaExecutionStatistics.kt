package memory.ssa

import interpreter.ssa.SsaNode

class SsaExecutionStatistics(
    val localVisitCounter: MutableList<MutableMap<SsaNode, Int>> = mutableListOf(mutableMapOf()),
    val nodesInfo: MutableMap<SsaNode, NodeInfo> = mutableMapOf()
) {
    data class NodeInfo(
        val visitStarted: Boolean = false,
        val visitEnded: Boolean = false,
        val globalVisitCounter: Int = 0
    )

    fun clone() =
        SsaExecutionStatistics(
            localVisitCounter.map { a -> a.map { (b, c) -> b to c }.toMap().toMutableMap() }.toMutableList(),
            nodesInfo.map { (a, b) -> a to b }.toMap().toMutableMap()
        )

    fun startVisit(node: SsaNode): NodeInfo {
        val oldInfo = nodesInfo[node] ?: NodeInfo()
        nodesInfo[node] = oldInfo.copy(
            visitStarted = true,
            globalVisitCounter = oldInfo.globalVisitCounter + 1
        )
        localVisitCounter.first()[node] = (localVisitCounter.first()[node] ?: 0) + 1
        return oldInfo
    }

    fun endVisit(node: SsaNode): NodeInfo {
        val oldInfo = nodesInfo[node] ?: NodeInfo()
        nodesInfo[node] = oldInfo.copy(
            visitEnded = true,
            globalVisitCounter = oldInfo.globalVisitCounter + 1
        )
        return oldInfo
    }

    fun pushToStack() {
        localVisitCounter.add(0, mutableMapOf())
    }

    fun popFromStack() {
        localVisitCounter.removeFirst()
    }
}