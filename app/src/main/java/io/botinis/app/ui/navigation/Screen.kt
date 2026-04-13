package io.botinis.app.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ScenarioSelect : Screen("scenario_select")
    object Conversation : Screen("conversation/{scenarioId}") {
        fun createRoute(scenarioId: String) = "conversation/$scenarioId"
    }
    object Dashboard : Screen("dashboard")
    object Settings : Screen("settings")
}
