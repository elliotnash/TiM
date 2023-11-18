package org.elliotnash.tim

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.elliotnash.blueify.EventListener
import org.elliotnash.blueify.model.Message
import org.elliotnash.tim.worker.*
import java.lang.Exception

class MessageListener(val workerPath: String, val poolSize: Int, val prefix: String) : EventListener {
    private val logger = KotlinLogging.logger {}
    private val renderer = TypstRenderer(workerPath, poolSize)
    private val codeRegex = Regex("(?<= |^)\\$[^$]+?\\$(?= |$)")

    override fun onMessage(message: Message) {
        val text = message.text.lowercase().trim()
        logger.trace {"received message: $text"}
        when {
            text == "${prefix}ping" -> {
                render(message, "#set text(fill: gradient.linear(..color.map.rainbow))\nPong!")
            }
            text == "${prefix}bing" -> {
                render(message, "#set text(fill: gradient.linear(rgb(\"#ff0000\"), rgb(\"#0000ff\")))\nBong!")
            }
            text.startsWith("${prefix}render") -> {
                render(message, message.text.substring(8))
            }
            else -> {
                val matches = codeRegex.findAll(message.text)
                for (match in matches) {
                    render(message, match.value)
                }
            }
        }
    }

    private fun render(message: Message, code: String) {
        runBlocking {
            try {
                val output = renderer.render(code)
                message.reply("typst.png", output)
            } catch (e: TypstRenderError) {
                message.reply("Typst compilation failed: ${e.message}!")
            } catch (e: TypstTimeoutError) {
                message.reply("Typst compilation timed out!")
            }
        }
    }
}

class TypstRenderer(private val workerPath: String, val poolSize: Int) {
    private val logger = KotlinLogging.logger {}
    private val workerPool = List(poolSize) { Worker(workerPath) }

    private fun getPooledWorker() = workerPool.minBy { it.queueLength }

    suspend fun render(
        code: String,
        pageSize: PageSize = PageSize.Auto,
        theme: Theme = Theme.Dark,
        transparent: Boolean = true
    ): ByteArray {
        val options = RenderOptions(pageSize, theme, transparent)
        val request = RenderRequest(code, options)

        val worker = getPooledWorker()
        val response = worker.request(Render(request))

        return when (response) {
            is RenderSuccess -> response.renderSuccess
            is RenderError -> throw TypstRenderError(response.renderError)
            else -> throw TypstTimeoutError()
        }
    }
}

abstract class TypstError(message: String) : Exception(message)

class TypstRenderError(message: String) : TypstError(message)

class TypstTimeoutError : TypstError("render timed out!")
