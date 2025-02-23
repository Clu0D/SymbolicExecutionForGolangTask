package memory

import interpreter.ssa.*
import io.ksmt.expr.KExpr
import io.ksmt.expr.KFpRoundingMode
import io.ksmt.sort.*
import io.ksmt.utils.asExpr

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
    fun createSymbolic(name: String, mem: Memory): Symbolic

    /**
     * creates a default value
     */
    fun defaultSymbolic(mem: Memory): Symbolic

    fun toSimple(): SimpleType =
        when (this) {
            is SimpleType -> this
            else -> StarType(this)
        }

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

        fun fromSsa(node: SsaType, mem: Memory): Type {
            return when (node) {
                is LinkSsaType ->
                    fromSsa(node.deLink(), mem)

                is BasicTypeNode ->
                    return fromName(node.name)

                is ArrayTypeNode ->
                    ArrayType(
                        fromSsa(node.elemType, mem),
                        Int64Type().fromInt(node.len, mem)
                    )

                is NamedTypeNode ->
                    if (node.underlying is InterfaceTypeNode)
                        UnknownType
                    else
                        NamedType(
                            node.name,
                            lazy { fromSsa(node.underlying, mem) as StructType }
                        )

                is PointerTypeNode ->
                    StarType(fromSsa(node.elemType, mem))

                is StructTypeNode ->
                    StructType(
                        node.fields.map {
                            val name = (it as StructFieldNode).name
                            val type = fromSsa(it.elemType, mem)
                            name to type
                        }
                    )

                is SliceTypeNode -> {
                    val elem = fromSsa(node.elemType, mem)
                    if (elem == UnknownType)
                        UnknownType
                    else
                        InfArrayType(elem)
                }

                is AliasTypeNode -> fromSsa(node.rhs, mem)

//                is InterfaceTypeNode -> UnknownType
                is InterfaceTypeNode -> UninterpretedType("interface")
                else -> TODO()
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

/**
 * types that can be represented with KExpr
 */
sealed interface SimpleType : Type, FiniteType {
    fun createSymbolicExpr(name: String, mem: Memory): KExpr<out KSort>
    fun defaultSymbolicExpr(mem: Memory): KExpr<out KSort>

    fun asSymbolic(expr: KExpr<out KSort>, mem: Memory): Symbolic

    fun sort(mem: Memory): KSort
}

/**
 * SimpleTypes, that are not Star
 */
sealed interface NonStarType : SimpleType

/**
 * types that are not InfArrayType
 */
interface FiniteType : Type

class BoolType : BaseType(), SimpleType, NonStarType {
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

sealed class IntType(val hasSign: Boolean = true) : BaseType(), NonStarType {
    open fun zeroExpr(mem: Memory): KExpr<KBvSort> = fromInt(0, mem).expr as KExpr<KBvSort>
    fun zero(mem: Memory): IntSymbolic = fromInt(0, mem)
    abstract fun fromInt(int: Long, mem: Memory): IntSymbolic

    abstract fun size(): Int

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

    companion object {
        /**
         * just assuming it is signed
         *
         * and fromSize < toSize
         */
        fun cast(int: Symbolic, toType: IntType, mem: Memory): IntSymbolic {
            return with(mem.ctx) {
                val expr = int.intExpr(mem)
                val fromSize = (int.type as IntType).size()
                val toSize = toType.size()

                if (fromSize == toSize)
                    int as IntSymbolic
                else {
                    val signBit = mkBvExtractExpr(fromSize - 1, fromSize - 1, expr)
                    val signExt = mkBvRepeatExpr(toSize - fromSize, signBit)
                    val concatExpr = mkBvConcatExpr(signExt, expr)
                    toType.asSymbolic(concatExpr, mem) as IntSymbolic
                }
            }
        }
    }
}

open class Int64Type(hasSign: Boolean = true) : IntType(hasSign) {
    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory) =
        Int64Symbolic(expr as KExpr<KBv64Sort>)

    override fun fromInt(int: Long, mem: Memory): Int64Symbolic =
        Int64Symbolic(mem.ctx.mkBv(int, sort(mem)) as KExpr<KBv64Sort>)

    override fun size(): Int = 64

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

    override fun size(): Int = 32

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

    override fun size(): Int = 16

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

    override fun size(): Int = 8

    override fun sort(mem: Memory): KBvSort =
        mem.ctx.mkBv8Sort()

    override fun toString() =
        "${hasSignString()}INT8"
}

sealed class FloatType : BaseType(), NonStarType {
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

class ComplexType : BaseType(), FiniteType {
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

// todo is it SimpleType?
class UninterpretedType(val typeName: String) : BaseType(), NonStarType, FiniteType {
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

class StarType(val elementType: Type) : BaseType(), SimpleType {
    override fun createSymbolicExpr(name: String, mem: Memory): KExpr<out KSort> =
        Int64Type().createSymbolicExpr(name, mem)

    override fun defaultSymbolicExpr(mem: Memory): KExpr<out KSort> {
        return Int64Type().defaultSymbolicExpr(mem)
    }

    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory): Symbolic {
        TODO()
//        return GlobalStarSymbolic(this, Int64Symbolic(expr as KExpr<KBv64Sort>), arrayBehaviour)
    }

    override fun sort(mem: Memory): KSort = Int64Type().sort(mem)

    override fun createSymbolic(name: String, mem: Memory): Symbolic {
        val address = mem.addNewSymbolicStar("", elementType, true, name)

        return GlobalStarSymbolic(this, address, BoolType.`true`(mem), "")
    }

    override fun defaultSymbolic(mem: Memory): Symbolic {
        return NilLocalStarSymbolic(this, "")
    }

    override fun toString() =
        "*$elementType"
}

interface ArrayAbstractType: Type {
    fun elementType(): Type
}

//class ArraySimpleType(elementType: SimpleType, length: Int64Symbolic) :
//    ArrayType(elementType, length), SimpleType {
//
//    override fun createSymbolicExpr(
//        name: String,
//        mem: Memory
//    ): KExpr<out KSort> =
//        mem.ctx.mkArrayConst(
//            mem.ctx.mkArraySort(Int64Type().sort(mem), (elementType() as SimpleType).sort(mem)),
//            (elementType() as SimpleType).createSymbolicExpr(name, mem) as KExpr<KSort>
//        )
//
//    override fun defaultSymbolicExpr(mem: Memory): KExpr<out KSort> =
//        mem.ctx.mkArrayConst(
//            mem.ctx.mkArraySort(Int64Type().sort(mem), (elementType() as SimpleType).sort(mem)),
//            (elementType() as SimpleType).defaultSymbolicExpr(mem) as KExpr<KSort>
//        )
//
//    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory): Symbolic =
//        error("should not be used, as there is not enough information about symbolicName or isFake")
//
//    override fun sort(mem: Memory): KSort =
//        mem.ctx.mkArraySort(Int64Type().sort(mem), (elementType() as SimpleType).sort(mem))
//}

open class InfArrayType(var elementType: Type) : BaseType(), ArrayAbstractType {
    override fun createSymbolic(name: String, mem: Memory): Symbolic {
        return ArrayType(
            elementType,
            Int64Type().createSymbolic("$name:len", mem).int64(mem)
        ).createSymbolic(name, mem)
    }

    override fun defaultSymbolic(mem: Memory): Symbolic {
        return ArrayType(
            elementType,
            Int64Type().defaultSymbolic(mem).int64(mem)
        ).defaultSymbolic(mem)
    }

    override fun toString() =
        "[$elementType]"

    override fun elementType() = elementType
}

class InfArraySimpleType(elementType: SimpleType) :
    InfArrayType(elementType), SimpleType {

    override fun createSymbolicExpr(
        name: String,
        mem: Memory
    ): KExpr<out KSort> =
        mem.ctx.mkArrayConst(
            mem.ctx.mkArraySort(Int64Type().sort(mem), (elementType() as SimpleType).sort(mem)),
            (elementType() as SimpleType).createSymbolicExpr(name, mem) as KExpr<KSort>
        )

    override fun defaultSymbolicExpr(mem: Memory): KExpr<out KSort> =
        mem.ctx.mkArrayConst(
            mem.ctx.mkArraySort(Int64Type().sort(mem), (elementType() as SimpleType).sort(mem)),
            (elementType() as SimpleType).defaultSymbolicExpr(mem) as KExpr<KSort>
        )

    override fun asSymbolic(expr: KExpr<out KSort>, mem: Memory): Symbolic =
        error("should not be used, as there is not enough information about symbolicName or isFake")

    override fun sort(mem: Memory): KSort =
        mem.ctx.mkArraySort(Int64Type().sort(mem), (elementType() as SimpleType).sort(mem))
}

open class ArrayType(var elementType: Type, val length: Int64Symbolic) : BaseType(), FiniteType,
    ArrayAbstractType {
    override fun createSymbolic(name: String, mem: Memory): Symbolic {
        val address = mem.addNewSymbolicStar("", elementType, true, name)

        return GlobalStarSymbolic(StarType(this), address, BoolType.`true`(mem), "")
    }

    override fun defaultSymbolic(mem: Memory): Symbolic {
        return FiniteArraySymbolic(
            ArrayType(
                elementType,
                length
            ),
            CombinedArrayBehaviour("!nil!", BoolType.`false`(mem)),
            mem
        )
    }

    override fun toString() =
        "[$elementType]"

    override fun elementType() = elementType
}

/**
 * methods are unsupported
 *
 * underlying is initialized lazily to support recursive structures
 */
class NamedType(val name: String, underlyingLazy: Lazy<StructType>) : BaseType(), FiniteType {
    val underlying: StructType by underlyingLazy

    override fun createSymbolic(name: String, mem: Memory) =
        NamedSymbolic(this, underlying.createSymbolic(name, mem))

    override fun defaultSymbolic(mem: Memory) =
        NamedSymbolic(this, underlying.defaultSymbolic(mem).struct(mem))

    override fun toString() = "NAMED:$name"
}

data object UnknownType : BaseType(), FiniteType {
    override fun createSymbolic(name: String, mem: Memory) =
        error("can't be implemented")

    override fun defaultSymbolic(mem: Memory) =
        error("can't be implemented")

    override fun toString() = "UNKNOWN TYPE"
}