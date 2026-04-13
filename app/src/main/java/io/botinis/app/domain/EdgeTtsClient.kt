package io.botinis.app.domain

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EdgeTtsClient @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"

    fun synthesize(
        text: String,
        outputFile: File,
        voice: String = "en-US-JennyNeural",
        rate: String = "+0%",
        pitch: String = "+0Hz"
    ): Result<Unit> {
        return try {
            val connectionId = UUID.randomUUID().toString()
            val url = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1?" +
                    "TrustedClientToken=$TRUSTED_CLIENT_TOKEN&ConnectionId=$connectionId"

            val latch = CountDownLatch(1)
            var audioBytes = byteArrayOf()
            var success = false
            var errorMessage: String? = null

            val request = Request.Builder()
                .url(url)
                .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36 Edg/114.0.1823.67")
                .build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                    // Send configuration
                    val configMessage = "X-Timestamp:${nowDate()}\r\n" +
                            "Content-Type:application/json; charset=utf-8\r\n" +
                            "Path:speech.config\r\n\r\n" +
                            "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{" +
                            "\"sentenceBoundaryEnabled\":\"false\"," +
                            "\"wordBoundaryEnabled\":\"false\"}," +
                            "\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"," +
                            "\"source\":\"EdgeTts\"}}}}"
                    webSocket.send(configMessage)

                    // Send SSML
                    val ssml = """
                        <speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>
                          <voice name='$voice'>
                            <prosody rate='$rate' pitch='$pitch'>
                              ${escapeXml(text)}
                            </prosody>
                          </voice>
                        </speak>
                    """.trimIndent()

                    val ssmlMessage = "X-Timestamp:${nowDate()}\r\n" +
                            "Content-Type:application/ssml+xml\r\n" +
                            "Path:ssml\r\n\r\n" + ssml
                    webSocket.send(ssmlMessage)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val data = bytes.toByteArray()
                    // Audio data starts after the header
                    val headerEnd = findHeaderEnd(data)
                    if (headerEnd > 0 && headerEnd < data.size) {
                        audioBytes += data.copyOfRange(headerEnd, data.size)
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    // Check for turn end
                    if (text.contains("turn.end")) {
                        webSocket.close(1000, "Done")
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    success = audioBytes.isNotEmpty()
                    if (!success && errorMessage == null) {
                        errorMessage = "No audio received"
                    }
                    latch.countDown()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                    errorMessage = "WebSocket error: ${t.message}"
                    latch.countDown()
                }
            }

            client.newWebSocket(request, listener)

            // Wait for completion (max 60 seconds)
            if (!latch.await(60, TimeUnit.SECONDS)) {
                return Result.failure(Exception("TTS timeout"))
            }

            if (success && audioBytes.isNotEmpty()) {
                FileOutputStream(outputFile).use { it.write(audioBytes) }
                Result.success(Unit)
            } else {
                Result.failure(Exception("TTS failed: $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findHeaderEnd(data: ByteArray): Int {
        // Look for \r\n\r\n which separates headers from body
        for (i in 0 until data.size - 3) {
            if (data[i] == '\r'.code.toByte() &&
                data[i + 1] == '\n'.code.toByte() &&
                data[i + 2] == '\r'.code.toByte() &&
                data[i + 3] == '\n'.code.toByte()) {
                // Header has a 2-byte length prefix
                val headerLen = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                return headerLen + 2
            }
        }
        return -1
    }

    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun nowDate(): String {
        return java.text.SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", java.util.Locale.US)
            .format(java.util.Date())
            .replace("GMT+00:00", "GMT")
    }
}
