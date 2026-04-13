package io.botinis.app.data.model

data class Scenario(
    val id: String,
    val name: String,
    val description: String,
    val character: String,
    val level: CEFRLevel,
    val objectives: List<String>,
    val targetVocabulary: List<String>,
    val systemPrompt: String,
    val voice: String,
    val iconResId: Int
)

enum class CEFRLevel(val displayName: String, val xpMultiplier: Float) {
    A2("A2 - Elementary", 1.0f),
    B1("B1 - Intermediate", 1.5f),
    B2("B2 - Upper Intermediate", 2.0f)
}
