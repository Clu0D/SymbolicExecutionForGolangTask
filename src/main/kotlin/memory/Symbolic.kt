package memory

import io.ksmt.expr.KExpr
import io.ksmt.sort.*

fun KExpr<KBoolSort>.toBoolSymbolic(): BoolSymbolic =
    BoolSymbolic(this)

fun KExpr<KBvSort>.toIntSymbolic(): IntSymbolic = when (this.sort) {
    is KBv64Sort -> Int64Symbolic(this as KExpr<KBv64Sort>)
    is KBv32Sort -> Int32Symbolic(this as KExpr<KBv32Sort>)
    is KBv16Sort -> Int16Symbolic(this as KExpr<KBv16Sort>)
    is KBv8Sort -> Int8Symbolic(this as KExpr<KBv8Sort>)
    else -> error("unsupported")
}

fun KExpr<KFpSort>.toFloatSymbolic(): FloatSymbolic = when (this.sort) {
    is KFp64Sort -> Float64Symbolic(this as KExpr<KFp64Sort>)
    is KFp32Sort -> Float32Symbolic(this as KExpr<KFp32Sort>)
    else -> error("unsupported")
}

interface MemoryObject {
    fun toSymbolic(arrayBehaviour: ArrayBehaviour, mem: Memory): Symbolic {
        return when (this) {
            is InfAbstractArray -> FiniteArraySymbolic(
                ArrayType(
                    this.arrayType.elementType,
                    arrayBehaviour.getLen(mem)
                ), this, arrayBehaviour, mem
            )

            else -> this as Symbolic
        }
    }
}

open class Symbolic(val type: Type) : MemoryObject {
    /**
     * only simple values
     * local stars will be added to Global
     */
    fun toExpr(mem: Memory): KExpr<KSort> {
        return when {
            this is BoolSymbolic -> this.expr as KExpr<KSort>
            this is Int8Symbolic -> this.expr as KExpr<KSort>
            this is Int16Symbolic -> this.expr as KExpr<KSort>
            this is Int32Symbolic -> this.expr as KExpr<KSort>
            this is Int64Symbolic -> this.expr as KExpr<KSort>
            type is FloatType -> this.floatExpr(mem) as KExpr<KSort>
            type is UninterpretedType -> this.uninterpretedExpr(mem) as KExpr<KSort>
            this is GlobalStarSymbolic -> this.address.expr as KExpr<KSort>
            this is LocalStarSymbolic -> this.toGlobal(mem).address.expr as KExpr<KSort>
            this is NilLocalStarSymbolic -> Int64Type().zeroExpr(mem) as KExpr<KSort>
            else -> TODO("${this.javaClass.name}")
        }
    }

    fun bool(mem: Memory): BoolSymbolic = this as BoolSymbolic

    fun boolExpr(mem: Memory): KExpr<KBoolSort> = bool(mem).expr

    fun int(mem: Memory): IntSymbolic = this as IntSymbolic

    fun int64(mem: Memory): Int64Symbolic = this as Int64Symbolic

    fun intExpr(mem: Memory): KExpr<KBvSort> = int(mem).expr as KExpr<KBvSort>

    fun float(mem: Memory): FloatSymbolic = this as FloatSymbolic

    fun float64(mem: Memory): Float64Symbolic = this as Float64Symbolic

    fun list(mem: Memory): ListSymbolic = this as ListSymbolic

    fun floatExpr(mem: Memory): KExpr<KFpSort> = when (val x = this) {
        is IntSymbolic -> Float64Type().round(x, mem).expr as KExpr<KFpSort>
        is Float64Symbolic -> x.expr as KExpr<KFpSort>
        is Float32Symbolic -> x.expr as KExpr<KFpSort>
        else -> error("can't cast $this to fp64Expr")
    }

    fun star(mem: Memory) = this as StarSymbolic

    fun complex(mem: Memory) = this as ComplexSymbolic

    fun uninterpretedExpr(mem: Memory) = (this as UninterpretedSymbolic).expr

    fun array(mem: Memory): FiniteArraySymbolic = this as FiniteArraySymbolic

    fun struct(mem: Memory) = this as StructSymbolic

    fun named(mem: Memory) = this as NamedSymbolic

    override fun toString() = type.toString()
}

class ListSymbolic(val list: List<Symbolic>) : Symbolic(ListType(list.map { it.type })) {
    override fun toString(): String {
        return list.joinToString(" ")
    }
}

class ListType(val list: List<Type>) : Type {
    override fun createSymbolic(name: String, mem: Memory): Symbolic {
        error("ListType is used only as call return")
    }

    override fun defaultSymbolic(mem: Memory): Symbolic {
        error("ListType is used only as call return")
    }

    override fun toString() = list.joinToString(", ", "{", "}")
}

open class SimpleSymbolic(type: Type) : Symbolic(type)

class BoolSymbolic(val expr: KExpr<KBoolSort>) : SimpleSymbolic(BoolType()) {
    fun not(mem: Memory) = BoolSymbolic(mem.ctx.mkNot(expr))
    override fun toString() = "bool $expr"
}

abstract class IntSymbolic(open val expr: KExpr<out KBvSort>, val intType: IntType) : SimpleSymbolic(intType) {
    fun hasSign() = intType.hasSign
    override fun toString() = "$type $expr"
}

class Int64Symbolic(override val expr: KExpr<KBv64Sort>) : IntSymbolic(expr, Int64Type())

class Int32Symbolic(override val expr: KExpr<KBv32Sort>) : IntSymbolic(expr, Int32Type())

class Int16Symbolic(override val expr: KExpr<KBv16Sort>) : IntSymbolic(expr, Int16Type())

class Int8Symbolic(override val expr: KExpr<KBv8Sort>) : IntSymbolic(expr, Int8Type())

abstract class FloatSymbolic(open val expr: KExpr<out KFpSort>, floatType: FloatType) : SimpleSymbolic(floatType) {
    override fun toString() = "$type $expr"
}

class Float32Symbolic(override val expr: KExpr<KFp32Sort>) : FloatSymbolic(expr, Float32Type()) {
    override fun toString() = "float32 $expr"
}

class Float64Symbolic(override val expr: KExpr<KFp64Sort>) : FloatSymbolic(expr, Float64Type()) {
    override fun toString() = "float64 $expr"
}

class ComplexSymbolic(real: KExpr<KFp64Sort>, img: KExpr<KFp64Sort>) : SimpleSymbolic(ComplexType()) {
    val real = Float64Symbolic(real)
    val img = Float64Symbolic(img)
    override fun toString() = "complex $real $img"
}

open class UninterpretedSymbolic(val expr: KExpr<KUninterpretedSort>) :
    SimpleSymbolic(UninterpretedType(expr.sort.name)) {

    override fun toString() = expr.sort.name
}

sealed class StarSymbolic(val field: String, val starType: StarType) :
    Symbolic(StarType(starType.elementType)) {

    abstract fun address(mem: Memory): IntSymbolic

    abstract fun toGlobal(mem: Memory): GlobalStarSymbolic

    abstract fun get(mem: Memory): Symbolic
    open fun put(value: Symbolic, mem: Memory): StarSymbolic {
        return this.toGlobal(mem).put(value, mem)
    }

    fun eq(other: StarSymbolic, mem: Memory): BoolSymbolic {
        return with(mem.ctx) {
            BoolSymbolic(toGlobal(mem).address.expr eq other.toGlobal(mem).address.expr)
        }
    }

    open fun findField(field: Int, mem: Memory): Symbolic {
        val namedType = when (starType.elementType) {
            is NamedType -> starType.elementType
            is StarType -> starType.elementType.elementType
            else -> error("only named types have fields, not ${starType.elementType}")
        } as NamedType

        val (name, type) = namedType.underlying.fields[field]
        return toFieldStar("${namedType.name}:${name}", type, mem)
    }

    abstract fun toFieldStar(name: String, type: Type, mem: Memory): StarSymbolic
}

class LocalStarSymbolic(
    private var symbolic: Symbolic,
    field: String
) :
    StarSymbolic(field, StarType(symbolic.type)) {
    override fun address(mem: Memory): IntSymbolic =
        toGlobal(mem).address

    override fun toGlobal(mem: Memory): GlobalStarSymbolic {
        val starType = StarType(symbolic.type)
        val globalAddress = mem.addNewDefaultStar(field, starType.elementType)
        val isSymbolic = BoolType.`false`(mem)

        mem.putStar(field, symbolic.type, globalAddress, symbolic, isSymbolic)
        return GlobalStarSymbolic(starType, globalAddress, isSymbolic, field)
    }

    override fun get(mem: Memory): Symbolic {
        if (symbolic is FiniteArraySymbolic)
            (symbolic as FiniteArraySymbolic).addressToSelf = this
        return symbolic
    }

    override fun toFieldStar(name: String, type: Type, mem: Memory): LocalStarSymbolic {
        return LocalStarSymbolic((symbolic.named(mem)).underlying.fields[name]!!, name)
    }

    override fun toString() =
        "L*(${starType.elementType})"
}

class NilLocalStarSymbolic(
    starType: StarType,
    field: String
) : StarSymbolic(field, starType) {
    override fun address(mem: Memory): IntSymbolic =
        Int64Type().zero(mem)

    override fun toGlobal(mem: Memory): GlobalStarSymbolic {
        return GlobalStarSymbolic(starType, Int64Type().zero(mem).int64(mem), BoolType.`false`(mem), field)
    }

    override fun findField(field: Int, mem: Memory): Symbolic {
        mem.addError(
            (BoolType.`true`(mem)),
            "something done with null"
        )
        return StopSymbolic
    }

    override fun get(mem: Memory): Symbolic {
        mem.addError(
            (BoolType.`true`(mem)),
            "something done with null"
        )
        return StopSymbolic
    }

    override fun put(value: Symbolic, mem: Memory): StarSymbolic {
        mem.addError(
            (BoolType.`true`(mem)),
            "something done with null"
        )
        error("error gives an exception")
    }

    override fun toFieldStar(name: String, type: Type, mem: Memory): StarSymbolic {
        mem.addError(
            (BoolType.`true`(mem)),
            "something done with null"
        )
        error("error gives an exception")
    }

    override fun toString() =
        "nil"
}

class GlobalStarSymbolic(
    starType: StarType,
    val address: Int64Symbolic,
    val isSymbolic: BoolSymbolic,
    field: String
) : StarSymbolic(field, starType) {
    override fun address(mem: Memory): IntSymbolic =
        address

    override fun toGlobal(mem: Memory): GlobalStarSymbolic =
        GlobalStarSymbolic(starType, address, isSymbolic, field)

    override fun get(mem: Memory): Symbolic {
        val get = mem.getStar(field, starType.elementType, address, isSymbolic)
        if (get is FiniteArraySymbolic)
            get.addressToSelf = this

        if (mem.print > 2) {
            println("GET address $address $starType")
            when (get) {
                is FiniteArraySymbolic -> println("GET len  ${get.arrayType.length}")
                is GlobalStarSymbolic -> println("GET address ${get.address}")
                else -> println("GET $get")
            }
        }
        return get
    }

    override fun put(value: Symbolic, mem: Memory): StarSymbolic {
        if (mem.print > 2) {
            println("PUT address $address $starType")
            when (value) {
                is FiniteArraySymbolic -> println("PUT len  ${value.arrayType.length}")
                is GlobalStarSymbolic -> println("PUT address ${value.address}")
                else -> println("PUT $value")
            }
        }
        mem.putStar(field, starType.elementType, address, value, isSymbolic)
        return this
    }

    override fun toFieldStar(name: String, type: Type, mem: Memory): StarSymbolic {
        return GlobalStarSymbolic(StarType(type), address, isSymbolic, name)
    }

    override fun toString() =
        "G*${starType.elementType}"
}

class StructSymbolic(
    type: StructType,
    val fields: Map<String, Symbolic>
) : Symbolic(type) {
    override fun toString() = "Struct(${fields.keys})"
}

class NamedSymbolic(type: NamedType, val underlying: StructSymbolic) : Symbolic(type) {
    override fun toString() = "named $type $underlying"
}

object StopSymbolic : Symbolic(UnknownType)