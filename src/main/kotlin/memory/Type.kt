package memory

import interpreter.AstArrayType
import interpreter.AstIdent
import interpreter.AstStar
import interpreter.AstNode
import interpreter.ssa.*
import io.ksmt.expr.KExpr
import io.ksmt.sort.*
import io.ksmt.utils.asExpr
import memory.IntType.Companion.intSort

sealed class BaseType : Type {
    override fun toString() = "TYPE"

    override fun equals(other: Any?): Boolean {
        return toString() == other.toString()
    }

    override fun hashCode() = toString().hashCode()
}

sealed interface Type {
    /**
     * creates a new symbolic variable
     */
    fun createSymbolic(name: String, mem: Memory, isLocal: Boolean = true): Symbolic

    /**
     * creates a default value
     */
    fun defaultSymbolic(mem: Memory, isLocal: Boolean): Symbolic

//    override fun toString() = "TYPE"
//
//    override fun equals(other: Any?): Boolean {
//        return toString() == other.toString()
//    }
//
//    override fun hashCode() = toString().hashCode()

    companion object {
        fun toSymbolic(expr: KExpr<out KSort>): Symbolic = when (expr.sort) {
            is KBv32Sort -> IntSymbolic(expr as KExpr<KBv32Sort>)
            is KBoolSort -> BoolSymbolic(expr as KExpr<KBoolSort>)
            is KFp64Sort -> FloatSymbolic(expr as KExpr<KFp64Sort>)
            is KUninterpretedSort -> UninterpretedSymbolic(expr as KExpr<KUninterpretedSort>)
            else -> error("toSymbolic from ${expr.sort}")
        }

        fun fromAst(node: AstNode): Type = when (node) {
            is AstIdent -> fromName(node.name)
            is AstArrayType -> ArrayType(fromAst(node.elementType) as StarType)
            is AstStar -> StarType(fromAst(node.x), false)
            else -> error("not type \"${node.javaClass.name}\"")
        }

        /**
         * simplify removes info about array sizes
         *
         * used in alloc to create z3 structures
         */
        fun fromSsa(node: SsaType, mem: Memory, simplify: Boolean): Type {
            return when (node) {
                is LinkSsaType -> fromSsa(node.deLink(), mem, simplify)

                is BasicTypeNode ->
                    return fromName(node.name)

                is ArrayTypeNode ->
                    if (simplify) {
                        when (val x = fromSsa(node.elemType, mem, true)) {
                            is ArrayType -> ArrayType(StarType(x, true))
//                            is SimpleType -> ArraySimpleType(x)
                            else -> ArrayType(x)
                        }
                    } else {
                        ArrayType(
                            fromSsa(node.elemType, mem, false),
                            IntType.fromInt(node.len, mem)
                        )
                    }

                is InterfaceTypeNode -> TODO()
                is NamedTypeNode -> {
                    val named = NamedType(
                        node.name,
                        fromSsa(node.underlying, mem, simplify) as StructType
                    )
//                    if (simplify)
//                        StarType(named, true)
//                    else
                    named
                }

                is PointerTypeNode -> StarType(fromSsa(node.elemType, mem, simplify), false)
                is StructTypeNode -> {
                    val struct = StructType(
                        node.fields.map {
                            val name = (it as StructFieldNode).name
                            val type = fromSsa(it.elemType, mem, simplify)
                            name to type
                        }
                    )
//                    if (simplify)
//                        StarType(struct, true)
//                    else
                    struct
                }

                is SignatureTypeNode -> TODO()
                is SliceTypeNode ->
                    if (simplify) {
                        when (val x = fromSsa(node.elemType, mem, true)) {
                            is ArrayType -> ArrayType(StarType(x, true))
//                            is SimpleType -> ArraySimpleType(x)
                            else -> ArrayType(x)
                        }
                    } else {
                        ArrayType(fromSsa(node.elemType, mem, false))
//                        when (val innerType = fromSsa(node.elemType, mem, false)) {
//                            is SimpleType -> ArraySimpleType(innerType)
//                            else -> ArrayType(innerType, null)
//                        }
                    }

                is AliasTypeNode -> TODO()
                is FuncTypeNode -> FunctionType
                is LinkFuncSsa -> TODO()
                is StructFieldNode -> TODO()
                is TupleTypeNode -> TODO()
                is UnknownSsaTypeNode -> TODO()
            }
        }

        fun fromName(name: String): Type = when (name.lowercase()) {
            "bool" -> BoolType()
            "int" -> IntType()
            "int16" -> IntType()
            "int32" -> IntType()
            "int64" -> IntType()
            "byte" -> IntType()
//            todo add other lengths working properly
            "float64" -> FloatType()
            "complex128" -> ComplexType()
            "string" -> UninterpretedType(String.Companion::class.java.name)
//            TODO обратиться к памяти и найти класс?
            else -> error("only simple types here, not $name")
        }
    }
}

class StructType(val fields: List<Pair<String, Type>>) : BaseType() {
    override fun createSymbolic(name: String, mem: Memory, isLocal: Boolean) =
        StructSymbolic(
            this,
            fields.associate { (name, type) -> name to type.defaultSymbolic(mem, isLocal) }.toMutableMap()
        )

    override fun defaultSymbolic(mem: Memory, isLocal: Boolean): Symbolic {
        return StructSymbolic(
            this,
            fields.associate { (name, type) ->
                name to type.defaultSymbolic(mem, isLocal)
            }.toMutableMap()
        )
    }

    override fun toString() = "Struct($fields)"
}

// types that can be represented with KExpr
sealed interface SimpleType : Type {
    fun defaultSymbolicExpr(mem: Memory, isLocal: Boolean): KExpr<out KSort>

    fun asSymbolic(expr: KExpr<out KSort>, mem: Memory): Symbolic

    fun sort(mem: Memory): KSort
}

class BoolType : BaseType(), SimpleType {
    override fun defaultSymbolicExpr(mem: Memory, isLocal: Boolean): KExpr<out KSort> =
        defaultSymbolic(mem, isLocal).expr

    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory) =
        BoolSymbolic(expr as KExpr<KBoolSort>)

    override fun sort(mem: Memory) = with(mem.ctx) { boolSort }

    override fun createSymbolic(name: String, mem: Memory, isLocal: Boolean) = with(mem.ctx) {
        mem.addConst(name, mkBoolSort()).toSymbolic()
    }

    override fun defaultSymbolic(mem: Memory, isLocal: Boolean) = with(mem.ctx) {
        false.expr.toSymbolic()
    }

    override fun toString() = "BOOL"

    companion object {
        fun TRUE(mem: Memory) = BoolSymbolic(mem.ctx.trueExpr)
        fun FALSE(mem: Memory) = BoolSymbolic(mem.ctx.falseExpr)
        fun fromBool(bool: Boolean, mem: Memory) = with(mem.ctx) { mkBv(bool).toSymbolic() }
    }
}

class IntType : BaseType(), SimpleType {
    companion object {
        fun intSort(mem: Memory) = with(mem.ctx) { bv32Sort }

        fun ZERO(mem: Memory) = with(mem.ctx) { mkBv(0).asExpr(intSort(mem)) }

        fun fromInt(int: Int, mem: Memory) = with(mem.ctx) { mkBv(int).asExpr(intSort(mem)).toSymbolic() }
    }

    override fun defaultSymbolicExpr(mem: Memory, isLocal: Boolean): KExpr<out KSort> =
        defaultSymbolic(mem, isLocal).expr

    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory) =
        IntSymbolic(expr as KExpr<KBv32Sort>)

    override fun sort(mem: Memory) = intSort(mem)

    override fun createSymbolic(name: String, mem: Memory, isLocal: Boolean): IntSymbolic = with(mem.ctx) {
        mem.addConst(name, bv32Sort).toSymbolic().int()
    }

    override fun defaultSymbolic(mem: Memory, isLocal: Boolean) = with(mem.ctx) {
        mkBv(0).toSymbolic()
    }

    override fun toString() = "INT"
}

class FloatType : BaseType(), SimpleType {
    companion object {
        fun floatSort(mem: Memory) = with(mem.ctx) { fp64Sort }

        fun ZERO(mem: Memory) = with(mem.ctx) { mkFp(0.0f, fp64Sort) }

        fun fromDouble(double: Double, mem: Memory) = with(mem.ctx) { mkFp(double, fp64Sort) }.toSymbolic()
    }

    override fun defaultSymbolicExpr(mem: Memory, isLocal: Boolean): KExpr<out KSort> =
        defaultSymbolic(mem, isLocal).expr

    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory) =
        FloatSymbolic(expr as KExpr<KFp64Sort>)

    override fun sort(mem: Memory) = floatSort(mem)

    override fun createSymbolic(name: String, mem: Memory, isLocal: Boolean) = with(mem.ctx) {
        mem.addConst(name, fp64Sort).toSymbolic()
    }

    override fun defaultSymbolic(mem: Memory, isLocal: Boolean) = with(mem.ctx) {
        0.0.expr.toSymbolic()
    }

    override fun toString() = "FLOAT"
}

class ComplexType : BaseType() {
    override fun createSymbolic(name: String, mem: Memory, isLocal: Boolean) =
        ComplexSymbolic(
            mem.addConst("$name.Real", FloatType.floatSort(mem)).toSymbolic().floatExpr(mem),
            mem.addConst("$name.Img", FloatType.floatSort(mem)).toSymbolic().floatExpr(mem)
        )

    override fun defaultSymbolic(mem: Memory, isLocal: Boolean) =
        ComplexSymbolic(
            FloatType.ZERO(mem),
            FloatType.ZERO(mem),
        )

    override fun toString() = "COMPLEX"
}

// todo is it simple?
class UninterpretedType(val typeName: String) : BaseType(), SimpleType {
    companion object {
        fun fromString(string: String, mem: Memory): UninterpretedSymbolic = with(mem.ctx) {
            UninterpretedSymbolic(mkUninterpretedSortValue(mkUninterpretedSort("string"), string.hashCode()))
        }

        fun fromAny(any: Any, mem: Memory): UninterpretedSymbolic = with(mem.ctx) {
            UninterpretedSymbolic(mkUninterpretedSortValue(mkUninterpretedSort(any.javaClass.name), any.hashCode()))
        }
    }

    override fun defaultSymbolicExpr(mem: Memory, isLocal: Boolean): KExpr<out KSort> =
        defaultSymbolic(mem, isLocal).expr

    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory) =
        UninterpretedSymbolic(expr as KExpr<KUninterpretedSort>)

    override fun sort(mem: Memory) =
        mem.ctx.mkUninterpretedSort(typeName)

    override fun createSymbolic(name: String, mem: Memory, isLocal: Boolean) =
        UninterpretedSymbolic(mem.ctx.mkConst(name, sort(mem)))

    override fun defaultSymbolic(mem: Memory, isLocal: Boolean) =
        UninterpretedSymbolic(mem.ctx.mkConst("nil", sort(mem)))

    override fun toString() = "UNINTERPRETED"
}


/** fake star is a star that should be dissolved immediately on get
 *
 *  it is used to get arrays of non-simple types working
 */
class StarType(val elementType: Type, val fake: Boolean) : BaseType(), SimpleType {

    override fun defaultSymbolicExpr(mem: Memory, isLocal: Boolean): KExpr<out KSort> {
        return IntType().defaultSymbolicExpr(mem, isLocal)
    }

    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory): Symbolic {
        val globalStar = GlobalStarSymbolic(this, IntType().asSymbolic(expr as KExpr<KBv32Sort>, mem))
        return if (fake)
            globalStar.get(mem)
        else
            globalStar
    }

    override fun sort(mem: Memory): KSort = intSort(mem)

    override fun createSymbolic(name: String, mem: Memory, isLocal: Boolean): Symbolic {
        return if (isLocal) {
            println("StarType $elementType")
            LocalStarSymbolic(elementType.createSymbolic(name, mem, true))
        } else {
            val address = mem.addNewStarObject("", elementType.defaultSymbolic(mem, false))
            val addressToAllGlobal = IntType().createSymbolic(name, mem, false)
            // can point to any global object created before
            mem.addCond(
                mem.ctx.mkBvSignedLessOrEqualExpr(
                    addressToAllGlobal.intExpr(),
                    IntType.fromInt(address, mem).intExpr()
                ).toSymbolic(), false
            )
            GlobalStarSymbolic(this, IntType.fromInt(address, mem))
        }
    }

    override fun defaultSymbolic(mem: Memory, isLocal: Boolean): Symbolic {
        return NilLocalStarSymbolic(elementType)
//        //        todo not nil
//        //        todo does not == to all of the old objects
//        return if (isLocal) {
//            LocalStarSymbolic(elementType.defaultSymbolic(mem, true))
//        } else {
////            val address = mem.addNewStarObject("", elementType.defaultSymbolic(mem, false))
//            NilLocalStarSymbolic(elementType)
//        }
    }

    override fun toString() = if (fake) {
        "FakeStar($elementType)"
    } else {
        "Star($elementType)"
    }
}


//class ArraySimpleType(override val elementType: SimpleType) :
//    ArrayType(elementType, null), SimpleType {
//    override fun defaultSymbolicExpr(mem: Memory, isLocal: Boolean): KExpr<out KSort> {
//        return mem.ctx.mkArrayConst(
//            mem.ctx.mkArraySort(IntType().sort(mem), elementType.sort(mem)),
//            elementType.defaultSymbolicExpr(mem, isLocal) as KExpr<KSort>
//        )
//    }
//
//    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory): Symbolic {
//        return InfiniteSimpleArraySymbolic(elementType, expr as KArrayConst<KArraySort<KBv32Sort, KSort>, KSort>)
////        println("ArrayType:asSymbolic")
////        return if (expr.sort == intSort(mem))
////            mem.getStarObject(GlobalStarSymbolic(StarType(elementType), expr as KExpr<KBv32Sort>))
////        else
////            error("should be link")
//    }
//
//    override fun sort(mem: Memory) =
//        mem.ctx.mkArraySort(intSort(mem), elementType.sort(mem))
//
//    override fun toString() = "S[${length ?: "_"}]($elementType)"
//}

open class ArrayType(open val elementType: Type, val length: IntSymbolic? = null) : BaseType() {
    override fun createSymbolic(name: String, mem: Memory, isLocal: Boolean): Symbolic {
        val nonNullLength = length?.int() ?: (mem.addConst("${name}.len", intSort(mem)).toSymbolic().int())
        return FiniteArraySymbolic(elementType, nonNullLength, mem)
    }

    override fun defaultSymbolic(mem: Memory, isLocal: Boolean): Symbolic {
        val nonNullLength = length?.int() ?: IntType().defaultSymbolic(mem, isLocal)
        return FiniteArraySymbolic(elementType, nonNullLength.int(), mem)
    }

    override fun toString() = "[${length ?: "_"}]($elementType)"
}

// methods are unsupported
class NamedType(val name: String, val underlying: StructType) : BaseType() {
    override fun createSymbolic(name: String, mem: Memory, isLocal: Boolean) =
        NamedSymbolic(this, underlying.createSymbolic(name, mem, isLocal))

    override fun defaultSymbolic(mem: Memory, isLocal: Boolean) =
        NamedSymbolic(this, underlying.defaultSymbolic(mem, isLocal).struct())

    override fun toString() = "NAMED:$name"
}

data object FunctionType : BaseType() {
    override fun createSymbolic(name: String, mem: Memory, isLocal: Boolean) =
        error("can't be implemented")

    override fun defaultSymbolic(mem: Memory, isLocal: Boolean) =
        error("can't be implemented")

    override fun toString() = "FUNCTION"
}

//class FakeStarType(val type: Type) : BaseType(), SimpleType {
//    override fun defaultSymbolicExpr(mem: Memory, isLocal: Boolean): KExpr<out KSort> {
//        return IntType().defaultSymbolicExpr(mem, isLocal)
//    }
//
//    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory): Symbolic {
//        TODO("Not yet implemented")
//    }
//
//    override fun sort(mem: Memory) = intSort(mem)
//
//    override fun createSymbolic(name: String, mem: Memory, isLocal: Boolean) =
//        IntType().createSymbolic(name, mem, isLocal)
//
//    override fun defaultSymbolic(mem: Memory, isLocal: Boolean): Symbolic {
//        TODO("Not yet implemented")
//    }
//
//    override fun toString() = "FakeStar($type)"
//}