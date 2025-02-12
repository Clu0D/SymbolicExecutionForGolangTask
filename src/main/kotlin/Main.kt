import interpreter.ssa.FuncSsaNode
import interpreter.ssa.SsaNode
import interpreter.ssa.StaticSsaInterpreter
import io.ksmt.KContext
import java.io.File
import kotlinx.serialization.json.Json
import memory.ssa.SsaState
import java.io.FileInputStream
import java.io.InputStreamReader

fun main() {
//    TODO  go run src/main/go/ast.go -input src/main/resources/code/ -output src/main/resources/ast/
    val directoryPath = "build/resources/main/ast/"
    val files = File(directoryPath).listFiles { file -> file.isFile && file.extension == "json" }

    var interpretationTime = 0L
    var testGenerationTime = 0L

    files!!.forEach { file ->
        println("FILE ${file.name}")

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

        val interpreter = StaticSsaInterpreter(funcDeclaration)

        funcDeclaration.forEach { (name, node) ->
            generateDotFile(node)
            interpreter.startFunction(node, null, ctx, SsaState(ctx))
        }

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
}