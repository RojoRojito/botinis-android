package io.botinis.app.domain

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
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
    private var initAttempts = 0
    private val maxInitAttempts = 3

    suspend fun initialize(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        Log.d(TAG, "Initializing TTS (attempt ${initAttempts + 1})")

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "TTS initialized successfully")

                // Set audio attributes for proper routing
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)

                tts?.language = Locale.US
                tts?.setSpeechRate(1.0f)
                tts?.setPitch(1.0f)
                isInitialized = true
                initAttempts = 0

                // Check volume
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                Log.d(TAG, "Audio volume: $currentVolume / $maxVolume")

                if (currentVolume == 0) {
                    Log.w(TAG, "Media volume is 0 - TTS will be silent")
                }

                continuation.resume(Result.success(Unit))
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
                initAttempts++
                if (initAttempts >= maxInitAttempts) {
                    continuation.resume(Result.failure(Exception("TTS initialization failed after $maxInitAttempts attempts")))
                } else {
                    continuation.resume(Result.failure(Exception("TTS initialization failed, will retry")))
                }
            }
        }
    }

    suspend fun speak(text: String): Result<Unit> {
        Log.d(TAG, "speak() called with text length: ${text.length}")

        if (!isInitialized) {
            Log.d(TAG, "TTS not initialized, calling initialize()")
            val initResult = initialize()
            if (initResult.isFailure) {
                Log.e(TAG, "Initialization failed: ${initResult.exceptionOrNull()?.message}")
                return initResult
            }
        }

        return suspendCancellableCoroutine { continuation ->
            val utteranceId = "utterance_${System.currentTimeMillis()}"
            Log.d(TAG, "Setting up utterance listener for: $utteranceId")

            // Set listener BEFORE speak() - critical order
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS started: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS completed: $utteranceId")
                    continuation.resume(Result.success(Unit))
                }

                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS error: $utteranceId")
                    continuation.resume(Result.failure(Exception("TTS playback error")))
                }
            })

            Log.d(TAG, "Calling tts.speak() with QUEUE_FLUSH")
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

            if (result != TextToSpeech.SUCCESS) {
                Log.e(TAG, "speak() returned error code: $result")
                continuation.resume(Result.failure(Exception("Failed to speak: error code $result")))
            } else {
                Log.d(TAG, "speak() succeeded, waiting for completion callback")
            }
        }
    }

    fun speakAsync(text: String, callback: (Boolean) -> Unit) {
        Log.d(TAG, "speakAsync() called with text length: ${text.length}")

        if (!isInitialized || tts == null) {
            Log.d(TAG, "TTS not initialized or null, initializing synchronously")
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    Log.d(TAG, "TTS initialized in speakAsync")

                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    tts?.setAudioAttributes(audioAttributes)

                    tts?.language = Locale.US
                    tts?.setSpeechRate(1.0f)
                    tts?.setPitch(1.0f)
                    isInitialized = true
                    doSpeak(text, callback)
                } else {
                    Log.e(TAG, "TTS init failed in speakAsync: $status")
                    callback(false)
                }
            }
        } else {
            doSpeak(text, callback)
        }
    }

    private fun doSpeak(text: String, callback: (Boolean) -> Unit) {
        val utteranceId = "utterance_${System.currentTimeMillis()}"
        Log.d(TAG, "doSpeak() setting up listener for: $utteranceId")

        // Set listener BEFORE speak() - critical order
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.d(TAG, "doSpeak started: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.d(TAG, "doSpeak completed: $utteranceId")
                callback(true)
            }

            override fun onError(utteranceId: String?) {
                Log.e(TAG, "doSpeak error: $utteranceId")
                callback(false)
            }
        })

        Log.d(TAG, "doSpeak() calling tts.speak()")
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        if (result != TextToSpeech.SUCCESS) {
            Log.e(TAG, "doSpeak() speak() returned error: $result")
            callback(false)
        } else {
            Log.d(TAG, "doSpeak() speak() succeeded")
        }
    }

    fun stop() {
        Log.d(TAG, "stop() called")
        tts?.stop()
    }

    fun shutdown() {
        Log.d(TAG, "shutdown() called")
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    fun isSpeaking(): Boolean {
        val speaking = tts?.isSpeaking ?: false
        Log.d(TAG, "isSpeaking(): $speaking")
        return speaking
    }

    companion object {
        private const val TAG = "AndroidTts"
    }
}
