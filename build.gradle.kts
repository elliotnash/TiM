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
    implementation("com.akuleshov7:ktoml-core:0.5.0")
    implementation("com.akuleshov7:ktoml-file:0.5.0")

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
        doFirst {
            exec {
                commandLine("chmod", "+x", "./scripts/build-rust.sh")
            }
        }
        commandLine("./scripts/build-rust.sh")
    }

    build {
        dependsOn(fatJar) // Trigger fat jar creation during build
    }

    // Build rust when building kotlin
    compileKotlin {
        dependsOn(buildRust)
    }
    compileTestKotlin {
        dependsOn(buildRust)
    }
}
