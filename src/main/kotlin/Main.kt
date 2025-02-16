import com.jetbrains.rd.util.printlnError
import interpreter.InterpreterQueue.Companion.randomTimeToNewCodeQueue
import interpreter.ssa.BlockSsaNode
import interpreter.ssa.FuncSsaNode
import interpreter.ssa.SsaDynamicInterpreter
import interpreter.ssa.SsaNode
import io.ksmt.KContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlinx.serialization.json.Json
import memory.ssa.SsaState
import java.io.FileInputStream
import java.io.InputStreamReader

fun main() = runBlocking {
//    TODO  go run src/main/go/ssa.go -input src/main/resources/code/ -output src/main/resources/ssa/
    val directoryPath = "build/resources/main/ssa/"
    val files = File(directoryPath).listFiles { file -> file.isFile && file.extension == "json" }

    var interpretationTime = 0L
    var testGenerationTime = 0L

    var globalDone = 0
    var globalTimeout = 0
    var globalTests = 0
    var globalInterpretationTime = 0L
    files!!.forEach { file ->
        println("FILE ${file.name}")

        SsaNode.allNodes = mutableMapOf()
        val nodesList = Json.decodeFromString<List<SsaNode>>(
            FileInputStream(file).use { fis ->
                InputStreamReader(fis).use { reader ->
                    reader.readText()
                }
            })

//        val astFile = Json.decodeFromString<AstNode>(
//            FileInputStream(file).use { fis ->
//                InputStreamReader(fis).use { reader ->
//                    reader.readText()
//                }
//            }) as AstFile


        val ctx = KContext(
//            for debug purposes
//            simplificationMode = KContext.SimplificationMode.NO_SIMPLIFY
        )


        val funcDeclaration = nodesList
            .filter { it is FuncSsaNode && it.body != null && it.paramsNull != null }
            .map { it as FuncSsaNode }
            .associateBy { it.name }

        var fileInterpretationTime = 0L
        var done = 0
        var timeout = 0
        funcDeclaration
//            .filter { it.key == "ByteArray" } // for testing 1 function
            .forEach { (funcName, node) ->
//                generateDotFile(file.name, node) // generates nice images of ssa graphs in /ssaGraphPictures/
                val startTime = System.currentTimeMillis()
                try {
                    withTimeout(20_000L) {
                        val allNodes = mutableSetOf<SsaNode>()
                        node.getAllReachable(allNodes)
                        val terminalNodes =
                            allNodes.filter { it.isTerminal && it.parentF.endsWith(".$funcName") }.toMutableSet()
                        val keyNodes = allNodes.filter { it is BlockSsaNode } + node
                        val distToTerminal = keyNodes.associate { it to it.bfs(terminalNodes) }

//                      val interpreter = SsaStaticInterpreter(funcDeclaration)
                        val interpreter = SsaDynamicInterpreter(
                            funcDeclaration,
                            randomTimeToNewCodeQueue<SsaState>(),
                            terminalNodes,
                            distToTerminal
                        )

                        interpreter.startFunction(node, null, ctx, SsaState(ctx))
                        println("$funcName\n\tdone")
                        done++
                    }
                } catch (_: TimeoutCancellationException) {
                    printlnError("$funcName\n\ttimeout")
                    timeout++
                }
//                catch (e: Exception) {
//                    printlnError("$funcName\n\terror:\n\t\t\t ${e.localizedMessage}")
//                } catch (e: Error) {
//                    printlnError("$funcName\n\terror:\n\t\t\t ${e.localizedMessage}")
//                }
                val curTime = System.currentTimeMillis()

                val interpretationTime = curTime - startTime
                fileInterpretationTime += interpretationTime
                println("\tin \t${timeToString(interpretationTime)}")
            }
        globalDone += done
        globalTimeout += timeout
        globalTests += funcDeclaration.size
        globalInterpretationTime += fileInterpretationTime
        println("\nSUCCESSFUL $done/${funcDeclaration.size} (TIMEOUT $timeout/${funcDeclaration.size}, ERROR ${funcDeclaration.size - done - timeout}/${funcDeclaration.size}))")
        println("\tin \t${timeToString(fileInterpretationTime)}\n\n")

//        val typeDeclarations = astFile.getTypes()
//        val functionDeclarations = astFile.getFunctions().associateBy { it.name }
////            val interpreter = StaticInterpreter(functionDeclarations, typeDeclarations)
//        val interpreter = DynamicInterpreter(
//            functionDeclarations, typeDeclarations,
////            DynamicInterpreter.dfsQueue()
//            DynamicInterpreter.randomTimeToNewCodeQueue()
//        )
//
//        typeDeclarations.forEach { (name, it) ->
//            println("TYPE $name")
//        }
//        functionDeclarations.forEach { (name, funcDeclaration) ->
//            println("______________________")
//            println("\nFUNCTION $name")
//
//            val startTime = System.currentTimeMillis()
//            val (results, createdConsts) = interpreter.startFunction(funcDeclaration, null, ctx, State(ctx))
//            val curTime = System.currentTimeMillis()
//
//            val timeForInterpretation = curTime - startTime
//            interpretationTime += timeForInterpretation
//            println("time for interpretation:  \t${timeForInterpretation / 1000}'${timeForInterpretation % 1000}\tms")
//            println()
//            results.forEach { result ->
//                val startTestTime = System.currentTimeMillis()
////                TestCreator().createTest(result, createdConsts, ctx, 60 * 1000)
//                val curTestTime = System.currentTimeMillis()
//
//                val timeForTest = curTestTime - startTestTime
//                testGenerationTime += timeForTest
//                println("time for test generation: \t${timeForTest / 1000}'${timeForTest % 1000}\tms")
//                println()
//            }
//        }
//    }
//    println("\n______________________")
//    println("full time for interpretation: \t${interpretationTime / 1000}'${interpretationTime % 1000}\tms")
//    println("full time for test generation: \t${testGenerationTime / 1000}'${testGenerationTime % 1000}\tms")
    }
    println()
    println("OVERALL:")
    println("SUCCESSFUL $globalDone/$globalTests (TIMEOUT $globalTimeout/$globalTests, ERROR ${globalTests - globalDone - globalTimeout}/$globalTests)")
    println("in \t${timeToString(globalInterpretationTime)}")
}

fun timeToString(time: Long) =
    "${time / 1000}'${(time % 1000).toString().padStart(3, '0')}  ms"