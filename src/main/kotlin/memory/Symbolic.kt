package memory

import io.ksmt.expr.KExpr
import io.ksmt.expr.KFpRoundingMode
import io.ksmt.sort.*

fun KExpr<KBoolSort>.toSymbolic() =
    BoolSymbolic(this)

fun KExpr<KBv32Sort>.toSymbolic() =
    IntSymbolic(this)

fun KExpr<KFp64Sort>.toSymbolic() =
    FloatSymbolic(this)

fun KExpr<out KSort>.toSymbolic() =
    Type.toSymbolic(this)

open class Symbolic(val type: Type) {
    fun expr(mem: Memory): KExpr<KSort> {
        return when {
            type is BoolType -> this.boolExpr() as KExpr<KSort>
            type is IntType -> this.intExpr() as KExpr<KSort>
            type is FloatType -> this.floatExpr(mem) as KExpr<KSort>
            this is InfiniteSimpleArraySymbolic -> this.arrayExpr as KExpr<KSort>
            this is GlobalStarSymbolic -> this.address.intExpr() as KExpr<KSort>
            this is ArrayStarSymbolic -> this.address.intExpr() as KExpr<KSort>
            else -> error("should be simple type $this ${this.type}")
        }
    }

    fun bool(): BoolSymbolic = this as BoolSymbolic

    fun boolExpr(): KExpr<KBoolSort> = (this as BoolSymbolic).expr

    fun int(): IntSymbolic = this as IntSymbolic

    fun intExpr(): KExpr<KBv32Sort> = (this as IntSymbolic).expr

    fun floatExpr(mem: Memory): KExpr<KFp64Sort> = when (val x = this) {
        is IntSymbolic -> with(mem.ctx) {
            val roundingMode = mkFpRoundingModeExpr(KFpRoundingMode.RoundTowardZero)
            mkBvToFpExpr(
                mkFp64Sort(), roundingMode, x.expr as KExpr<KBvSort>, true
            )
        }

        is FloatSymbolic -> (this as FloatSymbolic).expr

        else -> error("can't cast $this to fp64Expr")
    }

    fun star() = this as StarSymbolic

    fun complex() = this as ComplexSymbolic

    fun uninterpretedExpr() = (this as UninterpretedSymbolic).expr

    fun arrayFinite(): FiniteArraySymbolic = (this as FiniteArraySymbolic)

    fun struct() = this as StructSymbolic

    override fun toString() = type.toString()
}

class ListSymbolic(val list: List<Symbolic>) : Symbolic(ListType(list.map { it.type })) {
    override fun toString(): String {
        return list.joinToString(" ")
    }
}

class ListType(val list: List<Type>) : Type {
    override fun createSymbolic(name: String, mem: Memory, isLocal: Boolean): Symbolic {
        error("ListType is used only as call return")
    }

    override fun defaultSymbolic(mem: Memory, isLocal: Boolean): Symbolic {
        error("ListType is used only as call return")
    }
}

open class SimpleSymbolic(type: Type) : Symbolic(type)

class BoolSymbolic(val expr: KExpr<KBoolSort>) : SimpleSymbolic(BoolType()) {
    fun not(mem: Memory) = BoolSymbolic(mem.ctx.mkNot(expr))
    override fun toString() = "bool $expr"
}

class IntSymbolic(val expr: KExpr<KBv32Sort>) : SimpleSymbolic(IntType()) {
    override fun toString() = "int $expr"
}

class FloatSymbolic(val expr: KExpr<KFp64Sort>) : SimpleSymbolic(FloatType()) {
    override fun toString() = "float $expr"
}

class ComplexSymbolic(real: KExpr<KFp64Sort>, img: KExpr<KFp64Sort>) : SimpleSymbolic(ComplexType()) {
    val real = FloatSymbolic(real)
    val img = FloatSymbolic(img)
    override fun toString() = "complex $real $img"
}

open class UninterpretedSymbolic(val expr: KExpr<KUninterpretedSort>) :
    SimpleSymbolic(UninterpretedType(expr.sort.name)) {

    override fun toString() = expr.sort.name
}

sealed class StarSymbolic(val elementType: Type) : Symbolic(StarType(elementType)) {
//    /** if was local then will public Symbolic in global context
//     *
//     *  todo check it is unique
//     */
//    abstract fun toGlobal(mem: Memory): GlobalStarSymbolic

    abstract fun get(mem: Memory): Symbolic
    abstract fun put(value: Symbolic, mem: Memory)

    abstract fun eq(other: StarSymbolic, mem: Memory): BoolSymbolic

    fun findField(field: Int): StarSymbolic {
        elementType as NamedType
        val (name, type) = (elementType.underlying as StructType).fields[field]!!
        return toFieldStar(name, type)
    }

    abstract fun toFieldStar(name: String, type: Type): StarSymbolic
}

// we already checked bounds, so we can use InfiniteArray
class ArrayStarSymbolic(val address: IntSymbolic, val array: InfiniteArraySymbolic) :
    StarSymbolic(array.elementType) {
//    override fun toGlobal(mem: Memory): GlobalStarSymbolic {
//        return type.createSymbolic("star$", mem) as GlobalStarSymbolic
//    }

    override fun get(mem: Memory) = array.get(address, mem)
    override fun put(value: Symbolic, mem: Memory) {
        array.put(address, value, mem)
        TODO("Not yet implemented")
    }

    override fun eq(other: StarSymbolic, mem: Memory) = when (other) {
        is ArrayStarSymbolic -> {
            if (array != other.array)
                BoolType.FALSE(mem)
            with(mem.ctx) {
                address.intExpr() eq other.address.intExpr()
            }.toSymbolic()
        }

        is LocalStarSymbolic -> {
//                todo what if both null?
            BoolType.FALSE(mem)
        }

        is NilLocalStarSymbolic -> {
//                todo what if null in array?
            BoolType.FALSE(mem)
        }

        is GlobalStarSymbolic -> {
//                todo what if both null?
            BoolType.FALSE(mem)
        }
    }

    override fun toFieldStar(name: String, type: Type): ArrayStarSymbolic {
        val fieldArray = (array as InfiniteStructArraySymbolic).fields[name]!!
        return ArrayStarSymbolic(address, fieldArray)
    }

    override fun toString() = "StarA($elementType)"
}

class LocalStarSymbolic(private var symbolic: Symbolic) : StarSymbolic(symbolic.type) {
//    override fun toGlobal(mem: Memory): GlobalStarSymbolic {
//        return type.createSymbolic("star$", mem) as GlobalStarSymbolic
//    }

    override fun get(mem: Memory) = symbolic
    override fun put(value: Symbolic, mem: Memory) {
        symbolic = value
    }

    override fun eq(other: StarSymbolic, mem: Memory) = when (other) {
        is ArrayStarSymbolic -> {
//                todo what if both null?
            BoolType.FALSE(mem)
        }

        is LocalStarSymbolic -> {
            // this LocalStarSymbolic will be used as a wrapper
            if (this == other) {
                BoolType.TRUE(mem)
            } else {
                BoolType.FALSE(mem)
            }
        }

        is NilLocalStarSymbolic -> {
//                todo what if null in array?
            BoolType.FALSE(mem)
        }

        is GlobalStarSymbolic -> {
//                todo what if both null?
            BoolType.FALSE(mem)
        }
    }

    override fun toFieldStar(name: String, type: Type): LocalStarSymbolic {
        return LocalStarSymbolic(FieldSymbolic((symbolic as NamedSymbolic).underlying, name))
    }

    override fun toString() = "StarL($elementType)"
}

class NilLocalStarSymbolic(elementType: Type) : StarSymbolic(elementType) {
//    override fun toGlobal(mem: Memory): GlobalStarSymbolic {
//        return StarType(elementType).defaultSymbolic(mem, false) as GlobalStarSymbolic
//    }

    override fun get(mem: Memory) = TODO()
    override fun put(value: Symbolic, mem: Memory) {
        TODO("Not yet implemented")
    }

    override fun eq(other: StarSymbolic, mem: Memory) = when (other) {
        is ArrayStarSymbolic -> {
//                todo what if null in array?
            BoolType.FALSE(mem)
        }

        is LocalStarSymbolic -> {
//                todo what if both null?
            BoolType.FALSE(mem)
        }

        is NilLocalStarSymbolic -> {
            BoolType.TRUE(mem)
        }

        is GlobalStarSymbolic -> {
//                todo what if both null?
            BoolType.FALSE(mem)
        }
    }

    override fun toFieldStar(name: String, type: Type): StarSymbolic {
        TODO("Not yet implemented")
    }

    override fun toString() = "nil"
}

class GlobalStarSymbolic(elementType: Type, val address: IntSymbolic) : StarSymbolic(elementType) {
    override fun get(mem: Memory) =
        mem.getStarObject("", elementType, address)

    override fun put(value: Symbolic, mem: Memory) {
        mem.putStarObject("", value, address)
    }

    override fun eq(other: StarSymbolic, mem: Memory) = when (other) {
        is ArrayStarSymbolic -> {
//                todo what if both null?
            BoolType.FALSE(mem)
        }

        is LocalStarSymbolic -> {
//                todo what if both null?
            BoolType.FALSE(mem)
        }

        is NilLocalStarSymbolic -> {
//                todo what if null in global?
            BoolType.FALSE(mem)
        }

        is GlobalStarSymbolic -> {
            with(mem.ctx) {
                address.intExpr() eq other.address.intExpr()
            }.toSymbolic()
        }
    }

    override fun toFieldStar(name: String, type: Type): StarSymbolic {
        return GlobalStarSymbolic(type, address)
    }

    override fun toString() = "StarG($elementType)"
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