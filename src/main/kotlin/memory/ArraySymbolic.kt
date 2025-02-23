package memory

import com.jetbrains.rd.util.printlnError
import io.ksmt.expr.KArrayConst
import io.ksmt.expr.KExpr
import io.ksmt.sort.KArraySort
import io.ksmt.sort.KBv64Sort
import io.ksmt.sort.KFp64Sort
import io.ksmt.sort.KSort

sealed interface AbstractArray {
    fun get(address: Int64Symbolic, mem: Memory): MemoryObject

    fun put(address: Int64Symbolic, value: Symbolic, mem: Memory)
}

abstract class InfAbstractArray(
    var arrayType: InfArrayType
) : AbstractArray, MemoryObject {
    abstract fun toArrayExpr(mem: Memory): KExpr<KSort>

    companion object {
        fun create(elementType: Type, arrayBehaviour: SimpleArrayBehaviour, mem: Memory): InfAbstractArray {
            return when (elementType) {
                is StarType -> InfStarArray(elementType, arrayBehaviour, mem)

                is SimpleType -> InfSimpleArray(elementType, arrayBehaviour, mem)

                is InfArrayType if(elementType.elementType is SimpleType) -> InfSimpleArray(
                    InfArraySimpleType(elementType.elementType as SimpleType),
                    arrayBehaviour,
                    mem
                )

                is ArrayAbstractType ->
                    FiniteArraysArray(
                        InfArraySimpleType(StarType(elementType.elementType())),
                        arrayBehaviour,
                        mem
                    ).also {
                        it.arrayType = InfArrayType(InfArrayType(elementType.elementType()))
                        it.array.arrayType = InfArrayType(elementType.elementType())
                    }

                is ComplexType -> InfComplexArray(
                    InfSimpleArray(Int64Type(), arrayBehaviour, mem),
                    InfSimpleArray(Int64Type(), arrayBehaviour, mem),
                    arrayBehaviour
                )

                is StructType ->
                    InfStructArray(elementType, arrayBehaviour, mem)

                is NamedType ->
                    // todo something named????
                    InfStructArray(elementType.underlying, arrayBehaviour, mem)

                else -> error("array creation for type ${elementType.javaClass.name}")
            }
        }
    }
}


open class InfCombinedArray(
    val innerArray1: InfAbstractArray,
    val innerArray2: InfAbstractArray,
    val iteCond: BoolSymbolic
) : InfAbstractArray(
    innerArray1.arrayType
) {
    init {
        if (innerArray1.arrayType != innerArray2.arrayType)
            printlnError("ERROR innerArray1.arrayType != innerArray2.arrayType ${innerArray1.arrayType} ${innerArray2.arrayType}")
    }

    override fun get(address: Int64Symbolic, mem: Memory): MemoryObject {
        val from1 = innerArray1.get(address, mem)
        val from2 = innerArray2.get(address, mem)

        return if (from1 is InfAbstractArray && from2 is InfAbstractArray)
            InfCombinedArray(
                from1,
                from2,
                iteCond
            )
        else mem.ite(
            iteCond,
            from1 as Symbolic,
            from2 as Symbolic
        )
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        innerArray1.put(address, value, mem)
        innerArray2.put(address, value, mem)
    }

    override fun toArrayExpr(mem: Memory): KExpr<KSort> {
        return mem.ctx.mkIte(
            iteCond.boolExpr(mem),
            innerArray1.toArrayExpr(mem),
            innerArray2.toArrayExpr(mem)
        )
    }

    override fun toString() = "CombArr"
}

open class FiniteArraySymbolic(
    var arrayType: ArrayType,
    val array: InfAbstractArray,
    val arrayBehaviour: ArrayBehaviour?,
    mem: Memory
) : Symbolic(arrayType), AbstractArray {

    init {
        mem.addCond(
            mem.ctx.mkBvSignedGreaterOrEqualExpr(arrayType.length.expr, Int64Type().zeroExpr(mem) as KExpr<KBv64Sort>)
                .toBoolSymbolic(),
            false
        )
    }

    constructor(
        arrayType: ArrayType,
        arrayBehaviour: CombinedArrayBehaviour,
        mem: Memory
    ) : this(
        arrayType,
        InfCombinedArray(
            InfAbstractArray.create(arrayType.elementType, arrayBehaviour.symbolicPart(), mem),
            InfAbstractArray.create(arrayType.elementType, arrayBehaviour.defaultPart(), mem),
            arrayBehaviour.isSymbolic
        ),
        arrayBehaviour,
        mem
    )

    constructor(
        arrayType: ArrayType,
        innerArray: FiniteArraySymbolic,
        arrayBehaviour: ArrayBehaviour?,
        mem: Memory
    ) : this(
        arrayType,
        innerArray.array,
        arrayBehaviour,
        mem
    )

    private fun checkBounds(index: Int64Symbolic, mem: Memory) {
        mem.addError(
            mem.ctx.mkBvSignedLessExpr(index.expr, Int64Type().zeroExpr(mem) as KExpr<KBv64Sort>)
                .toBoolSymbolic(),
            "out of bounds"
        )
        mem.addError(
            mem.ctx.mkBvSignedGreaterOrEqualExpr(index.expr, arrayType.length.expr).toBoolSymbolic(),
            "out of bounds"
        )
    }

    override fun get(address: Int64Symbolic, mem: Memory): GlobalStarSymbolic {
        checkBounds(address, mem)
//        return if (arrayBehaviour == null) {
        return array.get(address, mem) as GlobalStarSymbolic
//        } else
//            array.get(address, mem)
//                .toSymbolic(arrayBehaviour, mem)
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        checkBounds(address, mem)
        array.put(address, value, mem)
    }

    fun length() = arrayType.length

    override fun toString() = "FinArr(${arrayType.elementType})"
}

open class FiniteArraysArray(
    arrayType: InfArrayType,
    val arrayBehaviour: SimpleArrayBehaviour,
    mem: Memory,
    val lengths: InfAbstractArray = InfSimpleArray(Int64Type(), arrayBehaviour, mem),
    val array: InfAbstractArray =
        InfSimpleArray(arrayType as InfArraySimpleType, arrayBehaviour, mem)

//    val array: InfAbstractArray = if (arrayType.elementType is NonStarType) {
//        InfSimpleArray(arrayType as SimpleType, arrayBehaviour, mem)
//    } else {
//        InfSimpleArray(InfArraySimpleType(StarType(arrayType.elementType)), arrayBehaviour, mem).also {
//            it.arrayType = arrayType
//        }
//    }
) : InfAbstractArray(arrayType) {

    override fun get(address: Int64Symbolic, mem: Memory): FiniteArraySymbolic {
        val len = (lengths.get(address, mem) as Int64Symbolic)
        val arr = array.get(address, mem) as InfAbstractArray
        val starArray = InfStarArray(
            arrayType.elementType as InfArrayType,
            StarType((arrayType.elementType as InfArrayType).elementType),
            arr.toArrayExpr(mem) as KExpr<KArraySort<KBv64Sort, KBv64Sort>>,
            arrayBehaviour
        )
        return FiniteArraySymbolic(
            ArrayType((arrayType.elementType as InfArrayType).elementType, len), starArray, arrayBehaviour, mem
        )
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        value as FiniteArraySymbolic

        lengths.put(address, value.length(), mem)
        array.put(address, value, mem)
    }

    override fun toArrayExpr(mem: Memory): KExpr<KSort> {
        TODO("Not yet implemented")
    }
}

//open class InfArraysArray(
//    override var arrayType: InfArrayType,
//    val arrayBehaviour: SimpleArrayBehaviour,
//    mem: Memory,
//    val array: InfAbstractArray = if (arrayType is InfArraySimpleType) {
//        InfSimpleArray(arrayType, arrayBehaviour, mem)
//    } else {
//        InfSimpleArray(InfArraySimpleType(StarType(arrayType.elementType)), arrayBehaviour, mem)
//    }
//) : InfAbstractArray(arrayType) {
//    init {
//        println("arrayElementTypeINFFF  $arrayType")
//    }
//
//    override fun get(address: Int64Symbolic, mem: Memory): MemoryObject {
//        val arr = array.get(address, mem) as InfAbstractArray
//
//        arr.arrayType = arrayType
//
//        return arr
//    }
//
//    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
//        value as InfAbstractArray
//
//        array.put(address, value, mem)
//    }
//
//    override fun toArrayExpr(mem: Memory): KExpr<KSort> {
//        TODO("Not yet implemented")
//    }
//}

class InfStructArray(
    val elementType: StructType,
    arrayBehaviour: SimpleArrayBehaviour,
    val mem: Memory
) : InfAbstractArray(InfArrayType(elementType)) {
    val fields: Map<String, InfAbstractArray> =
        (elementType.fields + (":id" to Int64Type())).toMap()
            .mapValues { (name, type) ->
                create(type, arrayBehaviour, mem)
            }

    override fun get(address: Int64Symbolic, mem: Memory): MemoryObject {
        return StructSymbolic(
            elementType,
            fields.mapValues { (_, field) -> field.get(address, mem) as Symbolic }.toMutableMap()
        )
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        (value.named(mem)).underlying.fields.map { (name, fieldValue) ->
            fields[name]!!.put(address, fieldValue, mem)
        }
    }

    override fun toArrayExpr(mem: Memory): KExpr<KSort> {
        TODO("Not yet implemented")
    }

    override fun toString() = "[]($elementType)"
}

// todo just a structure
class InfComplexArray(
    private val real: InfSimpleArray,
    private val img: InfSimpleArray,
    arrayBehaviour: SimpleArrayBehaviour
) : InfAbstractArray(InfArrayType(ComplexType())) {

    override fun get(address: Int64Symbolic, mem: Memory) =
        ComplexSymbolic(
            real.get(address, mem) as KExpr<KFp64Sort>,
            img.get(address, mem) as KExpr<KFp64Sort>
        )

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        real.put(address, value.complex(mem).real, mem)
        img.put(address, value.complex(mem).img, mem)
    }

    override fun toArrayExpr(mem: Memory): KExpr<KSort> {
        TODO("Not yet implemented")
    }

    override fun toString() = "[Complex]"
}

open class InfSimpleArray(
    elementType: SimpleType,
    var arrayExpr: KExpr<KArraySort<KBv64Sort, KSort>>,
    val arrayBehaviour: SimpleArrayBehaviour
) : InfAbstractArray(InfArraySimpleType(elementType)) {

    constructor(
        elementType: SimpleType,
        arrayBehaviour: SimpleArrayBehaviour,
        mem: Memory
    ) : this(
        elementType,
        newArrayExpr(elementType, arrayBehaviour, mem),
        arrayBehaviour
    )

    companion object {
        fun newArrayExpr(
            elementType: SimpleType,
            arrayBehaviour: SimpleArrayBehaviour,
            mem: Memory
        ): KArrayConst<KArraySort<KBv64Sort, KSort>, KSort> {
            val array = mem.ctx.mkArrayConst(
                mem.ctx.mkArraySort(Int64Type().sort(mem), elementType.sort(mem)) as KArraySort<KBv64Sort, KSort>,
                when (arrayBehaviour) {
                    is SymbolicArrayBehaviour -> elementType.createSymbolicExpr(
                        arrayBehaviour.symbolicName + ":",
                        mem
                    ) as KExpr<KSort>

                    is DefaultArrayBehaviour -> elementType.defaultSymbolicExpr(mem) as KExpr<KSort>
                }
            )
            return array
        }
    }

    override fun get(address: Int64Symbolic, mem: Memory): MemoryObject {
        val expr = mem.ctx.mkArraySelect(arrayExpr, address.expr)

        return when (arrayType.elementType()) {
            is ArrayAbstractType -> {
                val innerType = (arrayType.elementType() as ArrayAbstractType).elementType()

//                val infArray = if (innerType is StarType) {
                InfStarArray(
                    InfArrayType(StarType(innerType)),
                    StarType(innerType),
                    expr as KExpr<KArraySort<KBv64Sort, KBv64Sort>>,
                    arrayBehaviour
                )
//                } else {
//                    InfSimpleArray(
//                        innerType as SimpleType,
//                        expr as KExpr<KArraySort<KBv64Sort, KSort>>,
//                        arrayBehaviour
//                    )
//                }
//                infArray
//                FiniteArraySymbolic(
//                    ArrayType(
//                        innerType,
//                        arrayBehaviour.getLen(mem)
//                    ),
//                    infArray,
//                    mem
//                )
            }

            else ->
                (arrayType.elementType() as SimpleType).asSymbolic(expr, mem)
        }
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        arrayExpr = when (value) {
            is FiniteArraySymbolic -> mem.ctx.mkArrayStore(arrayExpr, address.expr, value.array.toArrayExpr(mem))

            else -> mem.ctx.mkArrayStore(arrayExpr, address.expr, value.toExpr(mem))
        }
    }

    override fun toArrayExpr(mem: Memory): KExpr<KSort> {
        return arrayExpr as KExpr<KSort>
    }

    override fun toString() = "[](${arrayType.elementType})"
}

class InfStarArray(
    arrayType: InfArrayType,
    val fakeElementType: StarType,
    var arrayExpr: KExpr<KArraySort<KBv64Sort, KBv64Sort>>,
    val arrayBehaviour: SimpleArrayBehaviour
) : InfAbstractArray(arrayType) {

    fun realElementType() = arrayType.elementType

    constructor(
        starType: StarType,
        arrayBehaviour: SimpleArrayBehaviour,
        mem: Memory,
    ) : this(
        InfArrayType(starType),
        starType,
        newArrayExpr(mem) as KExpr<KArraySort<KBv64Sort, KBv64Sort>>,
        arrayBehaviour
    )

    override fun get(address: Int64Symbolic, mem: Memory): StarSymbolic {
        println("GETINF $address")
        val expr = mem.ctx.mkArraySelect(arrayExpr, address.expr)

        val isFirstAccess = with(mem.ctx) {
            expr eq Int64Type().zeroExpr(mem) as KExpr<KBv64Sort>
        }.toBoolSymbolic()
        val newObjectAddress = when (arrayBehaviour) {
            is SymbolicArrayBehaviour -> mem.addNewSymbolicStar(realElementType(), true, arrayBehaviour.symbolicName)
            is DefaultArrayBehaviour -> mem.addNewDefaultStar(realElementType())
        }
        val element = mem.ite(isFirstAccess, newObjectAddress, Int64Symbolic(expr as KExpr<KBv64Sort>)).int64(mem)

        return when {
            realElementType() !is StarType ->
                GlobalStarSymbolic(
                    StarType(realElementType()),
                    element,
                    arrayBehaviour.isSymbolic(mem)
                )

            else -> GlobalStarSymbolic(
                fakeElementType,
                element,
                arrayBehaviour.isSymbolic(mem)
            )
        }

    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        println("PUTINF $address")
        arrayExpr = when {
            realElementType() !is StarType -> {
                if (value is StarSymbolic) {
                    mem.ctx.mkArrayStore(
                        arrayExpr,
                        address.expr,
                        value.toGlobal(mem).address.expr as KExpr<KBv64Sort>
                    )
                } else if (value is FiniteArraySymbolic) {
                    val expr = value.array.toArrayExpr(mem) as KExpr<KBv64Sort>
                    mem.ctx.mkArrayStore(arrayExpr, address.expr, expr)
                } else {
                    error("should not happen")
                }
            }

            value is StarSymbolic -> mem.ctx.mkArrayStore(
                arrayExpr,
                address.expr,
                value.address(mem).expr as KExpr<KBv64Sort>
            )

            else -> error("should be star or fake, not ${value.javaClass.name}")
        }
    }

    override fun toArrayExpr(mem: Memory): KExpr<KSort> {
        return arrayExpr as KExpr<KSort>
    }

    companion object {
        fun newArrayExpr(
            mem: Memory
        ): KExpr<KArraySort<KBv64Sort, KBv64Sort>> {
            with(mem.ctx) {
                val array = mkArrayConst(
                    mkArraySort(mkBv64Sort(), mkBv64Sort()),
                    Int64Type().defaultSymbolic(mem).int64(mem).expr
                )
                return array
            }
        }
    }
}

sealed class ArrayBehaviour {
    abstract fun isSymbolic(mem: Memory): BoolSymbolic
    abstract fun getLen(mem: Memory): Int64Symbolic

}

sealed class SimpleArrayBehaviour() : ArrayBehaviour()

object DefaultArrayBehaviour :
    SimpleArrayBehaviour() {
    override fun toString() = "default ${super.toString()}"
    override fun isSymbolic(mem: Memory): BoolSymbolic =
        BoolType.`false`(mem)

    override fun getLen(mem: Memory): Int64Symbolic =
        Int64Type().defaultSymbolic(mem).int64(mem)
}

data class SymbolicArrayBehaviour(val symbolicName: String) :
    SimpleArrayBehaviour() {
    override fun toString() = "symbolic($symbolicName) ${super.toString()}"
    override fun isSymbolic(mem: Memory): BoolSymbolic =
        BoolType.`true`(mem)

    override fun getLen(mem: Memory): Int64Symbolic =
        Int64Type().createSymbolic("$symbolicName:len", mem).int64(mem)
}

data class CombinedArrayBehaviour(
    val symbolicName: String,
    val isSymbolic: BoolSymbolic
) : ArrayBehaviour() {
    fun defaultPart(): DefaultArrayBehaviour =
        DefaultArrayBehaviour

    fun symbolicPart(): SymbolicArrayBehaviour =
        SymbolicArrayBehaviour(symbolicName)

    override fun isSymbolic(mem: Memory) = isSymbolic

    override fun getLen(mem: Memory) = mem.ite(
        isSymbolic,
        symbolicPart().getLen(mem),
        defaultPart().getLen(mem)
    ).int64(mem)
}