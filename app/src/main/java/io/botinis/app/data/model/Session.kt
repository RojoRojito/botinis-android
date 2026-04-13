package io.botinis.app.data.model

data class Session(
    val id: Int = 0,
    val scenarioId: String = "",
    val level: String = "",
    val startTime: Long = 0L,
    val endTime: Long? = null,
    val turnsCount: Int = 0,
    val xpEarned: Int = 0,
    val objectivesCompleted: Int = 0,
    val isComplete: Boolean = false
)
