package testCreation

import com.jetbrains.rd.util.printlnError
import io.ksmt.KContext
import io.ksmt.expr.KApp
import io.ksmt.expr.KExpr
import io.ksmt.solver.KModel
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.z3.KZ3Solver
import io.ksmt.sort.*
import io.ksmt.utils.asExpr
import memory.*
import java.lang.Double.longBitsToDouble
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class TestCreator {
    fun createTest(result: SymbolicResult, createdConsts: Map<String, KSort>, ctx: KContext, timeoutInMillis: Long) {
        val startTime = System.currentTimeMillis()

        val cond = result.cond

        val solver = KZ3Solver(ctx)
        solver.assert(cond.expr)

//        val sat = solver.check(timeoutInMillis.milliseconds / 2)
//        when (sat) {
//            KSolverStatus.SAT -> {}
//
//            KSolverStatus.UNSAT -> {
//                printlnError("UNSAT in test generation")
//                printlnError("\tresult\t${result}")
//                printlnError("\texpr\t${cond.expr}")
//                return
//            }
//
//            KSolverStatus.UNKNOWN -> {
//                printlnError("UNKNOWN SAT in test generation, reason:${solver.reasonOfUnknown()}")
//                printlnError("\tresult\t${result}")
//                printlnError("\texpr\t${cond.expr}")
//                return
//            }
//        }
//        val firstModel = solver.model()


        val variables = createdConsts.map { (name, sort) ->
            ctx.mkConst(name, sort)
        }

        val curTime = System.currentTimeMillis()

        val bestModel = evolutionaryAlgorithm(
            variables, ctx,
            5, 50, 5, timeoutInMillis - (curTime - startTime),
            solver
        )
        if (bestModel == null) {
            printlnError("No model found in time")
        } else {
            println(
                modelToMap(bestModel).joinToString("\n") { "${it.first}\t${it.second}" } +
                        "\n\t\t -> " +
                        when (result) {
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

                                    is UninterpretedSymbolic -> kExprToString(bestModel.eval(it.expr))
                                    else -> error("unsupported type ${it.type}")
                                }
                            }

                            is SymbolicError -> result.error
                        })
        }
    }

    private fun evolutionaryAlgorithm(
        variables: List<KApp<KSort, *>>, ctx: KContext,
        populationSize: Int, maxEpochs: Int, numberOfChildren: Int, timeoutInMillis: Long,
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
        val satisfiable = solver.checkWithAssumptions(creature.flatMap { it.value }, timeoutInMillis.milliseconds / 2)
        return when (satisfiable) {
            KSolverStatus.SAT -> {
                val model = solver.model()
                Triple(creature, evaluateModelSimplicity(model), model)
            }

            KSolverStatus.UNSAT -> {
                val core = solver.unsatCore()
                val fixedCreature = creature.map { (variable, values) ->
                    val cropped = values.filter { !core.contains(it) }
                    variable to cropped.ifEmpty {
                        cropped + simpleConstraintGenerator(variable, ctx)
                    }
                }.toMap()
                val res = testCreature(fixedCreature, timeoutInMillis / 2, ctx, solver)
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
            return when (variable.sort) {
                is KBv32Sort -> {
                    val x = variable.asExpr(bv32Sort)

                    val randomInt = listOf(
                        -1,
                        0,
                        1,
                        Random.nextInt(10) - 5,
                        Random.nextInt(100) - 50,
                        Random.nextInt(1000) - 500,
                        Random.nextInt()
                    ).random()

                    val rand0mIntExpr = mkBv(randomInt).asExpr(bv32Sort)

                    val operation = listOf<(KExpr<KBv32Sort>, KExpr<KBv32Sort>) -> KExpr<KBoolSort>>(
                        ::mkBvSignedLessExpr,
                        ::mkBvSignedLessExpr,
                        ::mkBvSignedGreaterExpr,
                        ::mkBvSignedGreaterExpr,
                        { a, b -> a eq b }
                    ).random()

                    operation(x, rand0mIntExpr)
                }

                is KFp64Sort -> {
                    val x = variable.asExpr(fp64Sort)

                    val randomDouble = listOf(
                        0.0,
                        1.0,
                        -1.0,
                        Double.NaN,
                        Double.POSITIVE_INFINITY,
                        Double.NEGATIVE_INFINITY,
                        Random.nextDouble(),
                        Random.nextDouble(1.0) - 0.5,
                        Random.nextDouble(10.0) - 5.0,
                        Random.nextDouble(100.0) - 50.0
                    ).random()

                    val randomFloatExpr = mkFp64(randomDouble).asExpr(fp64Sort)

                    val operation = listOf<(KExpr<KFp64Sort>, KExpr<KFp64Sort>) -> KExpr<KBoolSort>>(
                        ::mkFpGreaterExpr,
                        ::mkFpLessExpr,
                        ::mkFpEqualExpr,
                    ).random()

                    operation(x, randomFloatExpr)
                }

                is KBoolSort -> {
                    val x = variable.asExpr(boolSort)

                    val randomBoolean = Random.nextBoolean()

                    val randomBoolExpr = mkBool(randomBoolean).asExpr(boolSort)

                    x eq randomBoolExpr
                }

                else -> error("unknown type ${variable.sort}")
            }
        }
    }

    private fun modelToMap(model: KModel) =
        model.declarations.map { declaration ->
            val result = model.eval(declaration.apply(listOf()))
            declaration.name to kExprToString(result)
        }

    fun kExprToString(expr: KExpr<out KSort>): String {
        try {
            val binary = expr.toString()
            return when (expr.sort) {
                is KBv32Sort -> {
                    binary.removePrefix("#x").toLong(16).toInt()
                }

                is KFp64Sort -> {
                    val cleanBinary = binary.replace("(", "")
                        .replace("fp", "")
                        .replace("#b", "")
                        .replace("#x", "")
                        .replace(")", "")
                        .replace(" ", "")

                    val sign = cleanBinary.substring(0, 1).toInt(2)
                    val biasedExponent = cleanBinary.substring(1, 12).toLong(2)
                    val mantissa = cleanBinary.substring(12).toLong(16)

                    val normalizedBiasedExponent = biasedExponent + 1023
                    val doubleBits = (sign.toLong() shl 63) or (normalizedBiasedExponent shl 52) or mantissa

                    longBitsToDouble(doubleBits)
                }

                is KBoolSort -> binary.toBoolean()

//            todo string constants could be saved during execution
                is KUninterpretedSort -> binary
                else -> error("${expr.sort}")
            }.toString()
        } catch (e: Exception) {
            printlnError("can't parse: \n\t${e.message}")
            printlnError("happens if model could not be simplified for some reason")
            return "?"
        }
    }
}