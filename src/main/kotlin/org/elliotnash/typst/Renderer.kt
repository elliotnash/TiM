package org.elliotnash.typst

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.elliotnash.blueify.Client
import org.elliotnash.blueify.EventListener
import org.elliotnash.blueify.model.Message
import org.elliotnash.typst.worker.*
import java.io.File
import java.lang.Exception

val json = Json

val test = """
    {"RenderSuccess":"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uVc2V1111VVX3Q+9zMu9srnqqquuuup+6GVe7pXNVVddddVV90Mv83KvbK666qqrrrofepmXe2Vz1VVXXXXV/dDLvNwrm6uuuuqqq+6HXublXtlcddVVV111P/QyL/fK5qqrrrrqqvuhl3m5VzZXXXXVVVfdD73My72yueqqq6666n7oZV7ulc1VV1111VX3Qy/zcq9srrrqqquuuh96mZd7ZXPVVVddddX90Mu83Cubq6666qqr7ode5uVe2Vx11VVXXXU/9DIv98rmqquuuuqq+6GXeblXNlddddVVV90PvczLvbK56qqrrrrqfuhlXu6VzVVXXXXVVfdDL/Nyr2yuuuqqq666H3qZl3tlc9VVV1111f3Qy7zcK5urrrrqqqvuh17m5V7ZXHXVVVdddT/0Mi/3yuaqq6666qr7oZd5uVc2V1111VVX3Q+9zMu9srnqqquuuup+6GVe7pXNVVddddVV90Mv83KvbK666qqrrrofepmXe2Vz1VVXXXXV/dDLvNwrm6uuuuqqq+6HXublXtlcddVVV111P/QyL/fK5qqrrrrqqvuhl3m5VzZXXXXVVVfdD73My72yueqqq6666n7oZV7ulc1VV1111VX3Qy/zcq9srrrqqquuuh96mZd7ZXPVVVddddX90Mu83Cubq6666qqr7ode5uVe2Vx11VVXXXU/9DIv98rmqquuuuqq+6GXeblXNlddddVVV90PvczLvbK56qqrrrrqfuhlXu6VzVVXXXXVVfdDL/Nyr2yuuuqqq666H3qZl3tlc9VVV1111f3Qy7zcK5urrrrqqqvuh17m5V7ZXHXVVVdddT/0Mi/3yuaqq6666qr7oZd5uVc2V1111VVX3Q+9zMu9srnqqquuuup+6GVe7pXNVVddddVV90Mv83KvbK666qqrrrofepmXe2Vz1VVXXXXV/dDLvNwrm6uuuuqqq+6HXublXtlcddVVV111P/QyL/fK5qqrrrrqqvuhl3m5VzZXXXXVVVfdD73My72yueqqq6666n7oZV7ulc1VV1111VX3Qy/zcq9srrrqqquuuh96mZd7ZXPVVVddddX90Mu83Cubq6666qqr7ode5uVe2Vx11VVXXXU/9DIv98rmqquuuuqq+6GXeblXNlddddVVV90PvczLvbK56qqrrrrqfuhlXu6VzVVXXXXVVffjHwHofHb79tDKxQAAAABJRU5ErkJggg"}
""".trimIndent()

fun main(args: Array<String>): Unit = runBlocking {
    val client = Client(System.getenv("SERVER_URL"), System.getenv("PASSWORD"))
    client.registerEventListener(MessageListener(client))
    client.run()
}

class MessageListener(val client: Client) : EventListener {
    private val renderer = TypstRenderer()
    private val codeRegex = Regex("(?<=( |^|\\n))\\$[^\$]+?\\$(?=( |$|\n))")

    override fun onMessage(message: Message) {
        if (message.text.startsWith("?render")) {
            render(message, message.text.substring(8))
        } else {
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

class TypstRenderer {
    private val worker = Worker()
    val version: Version?
        get() = worker.version

    var queueLength = 0
        private set

    suspend fun render(
        code: String,
        pageSize: PageSize = PageSize.Auto,
        theme: Theme = Theme.Dark,
        transparent: Boolean = true
    ): ByteArray {
        queueLength++

        val options = RenderOptions(pageSize, theme, transparent)
        val request = RenderRequest(code, options)

        val response = worker.request(Render(request))

        queueLength--

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
