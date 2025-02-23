import com.jetbrains.rd.util.printlnError
import interpreter.InterpreterQueue.Companion.dfsQueue
import interpreter.InterpreterQueue.Companion.randomTimeAfterNewCodeQueue
import interpreter.ssa.*
import io.ksmt.KContext
import io.ksmt.sort.KSort
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlinx.serialization.json.Json
import memory.SymbolicResult
import memory.ssa.SsaState
import testCreation.TestGenerator
import testCreation.TestGenerator.Companion.FunctionInfo
import java.io.FileInputStream
import java.io.InputStreamReader

fun main() = runBlocking {
//    TODO  go run src/main/go/ssa.go -input src/main/resources/code/ -output src/main/resources/ssa/
    val realCodePath = "src/main/resources/samples"
    val directoryPath = "build/resources/main"
    val files = File("${directoryPath}/ssa").listFiles { file -> file.isFile && file.extension == "json" }

    var globalDone = 0
    var globalErrors = 0
    var globalTimeout = 0
    var globalTests = 0
    var globalGraphAnalyzingTime = 0L
    var globalInterpretationTime = 0L
    var globalTestGenerationTime = 0L

    files!!.forEach { file ->
        val fileName = file.name.removeSuffix(".json")
        println("FILE $fileName")

        SsaNode.allNodes = mutableMapOf()
        val reader = FileInputStream(file).use { fis ->
            InputStreamReader(fis).use { reader ->
                reader.readText()
            }
        }
        val nodesList = Json.decodeFromString<List<SsaNode>>(reader)

        val ctx = KContext(
//            simplificationMode = KContext.SimplificationMode.NO_SIMPLIFY
        )

        val funcDeclarations = nodesList
            .filter { it is FuncSsaNode && it.body != null && it.paramsNull != null }
            .map { it as FuncSsaNode }
            .associateBy { it.name }

        var fileInterpretationTime = 0L
        var fileGraphAnalyzingTime = 0L
        var fileTestGenerationTime = 0L

        var done = 0
        var timeout = 0
        var errors = 0

        val interpretationResults = funcDeclarations
            .filter {
                listOf(
                    "ExceptionInNestedMethod"
                ).contains(it.key)
            } // for testing 1 function
            .map { (funcName, node) ->
//                generateDotFile(fileName, node) // generates nice images of ssa graphs in /ssaGraphPictures/..

                var results = listOf<SymbolicResult<SsaNode>>()
                var createdConsts = mapOf<String, KSort>()
                val allNodes = mutableSetOf<SsaNode>()

                val startTime = System.currentTimeMillis()
                var graphAnalyzingTime = 0L

                println(funcName)
                try {
                    withTimeout(10_000L) {

                        node.getAllReachable(allNodes)
                        val terminalNodes =
                            allNodes.filter { it.isTerminal && it.parentF.endsWith(".$funcName") }.toMutableSet()
                        val keyNodes =
                            allNodes.filter {
                                it is BlockSsaNode || it is ReturnSsaNode || it is JumpSsaNode || it is PhiSsaNode
                            } + node
                        val distToTerminal = keyNodes.associate { it to it.bfs(terminalNodes) }

                        val graphAnalyzingFinishedTime = System.currentTimeMillis()
                        graphAnalyzingTime = graphAnalyzingFinishedTime - startTime + 1
                        println("\tgraph analyzed \n\t\tin\t${timeToString(graphAnalyzingTime)}")

//                        val interpreter = SsaStaticInterpreter()
                        val interpreter = SsaDynamicInterpreter(
//                            dfsQueue(),
//                            bfsQueue(),
//                            timeAfterNewCodeQueue(),
//                            randomQueue(),
                            randomTimeAfterNewCodeQueue<SsaState>(),
                            terminalNodes,
                            distToTerminal
                        )

                        val interpretationResults = interpreter.startFunction(node, null, ctx, SsaState(ctx))
                        println("\tinterpretation finished")
                        done++
                        results = interpretationResults.first
                        createdConsts = interpretationResults.second
                    }
                } catch (_: TimeoutCancellationException) {
                    printlnError("$funcName\n\ttimeout")
                    timeout++
                }
//                catch (e: Exception) {
//                    printlnError("$funcName\n\terror:\n\t\t\t ${e.localizedMessage}")
//                    errors++
//                } catch (e: Error) {
//                    printlnError("$funcName\n\terror:\n\t\t\t ${e.localizedMessage}")
//                    errors++
//                }

                val curTime = System.currentTimeMillis()

                if (graphAnalyzingTime == 0L)
                    graphAnalyzingTime = curTime - startTime

                val interpretationTime = curTime - startTime - graphAnalyzingTime
                fileInterpretationTime += interpretationTime
                fileGraphAnalyzingTime += graphAnalyzingTime

                println("\t\tin\t${timeToString(interpretationTime)}")

                Triple(results, createdConsts, FunctionInfo(funcName, allNodes))
            }

        println("\nGENERATING TESTS")

        val testGenerator = TestGenerator(15_000L)

        val tests = interpretationResults.associate { (resultsList, createdConsts, functionInfo) ->
            val testGenerationStart = System.currentTimeMillis()
            val (tests, terminalCoverage, coverage) = testGenerator
                .generateTests(resultsList, createdConsts, functionInfo, ctx)
            val testGenerationTime = System.currentTimeMillis() - testGenerationStart

            println("TESTS GENERATED FOR ${functionInfo.functionName}")
            println("\tin\t${timeToString(testGenerationTime)}")
            fileTestGenerationTime += testGenerationTime
            functionInfo to tests
        }

        val code = TestGenerator.generateGolangCode("$realCodePath/$fileName", tests)
        TestGenerator.writeGolangCode(
            code,
            "generatedTests/test_$fileName.go"
        )


        globalDone += done
        globalErrors += errors
        globalTimeout += timeout
        globalTests += funcDeclarations.size
        globalGraphAnalyzingTime += fileGraphAnalyzingTime
        globalInterpretationTime += fileInterpretationTime
        globalTestGenerationTime += fileTestGenerationTime

        println("\nSUCCESSFUL $done/${funcDeclarations.size} (TIMEOUT $timeout/${funcDeclarations.size}, ERROR $errors/${funcDeclarations.size}))")
        println("\tgraph analysis duration: \n\t\t${timeToString(fileGraphAnalyzingTime)}")
        println("\tinterpretation duration: \n\t\t${timeToString(fileInterpretationTime)}\n")
        println("\ttests generating duration: \n\t\t${timeToString(globalTestGenerationTime)}\n")
    }
    println()
    println("OVERALL:")
    println("SUCCESSFUL $globalDone/$globalTests (TIMEOUT $globalTimeout/$globalTests, ERROR $globalErrors/$globalTests)")
    println("\toverall graph analysis duration: \n\t\t${timeToString(globalGraphAnalyzingTime)}")
    println("\toverall interpretation duration: \n\t\t${timeToString(globalInterpretationTime)}")
    println("\toverall tests generating duration: \n\t\t${timeToString(globalTestGenerationTime)}\n")
}

fun timeToString(time: Long) =
    "${time / 1000}'${(time % 1000).toString().padStart(3, '0')}  ms"