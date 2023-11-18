import com.jtitor.plugin.gradle.rust.tasks.RustBuild
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    id("com.jtitor.rust") version "0.1.4"
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
    implementation("org.elliotnash.blueify:Blueify:1.3.0-SNAPSHOT")

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

//task<Exec>("buildWorkerDebug") {
//    doFirst {
//        workingDir("./worker")
//        commandLine("cargo", "build")
//    }
//}
//
//task("buildWorkerRelease") {
//    val cargo = ProcessBuilder("cargo", "build", "--release")
//        .directory(file("worker"))
//        .start()
//
//    // Block on rust building
//    while(cargo.isAlive) {
//        if (cargo.errorStream.available() > 0) {
//            val b = cargo.errorStream.read()
//            print(b.toChar())
//        }
//    }
//}

//task<Copy>("copyTest") {
//    dependsOn("buildWorkerRelease")
//    doLast {
//        println("DOING LAST")
//        from(file("worker/target/release/worker")).into(file("build/libs/worker"))
//    }
//}
//
//task<Exec>("copyFiles") {
//    dependsOn("buildWorkerRelease")
//
//    // Copies rust worker
//    val workerFile = file("build/libs/worker")
//    workerFile.ensureParentDirsCreated()
//    file("worker/target/release/worker").copyTo(workerFile, overwrite = true)
//    commandLine("chmod", "+x", workerFile.canonicalPath)
//    // Copies launcher script
//    val launcherFile = file("build/libs/TiM")
//    launcherFile.ensureParentDirsCreated()
//    file("scripts/launch-script.sh").copyTo(launcherFile, overwrite = true)
//    commandLine("chmod", "+x", launcherFile.canonicalPath)
//    // Copies .env file
//    file(".env").copyTo(file("build/libs/.env"), overwrite = true)
//}

//tasks.compileKotlin {
////    dependsOn("buildWorkerRelease")
//    dependsOn("copyFiles")
//}



tasks {
    val fatJar = register<Jar>("fatJar") {
        archiveFileName.set("TiM.jar")
        dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources")) // We need this for Gradle optimization to work
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest { attributes(mapOf("Main-Class" to application.mainClass)) } // Provided we set it up in the application plugin configuration
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }

    val buildRust = register<Exec>("buildRust") {
        dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources"))
        doFirst {
            exec {
                commandLine("chmod", "+x", "./scripts/build-rust.sh")
            }
        }
        commandLine("./scripts/build-rust.sh")
    }

    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
        dependsOn(buildRust)
    }
}
