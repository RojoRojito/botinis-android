package io.botinis.app.ui.screens.scenario

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.botinis.app.data.model.CEFRLevel
import io.botinis.app.domain.ScenarioCatalog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioSelectScreen(
    onScenarioSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Scenario") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "A2 - Elementary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(ScenarioCatalog.getScenariosByLevel(CEFRLevel.A2)) { scenario ->
                ScenarioCard(scenario = scenario, onClick = { onScenarioSelected(scenario.id) })
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "B1 - Intermediate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            items(ScenarioCatalog.getScenariosByLevel(CEFRLevel.B1)) { scenario ->
                ScenarioCard(scenario = scenario, onClick = { onScenarioSelected(scenario.id) })
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "B2 - Upper Intermediate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            items(ScenarioCatalog.getScenariosByLevel(CEFRLevel.B2)) { scenario ->
                ScenarioCard(scenario = scenario, onClick = { onScenarioSelected(scenario.id) })
            }
        }
    }
}

@Composable
fun ScenarioCard(scenario: io.botinis.app.data.model.Scenario, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = scenario.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = scenario.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text(scenario.character) },
                    enabled = false
                )
                AssistChip(
                    onClick = {},
                    label = { Text(scenario.level.displayName) },
                    enabled = false
                )
            }
        }
    }
}
