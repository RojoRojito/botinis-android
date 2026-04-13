package io.botinis.app.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepository @Inject constructor() {
    private val _progress = MutableStateFlow(UserProgressState())
    val progress: StateFlow<UserProgressState> = _progress.asStateFlow()

    fun updateAfterSession(
        xpEarned: Int,
        turnsCount: Int,
        perfectTurns: Int,
        grammarScore: Float,
        vocabularyScore: Float,
        fluencyScore: Float,
        scenarioId: String
    ) {
        val current = _progress.value
        val alpha = 0.1f
        _progress.value = current.copy(
            totalXP = current.totalXP + xpEarned,
            sessionsCompleted = current.sessionsCompleted + 1,
            totalTurns = current.totalTurns + turnsCount,
            perfectTurns = current.perfectTurns + perfectTurns,
            grammarScore = current.grammarScore * (1 - alpha) + grammarScore * alpha,
            vocabularyScore = current.vocabularyScore * (1 - alpha) + vocabularyScore * alpha,
            fluencyScore = current.fluencyScore * (1 - alpha) + fluencyScore * alpha,
            completedScenarios = current.completedScenarios + scenarioId
        )
    }
}

data class UserProgressState(
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
    val completedScenarios: List<String> = emptyList(),
    val unlockedAchievements: List<String> = emptyList()
)
