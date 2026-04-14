package io.botinis.app.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.botinis.app.ui.theme.*

@Composable
fun DashboardScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundPrimary)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "Tu Progreso",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Fluency score hero (circular progress placeholder)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BackgroundSecondary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "0",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary
                )
                Text(
                    text = "/10",
                    fontSize = 20.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fluency Score",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Score breakdown
        Text(
            text = "Score Breakdown",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        ScoreBar("Grammar", 0f)
        Spacer(modifier = Modifier.height(8.dp))
        ScoreBar("Vocabulary", 0f)
        Spacer(modifier = Modifier.height(8.dp))
        ScoreBar("Fluency", 0f)

        Spacer(modifier = Modifier.height(24.dp))

        // Stats
        Text(
            text = "Estadísticas",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("🔥", "0", "Racha")
            StatItem("📅", "0", "Sesiones")
            StatItem("⭐", "0", "XP")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Worlds completed
        Text(
            text = "Mundos Completados",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Aún no has completado ningún mundo. ¡Empieza a practicar!",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )
    }
}

@Composable
fun ScoreBar(label: String, score: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 14.sp, color = TextSecondary)
            Text(text = "${score.toInt()}/10", fontSize = 14.sp, color = AccentPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BackgroundTertiary)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(score / 10f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentPrimary)
            )
        }
    }
}

@Composable
fun StatItem(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = emoji, fontSize = 24.sp)
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
    }
}
