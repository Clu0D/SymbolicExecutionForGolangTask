import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "2.1.0"
    application
}

group = ""
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    // json parsing
    //    https://mvnrepository.com/artifact/com.google.code.gson/gson
    //    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0-RC")

    // smt solver
    // https://mvnrepository.com/artifact/org.sosy-lab/javasmt-solver-z3
    // implementation("org.sosy-lab:javasmt-solver-z3:4.13.3")
    implementation("io.ksmt:ksmt-core:0.5.26")
    implementation("io.ksmt:ksmt-z3:0.5.26")
    implementation("io.ksmt:ksmt-runner:0.5.26")

    implementation("it.unimi.dsi:fastutil:8.5.15")

    // graphviz
    implementation("org.graalvm.js:js:24.1.1")
    implementation("ch.qos.logback:logback-classic:1.5.12")
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation ("guru.nidi:graphviz-kotlin:0.18.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

val isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
val buildDir = project.layout.buildDirectory.get().toString()

task<Exec>("compileGo") {
    val goFile = "src/main/go/ast.go"
    environment("CGO_ENABLED", "1")

//    todo  go run src/main/go/ast.go -input src/main/resources/code/ -output src/main/resources/ast/

    // if you don't have Golang installed globally, add filepath to your go installment
    // also installed globally gcc required :(
    if (isWindows)
        commandLine("cmd", "/c", "go", "build", "-buildmode=c-shared", "-o", "$buildDir\\ast.dll", goFile)
    else
        commandLine("cmd", "/c", "./go", "build", "-buildmode=c-shared", "-o", "$buildDir\\ast.so", goFile)
    logging.captureStandardOutput(LogLevel.QUIET)
    logging.captureStandardError(LogLevel.ERROR)
}

tasks.getByPath("build").dependsOn("compileGo")