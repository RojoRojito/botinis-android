package io.botinis.app.data.model

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val iconResId: Int,
    val condition: AchievementCondition,
    val xpReward: Int
)

sealed class AchievementCondition {
    data class SessionsCompleted(val count: Int) : AchievementCondition()
    data class PerfectScore(val count: Int) : AchievementCondition()
    data class Streak(val days: Int) : AchievementCondition()
    data class LevelUp(val level: String) : AchievementCondition()
    data class ScenarioCompleted(val scenarioId: String) : AchievementCondition()
    data class TotalXP(val xp: Int) : AchievementCondition()
}

val DEFAULT_ACHIEVEMENTS = listOf(
    Achievement(
        id = "first_steps",
        name = "First Steps",
        description = "Complete your first conversation",
        iconResId = android.R.drawable.btn_star,
        condition = AchievementCondition.SessionsCompleted(1),
        xpReward = 50
    ),
    Achievement(
        id = "getting_started",
        name = "Getting Started",
        description = "Complete 10 conversations",
        iconResId = android.R.drawable.btn_star,
        condition = AchievementCondition.SessionsCompleted(10),
        xpReward = 200
    ),
    Achievement(
        id = "perfect_score",
        name = "Perfect Score",
        description = "Get a perfect score in a session",
        iconResId = android.R.drawable.btn_star_big_on,
        condition = AchievementCondition.PerfectScore(1),
        xpReward = 100
    ),
    Achievement(
        id = "week_warrior",
        name = "Week Warrior",
        description = "Maintain a 7-day streak",
        iconResId = android.R.drawable.ic_menu_compass,
        condition = AchievementCondition.Streak(7),
        xpReward = 150
    ),
    Achievement(
        id = "level_up_b1",
        name = "Level Up!",
        description = "Reach B1 level",
        iconResId = android.R.drawable.ic_menu_send,
        condition = AchievementCondition.LevelUp("B1"),
        xpReward = 300
    ),
    Achievement(
        id = "level_up_b2",
        name = "Advanced!",
        description = "Reach B2 level",
        iconResId = android.R.drawable.ic_menu_send,
        condition = AchievementCondition.LevelUp("B2"),
        xpReward = 500
    ),
    Achievement(
        id = "xp_hunter",
        name = "XP Hunter",
        description = "Earn 5000 total XP",
        iconResId = android.R.drawable.ic_menu_gallery,
        condition = AchievementCondition.TotalXP(5000),
        xpReward = 250
    )
)
