package interpreter.ssa

import io.ksmt.KContext
import io.ksmt.sort.KSort
import memory.*
import memory.ssa.SsaState

abstract class SsaInterpreter(
    val functionDeclarations: Map<String, FuncSsaNode>
) {
    abstract fun startFunction(
        func: FuncSsaNode,
        args: List<Symbolic?>? = null,
        ctx: KContext,
        initialState: SsaState
    ): Pair<Collection<SymbolicResult>, Map<String, KSort>>

    companion object {
        const val STATIC_FOR_MAX_LENGTH = 10
    }
}