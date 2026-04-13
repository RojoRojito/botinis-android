package io.botinis.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 0,
    val totalXP: Int = 0,
    val currentLevel: String = "A2",
    val sessionsCompleted: Int = 0,
    val totalTurns: Int = 0,
    val perfectTurns: Int = 0,
    val grammarScore: Float = 0f,
    val vocabularyScore: Float = 0f,
    val fluencyScore: Float = 0f,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastSessionDate: String? = null,
    val completedScenarios: List<String> = emptyList(),
    val unlockedAchievements: List<String> = emptyList()
)
