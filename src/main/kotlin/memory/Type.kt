package memory

import interpreter.ast.AstArrayType
import interpreter.ast.AstIdent
import interpreter.ast.AstStar
import interpreter.ast.AstNode
import interpreter.ssa.*
import io.ksmt.expr.KArrayConst
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFpRoundingMode
import io.ksmt.sort.*
import io.ksmt.utils.asExpr

sealed class BaseType : Type {
    override fun toString() = "TYPE"

    override fun equals(other: Any?): Boolean {
        return getDereferenced().toString() == (other as BaseType).getDereferenced().toString()
    }

    override fun hashCode() = getDereferenced().toString().hashCode()
}

sealed interface Type {
    fun getDereferenced(): Type {
        return when (this) {
            is StarType if(this.fake == false) -> this.elementType.getDereferenced()
            else -> this
        }
    }

    /**
     * creates a new symbolic variable
     */
    fun createSymbolic(name: String, mem: Memory): Symbolic

    /**
     * creates a default value
     */
    fun defaultSymbolic(mem: Memory): Symbolic

    companion object {
        fun toSymbolic(expr: KExpr<out KSort>): Symbolic = when (expr.sort) {
            is KBoolSort -> BoolSymbolic(expr as KExpr<KBoolSort>)
            is KBv8Sort -> Int8Symbolic(expr as KExpr<KBv8Sort>)
            is KBv16Sort -> Int16Symbolic(expr as KExpr<KBv16Sort>)
            is KBv32Sort -> Int32Symbolic(expr as KExpr<KBv32Sort>)
            is KBv64Sort -> Int64Symbolic(expr as KExpr<KBv64Sort>)
            is KFp32Sort -> Float32Symbolic(expr as KExpr<KFp32Sort>)
            is KFp64Sort -> Float64Symbolic(expr as KExpr<KFp64Sort>)
            is KUninterpretedSort -> UninterpretedSymbolic(expr as KExpr<KUninterpretedSort>)
            else -> error("toSymbolic from ${expr.sort}")
        }

        fun fromAst(node: AstNode): Type = when (node) {
            is AstIdent -> fromName(node.name)
            is AstArrayType -> ArrayType(fromAst(node.elementType) as StarType, null)
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
                is LinkSsaType ->
                    fromSsa(node.deLink(), mem, simplify)

                is BasicTypeNode ->
                    return fromName(node.name)

                is ArrayTypeNode ->
                    ArrayType(
                        fromSsa(node.elemType, mem, false),
                        Int64Type().fromInt(node.len, mem)
                    )

                is NamedTypeNode ->
                    if (node.underlying is InterfaceTypeNode)
                        UnknownType
                    else
                        NamedType(
                            node.name,
                            lazy { fromSsa(node.underlying, mem, simplify) as StructType }
                        )

                is PointerTypeNode ->
                    StarType(fromSsa(node.elemType, mem, simplify), false)

                is StructTypeNode ->
                    StructType(
                        node.fields.map {
                            val name = (it as StructFieldNode).name
                            val type = fromSsa(it.elemType, mem, simplify)
                            name to type
                        }
                    )

                is SliceTypeNode -> {
                    val elem = fromSsa(node.elemType, mem, simplify)
                    if (elem == UnknownType)
                        UnknownType
                    else
                        ArrayType(
                            elem,
                            null
                        )
                }

                is AliasTypeNode -> fromSsa(node.rhs, mem, simplify)

//                is InterfaceTypeNode -> UnknownType
                is InterfaceTypeNode -> UninterpretedType("interface")
                is SignatureTypeNode -> TODO()
                is LinkFuncSsa -> TODO()
                is StructFieldNode -> TODO()
                is TupleTypeNode -> TODO()
                is UnknownSsaTypeNode -> TODO()
                is FuncTypeNode -> TODO()
            }
        }

        fun fromName(name: String): Type = when (name.lowercase()) {
            "bool" -> BoolType()
            "byte" -> Int8Type()
            "int8" -> Int8Type()
            "int16" -> Int16Type()
            "int32" -> Int32Type()
            "int64" -> Int64Type()
            "int" -> Int64Type()
            // right now uint are just int
            // use Int_Type(false)
            "uint8" -> Int8Type()
            "uint16" -> Int16Type()
            "uint32" -> Int32Type()
            "uint64" -> Int64Type()
            "uint" -> Int64Type()

            "float32" -> Float32Type()
            "float64" -> Float64Type()
            "complex128" -> ComplexType()
            "string" -> UninterpretedType(String.Companion::class.java.name)
            else -> error("only simple types here, not $name")
        }
    }
}

class StructType(val fields: List<Pair<String, Type>>) : BaseType() {
    override fun createSymbolic(name: String, mem: Memory) =
        StructSymbolic(
            this,
            fields.associate { (name, type) -> name to type.defaultSymbolic(mem) }.toMutableMap()
        )

    override fun defaultSymbolic(mem: Memory): Symbolic {
        return StructSymbolic(
            this,
            fields.associate { (name, type) ->
                name to type.defaultSymbolic(mem)
            }.toMutableMap()
        )
    }

    override fun toString() = "Struct($fields)"
}

// types that can be represented with KExpr
sealed interface SimpleType : Type {
    fun createSymbolicExpr(name: String, mem: Memory): KExpr<out KSort>
    fun defaultSymbolicExpr(mem: Memory): KExpr<out KSort>

    fun asSymbolic(expr: KExpr<out KSort>, mem: Memory): Symbolic

    fun sort(mem: Memory): KSort
}

class BoolType : BaseType(), SimpleType {
    override fun createSymbolicExpr(name: String, mem: Memory): KExpr<out KSort> =
        createSymbolic(name, mem).expr

    override fun defaultSymbolicExpr(mem: Memory): KExpr<out KSort> =
        defaultSymbolic(mem).expr

    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory) =
        BoolSymbolic(expr as KExpr<KBoolSort>)

    override fun sort(mem: Memory) = with(mem.ctx) { boolSort }

    override fun createSymbolic(name: String, mem: Memory): BoolSymbolic = with(mem.ctx) {
        mem.addConst(name, mkBoolSort()).asExpr(sort(mem)).toBoolSymbolic()
    }

    override fun defaultSymbolic(mem: Memory) = with(mem.ctx) {
        false.expr.toBoolSymbolic()
    }

    override fun toString() = "BOOL"

    companion object {
        fun `true`(mem: Memory) = fromBool(true, mem)
        fun `false`(mem: Memory) = fromBool(false, mem)
        fun fromBool(bool: Boolean, mem: Memory) = with(mem.ctx) { mkBool(bool).toBoolSymbolic() }
    }
}

sealed class IntType(val hasSign: Boolean = true) : BaseType(), SimpleType {
    fun zeroExpr(mem: Memory): KExpr<KBvSort> = fromInt(0, mem).expr as KExpr<KBvSort>
    fun zero(mem: Memory): IntSymbolic = fromInt(0, mem)
    abstract fun fromInt(int: Long, mem: Memory): IntSymbolic

    override fun createSymbolic(name: String, mem: Memory): IntSymbolic = with(mem.ctx) {
        mem.addConst(name, sort(mem)).let { Type.toSymbolic(it) }.int(mem)
    }

    override fun defaultSymbolic(mem: Memory) =
        zero(mem)

    override fun createSymbolicExpr(name: String, mem: Memory): KExpr<out KSort> =
        createSymbolic(name, mem).expr

    override fun defaultSymbolicExpr(mem: Memory): KExpr<out KSort> =
        defaultSymbolic(mem).expr

    abstract override fun sort(mem: Memory): KBvSort

    fun hasSignString() =
        if (hasSign)
            ""
        else
            "U"
}

open class Int64Type(hasSign: Boolean = true) : IntType(hasSign) {
    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory) =
        Int64Symbolic(expr as KExpr<KBv64Sort>)

    override fun fromInt(int: Long, mem: Memory): Int64Symbolic =
        Int64Symbolic(mem.ctx.mkBv(int, sort(mem)) as KExpr<KBv64Sort>)

    override fun sort(mem: Memory): KBvSort =
        mem.ctx.mkBv64Sort()

    override fun toString() =
        "${hasSignString()}INT64"
}

class Int32Type(hasSign: Boolean = true) : IntType(hasSign) {
    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory) =
        Int32Symbolic(expr as KExpr<KBv32Sort>)

    override fun fromInt(int: Long, mem: Memory) =
        Int32Symbolic(mem.ctx.mkBv(int, sort(mem)) as KExpr<KBv32Sort>)

    override fun sort(mem: Memory): KBvSort =
        mem.ctx.mkBv32Sort()

    override fun toString() =
        "${hasSignString()}INT32"
}

class Int16Type(hasSign: Boolean = true) : IntType(hasSign) {
    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory) =
        Int16Symbolic(expr as KExpr<KBv16Sort>)

    override fun fromInt(int: Long, mem: Memory) =
        Int16Symbolic(mem.ctx.mkBv(int, sort(mem)) as KExpr<KBv16Sort>)

    override fun sort(mem: Memory): KBvSort =
        mem.ctx.mkBv16Sort()

    override fun toString() =
        "${hasSignString()}INT16"
}

class Int8Type(hasSign: Boolean = true) : IntType(hasSign) {
    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory) =
        Int8Symbolic(expr as KExpr<KBv8Sort>)

    override fun fromInt(int: Long, mem: Memory) =
        Int8Symbolic(mem.ctx.mkBv(int, sort(mem)) as KExpr<KBv8Sort>)

    override fun sort(mem: Memory): KBvSort =
        mem.ctx.mkBv8Sort()

    override fun toString() =
        "${hasSignString()}INT8"
}

class AddressType : Int64Type()

sealed class FloatType : BaseType(), SimpleType {
    fun zero(mem: Memory) = fromDouble(0.0, mem)
    abstract fun fromDouble(double: Double, mem: Memory): FloatSymbolic

    override fun createSymbolic(name: String, mem: Memory) = with(mem.ctx) {
        mem.addConst(name, sort(mem)).let { Type.toSymbolic(it) }.float(mem)
    }

    override fun defaultSymbolic(mem: Memory) =
        zero(mem)

    override fun createSymbolicExpr(name: String, mem: Memory): KExpr<out KSort> =
        createSymbolic(name, mem).expr

    override fun defaultSymbolicExpr(mem: Memory): KExpr<out KSort> =
        defaultSymbolic(mem).expr

    fun roundingMode(mem: Memory) = mem.ctx.mkFpRoundingModeExpr(KFpRoundingMode.RoundTowardZero)

    abstract override fun sort(mem: Memory): KFpSort
}

class Float32Type : FloatType() {
    fun round(int: IntSymbolic, mem: Memory): FloatSymbolic = with(mem.ctx) {
        Float32Symbolic(
            mkBvToFpExpr(
                mkFp32Sort(), roundingMode(mem), int.expr as KExpr<KBvSort>, true
            )
        )
    }

    override fun fromDouble(double: Double, mem: Memory) =
        Float32Symbolic(mem.ctx.mkFp(double, sort(mem)) as KExpr<KFp32Sort>)

    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory) =
        Float32Symbolic(expr as KExpr<KFp32Sort>)

    override fun sort(mem: Memory) = mem.ctx.mkFp32Sort()

    override fun toString() = "FLOAT32"
}

class Float64Type : FloatType() {
    fun round(int: IntSymbolic, mem: Memory): FloatSymbolic = with(mem.ctx) {
        Float64Symbolic(
            mkBvToFpExpr(
                mkFp64Sort(), roundingMode(mem), int.expr as KExpr<KBvSort>, true
            )
        )
    }

    override fun fromDouble(double: Double, mem: Memory) =
        Float64Symbolic(mem.ctx.mkFp(double, sort(mem)) as KExpr<KFp64Sort>)

    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory) =
        Float64Symbolic(expr as KExpr<KFp64Sort>)

    override fun sort(mem: Memory) = mem.ctx.mkFp64Sort()

    override fun toString() = "FLOAT64"
}

class ComplexType : BaseType() {
    override fun createSymbolic(name: String, mem: Memory) =
        ComplexSymbolic(
            mem.addConst("$name.Real", Float64Type().sort(mem)) as KExpr<KFp64Sort>,
            mem.addConst("$name.Img", Float64Type().sort(mem)) as KExpr<KFp64Sort>
        )

    override fun defaultSymbolic(mem: Memory) =
        ComplexSymbolic(
            Float64Type().zero(mem).expr as KExpr<KFp64Sort>,
            Float64Type().zero(mem).expr as KExpr<KFp64Sort>
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

    override fun createSymbolicExpr(name: String, mem: Memory): KExpr<out KSort> =
        createSymbolic(name, mem).expr

    override fun defaultSymbolicExpr(mem: Memory): KExpr<out KSort> =
        defaultSymbolic(mem).expr

    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory) =
        UninterpretedSymbolic(expr as KExpr<KUninterpretedSort>)

    override fun sort(mem: Memory) =
        mem.ctx.mkUninterpretedSort(typeName)

    override fun createSymbolic(name: String, mem: Memory) =
        UninterpretedSymbolic(mem.ctx.mkConst(name, sort(mem)))

    override fun defaultSymbolic(mem: Memory) =
        UninterpretedSymbolic(mem.ctx.mkConst("nil", sort(mem)))

    override fun toString() = "UNINTERPRETED"
}


/** fake star is a star that should be dissolved immediately on get
 *
 *  it is used to get arrays of non-simple types working
 */
class StarType(val elementType: Type, val fake: Boolean) : BaseType(), SimpleType {
    override fun createSymbolicExpr(name: String, mem: Memory): KExpr<out KSort> =
        AddressType().createSymbolicExpr(name, mem)

    override fun defaultSymbolicExpr(mem: Memory): KExpr<out KSort> {
        return AddressType().defaultSymbolicExpr(mem)
    }

    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory): Symbolic {
        return GlobalStarSymbolic("", this, Int64Symbolic(expr as KExpr<KBv64Sort>), fake)
    }

    override fun sort(mem: Memory): KSort = AddressType().sort(mem)

    override fun createSymbolic(name: String, mem: Memory): Symbolic {
        val address = mem.addNewStarObject("", elementType.defaultSymbolic(mem))
        val addressToAllGlobal = AddressType().createSymbolic(name, mem).int64(mem)

        mem.addCond(
            mem.ctx.mkBvSignedLessOrEqualExpr(
                addressToAllGlobal.expr,
                address.expr
            ).toBoolSymbolic(), false
        )
        return GlobalStarSymbolic("", this, address, false)
    }

    override fun defaultSymbolic(mem: Memory): Symbolic {
        return NilLocalStarSymbolic(elementType, false)
    }

    override fun toString() = if (fake) {
        "F($elementType)"
    } else {
        "*($elementType)"
    }
}

class ArraySimpleType(override val elementType: SimpleType, length: Int64Symbolic?) :
    ArrayType(elementType, length), SimpleType {

    override fun createSymbolicExpr(
        name: String,
        mem: Memory
    ): KExpr<out KSort> =
        mem.ctx.mkArrayConst(
            mem.ctx.mkArraySort(AddressType().sort(mem), elementType.sort(mem)),
            elementType.createSymbolicExpr(name, mem) as KExpr<KSort>
        )

    override fun defaultSymbolicExpr(mem: Memory): KExpr<out KSort> =
        mem.ctx.mkArrayConst(
            mem.ctx.mkArraySort(AddressType().sort(mem), elementType.sort(mem)),
            elementType.defaultSymbolicExpr(mem) as KExpr<KSort>
        )

    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory): Symbolic =
        InfiniteArraySymbolic(
            StarType(elementType, true),
            expr as KArrayConst<KArraySort<KBv64Sort, KSort>, KSort>
        )

    override fun sort(mem: Memory): KSort =
        mem.ctx.mkArraySort(AddressType().sort(mem), elementType.sort(mem))

    override fun toString() = "[]($elementType)"
}

open class ArrayType(open val elementType: Type, val length: Int64Symbolic?) :
    BaseType() {

    override fun createSymbolic(name: String, mem: Memory): Symbolic {
        val nonNullLength =
            length ?: (mem.addConst("${name}.len", AddressType().sort(mem)).let { Type.toSymbolic(it) }).int64(mem)
        return FiniteArraySymbolic(elementType, nonNullLength, mem, false)
    }

    override fun defaultSymbolic(mem: Memory): Symbolic {
        val nonNullLength = length ?: AddressType().defaultSymbolic(mem).int64(mem)
        return FiniteArraySymbolic(elementType, nonNullLength, mem, true)
    }

    fun toSimple(): SimpleType =
        ArraySimpleType(elementType as? SimpleType ?: StarType(elementType, true), length)
//    todo star should be fake?

    override fun toString() = "[]($elementType)"
}

/**
 * methods are unsupported
 *
 * underlying is initialized lazily to support recursive structures
 */
class NamedType(val name: String, underlyingLazy: Lazy<StructType>) : BaseType() {
    val underlying: StructType by underlyingLazy

    override fun createSymbolic(name: String, mem: Memory) =
        NamedSymbolic(this, underlying.createSymbolic(name, mem))

    override fun defaultSymbolic(mem: Memory) =
        NamedSymbolic(this, underlying.defaultSymbolic(mem).struct(mem))

    override fun toString() = "NAMED:$name"
}

data object UnknownType : BaseType() {
    override fun createSymbolic(name: String, mem: Memory) =
        error("can't be implemented")

    override fun defaultSymbolic(mem: Memory) =
        error("can't be implemented")

    override fun toString() = "UNKNOWN TYPE"
}