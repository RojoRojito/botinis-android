package io.botinis.app.data.remote

data class GroqTranscriptionResponse(
    val text: String
)

data class GroqChatRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 500
)

data class GroqMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

data class GroqChatResponse(
    val choices: List<GroqChoice>
)

data class GroqChoice(
    val message: GroqResponseMessage
)

data class GroqResponseMessage(
    val role: String,
    val content: String
)
