package memory

import io.ksmt.expr.KArrayConst
import io.ksmt.expr.KExpr
import io.ksmt.sort.KArraySort
import io.ksmt.sort.KBv64Sort
import io.ksmt.sort.KFp64Sort
import io.ksmt.sort.KSort

interface AbstractArray {
    fun elementType(): Type

    fun get(address: Int64Symbolic, mem: Memory): Symbolic

    fun put(address: Int64Symbolic, value: Symbolic, mem: Memory)

    fun toArrayExpr(mem: Memory): KExpr<KSort>
}

open class CombinedArray(
    val innerArray1: AbstractArray,
    val innerArray2: AbstractArray,
    val iteCond: BoolSymbolic
) : AbstractArray {
    override fun elementType() = innerArray1.elementType()

    override fun get(address: Int64Symbolic, mem: Memory): Symbolic {
        return mem.ite(
            iteCond,
            innerArray1.get(address, mem),
            innerArray2.get(address, mem)
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

    override fun toString() = "CombArr(${elementType()})"
}

open class FiniteArraySymbolic(
    val length: Int64Symbolic,
    val array: AbstractArray,
    mem: Memory
) : Symbolic(ArrayType(array.elementType(), length)), AbstractArray {
    override fun elementType() = array.elementType()

    init {
        mem.addCond(
            mem.ctx.mkBvSignedGreaterOrEqualExpr(length.expr, Int64Type().zeroExpr(mem) as KExpr<KBv64Sort>)
                .toBoolSymbolic(),
            false
        )
    }

    constructor(
        elementType: Type,
        length: Int64Symbolic,
        combinedArrayBehaviour: CombinedArrayBehaviour,
        mem: Memory
    ) : this(
        length,
        CombinedArray(
            InfiniteArray.create(elementType, combinedArrayBehaviour.symbolicPart(), mem),
            InfiniteArray.create(elementType, combinedArrayBehaviour.defaultPart(), mem),
            combinedArrayBehaviour.isSymbolic
        ),
        mem
    )

    constructor(
        length: Int64Symbolic,
        innerArray: FiniteArraySymbolic,
        mem: Memory
    ) : this(
        length,
        innerArray.array,
        mem
    )

    private fun checkBounds(index: Int64Symbolic, mem: Memory) {
        mem.addError(
            mem.ctx.mkBvSignedLessExpr(index.expr, Int64Type().zeroExpr(mem) as KExpr<KBv64Sort>)
                .toBoolSymbolic(),
            "out of bounds"
        )
        mem.addError(
            mem.ctx.mkBvSignedGreaterOrEqualExpr(index.expr, length.expr).toBoolSymbolic(),
            "out of bounds"
        )
    }

    override fun get(address: Int64Symbolic, mem: Memory): Symbolic {
        checkBounds(address, mem)
        return array.get(address, mem)
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        checkBounds(address, mem)
        array.put(address, value, mem)
    }

    override fun toArrayExpr(mem: Memory): KExpr<KSort> {
        return array.toArrayExpr(mem)
    }

    fun length() = length

    override fun toString() = "FinArr(${elementType()})"
}

abstract class InfiniteArray(
    val arrayBehaviour: SimpleArrayBehaviour
) : AbstractArray {

    companion object {
        fun create(elementType: Type, arrayBehaviour: SimpleArrayBehaviour, mem: Memory): InfiniteArray {
            return when (elementType) {
                is StarType -> InfiniteStarArray(elementType, arrayBehaviour, mem)

                // ArraySimpleType should go here
                is SimpleType -> InfiniteSimpleArray(elementType, arrayBehaviour, mem)
                is InfArrayType -> InfiniteInfArraysArray(elementType as ArrayInfSimpleType, arrayBehaviour, mem)
                is ArrayType -> InfiniteArraysArray(elementType as ArraySimpleType, arrayBehaviour, mem)

                is ComplexType -> InfiniteComplexArray(
                    InfiniteSimpleArray(Int64Type(), arrayBehaviour, mem),
                    InfiniteSimpleArray(Int64Type(), arrayBehaviour, mem),
                    arrayBehaviour
                )

                is StructType ->
                    InfiniteStructArray(elementType, arrayBehaviour, mem)

                is NamedType ->
                    // todo something named????
                    InfiniteStructArray(elementType.underlying, arrayBehaviour, mem)

                else -> error("array creation for type ${elementType.javaClass.name}")
            }
        }
    }
}

open class InfiniteArraysArray(
    arrayElementType: ArraySimpleType,
    arrayBehaviour: SimpleArrayBehaviour,
    mem: Memory,
    val array: InfiniteArray = create(
        arrayElementType.elementType,
        arrayBehaviour,
        mem
    )
) : InfiniteArray(arrayBehaviour) {
    override fun elementType(): Type {
        TODO("Not yet implemented")
    }

    override fun get(address: Int64Symbolic, mem: Memory): Symbolic {
        return array.get(address, mem)
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        array.put(address, value, mem)
    }

    override fun toArrayExpr(mem: Memory): KExpr<KSort> {
        TODO("Not yet implemented")
    }
}

open class InfiniteInfArraysArray(
    arrayElementType: ArrayInfSimpleType,
    arrayBehaviour: SimpleArrayBehaviour,
    mem: Memory,
    val array: InfiniteArray = create(
        arrayElementType.elementType,
        arrayBehaviour,
        mem
    )
) : InfiniteArray(arrayBehaviour) {
    override fun elementType(): Type {
        TODO("Not yet implemented")
    }

    override fun get(address: Int64Symbolic, mem: Memory): Symbolic {
        return array.get(address, mem)
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        array.put(address, value, mem)
    }

    override fun toArrayExpr(mem: Memory): KExpr<KSort> {
        TODO("Not yet implemented")
    }
}

class InfiniteStructArray(
    val elementType: StructType,
    arrayBehaviour: SimpleArrayBehaviour,
    val mem: Memory
) : InfiniteArray(arrayBehaviour) {
    val fields: Map<String, InfiniteArray> =
        (elementType.fields + (":id" to Int64Type())).toMap()
            .mapValues { (name, type) ->
                create(type, arrayBehaviour, mem)
            }

    override fun elementType() = elementType

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

    override fun toArrayExpr(mem: Memory): KExpr<KSort> {
        TODO("Not yet implemented")
    }

    override fun toString() = "[]($elementType)"
}

// todo just a structure
class InfiniteComplexArray(
    private val real: InfiniteSimpleArray,
    private val img: InfiniteSimpleArray,
    arrayBehaviour: SimpleArrayBehaviour,
) : InfiniteArray(arrayBehaviour) {
    override fun elementType() = ComplexType()

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

    override fun toString() = "[](${elementType()})"
}

open class InfiniteSimpleArray(
    open val elementType: SimpleType,
    val arrayExpr: KExpr<KArraySort<KBv64Sort, KSort>>,
    arrayBehaviour: SimpleArrayBehaviour
) : InfiniteArray(arrayBehaviour) {

    override fun elementType() = elementType

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
            val mkArrayConst = mem.ctx.mkArrayConst(
                mem.ctx.mkArraySort(Int64Type().sort(mem), elementType.sort(mem)) as KArraySort<KBv64Sort, KSort>,
                when (arrayBehaviour) {
                    is SymbolicArrayBehaviour -> elementType.createSymbolicExpr(
                        arrayBehaviour.symbolicName + ":",
                        mem
                    ) as KExpr<KSort>

                    is DefaultArrayBehaviour -> elementType.defaultSymbolicExpr(mem) as KExpr<KSort>
                }
            )
            return mkArrayConst
        }
    }

    override fun get(address: Int64Symbolic, mem: Memory): Symbolic {
        val expr = mem.ctx.mkArraySelect(arrayExpr, address.expr)

        return when (elementType) {
            is ArrayType -> {
                val innerType = (elementType as ArrayType).elementType

                val infArray = if (innerType is StarType) {
                    InfiniteStarArray(
                        innerType,
                        expr as KExpr<KArraySort<KBv64Sort, KSort>>,
                        arrayBehaviour
                    )
                } else {
                    InfiniteSimpleArray(
                        innerType as SimpleType,
                        expr as KExpr<KArraySort<KBv64Sort, KSort>>,
                        arrayBehaviour
                    )
                }
                infArray
                when (arrayBehaviour) {
                    is DefaultArrayBehaviour -> FiniteArraySymbolic(
                        (elementType as ArrayType).length!!,
                        infArray,
                        mem
                    )

                    is SymbolicArrayBehaviour -> FiniteArraySymbolic(
                        Int64Type().createSymbolic(arrayBehaviour.symbolicName + ":len", mem).int64(mem),
                        infArray,
                        mem
                    )
                }
            }

            is ArrayInfSimpleType -> {
                val innerType = (elementType as ArrayInfSimpleType).elementType

                val infArray = if (innerType is StarType) {
                    InfiniteStarArray(
                        innerType,
                        expr as KExpr<KArraySort<KBv64Sort, KSort>>,
                        arrayBehaviour
                    )
                } else {
                    InfiniteSimpleArray(
                        innerType as SimpleType,
                        expr as KExpr<KArraySort<KBv64Sort, KSort>>,
                        arrayBehaviour
                    )
                }

                when (arrayBehaviour) {
                    is DefaultArrayBehaviour -> FiniteArraySymbolic(
                        Int64Type().defaultSymbolic(mem).int64(mem),
                        infArray,
                        mem
                    )

                    is SymbolicArrayBehaviour -> FiniteArraySymbolic(
                        Int64Type().createSymbolic(arrayBehaviour.symbolicName + ":len", mem).int64(mem),
                        infArray,
                        mem
                    )
                }
            }

            else ->
                elementType.asSymbolic(expr, mem)
        }
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        when (value) {
            is FiniteArraySymbolic -> mem.ctx.mkArrayStore(arrayExpr, address.expr, value.toArrayExpr(mem))

            else -> mem.ctx.mkArrayStore(arrayExpr, address.expr, value.toExpr(mem))
        }
    }

    override fun toArrayExpr(mem: Memory): KExpr<KSort> {
        return arrayExpr as KExpr<KSort>
    }

    override fun toString() = "[]($elementType)"
}

class InfiniteStarArray(
    override val elementType: StarType,
    arrayExpr: KExpr<KArraySort<KBv64Sort, KSort>>,
    arrayBehaviour: SimpleArrayBehaviour
) : InfiniteSimpleArray(
    elementType,
    arrayExpr,
    arrayBehaviour
) {
    constructor(
        starType: StarType,
        arrayBehaviour: SimpleArrayBehaviour,
        mem: Memory,
    ) : this(
        starType,
        newArrayExpr(starType, arrayBehaviour, mem),
        arrayBehaviour
    )

    override fun get(address: Int64Symbolic, mem: Memory): StarSymbolic {
        val expr = mem.ctx.mkArraySelect(arrayExpr, address.expr)
        return GlobalStarSymbolic(
            StarType(elementType, arrayBehaviour.isStarFake),
            Int64Symbolic(expr as KExpr<KBv64Sort>),
            arrayBehaviour.isSymbolic(mem),
            arrayBehaviour.isStarFake
        )
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        when {
            arrayBehaviour.isStarFake -> {
                val globalStar = LocalStarSymbolic(value, arrayBehaviour.isStarFake).toGlobal(mem, true)
                mem.ctx.mkArrayStore(arrayExpr, address.expr, globalStar.address.expr as KExpr<KSort>)
            }

            value is StarSymbolic -> mem.ctx.mkArrayStore(
                arrayExpr,
                address.expr,
                value.address(mem).expr as KExpr<KSort>
            )

            value is FiniteArraySymbolic -> TODO()
//                (value.iteArray(mem) as InfiniteStarArray).arrayExpr
        }
    }
}

sealed class ArrayBehaviour(open val isStarFake: Boolean) {
    override fun toString() = if (isStarFake)
        "with fake * inside"
    else
        ""
}

sealed class SimpleArrayBehaviour(isStarFake: Boolean) : ArrayBehaviour(isStarFake) {
    abstract fun isSymbolic(mem: Memory): BoolSymbolic
}

data class DefaultArrayBehaviour(override val isStarFake: Boolean, val length: Int64Symbolic?) :
    SimpleArrayBehaviour(isStarFake) {
    override fun toString() = "default ${super.toString()}"
    override fun isSymbolic(mem: Memory): BoolSymbolic =
        BoolType.`false`(mem)
}

data class SymbolicArrayBehaviour(override val isStarFake: Boolean, val symbolicName: String?) :
    SimpleArrayBehaviour(isStarFake) {
    override fun toString() = "symbolic($symbolicName) ${super.toString()}"
    override fun isSymbolic(mem: Memory): BoolSymbolic =
        BoolType.`true`(mem)
}

data class CombinedArrayBehaviour(
    override val isStarFake: Boolean,
    val length: Int64Symbolic?,
    val symbolicName: String?,
    val isSymbolic: BoolSymbolic
) : ArrayBehaviour(isStarFake) {
    fun defaultPart(): DefaultArrayBehaviour =
        DefaultArrayBehaviour(isStarFake, length)

    fun symbolicPart(): SymbolicArrayBehaviour =
        SymbolicArrayBehaviour(isStarFake, symbolicName)
}