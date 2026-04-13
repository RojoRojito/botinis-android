package io.botinis.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scenarioId: String,
    val level: String,
    val startTime: Long,
    val endTime: Long? = null,
    val turnsCount: Int = 0,
    val xpEarned: Int = 0,
    val objectivesCompleted: Int = 0,
    val isComplete: Boolean = false
)
