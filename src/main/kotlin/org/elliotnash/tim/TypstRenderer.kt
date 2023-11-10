package org.elliotnash.tim

import kotlinx.coroutines.runBlocking
import org.elliotnash.blueify.Client
import org.elliotnash.blueify.EventListener
import org.elliotnash.blueify.model.Message
import org.elliotnash.tim.worker.*
import java.lang.Exception

class MessageListener(val poolSize: Int, val prefix: String) : EventListener {
    private val renderer = TypstRenderer(poolSize)
    private val codeRegex = Regex("(?<=( |^|\\n))\\$[^$]+?\\$(?=( |$|\\n))")

    override fun onMessage(message: Message) {
        val text = message.text.lowercase().trim()
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
                val matches = codeRegex.matchEntire(message.text)
                if (matches != null) {
                    for (value in matches.groupValues) {
                        if (value.isNotBlank()) {
                            render(message, value)
                        }
                    }
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

class TypstRenderer(val poolSize: Int) {
    private val workerPool = List(poolSize) { Worker() }

    private fun getPooledWorker() = workerPool.minBy { it.queueLength }

    suspend fun render(
        code: String,
        pageSize: PageSize = PageSize.Auto,
        theme: Theme = Theme.Dark,
        transparent: Boolean = true
    ): ByteArray {
        val options = RenderOptions(pageSize, theme, transparent)
        val request = RenderRequest(code, options)

        val response = getPooledWorker().request(Render(request))

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
