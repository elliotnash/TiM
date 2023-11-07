package org.elliotnash.typst.worker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import org.elliotnash.typst.json
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread

const val EOT = 0x04

class Worker {
    private var process = ProcessBuilder("worker/target/release/worker").start()
    private var processMutex = Mutex()
    private var requestMutex = Mutex()
    private var _version = CompletableFuture<Version>()
    val version: Version?
        get() = _version.getNow(null)

    val isReady: Boolean
        get() = version != null

    suspend fun awaitReady() {
        _version.await()
    }

    init {
        thread {
            runBlocking {
                while (true) {
                    var alive = true
                    while (alive ) {
                        val buf = StringBuffer()
                        while (true) {
                            val byte = process.inputStream.read()
                            if (byte < 0) {
                                // The process died, we need to start a new one :)
                                alive = false
                                break
                            }
                            if (byte == EOT) {
                                val res = tryParseResponse(buf.toString())
                                if (res != null) {
                                    onResponseReceived(res)
                                }
                                break
                            } else {
                                buf.append(byte.toChar())
                            }
                        }
                    }
                    // Start new worker
                    println("Starting new worker")
                    processMutex.lock(this)
                    process.destroy()
                    process = ProcessBuilder("worker/target/release/worker").start()
                    // If we got here we also didn't complete the last request
                    // so to prevent it from hanging, lets complete it
                    futures.poll()?.complete(null)
                    processMutex.unlock(this)
                }
            }
        }
        thread {
            runBlocking {
                val response = request(VersionRequest()) as VersionResponse
                _version.complete(response.version)
            }
        }
    }

    private fun tryParseResponse(data: String): Response? {
        return try {
            json.decodeFromString(data)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private val futures: Queue<CompletableFuture<Response?>> = LinkedList()

    private fun onResponseReceived(response: Response) {
        futures.poll()?.complete(response)
    }


    suspend fun request(request: Request): Response? {
        processMutex.lock(request)
        requestMutex.lock(request)

        val future = CompletableFuture<Response?>()
        futures.add(future)

        val data = json.encodeToString(request)
        withContext(Dispatchers.IO) {
            process.outputStream.write(data.toByteArray())
            process.outputStream.flush()
        }

        processMutex.unlock(request)

        val response =  future.await()
        requestMutex.unlock(request)
        return response
    }
}
