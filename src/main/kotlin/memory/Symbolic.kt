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

open class Symbolic(val type: Type) {
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
            this is InfiniteArraySymbolic -> this.arrayExpr as KExpr<KSort>
            this is GlobalStarSymbolic -> this.address.expr as KExpr<KSort>
            this is LocalStarSymbolic -> this.toGlobal(mem).address.expr as KExpr<KSort>
            this is NilLocalStarSymbolic -> AddressType().zeroExpr(mem) as KExpr<KSort>
            else -> error("should be simple type $this ${this.type}")
        }
    }

    fun getDereferenced(mem: Memory): Symbolic =
        when {
            this is StarSymbolic && this.fake -> this.get(mem)
            else -> this
        }

    fun bool(mem: Memory): BoolSymbolic = getDereferenced(mem) as BoolSymbolic

    fun boolExpr(mem: Memory): KExpr<KBoolSort> = bool(mem).expr

    fun int(mem: Memory): IntSymbolic = getDereferenced(mem) as IntSymbolic

    fun int64(mem: Memory): Int64Symbolic = getDereferenced(mem) as Int64Symbolic

    fun intExpr(mem: Memory): KExpr<KBvSort> = int(mem).expr as KExpr<KBvSort>

    fun float(mem: Memory): FloatSymbolic = getDereferenced(mem) as FloatSymbolic

    fun float64(mem: Memory): Float64Symbolic = getDereferenced(mem) as Float64Symbolic

    fun list(mem: Memory): ListSymbolic = getDereferenced(mem) as ListSymbolic

    fun floatExpr(mem: Memory): KExpr<KFpSort> = when (val x = this.getDereferenced(mem)) {
        is IntSymbolic -> Float64Type().round(x, mem) as KExpr<KFpSort>
        is Float64Symbolic -> x.expr as KExpr<KFpSort>
        is Float32Symbolic -> x.expr as KExpr<KFpSort>
        else -> error("can't cast $this to fp64Expr")
    }

    fun star(mem: Memory) = getDereferenced(mem) as StarSymbolic

    fun complex(mem: Memory) = getDereferenced(mem) as ComplexSymbolic

    fun uninterpretedExpr(mem: Memory) = (getDereferenced(mem) as UninterpretedSymbolic).expr

    fun array(mem: Memory): FiniteArraySymbolic = getDereferenced(mem) as FiniteArraySymbolic

    fun struct(mem: Memory) = getDereferenced(mem) as StructSymbolic

    fun named(mem: Memory) = getDereferenced(mem) as NamedSymbolic

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

sealed class StarSymbolic(val elementType: Type, val fake: Boolean) : Symbolic(StarType(elementType, fake)) {

    abstract fun toGlobal(mem: Memory): GlobalStarSymbolic

    abstract fun dereference(): StarSymbolic

    abstract fun get(mem: Memory): Symbolic
    abstract fun put(value: Symbolic, mem: Memory): Boolean

    abstract fun eq(other: StarSymbolic, mem: Memory): BoolSymbolic

    open fun findField(field: Int, mem: Memory): Symbolic {
        val namedType = when (val dereferenced = elementType.getDereferenced()) {
            is NamedType -> dereferenced
            is StarType -> dereferenced.elementType
            else -> error("only named types have fields, not $elementType")
        } as NamedType

        val (name, type) = namedType.underlying.fields[field]
        return toFieldStar(name, type, mem)
    }

    abstract fun toFieldStar(name: String, type: Type, mem: Memory): Symbolic
}

class ArrayStarSymbolic(val address: Int64Symbolic, val array: FiniteArraySymbolic, fake: Boolean) :
    StarSymbolic(array.elementType, fake) {

    override fun toGlobal(mem: Memory): GlobalStarSymbolic {
        println("toGLOBAL")
        val globalAddress = mem.addNewStarObject("", get(mem))
        return GlobalStarSymbolic("", array.elementType, globalAddress, fake)
    }

    override fun dereference(): ArrayStarSymbolic =
        if (fake)
            error("dereferencing dereferenced")
        else
            ArrayStarSymbolic(address, array, true)

    override fun get(mem: Memory): Symbolic {
        if (!fake) {
            error("!")
        }

        return array.get(address, mem).dereference().getDereferenced(mem)
    }

    override fun put(value: Symbolic, mem: Memory): Boolean {
        array.put(address, value, mem)
        return true
    }

    override fun eq(other: StarSymbolic, mem: Memory) = when (other) {
        is ArrayStarSymbolic -> {
            if (array != other.array)
                BoolType.`false`(mem)
            with(mem.ctx) {
                address.expr as KExpr<KSort> eq other.address.expr as KExpr<KSort>
            }.toBoolSymbolic()
        }

        is LocalStarSymbolic -> {
//                todo what if both null?
            BoolType.`false`(mem)
        }

        is NilLocalStarSymbolic -> {
//                todo what if null in array?
            BoolType.`false`(mem)
        }

        is GlobalStarSymbolic -> {
//                todo what if both null?
            BoolType.`false`(mem)
        }
    }

    override fun toFieldStar(name: String, type: Type, mem: Memory): StarSymbolic {
        return this.toGlobal(mem).toFieldStar(name, type, mem)
    }

    override fun toString() =
        if (fake)
            "FA($elementType)"
        else
            "A*($elementType)"
}

class LocalStarSymbolic(private var symbolic: Symbolic, fake: Boolean) : StarSymbolic(symbolic.type, fake) {
    override fun toGlobal(mem: Memory): GlobalStarSymbolic {
        val globalAddress = mem.addNewStarObject("", symbolic)
        return GlobalStarSymbolic("", symbolic.type, globalAddress, fake)
    }


    override fun dereference(): LocalStarSymbolic = if (fake) {
        error("dereferencing dereferenced")
    } else {
        LocalStarSymbolic(symbolic, true)
    }
//    override fun toGlobal(mem: Memory): GlobalStarSymbolic {
//        return type.createSymbolic("star$", mem) as GlobalStarSymbolic
//    }

    override fun get(mem: Memory): Symbolic {
        if (!fake) {
            error("!")
        }
        return symbolic.getDereferenced(mem)
    }

    override fun put(value: Symbolic, mem: Memory): Boolean {
        symbolic = value
        return true
    }

    override fun eq(other: StarSymbolic, mem: Memory) = when (other) {
        is ArrayStarSymbolic -> {
//                todo what if both null?
            BoolType.`false`(mem)
        }

        is LocalStarSymbolic -> {
            // this LocalStarSymbolic will be used as a wrapper
            if (this == other) {
                BoolType.`true`(mem)
            } else {
                BoolType.`false`(mem)
            }
        }

        is NilLocalStarSymbolic -> {
//                todo what if null in array?
            BoolType.`false`(mem)
        }

        is GlobalStarSymbolic -> {
//                todo what if both null?
            BoolType.`false`(mem)
        }
    }

    override fun toFieldStar(name: String, type: Type, mem: Memory): LocalStarSymbolic {
        return LocalStarSymbolic(FieldSymbolic((symbolic.named(mem)).underlying, name), fake)
    }

    override fun toString() =
        if (fake)
            "FL($elementType)"
        else
            "L*($elementType)"
}

class NilLocalStarSymbolic(elementType: Type, fake: Boolean) : StarSymbolic(elementType, fake) {
    override fun toGlobal(mem: Memory): GlobalStarSymbolic {
        TODO("Not yet implemented")
    }

    override fun dereference(): StarSymbolic =
        if (fake) {
            error("dereferencing dereferenced")
        } else {
            NilLocalStarSymbolic(elementType, true)
        }

    override fun findField(field: Int, mem: Memory): Symbolic {
        mem.addError(
            (BoolType.`true`(mem)),
            "something done with null"
        )
        return StopSymbolic
    }

    override fun get(mem: Memory): Symbolic {
        if (!fake) {
            error("!")
        }
        mem.addError(
            (BoolType.`true`(mem)),
            "something done with null"
        )
        return StopSymbolic
    }

    override fun put(value: Symbolic, mem: Memory): Boolean {
        mem.addError(
            (BoolType.`true`(mem)),
            "something done with null"
        )
        return false
    }

    override fun eq(other: StarSymbolic, mem: Memory) = when (other) {
        is ArrayStarSymbolic -> {
//                todo what if null in array?
            BoolType.`false`(mem)
        }

        is LocalStarSymbolic -> {
//                todo what if both null?
            BoolType.`false`(mem)
        }

        is NilLocalStarSymbolic -> {
            BoolType.`true`(mem)
        }

        is GlobalStarSymbolic -> {
//                todo what if both null?
            BoolType.`false`(mem)
        }
    }

    override fun toFieldStar(name: String, type: Type, mem: Memory): Symbolic {
        mem.addError(
            (BoolType.`true`(mem)),
            "something done with null"
        )
        return StopSymbolic
    }

    override fun toString() =
        if (fake)
            "nil"
        else
            "L*(nil)"
}

class GlobalStarSymbolic(val prefix: String, elementType: Type, val address: Int64Symbolic, fake: Boolean) :
    StarSymbolic(elementType, fake) {
    val name = elementType.toString()

    override fun toGlobal(mem: Memory): GlobalStarSymbolic =
        this

    override fun dereference(): GlobalStarSymbolic =
        if (fake) {
            error("dereferencing dereferenced")
        } else {
            GlobalStarSymbolic(prefix, elementType, address, true)
        }

    override fun get(mem: Memory): Symbolic {
        if (!fake) {
            error("!")
        }
        return mem.getStarObject(prefix, elementType, address).getDereferenced(mem)
    }

    override fun put(value: Symbolic, mem: Memory): Boolean {
        mem.putStarObject(prefix, value, address)
        return true
    }

    override fun eq(other: StarSymbolic, mem: Memory) = when (other) {
        is ArrayStarSymbolic -> {
//                todo what if both null?
            BoolType.`false`(mem)
        }

        is LocalStarSymbolic -> {
//                todo what if both null?
            BoolType.`false`(mem)
        }

        is NilLocalStarSymbolic -> {
//                todo what if null in global?
            BoolType.`false`(mem)
        }

        is GlobalStarSymbolic -> {
            with(mem.ctx) {
                address.expr eq other.address.expr
            }.toBoolSymbolic()
        }
    }

    override fun toFieldStar(name: String, type: Type, mem: Memory): StarSymbolic {
        return GlobalStarSymbolic(name, type, address, fake)
    }

    override fun toString() =
        if (fake)
            "FG($elementType)"
        else
            "G*($elementType)"
}

class StructSymbolic(
    type: StructType,
    val fields: MutableMap<String, Symbolic>
) : Symbolic(type) {
    override fun toString() = "Struct(${fields.keys})"
}

class FieldSymbolic(
    val struct: StructSymbolic,
    val name: String
) : Symbolic(struct.fields[name]!!.type) {
    override fun toString() = "Field($name)"
}

class NamedSymbolic(type: NamedType, val underlying: StructSymbolic) : Symbolic(type) {
    override fun toString() = "named $type $underlying"
}

object StopSymbolic : Symbolic(UnknownType)