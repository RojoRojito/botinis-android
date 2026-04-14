package io.botinis.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val apiKey by viewModel.apiKey.collectAsState()
    val isConfigured by viewModel.isConfigured.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = io.botinis.app.ui.theme.BackgroundPrimary)
            )
        },
        containerColor = io.botinis.app.ui.theme.BackgroundPrimary
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "API Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = io.botinis.app.ui.theme.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { viewModel.saveApiKey(it) },
                label = { Text("Groq API Key") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("gsk_...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = io.botinis.app.ui.theme.AccentPrimary,
                    unfocusedBorderColor = io.botinis.app.ui.theme.TextDisabled
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isConfigured) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = io.botinis.app.ui.theme.Success)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("API Key configured", color = io.botinis.app.ui.theme.Success, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = io.botinis.app.ui.theme.Error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("API Key required", color = io.botinis.app.ui.theme.Error, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = io.botinis.app.ui.theme.AccentPrimary.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("ℹ️ How to get your API key", fontWeight = FontWeight.Bold, color = io.botinis.app.ui.theme.TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "1. Go to console.groq.com/keys\n2. Create an account or log in\n3. Generate a new API key\n4. Paste it here",
                        color = io.botinis.app.ui.theme.TextSecondary
                    )
                }
            }
        }
    }
}
