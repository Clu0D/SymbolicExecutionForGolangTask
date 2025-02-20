package memory.ssa

import interpreter.ssa.SsaNode
import io.ksmt.KContext
import memory.Memory
import memory.State
import memory.Symbolic

class SsaState(
    val waitingNodes: MutableList<Pair<SsaNode?, MutableList<Symbolic?>>>,
    val startedNodes: MutableList<SsaNode>,
    internal val visitedNodes: MutableSet<SsaNode>,
    time: Long,
    newCodeTime: Long,
    val executionStatistics: SsaExecutionStatistics,
    val stateId: Int = stateCounter
) : State(time, newCodeTime) {
    lateinit var mem: Memory

    init {
        stateCounter++
    }

    constructor(ctx: KContext) : this(
        mutableListOf(),
        mutableListOf(),
        mutableSetOf(),
        0,
        0,
        SsaExecutionStatistics()
    ) {
        mem = Memory(ctx, this)
    }

    fun clone() = SsaState(
        waitingNodes.map { (a, b) -> a to (b.map { it }.toMutableList()) }.toMutableList(),
        startedNodes.map { it }.toMutableList(),
        visitedNodes.map { it }.toMutableSet(),
        time,
        newCodeTime,
        executionStatistics.clone()
    ).also {
        it.mem = mem.clone(it)
    }

    override fun getVisitedNodes() =
        visitedNodes.map { it }.toSet()

    companion object {
        var stateCounter = 0
    }
}