package memory

sealed class SymbolicResult(val cond: BoolSymbolic)

class SymbolicError(cond: BoolSymbolic, val error: String) : SymbolicResult(cond) {
    override fun toString() =
        "symbolic error: $error"
}

class SymbolicReturn(cond: BoolSymbolic, val returns: List<Symbolic>) : SymbolicResult(cond) {
    constructor(cond: BoolSymbolic, result: Symbolic?) : this(
        cond, if (result is ListSymbolic) result.list
        else listOfNotNull(result)
    )

    override fun toString() =
        "symbolic return: $returns"
}

fun List<SymbolicReturn>.combineToSymbolic(mem: Memory) = ListSymbolic(
    if (isEmpty()) {
        listOf()
    } else (0 until first().returns.size).map { i ->
        var combinedResult = first().returns[i]

        for (j in 1 until size) {
            val current = this[j]
            combinedResult = mem.ite(current.cond, current.returns[i], combinedResult)
        }

        combinedResult
    }
)
