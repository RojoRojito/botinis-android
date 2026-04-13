package io.botinis.app.data.remote

import io.botinis.app.data.model.ConversationTurn
import io.botinis.app.data.model.Feedback
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface GroqApiService {

    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribeAudio(
        @Header("Authorization") apiKey: String,
        @Part audio: MultipartBody.Part,
        @Part("model") model: RequestBody
    ): Response<GroqTranscriptionResponse>

    @POST("v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: GroqChatRequest
    ): Response<GroqChatResponse>

    @POST("v1/chat/completions")
    suspend fun analyzeFeedback(
        @Header("Authorization") apiKey: String,
        @Body request: GroqChatRequest
    ): Response<GroqChatResponse>
}
