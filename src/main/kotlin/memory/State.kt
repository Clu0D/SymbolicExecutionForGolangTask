package memory

abstract class State(
    var time: Long,
    var newCodeTime: Long,
) {
    abstract fun getVisitedNodes(): Set<Any>
}