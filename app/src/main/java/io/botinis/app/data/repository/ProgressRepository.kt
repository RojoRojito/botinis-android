package io.botinis.app.data.repository

import io.botinis.app.data.local.dao.SessionDao
import io.botinis.app.data.local.dao.UserProgressDao
import io.botinis.app.data.model.CEFRLevel
import io.botinis.app.data.model.Session
import io.botinis.app.data.model.UserProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepository @Inject constructor(
    private val userProgressDao: UserProgressDao,
    private val sessionDao: SessionDao
) {
    fun getProgress(): Flow<UserProgress?> = userProgressDao.getProgress()

    fun getSessions(): Flow<List<Session>> = sessionDao.getAllSessions()

    suspend fun initializeProgressIfMissing() {
        if (userProgressDao.getProgressOnce() == null) {
            userProgressDao.insert(
                UserProgress(
                    id = 0,
                    totalXP = 0,
                    currentLevel = CEFRLevel.A2.name,
                    sessionsCompleted = 0,
                    totalTurns = 0,
                    perfectTurns = 0,
                    grammarScore = 0f,
                    vocabularyScore = 0f,
                    fluencyScore = 0f,
                    currentStreak = 0,
                    longestStreak = 0,
                    lastSessionDate = null,
                    completedScenarios = emptyList(),
                    unlockedAchievements = emptyList()
                )
            )
        }
    }

    suspend fun updateAfterSession(
        xpEarned: Int,
        turnsCount: Int,
        perfectTurns: Int,
        grammarScore: Float,
        vocabularyScore: Float,
        fluencyScore: Float,
        scenarioId: String
    ) {
        val current = userProgressDao.getProgressOnce() ?: return

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val lastDate = current.lastSessionDate

        val newStreak = calculateStreak(lastDate, today)
        val longestStreak = maxOf(current.longestStreak, newStreak)

        val alpha = 0.1f
        val newGrammar = current.grammarScore * (1 - alpha) + grammarScore * alpha
        val newVocab = current.vocabularyScore * (1 - alpha) + vocabularyScore * alpha
        val newFluency = current.fluencyScore * (1 - alpha) + fluencyScore * alpha

        val newLevel = determineLevelUp(newFluency, current.sessionsCompleted + 1, current.currentLevel)

        val updated = current.copy(
            totalXP = current.totalXP + xpEarned,
            currentLevel = newLevel,
            sessionsCompleted = current.sessionsCompleted + 1,
            totalTurns = current.totalTurns + turnsCount,
            perfectTurns = current.perfectTurns + perfectTurns,
            grammarScore = newGrammar,
            vocabularyScore = newVocab,
            fluencyScore = newFluency,
            currentStreak = newStreak,
            longestStreak = longestStreak,
            lastSessionDate = today,
            completedScenarios = current.completedScenarios + scenarioId
        )

        userProgressDao.update(updated)
    }

    private fun calculateStreak(lastDate: String?, today: String): Int {
        if (lastDate == null) return 1
        if (lastDate == today) return 1

        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val last = LocalDate.parse(lastDate, formatter)
        val current = LocalDate.parse(today, formatter)

        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(last, current).toInt()
        return if (daysBetween == 1) 1 else 0
    }

    private fun determineLevelUp(
        fluencyScore: Float,
        sessionsCompleted: Int,
        currentLevel: String
    ): String {
        return when {
            currentLevel == CEFRLevel.A2.name && fluencyScore >= 70f && sessionsCompleted >= 5 ->
                CEFRLevel.B1.name
            currentLevel == CEFRLevel.B1.name && fluencyScore >= 75f && sessionsCompleted >= 10 ->
                CEFRLevel.B2.name
            else -> currentLevel
        }
    }
}
