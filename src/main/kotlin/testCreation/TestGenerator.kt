package testCreation

import com.jetbrains.rd.util.printlnError
import interpreter.ssa.SsaNode
import io.ksmt.KContext
import io.ksmt.expr.KApp
import io.ksmt.expr.KExpr
import io.ksmt.solver.KModel
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.*
import io.ksmt.utils.asExpr
import memory.*
import testCreation.TestGenerator.Companion.TestCase
import testCreation.TestGenerator.Companion.floatToString
import java.io.File
import java.lang.Double.longBitsToDouble
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class TestGenerator(
    val initialTimeoutInMillis: Long,
    val populationSize: Int = 5,
    val maxEpochs: Int = 50,
    val numberOfChildren: Int = 5
) {
    fun generateTests(
        resultsList: List<SymbolicResult<SsaNode>>,
        createdConsts: Map<String, KSort>,
        functionInfo: FunctionInfo,
        ctx: KContext
    ): Triple<List<TestCase>, Double, Double> {
        val tests = mutableListOf<TestCase>()

        val allFunctionNodes = functionInfo.allNodes
            .filter { it.parentF.endsWith(".${functionInfo.functionName}") }

        val remainingNodes = mutableSetOf(*allFunctionNodes.toTypedArray())

        val terminalsNodes = remainingNodes.filter { it.isTerminal }

        val remainingTerminals = terminalsNodes.toMutableSet()

        for (result in resultsList.sortedBy { -it.visitedNodes.size }) {
            if (remainingTerminals.isEmpty())
                break

            val newNodes = remainingNodes.intersect(result.visitedNodes)
            val newTerminals = remainingTerminals.intersect(result.visitedNodes.filter { it.isTerminal })

            if (newTerminals.isNotEmpty()) {
                val test = generateTest(result, createdConsts, ctx)

                if (test != null) {
                    tests.add(test)
                    remainingNodes -= newNodes
                    remainingTerminals -= newTerminals
                }
            }

        }

        for (result in resultsList.sortedBy { -it.visitedNodes.size }) {
            if (remainingNodes.isEmpty())
                break

            val newNodes = remainingNodes.intersect(result.visitedNodes)
            val newTerminals = remainingTerminals.intersect(result.visitedNodes.filter { it.isTerminal })

            if (newNodes.isNotEmpty()) {
                val test = generateTest(result, createdConsts, ctx)

                if (test != null) {
                    tests.add(test)
                    remainingNodes -= newNodes
                    remainingTerminals -= newTerminals
                }
            }
        }

        if (remainingTerminals.isNotEmpty()) {
            printlnError("\tnot all terminal nodes covered in ${functionInfo.functionName}")
            printlnError("\t${terminalsNodes.size - remainingTerminals.size}/${terminalsNodes.size}")
            printlnError("\tremaining terminal nodes: ${remainingTerminals.map { it.printItself() }}")
        }
        val terminalCoverage = (terminalsNodes.size - remainingTerminals.size).toDouble() / terminalsNodes.size
        val coverage = (allFunctionNodes.size - remainingNodes.size).toDouble() / allFunctionNodes.size
        println("\tcoverage: ${"%.${2}f".format(coverage * 100)}%")

        return Triple(tests, terminalCoverage, coverage)
    }

    fun generateTest(
        result: SymbolicResult<SsaNode>,
        createdConsts: Map<String, KSort>,
        ctx: KContext
    ): TestCase? {
        val startTime = System.currentTimeMillis()

        val cond = result.cond

        val solver = KZ3Solver(ctx)
        solver.assert(cond.expr)

        val sat = solver.check(initialTimeoutInMillis.milliseconds)
        val firstModel = when (sat) {
            KSolverStatus.SAT -> {
                solver.model()
            }

            KSolverStatus.UNSAT -> {
                printlnError("UNSAT in test.go generation")
                printlnError("\tresult\t${result}")
                printlnError("\texpr\t${cond.expr}")
                return null
            }

            KSolverStatus.UNKNOWN -> {
                printlnError("No model found in time")
                return null
            }
        }


        val variables = createdConsts.map { (name, sort) ->
            ctx.mkConst(name, sort)
        }

        val curTime = System.currentTimeMillis()

        val bestModel = evolutionaryAlgorithm(
            variables, ctx, initialTimeoutInMillis - (curTime - startTime),
            solver
        ) ?: firstModel

//        println(modelToMap(bestModel).joinToString("\n") { "${it.first}\t${it.second}" } +
//                "\n\t -> " + symbolicToString(result, bestModel)
//        )
        return TestCase(modelToMap(bestModel), symbolicToString(result, bestModel))
    }

    private fun symbolicToString(result: SymbolicResult<SsaNode>, bestModel: KModel): String {
        return when (result) {
            is SymbolicReturn -> result.returns.joinToString(", ") {
                when (it) {
                    is IntSymbolic -> kExprToString(bestModel.eval(it.expr))
                    is FloatSymbolic -> kExprToString(bestModel.eval(it.expr))
                    is BoolSymbolic -> kExprToString(bestModel.eval(it.expr))
                    is ComplexSymbolic -> {
                        "(${kExprToString(bestModel.eval(it.real.expr))}, ${
                            kExprToString(
                                bestModel.eval(
                                    it.img.expr
                                )
                            )
                        })"
                    }

                    is FiniteArraySymbolic -> {
                        "\"todo[${it.length()}]\""
                    }

                    is UninterpretedSymbolic -> kExprToString(bestModel.eval(it.expr))
                    is NamedSymbolic -> it.toString()
                    is StarSymbolic -> it.toString()

                    is ListSymbolic -> it.list.joinToString(", ")

                    else -> error("unsupported type ${it.javaClass.name}")
                }
            }

            is SymbolicError -> "\"${result.error}\""
        }
    }

    private fun evolutionaryAlgorithm(
        variables: List<KApp<KSort, *>>, ctx: KContext,
        timeoutInMillis: Long,
        solver: KZ3Solver
    ): KModel? {
        val startTime = System.currentTimeMillis()
        var population = generateInitialPopulation(variables, ctx, populationSize, timeoutInMillis, solver)
        if (population.isEmpty())
            return null
        var bestFitness = population[0].second
        var equalTimes = -1
        for (i in 0..maxEpochs) {
            if (population[0].second >= bestFitness * 0.90) {
                bestFitness = population[0].second
                equalTimes++
                if (equalTimes == 3) break
            } else {
                bestFitness = population[0].second
                equalTimes = 0
            }
            var curTime = System.currentTimeMillis()
            if (curTime - startTime > timeoutInMillis) {
                break
            }

            val newPopulation = (population + generateInitialPopulation(
                variables,
                ctx,
                equalTimes,
                (timeoutInMillis - (curTime - startTime)) / 2,
                solver
            )).map { (creature, _) ->
                curTime = System.currentTimeMillis()
                List(numberOfChildren) {
                    val mutated = mutateCreature(creature, ctx)
                    testCreature(mutated, timeoutInMillis - (curTime - startTime) / 2, ctx, solver)

                }.filterNotNull()
            }.flatten() + population

            population = newPopulation.sortedBy { it.second }.take(populationSize)
        }
        return population.first().third
    }

    private fun generateInitialPopulation(
        variables: List<KApp<KSort, *>>,
        ctx: KContext,
        size: Int,
        timeoutInMillis: Long,
        solver: KZ3Solver
    ): List<Triple<Map<KApp<KSort, *>, List<KExpr<KBoolSort>>>, Int, KModel>> {
        val startTime = System.currentTimeMillis()
        val initialPopulation = mutableListOf<Triple<Map<KApp<KSort, *>, List<KExpr<KBoolSort>>>, Int, KModel>>()
        while (initialPopulation.size < size) {
            val curTime = System.currentTimeMillis()
            if (curTime - startTime > timeoutInMillis) {
                break
            }
            val creature = variables.associateWith {
                listOf(simpleConstraintGenerator(it, ctx))
            }
            val testResult = testCreature(creature, timeoutInMillis, ctx, solver)
            if (testResult != null) {
                initialPopulation.add(testResult)
            }
        }
        return initialPopulation
    }

    private fun testCreature(
        creature: Map<KApp<KSort, *>, List<KExpr<KBoolSort>>>,
        timeoutInMillis: Long,
        ctx: KContext,
        solver: KZ3Solver
    ): Triple<Map<KApp<KSort, *>, List<KExpr<KBoolSort>>>, Int, KModel>? {
        val startTime = System.currentTimeMillis()
        val satisfiable = solver.checkWithAssumptions(creature.flatMap { it.value }, timeoutInMillis.milliseconds / 2)
        val curTime = System.currentTimeMillis()
        return when (satisfiable) {
            KSolverStatus.SAT -> {
                val model = solver.model()
                Triple(creature, evaluateModelSimplicity(model), model)
            }

            KSolverStatus.UNSAT -> {
                if (curTime - startTime < timeoutInMillis) {
                    return null
                }
                val core = solver.unsatCore()
                val fixedCreature = creature.map { (variable, values) ->
                    val cropped = values.filter { !core.contains(it) }
                    variable to cropped.ifEmpty {
                        cropped + simpleConstraintGenerator(variable, ctx)
                    }
                }.toMap()
                val curTime = System.currentTimeMillis()
                val res = testCreature(fixedCreature, timeoutInMillis - (curTime - startTime), ctx, solver)
                return res
            }

            KSolverStatus.UNKNOWN -> null
        }
    }

    private fun mutateCreature(constraints: Map<KApp<KSort, *>, List<KExpr<KBoolSort>>>, ctx: KContext):
            Map<KApp<KSort, *>, List<KExpr<KBoolSort>>> {
        return constraints.map { (variable, list) ->
            val afterRemoved = list.mapNotNull {
                if (Random.nextDouble() < 0.25) null
                else it
            }
            variable to (afterRemoved + if (afterRemoved.isEmpty() || Random.nextDouble() < 0.5)
                simpleConstraintGenerator(variable, ctx)
            else null).filterNotNull()
        }.toMap()
    }

    private fun evaluateModelSimplicity(model: KModel): Int {
        val values = modelToMap(model).map { it.second }
        val string = values.joinToString("") { it }
        val length = values.map { it.length }.average().toInt()
        val numberOfOnes = string.count { "1".contains(it) }
        val numberOfFives = string.count { "5".contains(it) }
        val numberOfUgly = string.count { "2346789".contains(it) }
        val numberOfE = string.count { "E".contains(it) }
        return length * 100 + numberOfE * 50 + numberOfUgly * 10 + numberOfFives * 5 + numberOfOnes
    }

    private fun simpleConstraintGenerator(variable: KApp<KSort, *>, ctx: KContext): KExpr<KBoolSort> {
        with(ctx) {
            fun generateInt(sort: KBvSort, bits: Int): KExpr<KBoolSort> {
                val x = variable.asExpr(sort)

                fun Long.pow(x: Long): Long = (2..x).fold(this) { r, _ -> r * this }

                val min = (-2L).pow(bits - 1L)
                val max = 2L.pow(bits - 1L) - 1L

                val randomLong = listOfNotNull(
                    -1,
                    0,
                    1,
                    Random.nextLong(10) - 5,
                    Random.nextLong(100) - 50,
                    max,
                    min,
                    if (bits > 8) Random.nextLong(1000) - 500 else null,
                    if (bits > 16) Random.nextLong(100000) - 50000 else null,
                    Random.nextLong(min, max)
                ).random()

                val randomLongExpr = mkBv(randomLong, sort)

                val operation = listOf<(KExpr<KBvSort>, KExpr<KBvSort>) -> KExpr<KBoolSort>>(
                    ::mkBvSignedLessExpr,
                    ::mkBvSignedLessExpr,
                    ::mkBvSignedGreaterExpr,
                    ::mkBvSignedGreaterExpr,
                    { a, b -> a eq b }
                ).random()

                return operation(x, randomLongExpr)
            }

            fun generateFloat(sort: KFpSort, bits: Int): KExpr<KBoolSort> {
                val x = variable.asExpr(sort)

                val randomDouble = (if (bits == 32)
                    listOf(
                        Float.NaN,
                        Float.POSITIVE_INFINITY,
                        Float.NEGATIVE_INFINITY,
                        Random.nextFloat() - 0.5f,
                        Random.nextFloat() * 10.0f - 5.0f,
                        Random.nextFloat() * 100.0f - 50.0f,
                        Random.nextFloat()
                    )
                else listOf(
                    0.0,
                    1.0,
                    -1.0,
                    Double.NaN,
                    Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY,
                    Random.nextDouble(1.0) - 0.5,
                    Random.nextDouble(10.0) - 5.0,
                    Random.nextDouble(100.0) - 50.0,
                    Random.nextDouble(),
                )).random()

                val randomFloatExpr = mkFp(randomDouble.toDouble(), sort).asExpr(sort)

                val operation = listOf<(KExpr<KFpSort>, KExpr<KFpSort>) -> KExpr<KBoolSort>>(
                    ::mkFpGreaterExpr,
                    ::mkFpLessExpr,
                    ::mkFpEqualExpr,
                ).random()

                return operation(x, randomFloatExpr)
            }

            fun generateBool(): KExpr<KBoolSort> {
                val x = variable.asExpr(boolSort)

                val randomBoolean = Random.nextBoolean()

                val randomBoolExpr = mkBool(randomBoolean).asExpr(boolSort)

                return x eq randomBoolExpr
            }

            return when (val sort = variable.sort) {
                is KBv8Sort -> generateInt(sort, 8)
                is KBv16Sort -> generateInt(sort, 16)
                is KBv32Sort -> generateInt(sort, 32)
                is KBv64Sort -> generateInt(sort, 64)

                is KFp32Sort -> generateFloat(fp32Sort, 32)
                is KFp64Sort -> generateFloat(fp64Sort, 64)

                is KBoolSort -> generateBool()

                else -> error("unknown type ${variable.sort}")
            }
        }
    }

    private fun modelToMap(model: KModel): List<Pair<String, String>> =
        model.declarations.mapNotNull { declaration ->
            try {
                val result = model.eval(declaration.apply(listOf()))
                declaration.name to kExprToString(result)
            } catch (_: Exception) {
                null
            }
        }

    @OptIn(ExperimentalStdlibApi::class)
    fun kExprToString(expr: KExpr<out KSort>): String {
        try {
            val binary = expr.toString()
            return when (expr.sort) {
                is KBvSort -> binary.removePrefix("#x").hexToLong()
                is KFp32Sort -> floatToString(binary, 32)
                is KFp64Sort -> floatToString(binary, 64)
                is KBoolSort -> binary.toBoolean()

//            todo string constants could be saved during execution
                is KUninterpretedSort -> binary
                else -> error("${expr.sort}")
            }.toString()
        } catch (_: Exception) {
            printlnError("can't parse: \n\t${expr}")
            printlnError("happens if model could not be simplified for some reason")
            return "?"
        }
    }

    companion object {

        @OptIn(ExperimentalStdlibApi::class)
        fun floatToString(binary: String, bits: Int): Double {
            val biasSize = if (bits == 64) 11 else 8
            val mantisSize = if (bits == 64) 52 else 23
            val offset = if (bits == 64) 1023 else 127

            val cleanBinary = binary
                .replace("(", "")
                .replace("fp", "")
                .replace(")", "")
                .replace(" ", "")
                .split("#")
                .filter(String::isNotEmpty)

            val sign = cleanBinary[0].drop(1).toInt(2)
            val mantissa = if (cleanBinary[1].startsWith("b")) {
                cleanBinary[1].drop(1).toLong(2)
            } else {
                cleanBinary[1].drop(1).hexToLong()
            }
            val biasedExponent = if (cleanBinary[2].startsWith("b")) {
                cleanBinary[2].drop(1).toLong(2)
            } else {
                cleanBinary[2].drop(1).hexToLong()
            }

            val normalizedBiasedExponent = biasedExponent + offset
            val doubleBits = (sign.toLong() shl (bits - 1)) or (normalizedBiasedExponent shl mantisSize) or mantissa

            return longBitsToDouble(doubleBits)
        }

        data class TestCase(val input: List<Pair<String, String>>, val output: String)
        data class FunctionInfo(val functionName: String, val allNodes: MutableSet<SsaNode>)

        fun generateGolangCode(originalCodeDir: String, tests: Map<FunctionInfo, List<TestCase>>): String {
            val code = StringBuilder()
            code.append(
                """
                package main
                
                import (
                )
                
                
            """.trimIndent()
            )

            val testsCode = tests.mapValues { (funcInfo, testCases) ->
                generateTestCode(funcInfo.functionName, testCases)
            }

            testsCode.map { (_, testCode) ->
                code.append("$testCode\n")
            }

            code.append("func main() {\n")
            testsCode.map { (funcInfo, _) ->
                code.append("\ttest_${funcInfo.functionName}()\n")
            }
            code.append("}\n")

            code.append("\n")
            code.append("// Original code:\n")

            val files = File(originalCodeDir).listFiles { file -> file.isFile && file.extension == "go" }
            files.forEach { file ->
                val originalFile = file.readLines()
                var packageRemoved = false
                var importsStarted = false
                var importsRemoved = false
                for (line in originalFile) {
                    if (!packageRemoved && line.startsWith("package")) {
                        packageRemoved = true
                        continue
                    }
                    if (line.startsWith("import (")) {
                        importsStarted = true
                        continue
                    }
                    if (!importsRemoved && importsStarted) {
                        if (line.endsWith(")")) {
                            importsRemoved = true
                        }
                        packageRemoved = true
                        continue
                    }

                    code.append("$line\n")
                }
            }

            return code.toString()
        }

        fun generateTestCode(funcName: String, testCases: List<TestCase>): String {
            val code = StringBuilder()

            code.append("func test_${funcName}() {\n")

            for ((index, testCase) in testCases.withIndex()) {
                code.append("\n\t// Test case ${index + 1}\n")

                testCase.input.joinToString("\n") { (variable, value) ->
                    code.append("\t$variable := $value\n")
                }
                code.append(
                    """
                        result := $funcName(${testCase.input.joinToString(", ") { it.first }})
                    if result != ${testCase.output} {
                        t.Errorf("test_${funcName}${index + 1} failed")
                    }
                    
                """.trimIndent().replace("\n", "\n\t")
                )
            }

            code.append("\n}\n")

            return code.toString()
        }

        fun writeGolangCode(code: String, filePath: String) {
            println(filePath)
            File(filePath).printWriter().use { out ->
                out.println(code)
            }
        }
    }
}