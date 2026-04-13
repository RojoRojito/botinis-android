package io.botinis.app.domain

import io.botinis.app.data.model.ConversationTurn
import io.botinis.app.data.model.Feedback
import io.botinis.app.data.remote.GroqApiService
import io.botinis.app.data.remote.GroqChatRequest
import io.botinis.app.data.remote.GroqMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroqRepository @Inject constructor(
    private val apiService: GroqApiService
) {
    private val apiKey: String
        get() = BuildConfig.GROQ_API_KEY

    suspend fun transcribeAudio(audioFile: File): Result<String> {
        return try {
            val requestFile = audioFile.asRequestBody("audio/ogg".toMediaType())
            val audioPart = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            val model = "whisper-large-v3-turbo".toRequestBody("text/plain".toMediaType())

            val response = apiService.transcribeAudio(
                apiKey = "Bearer $apiKey",
                audio = audioPart,
                model = model
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.text)
            } else {
                Result.failure(Exception("Transcription failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChatResponse(
        systemPrompt: String,
        conversationHistory: List<ConversationTurn>
    ): Result<String> {
        return try {
            val messages = mutableListOf<GroqMessage>()
            messages.add(GroqMessage("system", systemPrompt))

            conversationHistory.forEach { turn ->
                messages.add(GroqMessage("user", turn.userTranscript))
                messages.add(GroqMessage("assistant", turn.botResponse))
            }

            val request = GroqChatRequest(
                model = "llama-3.1-8b-instant",
                messages = messages
            )

            val response = apiService.chatCompletion(
                apiKey = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful && response.body()?.choices?.isNotEmpty() == true) {
                Result.success(response.body()!!.choices[0].message.content)
            } else {
                Result.failure(Exception("Chat request failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeTurn(userText: String): Result<Feedback> {
        return try {
            val systemMsg = GroqMessage(
                "system",
                """You are an English grammar coach for a Spanish speaker. Analyze the user's English utterance.
                Return a JSON object with: 
                - corrections: array of {original, corrected, explanation}
                - strengths: array of things they did well
                - suggestions: array of improvement tips
                Keep explanations in Spanish. Be encouraging but precise."""
            )
            val userMsg = GroqMessage("user", "Analyze: $userText")

            val request = GroqChatRequest(
                model = "llama-3.1-8b-instant",
                messages = listOf(systemMsg, userMsg),
                temperature = 0.3f,
                max_tokens = 300
            )

            val response = apiService.analyzeFeedback(
                apiKey = "Bearer $apiKey",
                request = request
            )

            if (response.isSuccessful && response.body()?.choices?.isNotEmpty() == true) {
                val content = response.body()!!.choices[0].message.content
                // Parse JSON response into Feedback object
                Result.success(parseFeedbackFromResponse(content))
            } else {
                Result.failure(Exception("Feedback request failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseFeedbackFromResponse(content: String): Feedback {
        // Simplified parser — in production, use proper JSON parsing
        return Feedback(
            corrections = emptyList(),
            strengths = listOf("Good attempt!"),
            suggestions = listOf("Keep practicing!")
        )
    }
}
