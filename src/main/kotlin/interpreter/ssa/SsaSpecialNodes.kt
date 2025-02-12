package interpreter.ssa

import memory.BoolSymbolic
import memory.Symbolic

class SsaKeepResult(val result: Symbolic?) : SsaNode() {
    override fun printItself() = "keep result"
    override fun toString() = printItself()
}

class SsaStartFunctionNode(val functionNode: FuncSsaNode) : SsaNode() {
    override fun printItself() = "start function"
    override fun toString() = printItself()
}

class SsaStopNode : SsaNode() {
    override fun printItself() = "stop"
    override fun toString() = printItself()
}

class SsaBranchControlNode(val body: SsaNode, val elseBody: SsaNode?) : SsaNode() {
    override fun printItself() = "branch control"
    override fun toString() = printItself()
}

class SsaStartBranchNode(val branch: Boolean, val body: SsaNode?, val cond: BoolSymbolic, val stopOrContinue: SsaNode?) :
    SsaNode() {
    override fun printItself() = "start branch $branch"
    override fun toString() = printItself()
}

class SsaEndBranchNode(val removesCond: Boolean) : SsaNode() {
    override fun printItself() = "end branch"
    override fun toString() = printItself()
}