package memory

import interpreter.AstNode

class ExecutionStatistics(val nodesInfo: MutableMap<AstNode, NodeInfo> = mutableMapOf()) {
    data class NodeInfo(
        val visitStarted: Boolean = false,
        val visitEnded: Boolean = false,
        val visitCounter: Int = 0
    )

    fun clone() =
        ExecutionStatistics(nodesInfo.map { (a, b) -> a to b }.toMap().toMutableMap())

    fun startVisit(node: AstNode): NodeInfo {
        val oldInfo = nodesInfo[node] ?: NodeInfo()
        nodesInfo[node] = oldInfo.copy(
            visitStarted = true,
            visitCounter = oldInfo.visitCounter + 1
        )
        return oldInfo
    }

    fun endVisit(node: AstNode): NodeInfo {
        val oldInfo = nodesInfo[node] ?: NodeInfo()
        nodesInfo[node] = oldInfo.copy(
            visitEnded = true,
            visitCounter = oldInfo.visitCounter + 1
        )
        return oldInfo
    }
}