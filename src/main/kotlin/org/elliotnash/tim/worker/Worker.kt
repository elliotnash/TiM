package org.elliotnash.tim.worker

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import org.elliotnash.tim.PageSize
import org.elliotnash.tim.Theme
import org.elliotnash.tim.json
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val EOT = 0x04

class Worker(private val workerPath: String, private val fontDir: String, val timeout: Duration = 4.seconds) {
    private var process: Process? = null
    private var requestChannel = Channel<QueuedRequest>(Channel.UNLIMITED)
    private val logger = KotlinLogging.logger("Worker@${hashCode().toString(16)}")
    private val rustLogger = LoggerFactory.getLogger("Worker-Rust@${hashCode().toString(16)}")

    val isWorking
        get() = queueLength == 0

    var queueLength = 0
        private set

    private var _version = CompletableFuture<Version>()
    val version: Version?
        get() = _version.getNow(null)

    val isReady: Boolean
        get() = version != null

    suspend fun awaitReady() {
        _version.await()
    }

    fun stop() {
        logger.debug {"stopping worker"}
        rwAlive = false
        rwThread.interrupt()
        logThread.interrupt()
        process?.destroy()
        // Wait until thread stops
        while (rwThread.isAlive) {}
        logger.debug {"RW THREAD DOWN"}
        while (logThread.isAlive) {}
        logger.debug {"LOG THREAD DOWN"}
    }

    private val logThread = thread {
        try {
            while (!Thread.currentThread().isInterrupted) {
                if (process != null) {
                    try {
                        // Read from stderr
                        val buf = StringBuffer()
                        while (!Thread.currentThread().isInterrupted && process != null) {
                            if (process!!.errorStream.available() > 0) {
                                val byte = process!!.errorStream.read()
                                if (byte < 0) {
                                    break
                                }
                                if (byte == EOT) {
                                    // We hit the terminator so the log is complete, time to parse
                                    try {
                                        val message: LogMessage = json.decodeFromString(buf.toString())
                                        rustLogger.atLevel(message.level).log("(${message.file}:${message.line}) - ${message.message}")
                                    } catch (_: IllegalArgumentException) {}
                                    break
                                } else {
                                    buf.append(byte.toChar())
                                }
                            }
                        }
                    } catch (_: IOException) {}
                }
            }
        } catch (_: InterruptedException) {
            logger.debug {"Log thread interrupted"}
        }
    }

    private var rwAlive = true
    private val rwThread = thread {
        try {
            runBlocking {
                while (rwAlive) {
                    // Need to start a new process
                    logger.debug { "Starting new worker" }
                    process = newWorkerProcess()

                    process@ while (rwAlive) {
                        // We wait to receive the next request
                        val request = requestChannel.receive()

                        // Once we have a request, we send it
                        val data = json.encodeToString(request.request)
                        withContext(Dispatchers.IO) {
                            process!!.outputStream.write(data.toByteArray())
                            process!!.outputStream.flush()
                        }

                        // Now we read the response
                        val endTime = Clock.System.now() + timeout
                        val buf = StringBuffer()
                        while (rwAlive) {
                            if (Clock.System.now() > endTime) {
                                // We should kill the process if it's been longer than the timeout
                                logger.debug { "Worker timed out!" }
                                request.response.complete(null)
                                break@process
                            } else if (process!!.inputStream.available() > 0) {
                                val byte = process!!.inputStream.read()
                                if (byte < 0) {
                                    // The process died, we need to start a new one :)
                                    logger.debug { "Worker died!" }
                                    request.response.complete(null)
                                    break@process
                                }
                                if (byte == EOT) {
                                    // We hit the terminator so our response is complete, time to parse
                                    val message = tryParseResponse(buf.toString())
                                    logger.trace {"Received message: $message"}
                                    request.response.complete(message)
                                    break
                                } else {
                                    buf.append(byte.toChar())
                                }
                            }
                        }
                    }

                    // The process either died or we're shutting down, either way the old process should be cleaned up
                    process?.destroy()
                }
            }
        } catch (_: InterruptedException) {
            logger.debug {"Read write thread interrupted"}
            process?.destroy()
        }
    }

    private fun newWorkerProcess(): Process {
        return ProcessBuilder(workerPath, fontDir).start()
    }

    init {
        thread {
            runBlocking {
                // fetch version info
                val response = request(VersionRequest()) as VersionResponse
                logger.debug {response.version}
                _version.complete(response.version)

//                val test = request(Render(RenderRequest("hi", RenderOptions(PageSize.Default, Theme.Dark, false))))
            }
        }

        Runtime.getRuntime().addShutdownHook(thread(start = false) {
            logger.debug {"Shutting down worker"}
            stop()
        })
    }

    private fun tryParseResponse(data: String): Response? {
        return try {
            json.decodeFromString(data)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    suspend fun request(request: Request): Response? {
        queueLength++
        val qr = QueuedRequest(request)

        requestChannel.send(qr)

        val result = qr.response.await()
        queueLength--
        return result
    }
}

data class QueuedRequest(
    val request: Request,
    val response: CompletableFuture<Response?> = CompletableFuture<Response?>()
)
