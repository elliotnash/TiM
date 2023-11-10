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

    // KotlinX libraries
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
    implementation("io.github.oshai:kotlin-logging-jvm:5.0.2")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("org.elliotnash.tim.TiMKt")
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
