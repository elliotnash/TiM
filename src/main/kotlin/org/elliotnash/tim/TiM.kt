package org.elliotnash.tim

import com.akuleshov7.ktoml.Toml
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.elliotnash.blueify.Client
import sun.misc.Signal
import java.io.File

val json = Json

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>): Unit = runBlocking {
    if (args.size != 3) {
        println("Error: Incorrect arguments!\nUsage: java -jar TiM.jar <path/to/config.toml> <path/to/worker> <path/to/fonts>")
        return@runBlocking
    }

    val configFile = File(args[0])
    val workerFile = File(args[1])
    val fontsDir = File(args[2])

    if (!configFile.isFile || !workerFile.isFile || !fontsDir.isDirectory) {
        println("Error: Incorrect arguments!\nUsage: java -jar TiM.jar <path/to/config.toml> <path/to/worker> <path/to/fonts>")
        return@runBlocking
    }

    val config: Config = Toml.decodeFromString(configFile.readText())

    val client = Client(config.url, config.password)

    val listener = MessageListener(prefix = config.prefix, renderer = TypstRenderer(workerFile.canonicalPath, fontsDir.canonicalPath, config.poolSize))
    client.registerEventListener(listener)

    Signal.handle(Signal("INT")) {
        client.shutdown()
    }

    client.run()
}
