package memory

import interpreter.AstNode
import io.ksmt.KContext

class State(
    val stateId: Int,
    val waitingNodes: MutableList<Pair<AstNode?, MutableList<Symbolic?>>>,
    val startedNodes: MutableList<AstNode>,
    val mem: Memory,
    var time: Long,
    var newCodeTime: Long,
    val executionStatistics: ExecutionStatistics
) {
    constructor(ctx: KContext) : this(
        0,
        mutableListOf(),
        mutableListOf(),
        Memory(ctx),
        0,
        0,
        ExecutionStatistics()
    )

    fun clone() = State(
        stateId + 1,
        waitingNodes.map { (a, b) -> a to (b.map { it }.toMutableList()) }.toMutableList(),
        startedNodes.map { it }.toMutableList(),
        mem.clone(),
        time,
        newCodeTime,
        executionStatistics.clone()
    )
}