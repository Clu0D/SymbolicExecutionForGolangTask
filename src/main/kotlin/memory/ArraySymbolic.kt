package memory

import io.ksmt.expr.KArrayConst
import io.ksmt.sort.KArraySort
import io.ksmt.sort.KBv32Sort
import io.ksmt.sort.KSort

interface AbstractArraySymbolic {
    val elementType: Type

    fun eq(array: AbstractArraySymbolic, mem: Memory): BoolSymbolic {
        if (array.elementType != this.elementType)
            return BoolType.FALSE(mem)
        return eqSameType(array, mem)
    }

    fun eqSameType(array: AbstractArraySymbolic, mem: Memory): BoolSymbolic

    fun get(address: IntSymbolic, mem: Memory): Symbolic

    fun put(address: IntSymbolic, value: Symbolic, mem: Memory)

    fun put(address: Int, value: Symbolic, mem: Memory) {
        put(IntType.fromInt(address, mem), value, mem)
    }
}

class FiniteArraySymbolic(
    private val length: IntSymbolic,
    mem: Memory,
    private val innerArray: InfiniteArraySymbolic
) : Symbolic(ArrayType(innerArray.elementType, length)), AbstractArraySymbolic {
    init {
        mem.addCond(
            mem.ctx.mkBvSignedGreaterOrEqualExpr(length.intExpr(), IntType.ZERO(mem)).toSymbolic(),
            false
        )
    }

    override val elementType = innerArray.elementType

    constructor(
        elementType: Type,
        length: IntSymbolic,
        mem: Memory
    ) : this(length, mem, InfiniteArraySymbolic.create(elementType, mem))

    constructor(
        length: IntSymbolic,
        mem: Memory,
        innerArray: FiniteArraySymbolic
    ) : this(length, mem, innerArray.innerArray) {
        mem.addCond(
            mem.ctx.mkBvSignedLessOrEqualExpr(length.intExpr(), innerArray.length.intExpr()).toSymbolic(),
            false
        )
    }

    override fun eqSameType(array: AbstractArraySymbolic, mem: Memory): BoolSymbolic {
        array as FiniteArraySymbolic
        return mem.ctx.mkAnd(
            mem.ctx.mkEq(array.length.intExpr(), length.intExpr()),
            array.innerArray.eq(innerArray, mem).boolExpr()
        ).toSymbolic()
    }

    private fun checkBounds(index: IntSymbolic, mem: Memory) {
        mem.addError(
            mem.ctx.mkBvSignedLessExpr(index.expr, IntType.ZERO(mem)).toSymbolic(),
            "out of bounds"
        )
        mem.addError(
            mem.ctx.mkBvSignedGreaterOrEqualExpr(index.expr, length.expr).toSymbolic(),
            "out of bounds"
        )
    }

    override fun get(address: IntSymbolic, mem: Memory): Symbolic {
        checkBounds(address, mem)
        return innerArray.get(address, mem)
    }

    override fun put(address: IntSymbolic, value: Symbolic, mem: Memory) {
        checkBounds(address, mem)
        return innerArray.put(address, value, mem)
    }

    fun length() = length

    override fun toString() = "FinArr($elementType)"
}

interface InfiniteArraySymbolic : AbstractArraySymbolic {
    companion object {
        fun create(elementType: Type, mem: Memory): InfiniteArraySymbolic = when (elementType) {
//            is StarType -> InfiniteStarArraySymbolic(elementType, mem)

            is SimpleType -> InfiniteSimpleArraySymbolic(elementType, mem)

            is ComplexType -> InfiniteComplexArraySymbolic(
                InfiniteSimpleArraySymbolic(FloatType(), mem),
                InfiniteSimpleArraySymbolic(FloatType(), mem)
            )

            is StructType -> {
                InfiniteStructArraySymbolic(
                    elementType,
                    (elementType.fields + (":id" to IntType())).toMap()
                        .mapValues { (name, type) ->
                            create(type, mem)
                        }
                )
            }

            is NamedType -> {
                // todo something named????
                InfiniteStructArraySymbolic(
                    elementType.underlying,
                    (elementType.underlying.fields + (":id" to IntType())).toMap()
                        .mapValues { (name, type) ->
                            create(type, mem)
                        }
                )
            }

            else -> TODO("array creation for type $elementType")
        }
    }
}

//class InfiniteArraysArraySymbolic(elementType: ArrayType, mem: Memory) : InfiniteArraySymbolic(elementType) {
//    private val arrayExprs: KArrayConst<KArraySort<KBv32Sort, KSort>, KSort> =
//        mem.ctx.mkArrayConst(
//            elementType.sort(mem),
//            elementType.elementType.defaultSymbolic(mem, true).expr(mem)
//        )
//
//    override fun eqSameType(array: AbstractArraySymbolic, mem: Memory): BoolSymbolic {
//        array as InfiniteArraysArraySymbolic
//        return mem.ctx.mkEq(arrayExprs, array.arrayExprs).toSymbolic()
//    }
//
//    override fun get(address: IntSymbolic, mem: Memory): Symbolic {
//        val value = arrayExprs.get(address, mem)
//        return FiniteArraySymbolic(size, array, mem)
//    }
//
//    override fun put(address: IntSymbolic, value: Symbolic, mem: Memory) {
//        TODO("Not yet implemented")
//    }
//}

class InfiniteStructArraySymbolic(
    override val elementType: StructType,
    val fields: Map<String, InfiniteArraySymbolic>
) : Symbolic(ArrayType(elementType)), InfiniteArraySymbolic {
    override fun eqSameType(array: AbstractArraySymbolic, mem: Memory): BoolSymbolic {
        array as InfiniteStructArraySymbolic
        return array.fields[":id"]!!.eq(fields[":id"]!!, mem)
    }

    override fun get(address: IntSymbolic, mem: Memory): Symbolic {
        return StructSymbolic(
            elementType,
            fields.mapValues { (_, field) -> field.get(address, mem) }.toMutableMap()
        )
    }

    override fun put(address: IntSymbolic, value: Symbolic, mem: Memory) {
        val x = value as NamedType

        TODO("Not yet implemented")
    }

    override fun toString() = "InfArr($elementType)"
}

// todo just a structure
class InfiniteComplexArraySymbolic(
    private val real: InfiniteSimpleArraySymbolic,
    private val img: InfiniteSimpleArraySymbolic
) : Symbolic(ArrayType(ComplexType())), InfiniteArraySymbolic {
    override val elementType = ComplexType()

    override fun eqSameType(array: AbstractArraySymbolic, mem: Memory): BoolSymbolic {
        array as InfiniteComplexArraySymbolic
        return mem.ctx.mkAnd(
            array.real.eq(real, mem).boolExpr(),
            array.img.eq(img, mem).boolExpr()
        ).toSymbolic()
    }

    override fun get(address: IntSymbolic, mem: Memory) =
        ComplexSymbolic(
            real.get(address, mem).floatExpr(mem),
            img.get(address, mem).floatExpr(mem)
        )

    override fun put(address: IntSymbolic, value: Symbolic, mem: Memory) {
        real.put(address, value.complex().real, mem)
        img.put(address, value.complex().img, mem)
    }

    override fun toString() = "InfArr($elementType)"
}

class InfiniteSimpleArraySymbolic(
    override val elementType: SimpleType,
    val arrayExpr: KArrayConst<KArraySort<KBv32Sort, KSort>, KSort>
) : Symbolic(ArrayType(elementType)), InfiniteArraySymbolic {

    constructor(elementType: SimpleType, mem: Memory) : this(
        elementType,
        mem.ctx.mkArrayConst(
            mem.ctx.mkArraySort(IntType.intSort(mem), elementType.sort(mem)),
            elementType.defaultSymbolic(mem, false).expr(mem)
        )
    )

    override fun eqSameType(array: AbstractArraySymbolic, mem: Memory): BoolSymbolic {
        array as InfiniteSimpleArraySymbolic
        return mem.ctx.mkEq(
            array.arrayExpr,
            arrayExpr
        ).toSymbolic()
    }

    override fun get(address: IntSymbolic, mem: Memory): Symbolic =
        elementType.asSymbolic(mem.ctx.mkArraySelect(arrayExpr, address.expr), mem)

    override fun put(address: IntSymbolic, value: Symbolic, mem: Memory) {
        elementType.asSymbolic(mem.ctx.mkArrayStore(arrayExpr, address.expr, value.expr(mem)), mem)
    }

    override fun toString() = "InfArr($elementType)"
}

//class InfiniteStarArraySymbolic(
//    override val elementType: StarType,
//    mem: Memory
//) : InfiniteArraySymbolic(elementType) {
//    private val addressArray: InfiniteSimpleArraySymbolic =
//        InfiniteSimpleArraySymbolic(IntType(), mem)
//
//    override fun eqSameType(array: AbstractArraySymbolic, mem: Memory): BoolSymbolic {
//        array as InfiniteStarArraySymbolic
//        return array.addressArray.eq(addressArray, mem)
//    }
//
//    override fun get(address: IntSymbolic, mem: Memory) =
//        ArrayStarSymbolic(addressArray.get(address, mem).int(), this)
//
//    override fun put(address: IntSymbolic, value: Symbolic, mem: Memory) {
////        val addressInGlobal = IntType.fromInt(mem.addNewStarObject("", value), mem)
////        innerArray.put(address, addressInGlobal, mem)
//        println("elementType $elementType")
//        println(value)
//        val addressInGlobal = addressArray.get(address, mem).int()
//        mem.putStarObject("", value, addressInGlobal)
//    }
//}
