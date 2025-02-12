package interpreter.ssa

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SsaNode {
    val id: Int = 0

    override fun toString() = "${id}Node}"

    abstract fun printItself(): String

    init {
        allNodes[id] = this
    }

    companion object {
        val allNodes = mutableMapOf<Int, SsaNode>()
    }
}

@Serializable
sealed class ValueSsaNode : SsaNode() {
    val name: String = "Unknown"
    val valueType: SsaType? = null

    override fun toString() = "${this.javaClass.simpleName.replace("SsaNode", "")}(name=$name, paramType=$valueType)"
    override fun printItself() = "$id${this.javaClass.simpleName.replace("SsaNode", "")}"
}

@Serializable
@SerialName("*ssa.BasicBlock")
data class BlockSsaNode(
    val instr: List<SsaNode>?
) : SsaNode() {
    override fun toString() = "Block(instr=$instr)"
    override fun printItself() = "${id}Block"
}

@Serializable
@SerialName("*ssa.If")
data class IfSsaNode(
    val cond: SsaNode,
    val body: SsaNode,
    val elseBody: SsaNode
) : SsaNode() {
    override fun toString() = "If(cond=$cond, body=$body, elseBody=$elseBody)"
    override fun printItself() = "${id}If"
}

@Serializable
@SerialName("*ssa.BinOp")
data class BinOpSsaNode(
    val x: SsaNode,
    val y: SsaNode,
    val op: String
) : ValueSsaNode() {
    override fun toString() = "BinOp(x=$x, y=$y, op=$op)"
    override fun printItself() = "${id}BinOp $op"
}

@Serializable
@SerialName("*ssa.Return")
data class ReturnSsaNode(
    val results: List<SsaNode>
) : SsaNode() {
    override fun toString() = "Return(results=$results)"
    override fun printItself() = "${id}Return"
}

@Serializable
@SerialName("*ssa.Function")
data class FuncSsaNode(
    val paramsNull: List<SsaNode>?,
    val body: SsaNode?
) : ValueSsaNode() {
    val params = paramsNull ?: listOf()
    override fun toString() = "Func(name=$name, params=$params, body=$body)"
    override fun printItself() = "${id}Func $name"
}

@Serializable
@SerialName("*ssa.Parameter")
class ParamSsaNode : ValueSsaNode() {
    override fun toString() = "Param $name"
    override fun printItself() = "${id}Param $name"
}

@Serializable
@SerialName("*ssa.Const")
class ConstSsaNode : ValueSsaNode() {
    override fun toString() = "Const $name"
    override fun printItself() = "${id}Const $name"
}

@Serializable
@SerialName("*ssa.Alloc")
class AllocSsaNode : ValueSsaNode()

@Serializable
@SerialName("*ssa.Slice")
data class SliceSsaNode(
    val x: SsaNode,
    val high: SsaNode?
) : ValueSsaNode() {
    override fun toString() = "Slice($x)"
    override fun printItself() = "${id}Slice $name"
}

@Serializable
@SerialName("*ssa.Phi")
data class PhiSsaNode(
    private val edges: List<SsaNode>,
    private val preds: List<SsaNode>
) : ValueSsaNode() {
    fun edgesMap(): Map<SsaNode, SsaNode> =
        preds.zip(edges).associate { (p, e) ->
            when (p) {
                is LinkToSsaNode -> p.deLink()
                else -> p
            } to e
        }

    override fun toString() = "Phi(edges=$edges)"
    override fun printItself() = "${id}Phi"
}

@Serializable
@SerialName("*ssa.MakeSlice")
class MakeSliceSsaNode : ValueSsaNode()

@Serializable
@SerialName("*ssa.IndexAddr")
class IndexAddrSsaNode(
    val x: SsaNode,
    val index: SsaNode
) : ValueSsaNode() {
    override fun toString() = "IndexAddr(name=$name x=$x, index=$index)"
    override fun printItself() = "${id}IndexAddr"
}

@Serializable
@SerialName("*ssa.FieldAddr")
data class FieldAddrSsaNode(
    val x: SsaNode,
    val field: Int
) : ValueSsaNode()

@Serializable
@SerialName("*ssa.Builtin")
class BuiltInSsaNode : ValueSsaNode()

@Serializable
@SerialName("*ssa.Convert")
data class ConvertSsaNode(
    val x: SsaNode
) : ValueSsaNode()

@Serializable
@SerialName("*ssa.Global")
class GlobalSsaNode : ValueSsaNode()

@Serializable
@SerialName("*ssa.Extract")
class ExtractSsaNode : ValueSsaNode()

@Serializable
@SerialName("*ssa.MakeInterface")
class MakeInterfaceSsaNode : ValueSsaNode()

@Serializable
@SerialName("*ssa.Call")
class CallSsaNode(
    val value: SsaNode,
    val args: List<SsaNode>
) : ValueSsaNode() {
    //  todo remove  val call = value.deLink()
    //   todo val params = args.map { it.deLink() as ValueSsaNode }

    override fun toString() = "Call(call=$value, args=$args)"
    override fun printItself() = "${id}Call"
}

@Serializable
@SerialName("*ssa.Call invoke")
data class InvokeSsaNode(
    val value: SsaNode,
    val method: String,
    val args: List<SsaNode>
) : SsaNode() {
    init {
        error("invoke mode for method '$method'")
    }

    override fun toString() = "Call invoke(call=$value, method=$method, args=$args)"
    override fun printItself() = "${id}Call invoke"
}

@Serializable
@SerialName("*ssa.Store")
data class StoreSsaNode(
    val addr: SsaNode,
    val value: SsaNode
) : SsaNode() {
    override fun toString() = "Store(addr=$addr, value=$value)"
    override fun printItself() = "${id}Store"
}

@Serializable
@SerialName("*ssa.Jump")
data class JumpSsaNode(
    val successor: SsaNode
) : SsaNode() {
    override fun toString() = "Jump(successor=$successor)"
    override fun printItself() = "${id}Jump"
}

@Serializable
@SerialName("*ssa.UnOp")
data class UnOpSsaNode(
    val op: String,
    val x: SsaNode,
    val commaOk: Boolean
) : ValueSsaNode() {
    override fun toString() = "UnOp(op=$op, x=$x, commaOk=$commaOk)"
    override fun printItself() = "${id}UnOp $op"
}

@Serializable
@SerialName("*ssa.Panic")
data class PanicSsaNode(
    val x: String
) : SsaNode() {
    override fun toString() = "Panic(x=$x)"
    override fun printItself() = "${id}Panic"
}


/** represents a link to another node through id
 *
 *  all other nodes should have unique ids, except these nodes
 */
interface LinkToSsaNode {
    fun deLink(): SsaNode
}

interface LinkToSsaType {
    fun deLink(): SsaType
}

@Serializable
@SerialName("LinkToNode")
class LinkSsaNode(
    val linkId: Int
) : SsaNode(), LinkToSsaNode {
    override fun toString() = "Link(${linkId})"
    override fun printItself() = "${id}Link"

    override fun deLink(): SsaNode = when (val linked = allNodes[linkId]!!) {
        is LinkToSsaNode -> linked.deLink()
        else -> linked
    }

}

@Serializable
@SerialName("LinkToBlock")
class LinkBlockSsaNode(
    val linkId: Int
) : SsaNode(), LinkToSsaNode {
    override fun toString() = "LinkBlock(${linkId}})"
    override fun printItself() = "${id}LinkBlock"

    override fun deLink(): BlockSsaNode =
        allNodes[linkId]!! as BlockSsaNode
}

@Serializable
@SerialName("LinkToParam")
class LinkParamSsaNode(
    val linkId: Int
) : SsaNode(), LinkToSsaNode {
    override fun toString() = "LinkParam(${linkId})"
    override fun printItself() = "${id}LinkParam"

    override fun deLink(): ParamSsaNode =
        allNodes[linkId]!! as ParamSsaNode
}

@Serializable
@SerialName("LinkToType")
class LinkSsaType(
    val linkId: Int
) : SsaType(), LinkToSsaType {
    override fun toString() = "LinkType(${linkId})"
    override fun printItself() = "${id}LinkType"

    override fun deLink(): SsaType =
        allTypes[linkId]!!
}

@Serializable
@SerialName("LinkToFunc")
class LinkFuncSsa(
    val linkId: Int
) : SsaType(), LinkToSsaType {
    override fun toString() = "LinkFunc(${linkId})"
    override fun printItself() = "${id}LinkFunc"

    override fun deLink(): FuncTypeNode =
        allTypes[linkId]!! as FuncTypeNode
}

@Serializable
@SerialName("Unknown")
class UnknownSsaNode : SsaNode() {
    override fun toString() = "Unknown"
    override fun printItself() = "${id}Unknown"
}

// types
@Serializable
sealed class SsaType {
    val id: Int = 0

    override fun toString() = "${id}Type}"

    abstract fun printItself(): String

    init {
        allTypes[id] = this
    }

    companion object {
        val allTypes = mutableMapOf<Int, SsaType>()
    }
}

@Serializable
@SerialName("*types.Basic")
data class BasicTypeNode(
    val name: String
) : SsaType() {
    override fun toString() = "BasicType(name=$name)"
    override fun printItself() = "${id}BasicType"
}

@Serializable
@SerialName("*types.Struct")
data class StructTypeNode(
    val fields: List<SsaType>
) : SsaType() {
    override fun toString() = "StructType(fields=$fields)"
    override fun printItself() = "${id}StructType"
}

@Serializable
@SerialName("Field")
data class StructFieldNode(
    val name: String,
    val elemType: SsaType,
//    val tag: String? = null
) : SsaType() {
    override fun toString() = "StructField(name=$name, elemType=$elemType)"
    override fun printItself() = "${id}StructField"
}

@Serializable
@SerialName("*types.Pointer")
data class PointerTypeNode(
    val elemType: SsaType
) : SsaType() {
    override fun toString() = "PointerType(elemType=$elemType)"
    override fun printItself() = "${id}PointerType"
}

@Serializable
@SerialName("*types.Slice")
data class SliceTypeNode(
    val elemType: SsaType
) : SsaType() {
    override fun toString() = "SliceType(elemType=$elemType)"
    override fun printItself() = "${id}SliceType"
}

@Serializable
@SerialName("*types.Array")
data class ArrayTypeNode(
    val elemType: SsaType,
    val len: Int
) : SsaType() {
    override fun toString() = "ArrayType(elemType=$elemType, len=$len)"
    override fun printItself() = "${id}ArrayType"
}

@Serializable
@SerialName("*types.Signature")
class SignatureTypeNode : SsaType() {
    override fun toString() = "SignatureType()"
    override fun printItself() = "${id}SignatureType"
}

@Serializable
@SerialName("*types.Func")
class FuncTypeNode : SsaType() {
    override fun toString() = "FuncType()"
    override fun printItself() = "${id}FuncType"
}

@Serializable
@SerialName("*types.Alias")
class AliasTypeNode : SsaType() {
    override fun toString() = "AliasType()"
    override fun printItself() = "${id}AliasType"
}

@Serializable
@SerialName("*types.Tuple")
class TupleTypeNode : SsaType() {
    override fun toString() = "Tuple()"
    override fun printItself() = "${id}Tuple"
}

@Serializable
@SerialName("*types.Interface")
data class InterfaceTypeNode(
    val methods: List<SsaType>,
    val embedded: List<SsaType>?
) : SsaType() {
    override fun toString() = "InterfaceType(methods=$methods, embedded=$embedded)"
    override fun printItself() = "${id}InterfaceType"
}

@Serializable
@SerialName("*types.Named")
data class NamedTypeNode(
    val name: String,
    val underlying: SsaType,
    val methods: List<SsaType>? = null
) : SsaType() {
    override fun toString() = "NamedType(name=$name, underlying=$underlying, methods=$methods)"
    override fun printItself() = "${id}NamedType"
}

@Serializable
@SerialName("UnknownType")
class UnknownSsaTypeNode : SsaType() {
    override fun toString() = "UnknownType"
    override fun printItself() = "${id}UnknownType"
}
