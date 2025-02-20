package interpreter.ast

import memory.BoolSymbolic

class StartFunctionNode(val functionName: String) : AstNode() {
    override fun printItself() = "start function"
    override fun toString() = printItself()
}

class ContinueForNode(val counter: Int, val body: AstNode, val post: AstNode, val cond: AstNode) : AstNode() {
    override fun printItself() = "continue for $counter"
    override fun toString() = printItself()
}

class StopNode : AstNode() {
    override fun printItself() = "stop"
    override fun toString() = printItself()
}

class BranchControlNode(val body: AstNode, val elseBody: AstNode?) : AstNode() {
    override fun printItself() = "branch control"
    override fun toString() = printItself()
}

class StartBranchNode(val branch: Boolean, val body: AstNode?, val cond: BoolSymbolic, val stopOrContinue: AstNode?) :
    AstNode() {
    override fun printItself() = "start branch $branch"
    override fun toString() = printItself()
}

class EndBranchNode(val removesCond: Boolean) : AstNode() {
    override fun printItself() = "end branch"
    override fun toString() = printItself()
}