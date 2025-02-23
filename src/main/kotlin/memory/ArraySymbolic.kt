package memory

import com.jetbrains.rd.util.printlnError
import io.ksmt.expr.KArrayConst
import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolverStatus
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

                is InfArrayType ->
                    FiniteArraysArray(
                        InfArraySimpleType(StarType(elementType.elementType())),
                        arrayBehaviour,
                        mem
                    ).also {
                        it.arrayType = InfArrayType(InfArrayType(elementType.elementType()))
                        it.array.arrayType = InfArrayType(InfArrayType(elementType.elementType()))
                    }

                is ComplexType -> InfComplexArray(
                    InfSimpleArray(Int64Type(), arrayBehaviour, mem),
                    InfSimpleArray(Int64Type(), arrayBehaviour, mem)
                )

                is StructType ->
                    InfStructArray(elementType, arrayBehaviour)

                is NamedType ->
                    InfStructArray(elementType.underlying, arrayBehaviour)

                else -> error("array creation for type ${elementType.javaClass.name}")
            }
        }
    }
}


class InfCombinedArray(
    val innerArray1: InfAbstractArray,
    val innerArray2: InfAbstractArray,
    val iteCond: BoolSymbolic
) : InfAbstractArray(
    innerArray1.arrayType
) {
    companion object {
        fun create(
            innerArray1: InfAbstractArray,
            innerArray2: InfAbstractArray,
            iteCond: BoolSymbolic,
            mem: Memory
        ): InfAbstractArray {
            val result1 = mem.solver.checkWithAssumptions(listOf(iteCond.expr), mem.SOLVER_TIMEOUT / 5)
            if (result1 == KSolverStatus.UNSAT)
                return innerArray2

            val result2 = mem.solver.checkWithAssumptions(listOf(iteCond.not(mem).expr), mem.SOLVER_TIMEOUT / 5)
            if (result2 == KSolverStatus.UNSAT)
                return innerArray1

            return InfCombinedArray(innerArray1, innerArray2, iteCond)
        }
    }

    init {
        if (innerArray1.arrayType != innerArray2.arrayType)
            printlnError("ERROR innerArray1.arrayType != innerArray2.arrayType ${innerArray1.arrayType} ${innerArray2.arrayType}")
    }

    override fun get(address: Int64Symbolic, mem: Memory): MemoryObject {
        val from1 = innerArray1.get(address, mem)
        val from2 = innerArray2.get(address, mem)

        return if (from1 is InfAbstractArray && from2 is InfAbstractArray)
            create(
                from1,
                from2,
                iteCond,
                mem
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
    var addressToSelf: StarSymbolic? = null

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
        InfCombinedArray.create(
            InfAbstractArray.create(arrayType.elementType, arrayBehaviour.symbolicPart(), mem),
            InfAbstractArray.create(arrayType.elementType, arrayBehaviour.defaultPart(), mem),
            arrayBehaviour.isSymbolic,
            mem
        ),
        arrayBehaviour,
        mem
    )

    private fun checkBounds(index: Int64Symbolic, mem: Memory) {
        mem.addCond(
            mem.ctx.mkBvSignedLessOrEqualExpr(Int64Type().zeroExpr(mem) as KExpr<KBv64Sort>, index.expr)
                .toBoolSymbolic(),
            false
        )
        mem.addError(
            mem.ctx.mkBvSignedGreaterOrEqualExpr(index.expr, arrayType.length.expr).toBoolSymbolic(),
            "out of bounds"
        )
    }

    override fun get(address: Int64Symbolic, mem: Memory): GlobalStarSymbolic {
        checkBounds(address, mem)
        return (array.get(address, mem) as GlobalStarSymbolic).also {
            addressToSelf?.put(this, mem)
        }
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        checkBounds(address, mem)
        array.put(address, value, mem)
        addressToSelf?.put(this, mem)
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

class InfStructArray(
    val elementType: StructType,
    val arrayBehaviour: SimpleArrayBehaviour
) : InfAbstractArray(InfArrayType(elementType)) {

    override fun get(address: Int64Symbolic, mem: Memory): MemoryObject {
        return StructSymbolic(
            elementType,
            elementType.fields.associate { (name, _) ->
                name to mem.getStar(name, elementType, address, arrayBehaviour.isSymbolic(mem))
            }
        )
    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        val struct = if (value is NamedSymbolic) {
            value.underlying
        } else
            value as StructSymbolic
        elementType.fields.map { (name, _) ->
            mem.putStar(name, elementType, address, struct.fields[name]!!, arrayBehaviour.isSymbolic(mem))
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
    private val img: InfSimpleArray
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

                InfStarArray(
                    InfArrayType(StarType(innerType)),
                    StarType(innerType),
                    expr as KExpr<KArraySort<KBv64Sort, KBv64Sort>>,
                    arrayBehaviour
                )
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
        newArrayExpr(mem),
        arrayBehaviour
    )

    override fun get(address: Int64Symbolic, mem: Memory): StarSymbolic {
        val expr = mem.ctx.mkArraySelect(arrayExpr, address.expr)

        val isFirstAccess = with(mem.ctx) {
            expr eq Int64Type().zeroExpr(mem) as KExpr<KBv64Sort>
        }.toBoolSymbolic()
        val newObjectAddress = when (arrayBehaviour) {
            is SymbolicArrayBehaviour -> mem.addNewSymbolicStar(
                "",
                realElementType(),
                true,
                arrayBehaviour.symbolicName
            )

            is DefaultArrayBehaviour -> mem.addNewDefaultStar("", realElementType())
        }
        val element = mem.ite(isFirstAccess, newObjectAddress, Int64Symbolic(expr)).int64(mem)

        arrayExpr = mem.ctx.mkArrayStore(arrayExpr, address.expr, element.expr)

        return when {
            realElementType() !is StarType ->
                GlobalStarSymbolic(
                    StarType(realElementType()),
                    element,
                    arrayBehaviour.isSymbolic(mem),
                    ""
                )

            else -> GlobalStarSymbolic(
                fakeElementType,
                element,
                arrayBehaviour.isSymbolic(mem),
                ""
            )
        }

    }

    override fun put(address: Int64Symbolic, value: Symbolic, mem: Memory) {
        arrayExpr = when {
            realElementType() !is StarType -> {
                if (value is StarSymbolic) {
                    mem.ctx.mkArrayStore(
                        arrayExpr,
                        address.expr,
                        value.toGlobal(mem).address.expr
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