package org.elliotnash.tim

import io.github.cdimascio.dotenv.dotenv
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.elliotnash.blueify.Client

const val DEFAULT_POOL_SIZE = 5
const val DEFAULT_PREFIX = "!"

val json = Json
val dotenv = dotenv()

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>): Unit = runBlocking {
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

    val client = Client(url, password)
    val listener = MessageListener(poolSize = poolSize, prefix = prefix)
    client.registerEventListener(listener)

    client.run()
}
