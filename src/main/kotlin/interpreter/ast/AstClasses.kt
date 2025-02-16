package interpreter.ast

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import memory.Type

@Serializable
sealed class AstNode {
    val id: Int = 0

    override fun toString() =
        "$id"

    abstract fun printItself(): String
}

@SerialName("*ast.StructType")
@Serializable
class AstStruct(
    val fields: List<AstField>?,
    val incomplete: Boolean
) : AstNode() {
    override fun toString() = printItself()
    override fun printItself() = "${super.toString()}struct"
}

@SerialName("*ast.File")
@Serializable
data class AstFile(
    val decl: List<AstNode>
) : AstNode() {
    override fun toString() = "${printItself()}{\n${decl.joinToString("\n")}}"
    override fun printItself() = "${super.toString()}decl"

    fun getFunctions() =
        decl.filterIsInstance<AstFuncDecl>()

    fun getTypes(): List<AstType> =
        decl.filterIsInstance<AstGenDecl>().flatMap { it.specs }
            .filterIsInstance<AstType>()
}

@SerialName("*ast.Ident")
@Serializable
data class AstIdent(
    val name: String
) : AstNode() {
    override fun toString() = printItself()
    override fun printItself() = "${super.toString()}!$name"
}

@SerialName("*ast.FuncDecl")
@Serializable
data class AstFuncDecl(
    val name: String,
    val body: AstNode,
    val recv: List<AstField>?, // nil if functions, not nil if method
//    val funcType: AstFuncType,
    val params: List<AstField>?,
    val results: List<AstField>?
) : AstNode() {
    // body is always block (?)
    override fun toString() = "${printItself()} ($params:$results) $body"
    override fun printItself() = "${super.toString()}$name"
}

@SerialName("*ast.IfStmt")
@Serializable
data class AstIf(
    val cond: AstNode,
    val body: AstNode,
    val elseBody: AstNode?,
) : AstNode() {
    //    body and elseBody block is
    override fun toString() = "\t${printItself()} $cond $body\n\telse $elseBody"
    override fun printItself() = "${super.toString()}if"
}

@SerialName("*ast.BinaryExpr")
@Serializable
data class AstBinaryExpr(
    val x: AstNode,
    val op: String,
    val y: AstNode,
) : AstNode() {
    override fun toString() = "$x ${printItself()} $y"
    override fun printItself() = "${super.toString()}$op"
}

@SerialName("*ast.UnaryExpr")
@Serializable
data class AstUnaryExpr(
    val op: String,
    val x: AstNode,
) : AstNode() {
    override fun toString() = "${printItself()} $x"
    override fun printItself() = "${super.toString()}$op"
}

@Serializable
data class AstField(
    val names: List<String>,
    val typeName: AstNode
) : AstNode() {
    fun type() = when (typeName) {
        is AstIdent -> Type.fromName(typeName.name)
        is AstArrayType -> Type.fromAst(typeName)
        else -> error("should not be $typeName ${typeName.javaClass.name}")
    }

    fun toPairs() = names.map { it to type() }

    override fun toString() = printItself()
    override fun printItself() = "$names: ${type()}"
}

@SerialName("*ast.BlockStmt")
@Serializable
data class AstBlockStmt(
    val statements: List<AstNode>?
) : AstNode() {
    override fun toString() = "${printItself()}{\n${statements?.joinToString("\n")}}"
    override fun printItself() = "${super.toString()}block"
}

@SerialName("*ast.ReturnStmt")
@Serializable
data class AstReturn(
    val result: List<AstNode>
) : AstNode() {
    override fun toString() = "${printItself()} $result"
    override fun printItself() = "${super.toString()}ret"
}

@SerialName("*ast.CallExpr")
@Serializable
data class AstCallExpr(
    val call: AstNode,
    val args: List<AstNode>
) : AstNode() {
    override fun toString() = "${printItself()}($args)"
    override fun printItself() = "${super.toString()}$call"
}

@SerialName("*ast.BasicLit")
@Serializable
data class AstBasicLit(
    val kind: String,
    val value: String
) : AstNode() {
    override fun toString() = printItself()
    override fun printItself() = "${super.toString()}{$value}"
}

@SerialName("*ast.AssignStmt")
@Serializable
data class AstAssignStmt(
    val lhs: List<AstNode>,
    val rhs: List<AstNode>,
    val token: String
) : AstNode() {
    override fun toString() = "${printItself()}:$lhs = $rhs"
    override fun printItself() = "${super.toString()}assign"
}

@SerialName("*ast.GenDecl")
@Serializable
data class AstGenDecl(
    val token: String,
    val specs: List<AstNode>
) : AstNode() {
    override fun toString() = "${printItself()}:\n${specs.joinToString("\n")}"
    override fun printItself() = "${super.toString()}specs"
}

@SerialName("*ast.ImportSpec")
@Serializable
data class AstImportSpec(
    val path: String
) : AstNode() {
    override fun toString() = printItself()
    override fun printItself() = "${super.toString()}import $path"
}

@SerialName("*ast.ValueSpec")
@Serializable
data class AstValueSpec(
    val names: List<String>,
    val typeNode: AstNode,
    val values: List<String>?
) : AstNode() {
    val typeName = (typeNode as AstIdent).name

    override fun toString() = "${printItself()} $names: $typeName = $values"
    override fun printItself() = "${super.toString()}valueSpec"
}

@SerialName("*ast.ForStmt")
@Serializable
data class AstFor(
    val init: AstNode,
    val cond: AstNode,
    val post: AstNode,
    val body: AstNode,
) : AstNode() {
    override fun toString() = "${printItself()} $init if $cond { $body } $post"
    override fun printItself() = "${super.toString()}for"
}

@SerialName("*ast.IncDecStmt")
@Serializable
data class AstIncDec(
    val x: AstNode,
    val tok: String,
) : AstNode() {
    override fun toString() = "${printItself()} $x"
    override fun printItself() = "${super.toString()}$tok"
}

@SerialName("*ast.IndexExpr")
@Serializable
data class AstIndexExpr(
    val x: AstNode,
    val index: AstNode,
) : AstNode() {
    override fun toString() = "${super.toString()}$x[$index]"
    override fun printItself() = "${super.toString()}indexExpr"
}

@SerialName("*ast.ArrayType")
@Serializable
data class AstArrayType(
    val length: AstNode?,
    val elementType: AstNode,
) : AstNode() {
    override fun toString() = "${super.toString()}$elementType[$length]"
    override fun printItself() = "${super.toString()}type"
}

@SerialName("*ast.TypeSpec")
@Serializable
data class AstType(
    val name: String,
//    val typeParams: List<Node>,
    val typeNode: AstNode,
) : AstNode() {
    override fun toString() = "${printItself()} $typeNode"

    //    override fun toString() = "${printItself()} $typeParams: $typeNode"
    override fun printItself() = "${super.toString()}type $name"
}

@SerialName("*ast.StarExpr")
@Serializable
data class AstStar(
    val x: AstNode,
) : AstNode() {
    override fun toString() = "${printItself()} $x"
    override fun printItself() = "${super.toString()}*"
}

@SerialName("*ast.SelectorExpr")
@Serializable
data class AstSelector(
    val x: AstNode,
    val selector: AstNode,
) : AstNode() {
    val selectorName = (selector as AstIdent).name

    override fun toString() = "${super.toString()} $x.$selector"
    override fun printItself() = "${super.toString()}selector"
}

//@SerialName("*ast.RangeStmt")
//@Serializable
//data class AstRange(
//    val key: AstNode?,
//    val value: AstNode?,
//    val tok: String,
//    val x: AstNode,
//    val body: AstNode
//) : AstNode() {
//    val existingKey = if (key == null) {
//
//    } else {
//
//    }
//    val astFor: AstFor = AstFor(
//        AstAssignStmt(
//            listOfNotNull(key),
//            listOf(AstBasicLit("int", "0")),
//            tok
//        ),
//        AstBinaryExpr(key)
//        val cond : AstNode,
//    val post: AstNode,
//    val body: AstNode,
//    )
//
//    override fun toString() = printItself()
//    override fun printItself() = "${super.toString()}range"
//}

@SerialName("Unsupported")
@Serializable
data class AstUnsupported(
    val unsupported: String
) : AstNode() {
    override fun toString() = printItself()
    override fun printItself() = "${super.toString()}${unsupported}"
}