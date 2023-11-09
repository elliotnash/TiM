plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    application
}

group = "org.elliotnash"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.elliotnash.org/snapshots")
    mavenLocal()
}

dependencies {
    implementation("org.elliotnash.blueify:Blueify:1.2.0-SNAPSHOT")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("org.elliotnash.typst.RendererKt")
}

task<Exec>("buildWorkerDebug") {
    doFirst {
        workingDir("./worker")
        commandLine("cargo", "build")
    }
}

task<Exec>("buildWorkerRelease") {
    doFirst {
        workingDir("./worker")
        commandLine("cargo", "build", "--release")
//        file("worker/target/release/worker").renameTo(file("src/main/resources/worker"))
    }
}

tasks.compileKotlin {
    dependsOn("buildWorkerRelease")
}
