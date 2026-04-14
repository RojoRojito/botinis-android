package io.botinis.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.botinis.app.ui.components.BottomNavigationBar
import io.botinis.app.ui.screens.conversation.ConversationScreen
import io.botinis.app.ui.screens.dashboard.DashboardScreen
import io.botinis.app.ui.screens.home.HomeScreen
import io.botinis.app.ui.screens.scenario.ScenarioSelectScreen
import io.botinis.app.ui.screens.settings.SettingsScreen
import io.botinis.app.ui.screens.splash.SplashScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            // Only show bottom nav on main screens
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute in listOf(Screen.Home.route, Screen.Worlds.route, Screen.Progress.route)) {
                BottomNavigationBar(navController = navController)
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onFinish = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onStartPractice = { navController.navigate(Screen.Worlds.route) },
                    onSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Worlds.route) {
                ScenarioSelectScreen(
                    onScenarioSelected = { scenarioId ->
                        navController.navigate(Screen.PreSession.createRoute(scenarioId))
                    }
                )
            }
            composable(Screen.Progress.route) {
                DashboardScreen()
            }
            composable(Screen.PreSession.route) { backStackEntry ->
                val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: return@composable
                // TODO: PreSessionScreen, for now go directly to conversation
                navController.navigate(Screen.Conversation.createRoute(scenarioId))
            }
            composable(Screen.Conversation.route) { backStackEntry ->
                val scenarioId = backStackEntry.arguments?.getString("scenarioId") ?: return@composable
                ConversationScreen(
                    scenarioId = scenarioId,
                    onBack = { navController.popBackStack() },
                    onComplete = { navController.navigate(Screen.SessionComplete.route) }
                )
            }
            composable(Screen.SessionComplete.route) {
                // TODO: SessionCompleteScreen, for now go back to home
                HomeScreen(
                    onStartPractice = { navController.navigate(Screen.Worlds.route) },
                    onSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
