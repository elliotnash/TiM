package org.elliotnash.tim

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.elliotnash.blueify.EventListener
import org.elliotnash.blueify.model.Message
import org.elliotnash.tim.worker.*
import java.lang.Exception

private val logger = KotlinLogging.logger {}

class MessageListener(private val prefix: String, private val renderer: TypstRenderer) : EventListener {
    private val codeRegex = Regex("(?<= |^)\\$[^$]+?\\$(?= |$)")

    override fun onMessage(message: Message) {
        val text = message.text.trim()
            // Replace fancy quotes
            .replace(Regex("[\u201C\u201D]"), "\"")
            .replace(Regex("[\u2018\u2019]"), "\'")
        val lc = text.lowercase()
        logger.trace {"received message: $text"}
        when {
            lc == "${prefix}ping" -> {
                render(message, "#set text(fill: gradient.linear(..color.map.rainbow))\nPong!")
            }
            lc == "${prefix}bing" -> {
                render(message, "#set text(fill: gradient.linear(rgb(\"#ff0000\"), rgb(\"#0000ff\")))\nBong!")
            }
            lc.startsWith("${prefix}render") -> {
                render(message, text.substring(8))
            }
            else -> {
                val matches = codeRegex.findAll(text)
                for (match in matches) {
                    render(message, match.value)
                }
            }
        }
    }

    override fun onShutdown() {
        renderer.shutdown()
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

class TypstRenderer(private val workerPath: String, private val fontDir: String, val poolSize: Int) {
    private val workerPool = List(poolSize) { Worker(workerPath, fontDir) }

    private fun getPooledWorker() = workerPool.minBy { it.queueLength }

    suspend fun render(
        code: String,
        pageSize: PageSize = PageSize.Auto,
        theme: Theme = Theme.Dark,
        transparent: Boolean = false
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

    fun shutdown() {
        for (worker in workerPool) {
            worker.stop()
        }
    }
}

abstract class TypstError(message: String) : Exception(message)

class TypstRenderError(message: String) : TypstError(message)

class TypstTimeoutError : TypstError("render timed out!")
