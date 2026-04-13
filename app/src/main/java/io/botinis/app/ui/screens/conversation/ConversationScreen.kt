package io.botinis.app.ui.screens.conversation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    scenarioId: String,
    onBack: () -> Unit,
    viewModel: ConversationViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scenario by viewModel.scenario.collectAsState()
    val listState = rememberLazyListState()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording()
        }
    }

    LaunchedEffect(scenarioId) {
        viewModel.initScenario(scenarioId)
    }

    LaunchedEffect(uiState.turns.size) {
        if (uiState.turns.isNotEmpty()) {
            listState.animateScrollToItem(uiState.turns.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(scenario?.name ?: "Conversation")
                        Text(
                            text = scenario?.character ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Toggle transcript visibility
                    IconButton(onClick = { viewModel.toggleTranscriptVisibility() }) {
                        Icon(
                            if (uiState.showTranscript) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (uiState.showTranscript) "Hide transcript" else "Show transcript"
                        )
                    }
                    if (uiState.allObjectivesComplete) {
                        FilledTonalIconButton(onClick = { viewModel.endSession() }) {
                            Icon(Icons.Default.Check, contentDescription = "Complete")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isBusy = uiState.isTranscribing || uiState.isGeneratingResponse || uiState.isPlayingAudio
                    val fabText = when {
                        uiState.isRecording -> "⏹ Parar"
                        uiState.isTranscribing -> "🎙 Transcribiendo..."
                        uiState.isGeneratingResponse -> "💭 Pensando..."
                        uiState.isPlayingAudio -> "🔊 Reproduciendo..."
                        else -> "🎤 Hablar"
                    }

                    Button(
                        onClick = {
                            if (uiState.isRecording) {
                                viewModel.stopRecording()
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    viewModel.startRecording()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isBusy || uiState.isRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isRecording)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        if (isBusy && !uiState.isRecording) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Text(fabText, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Error card
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Objectives
            if (scenario != null && uiState.objectivesCompleted.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🎯 Objetivos",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        scenario!!.objectives.forEachIndexed { index, objective ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = uiState.objectivesCompleted.getOrNull(index) == true,
                                    onCheckedChange = null
                                )
                                Text(
                                    text = objective,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Session complete
            if (uiState.isSessionComplete) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉 ¡Sesión Completa!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("XP ganados: ${uiState.xpEarned}")
                        Text("Turnos: ${uiState.totalTurns} | Perfectos: ${uiState.perfectTurns}")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Transcript
            if (uiState.showTranscript) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.turns.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Presiona el botón y empieza a hablar en inglés",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(uiState.turns, key = { it.id }) { turn ->
                        Column {
                            UserMessageBubble(turn.userTranscript, turn.isPerfect)
                            if (turn.feedback != null && (turn.feedback.corrections.isNotEmpty() || turn.feedback.strengths.isNotEmpty())) {
                                FeedbackBubble(turn.feedback)
                            }
                            // Bot response: audio first + transcript button
                            BotAudioBubble(turn.botResponse, turn.botResponse)
                        }
                    }
                }
            } else {
                // Placeholder when transcript is hidden
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "👁",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Transcripción oculta\nToca el ojo para verla",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserMessageBubble(message: String, isPerfect: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPerfect)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (isPerfect) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "¡Perfecto!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun BotMessageBubble(message: String) {
    // Kept for compatibility, not used anymore
}

@Composable
fun BotAudioBubble(
    text: String,
    fullText: String
) {
    var showTranscript by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔊", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Escucha la respuesta",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Play button
            OutlinedButton(
                onClick = { /* Audio already auto-plays, this is placeholder */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            ) {
                Text("▶ Reproduciendo...", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transcript toggle
            OutlinedButton(
                onClick = { showTranscript = !showTranscript },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showTranscript) {
                    Text("📝 Ocultar transcripción")
                } else {
                    Text("📝 Ver transcripción")
                }
            }

            // Transcript (hidden by default)
            if (showTranscript) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = fullText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun FeedbackBubble(feedback: io.botinis.app.data.model.Feedback) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "📝 Feedback",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            if (feedback.corrections.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                feedback.corrections.forEach { c ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("❌ \"${c.original}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        Text("✅ \"${c.corrected}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        if (c.explanation.isNotEmpty()) {
                            Text("💡 ${c.explanation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }

            if (feedback.strengths.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                feedback.strengths.forEach { s ->
                    Text("👍 $s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}
