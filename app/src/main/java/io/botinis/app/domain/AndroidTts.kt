package io.botinis.app.domain

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AndroidTts @Inject constructor(
    private val context: Context
) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    suspend fun initialize(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(1.0f)
                tts?.setPitch(1.0f)
                isInitialized = true
                continuation.resume(Result.success(Unit))
            } else {
                continuation.resume(Result.failure(Exception("TTS initialization failed")))
            }
        }
    }

    suspend fun speak(text: String): Result<Unit> {
        if (!isInitialized) {
            val initResult = initialize()
            if (initResult.isFailure) return initResult
        }

        return suspendCancellableCoroutine { continuation ->
            val utteranceId = "utterance_${System.currentTimeMillis()}"

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    continuation.resume(Result.success(Unit))
                }

                override fun onError(utteranceId: String?) {
                    continuation.resume(Result.failure(Exception("TTS playback error")))
                }
            })

            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                continuation.resume(Result.failure(Exception("Failed to speak: error code $result")))
            }
        }
    }

    fun speakAsync(text: String, callback: (Boolean) -> Unit) {
        if (!isInitialized) {
            // Initialize synchronously (best effort)
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.US
                    tts?.setSpeechRate(1.0f)
                    tts?.setPitch(1.0f)
                    isInitialized = true
                    doSpeak(text, callback)
                } else {
                    callback(false)
                }
            }
        } else {
            doSpeak(text, callback)
        }
    }

    private fun doSpeak(text: String, callback: (Boolean) -> Unit) {
        val utteranceId = "utterance_${System.currentTimeMillis()}"

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                callback(true)
            }

            override fun onError(utteranceId: String?) {
                callback(false)
            }
        })

        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            callback(false)
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking ?: false
}
