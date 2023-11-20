package org.elliotnash.tim

import io.github.cdimascio.dotenv.dotenv
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.elliotnash.blueify.Client
import sun.misc.Signal
import java.io.File

const val DEFAULT_POOL_SIZE = 5
const val DEFAULT_PREFIX = "!"

val json = Json

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>): Unit = runBlocking {
    if (args.size != 3) {
        println("Error: Incorrect arguments!\nUsage: java -jar TiM.jar <path/to/.env> <path/to/worker>")
        return@runBlocking
    }

    val deFile = File(args[0])
    val workerFile = File(args[1])
    val fontsDir = File(args[2])

    if (!deFile.isFile || !workerFile.isFile || !fontsDir.isDirectory) {
        println("Error: Incorrect arguments!\nUsage: java -jar TiM.jar <path/to/.env> <path/to/worker> <path/to/fonts>")
        return@runBlocking
    }

    val dotenv = dotenv {
        directory = deFile.canonicalFile.parent
        filename = deFile.name
    }

    val url = dotenv["BB_URL"]
    val password = dotenv["BB_PASSWORD"]
    val poolSize = dotenv["POOL_SIZE"]?.toIntOrNull() ?: DEFAULT_POOL_SIZE
    val prefix = dotenv["PREFIX"] ?: DEFAULT_PREFIX

    if (url.isNullOrBlank()) {
        logger.error { "BB_URL must be set in .env" }
        return@runBlocking
    }
    if (password.isNullOrBlank()) {
        logger.error { "BB_PASSWORD must be set in .env" }
        return@runBlocking
    }

    logger.debug {"DEBUG TEST"}

    val client = Client(url, password)

    val listener = MessageListener(prefix = prefix, renderer = TypstRenderer(workerFile.canonicalPath, fontsDir.canonicalPath, poolSize))
    client.registerEventListener(listener)

    Signal.handle(Signal("INT")) {
        client.shutdown()
    }

    client.run()
}
