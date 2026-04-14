package io.botinis.app.ui.screens.scenario

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.botinis.app.data.model.CEFRLevel
import io.botinis.app.domain.ScenarioCatalog
import io.botinis.app.ui.theme.*

@Composable
fun ScenarioSelectScreen(
    onScenarioSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(io.botinis.app.ui.theme.BackgroundPrimary)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Choose Your World",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = io.botinis.app.ui.theme.TextPrimary
                )
                Text(
                    text = "Each world unlocks new vocabulary",
                    fontSize = 14.sp,
                    color = io.botinis.app.ui.theme.TextSecondary
                )
            }
            // Current level badge
            Card(
                colors = CardDefaults.cardColors(containerColor = io.botinis.app.ui.theme.AccentPrimary.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "A2",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = io.botinis.app.ui.theme.AccentPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Worlds list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ScenarioCatalog.allScenarios) { scenario ->
                val isUnlocked = isScenarioUnlocked(scenario.level)
                WorldCard(
                    scenario = scenario,
                    isUnlocked = isUnlocked,
                    onClick = { if (isUnlocked) onScenarioSelected(scenario.id) }
                )
            }
        }
    }
}

@Composable
fun WorldCard(
    scenario: io.botinis.app.data.model.Scenario,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isUnlocked, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) io.botinis.app.ui.theme.BackgroundSecondary else io.botinis.app.ui.theme.BackgroundSecondary.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getWorldEmoji(scenario.id),
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = scenario.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = io.botinis.app.ui.theme.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${scenario.character} • ${scenario.level.displayName}",
                    fontSize = 13.sp,
                    color = io.botinis.app.ui.theme.TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Vocabulary preview
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    scenario.targetVocabulary.take(3).forEach { vocab ->
                        Text(
                            text = vocab,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, io.botinis.app.ui.theme.AccentPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = io.botinis.app.ui.theme.TextPrimary
                        )
                    }
                    Text(
                        text = "+${scenario.targetVocabulary.size - 3}",
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(io.botinis.app.ui.theme.BackgroundTertiary)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        color = io.botinis.app.ui.theme.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${scenario.objectives.size} objectives • ${scenario.targetVocabulary.size} words",
                    fontSize = 12.sp,
                    color = io.botinis.app.ui.theme.TextDisabled
                )
            }

            if (isUnlocked) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Go",
                    tint = io.botinis.app.ui.theme.AccentPrimary
                )
            } else {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = io.botinis.app.ui.theme.AccentPrimary
                )
            }
        }
    }
}

fun isScenarioUnlocked(level: CEFRLevel): Boolean {
    // For MVP: A2 unlocked, B1/B2 locked
    return level == CEFRLevel.A2
}

fun getWorldEmoji(scenarioId: String): String {
    return when (scenarioId) {
        "coffee_friend" -> "☕"
        "job_interview" -> "💼"
        "supermarket" -> "🛒"
        "trip_planning" -> "✈️"
        "doctor_visit" -> "🏥"
        "movie_discussion" -> "🎬"
        "restaurant" -> "🍕"
        "tech_support" -> "📱"
        else -> "🌍"
    }
}
