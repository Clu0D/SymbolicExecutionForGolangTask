package memory.ssa

import interpreter.ssa.SsaNode

class SsaExecutionStatistics(val nodesInfo: MutableMap<SsaNode, NodeInfo> = mutableMapOf()) {
    data class NodeInfo(
        val visitStarted: Boolean = false,
        val visitEnded: Boolean = false,
        val visitCounter: Int = 0
    )

    fun clone() =
        SsaExecutionStatistics(nodesInfo.map { (a, b) -> a to b }.toMap().toMutableMap())

    fun startVisit(node: SsaNode): NodeInfo {
        val oldInfo = nodesInfo[node] ?: NodeInfo()
        nodesInfo[node] = oldInfo.copy(
            visitStarted = true,
            visitCounter = oldInfo.visitCounter + 1
        )
        return oldInfo
    }

    fun endVisit(node: SsaNode): NodeInfo {
        val oldInfo = nodesInfo[node] ?: NodeInfo()
        nodesInfo[node] = oldInfo.copy(
            visitEnded = true,
            visitCounter = oldInfo.visitCounter + 1
        )
        return oldInfo
    }
}