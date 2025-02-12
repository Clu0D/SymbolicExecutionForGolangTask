package memory.ssa

import interpreter.ssa.SsaNode
import io.ksmt.KContext
import memory.Memory
import memory.Symbolic

class SsaState(
    val stateId: Int,
    val waitingNodes: MutableList<Pair<SsaNode?, MutableList<Symbolic?>>>,
    val startedNodes: MutableList<SsaNode>,
    val mem: Memory,
    var time: Long,
    var newCodeTime: Long,
    val executionStatistics: SsaExecutionStatistics
) {
    constructor(ctx: KContext) : this(
        0,
        mutableListOf(),
        mutableListOf(),
        Memory(ctx),
        0,
        0,
        SsaExecutionStatistics()
    )

    fun clone() = SsaState(
        stateId + 1,
        waitingNodes.map { (a, b) -> a to (b.map { it }.toMutableList()) }.toMutableList(),
        startedNodes.map { it }.toMutableList(),
        mem.clone(),
        time,
        newCodeTime,
        executionStatistics.clone()
    )
}