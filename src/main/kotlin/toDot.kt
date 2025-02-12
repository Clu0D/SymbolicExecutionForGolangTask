import guru.nidi.graphviz.attribute.Color
import guru.nidi.graphviz.attribute.Label
import guru.nidi.graphviz.engine.Engine
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.toGraphviz
import interpreter.ssa.*
import java.io.File

/** needs graphviz to be installed to work
 *
 *  https://graphviz.org/
 */
fun generateDotFile(initNode: FuncSsaNode) {
    println("!@${initNode.name}")
    guru.nidi.graphviz.graph(directed = true, name = initNode.name) {
        val set = mutableSetOf<SsaNode>()
        val queue = mutableListOf<SsaNode>(initNode)

        fun draw(node: SsaNode, other: SsaNode, name: String = "") {
            (node.printItself() - other.printItself())[Color.BLACK, Label.of(name)]
            queue.add(other)
        }

        fun draw(node: SsaNode, list: List<SsaNode>?, name: String = "") {
            list?.forEachIndexed { i, it ->
                draw(node, it, name + "$i")
            }
        }

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (set.contains(node)) continue
            set.add(node)
            when (node) {
                is BlockSsaNode -> draw(node, node.instr)

                is IfSsaNode -> {
                    draw(node, node.cond, "cond")
                    draw(node, node.body, "true")
                    draw(node, node.elseBody, "false")
                }

                is InvokeSsaNode -> TODO()

                is JumpSsaNode -> draw(node, node.successor)
                is LinkBlockSsaNode -> draw(node, node.deLink())
                is LinkParamSsaNode -> draw(node, node.deLink())
                is LinkSsaNode -> draw(node, node.deLink())

                is PanicSsaNode -> {}
                is ReturnSsaNode -> draw(node, node.results)

                is SsaBranchControlNode -> {}
                is SsaEndBranchNode -> {}
                is SsaKeepResult -> {}
                is SsaStartBranchNode -> {}
                is SsaStartFunctionNode -> {}
                is SsaStopNode -> {}

                is StoreSsaNode -> {
                    draw(node, node.addr, "addr")
                    draw(node, node.value, "value")
                }
                is UnOpSsaNode -> draw(node, node.x)
                is UnknownSsaNode -> {}
                is AllocSsaNode -> {}
                is BinOpSsaNode -> {
                    draw(node, node.x)
                    draw(node, node.y)
                }

                is BuiltInSsaNode -> {}
                is CallSsaNode -> {
                    draw(node, node.value, "call")
                    draw(node, node.args, "args")
                }

                is ConstSsaNode -> {}
                is ConvertSsaNode -> draw(node, node.x)
                is ExtractSsaNode -> {}
                is FieldAddrSsaNode -> {}
                is FuncSsaNode -> {
                    draw(node, node.params, "params")
                    if (node.body != null)
                        draw(node, node.body, "body")
                }

                is GlobalSsaNode -> {}
                is IndexAddrSsaNode -> {
                    draw(node, node.x, "params")
                    draw(node, node.index, "params")
                }

                is MakeInterfaceSsaNode -> {}
                is MakeSliceSsaNode -> {}
                is ParamSsaNode -> {}
                is PhiSsaNode -> draw(node, node.edgesMap().map { it.value })


                is SliceSsaNode -> {
                    draw(node, node.x, "params")
                    if (node.high != null)
                        draw(node, node.high, "params")
                }
            }
        }
    }.toGraphviz()
        // Engine.FDP and Engine.DOT look good
        .engine(Engine.DOT)
        .render(Format.PNG)
        .toFile(File("${initNode.name}.png"))
}