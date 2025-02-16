package memory

import io.ksmt.expr.KArrayConst
import io.ksmt.expr.KExpr
import io.ksmt.sort.KArraySort
import io.ksmt.sort.KBv64Sort
import io.ksmt.sort.KFp64Sort
import io.ksmt.sort.KSort

interface AbstractArray {
    val elementType: Type

    fun get(address: Int64Symbolic, mem: Memory): Symbolic

    fun put(address: Int64Symbolic, value: Symbolic, mem: Memory)

    fun put(address: Long, value: Symbolic, mem: Memory) {
        put(AddressType().fromInt(address, mem).int64(mem), value, mem)
    }
}

class InfiniteArraySymbolic(
    val elementType: SimpleType,
    val arrayExpr: KArrayConst<KArraySort<KBv64Sort, KSort>, KSort>
) : Symbolic(ArrayType(elementType, null)) {
    fun toNormalArray(): InfiniteSimpleArray {
        return if (elementType is StarType)
            InfiniteStarArray(
                elementType,
                arrayExpr
            )
        else InfiniteSimpleArray(
            elementType,
            arrayExpr
        )
    }
}

open class FiniteArraySymbolic(
    val length: Int64Symbolic,
    mem: Memory,
    val innerArray: InfiniteSimpleArray
) : Symbolic(ArrayType(innerArray.elementType, length)), AbstractArray {
    init {
        mem.addCond(
            mem.ctx.mkBvSignedGreaterOrEqualExpr(length.expr, AddressType().zeroExpr(mem) as KExpr<KBv64Sort>)
                .toBoolSymbolic(),
            false
        )
    }

    override val elementType = innerArray.elementType

    constructor(
        elementType: Type,
        length: Int64Symbolic,
        mem: Memory,
        isDefault: Boolean
    ) : this(
        length,
        mem,
        InfiniteStarArray(
            StarType(elementType, false),
            mem,
            isDefault
        )
    )

    constructor(
        length: Int64Symbolic,
        mem: Memory,
        infSymbolic: InfiniteArraySymbolic
    ) : this(
        length,
        mem,
        infSymbolic.toNormalArray()
    )

    constructor(
        length: Int64Symbolic,
        mem: Memory,
        innerArray: FiniteArraySymbolic
    ) : this(length, mem, innerArray.innerArray) {
        mem.addCond(
            mem.ctx.mkBvSignedLessOrEqualExpr(length.expr, innerArray.length.expr).toBoolSymbolic(),
            false
        )
    }

    private fun checkBounds(index: Int64Symbolic, mem: Memory) {
        mem.addError(
            mem.ctx.mkBvSignedLessExpr(index.expr, AddressType().zeroExpr(mem) as KExpr<KBv64Sort>).toBoolSymbolic(),
            "out of bounds"
        )
        mem.addError(
            mem.ctx.mkBvSignedGreaterOrEqualExpr(index.expr, length.expr).toBoolSymbolic(),
            "out of bounds"
        )
    }

    override fun get(address: Int64Symbolic, mem: Memory): StarSymbolic {
        checkBounds(address, mem)
        return innerArray.get(address, mem) as StarSymbolic
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        checkBounds(address, mem)
        return innerArray.put(address, value, mem)
    }

    fun length() = length

    override fun toString() = "FinArr($elementType)"
}

abstract class InfiniteArray(type: ArrayType) : AbstractArray {
    override val elementType: Type = type.elementType

    companion object {
        fun create(elementType: Type, mem: Memory, isDefault: Boolean): InfiniteArray {
            return when (elementType) {
                is StarType -> InfiniteStarArray(elementType, mem, isDefault)

                is SimpleType -> InfiniteSimpleArray(elementType, mem, isDefault)
                is ArrayType -> InfiniteArraysArray(elementType, mem, isDefault)

                is ComplexType -> InfiniteComplexArray(
                    InfiniteSimpleArray(Float64Type(), mem, isDefault),
                    InfiniteSimpleArray(Float64Type(), mem, isDefault)
                )

                is StructType ->
                    InfiniteStructArray(elementType, mem, isDefault)

                is NamedType ->
                    // todo something named????
                    InfiniteStructArray(elementType.underlying, mem, isDefault)

                else -> error("array creation for type $elementType")
            }
        }
    }
}

class InfiniteArraysArray(
    arrayElementType: ArrayType,
    mem: Memory,
    isDefault: Boolean
) : InfiniteArray(ArrayType(StarType(arrayElementType, true), null)) {
    private val len = InfiniteSimpleArray(AddressType(), mem, isDefault)

    private val array = InfiniteSimpleArray(arrayElementType.toSimple(), mem, isDefault)

//    override fun eqSameType(array: AbstractArray, mem: Memory): BoolSymbolic {
//        TODO("Not yet implemented")
//    }

    override fun get(address: Int64Symbolic, mem: Memory): StarSymbolic {
        val lenI = len.get(address, mem).int64(mem)

        val arrI = array.get(address, mem).getDereferenced(mem) as InfiniteArraySymbolic

        return LocalStarSymbolic(
            FiniteArraySymbolic(
                lenI,
                mem,
                arrI.toNormalArray()
            ), true
        )
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        val finiteArray = value.array(mem)

        len.put(address, finiteArray.length, mem)
        array.put(address, finiteArray.innerArray.toSymbolic(), mem)
    }
}

class InfiniteStructArray(
    override val elementType: StructType,
    val mem: Memory,
    isDefault: Boolean
) : InfiniteArray(ArrayType(elementType, null)) {
    val fields: Map<String, InfiniteArray> =
        (elementType.fields + (":id" to AddressType())).toMap()
            .mapValues { (name, type) ->
                create(type, mem, isDefault)
            }

//    override fun eqSameType(array: AbstractArray, mem: Memory): BoolSymbolic {
//        array as InfiniteStructArray
//        return array.fields[":id"]!!.eq(fields[":id"]!!, mem)
//    }

    override fun get(address: Int64Symbolic, mem: Memory): Symbolic {
        return StructSymbolic(
            elementType,
            fields.mapValues { (_, field) -> field.get(address, mem) }.toMutableMap()
        )
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        (value.named(mem)).underlying.fields.map { (name, fieldValue) ->
            fields[name]!!.put(address, fieldValue, mem)
        }
    }

    override fun toString() = "InfArr($elementType)"
}

// todo just a structure
class InfiniteComplexArray(
    private val real: InfiniteSimpleArray,
    private val img: InfiniteSimpleArray
) : InfiniteArray(ArrayType(ComplexType(), null)) {
//
//    override fun eqSameType(array: AbstractArray, mem: Memory): BoolSymbolic {
//        array as InfiniteComplexArray
//        return mem.ctx.mkAnd(
//            array.real.eq(real, mem).boolExpr(mem),
//            array.img.eq(img, mem).boolExpr(mem)
//        ).toBoolSymbolic()
//    }

    override fun get(address: Int64Symbolic, mem: Memory) =
        ComplexSymbolic(
            real.get(address, mem) as KExpr<KFp64Sort>,
            img.get(address, mem) as KExpr<KFp64Sort>
        )

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        real.put(address, value.complex(mem).real, mem)
        img.put(address, value.complex(mem).img, mem)
    }

    override fun toString() = "InfArr($elementType)"
}

open class InfiniteSimpleArray(
    override val elementType: SimpleType,
    val arrayExpr: KArrayConst<KArraySort<KBv64Sort, KSort>, KSort>
) : InfiniteArray(ArrayType(elementType, null)) {

    constructor(elementType: SimpleType, mem: Memory, isDefault: Boolean) : this(
        elementType,
        newArrayExpr(elementType, mem, isDefault)
    )

    companion object {
        fun newArrayExpr(elementType: SimpleType, mem: Memory, isDefault: Boolean) =
            mem.ctx.mkArrayConst(
                mem.ctx.mkArraySort(AddressType().sort(mem), elementType.sort(mem)) as KArraySort<KBv64Sort, KSort>,
                if (isDefault)
                    elementType.defaultSymbolicExpr(mem) as KExpr<KSort>
                else
                    elementType.createSymbolicExpr("inArray", mem) as KExpr<KSort>
            )
    }

    override fun get(address: Int64Symbolic, mem: Memory): Symbolic {
        return elementType.asSymbolic(mem.ctx.mkArraySelect(arrayExpr, address.expr), mem)
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        mem.ctx.mkArrayStore(arrayExpr, address.expr, value.toExpr(mem))
    }

    override fun toString() = "InfArr($elementType)"

    open fun toSymbolic(): Symbolic {
        return InfiniteArraySymbolic(elementType, arrayExpr)
    }
}

class InfiniteStarArray(
    override val elementType: StarType,
    arrayExpr: KArrayConst<KArraySort<KBv64Sort, KSort>, KSort>
) : InfiniteSimpleArray(
    elementType,
    arrayExpr
) {
    constructor(
        starType: StarType,
        mem: Memory,
        isDefault: Boolean
    ) : this(
        starType,
        newArrayExpr(starType, mem, isDefault)
    )

    override fun get(address: Int64Symbolic, mem: Memory): StarSymbolic {
        val expr = mem.ctx.mkArraySelect(arrayExpr, address.expr)
        return GlobalStarSymbolic(
            "",
            elementType.elementType,
            Int64Symbolic(expr as KExpr<KBv64Sort>),
            false
        )
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        println("VVV ")
        println("VVV ${value}")
        println("VVV ${value.type}")
        println("VVV ${elementType}")
        val globalStar = LocalStarSymbolic(value, false).toGlobal(mem)
        mem.ctx.mkArrayStore(arrayExpr, address.expr, globalStar.address.expr as KExpr<KSort>)
    }
}