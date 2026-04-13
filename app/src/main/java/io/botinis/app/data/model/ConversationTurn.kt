package io.botinis.app.data.model

data class ConversationTurn(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userTranscript: String,
    val feedback: Feedback? = null,
    val botResponse: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPerfect: Boolean = false
)

data class Feedback(
    val corrections: List<Correction>,
    val strengths: List<String>,
    val suggestions: List<String>
)

data class Correction(
    val original: String,
    val corrected: String,
    val explanation: String
)
