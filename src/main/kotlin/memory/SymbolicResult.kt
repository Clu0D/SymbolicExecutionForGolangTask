package memory

sealed class SymbolicResult<T>(val cond: BoolSymbolic, val visitedNodes: Set<T>)

class SymbolicError<T>(cond: BoolSymbolic, val error: String, visitedNodes: Set<T>) :
    SymbolicResult<T>(cond, visitedNodes) {
    override fun toString() =
        "symbolic error: $error"
}

class SymbolicReturn<T>(cond: BoolSymbolic, val returns: List<Symbolic>, visitedNodes: Set<T>) :
    SymbolicResult<T>(cond, visitedNodes) {
    constructor(cond: BoolSymbolic, result: Symbolic?, visitedNodes: Set<T>) : this(
        cond,
        if (result is ListSymbolic)
            result.list
        else
            listOfNotNull(result),
        visitedNodes
    )

    override fun toString() =
        "symbolic return: $returns"
}

fun <T> List<SymbolicReturn<T>>.combineToSymbolic(mem: Memory) = ListSymbolic(
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
