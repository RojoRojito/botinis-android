package io.botinis.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.botinis.app.ui.screens.conversation.ConversationScreen
import io.botinis.app.ui.screens.dashboard.DashboardScreen
import io.botinis.app.ui.screens.home.HomeScreen
import io.botinis.app.ui.screens.scenario.ScenarioSelectScreen
import io.botinis.app.ui.screens.settings.SettingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStartPractice = { navController.navigate(Screen.ScenarioSelect.route) },
                onDashboard = { navController.navigate(Screen.Dashboard.route) },
                onSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.ScenarioSelect.route) {
            ScenarioSelectScreen(
                onScenarioSelected = { scenarioId ->
                    navController.navigate(Screen.Conversation.createRoute(scenarioId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Conversation.route) { backStackEntry ->
            val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: return@composable
            ConversationScreen(
                scenarioId = scenarioId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
