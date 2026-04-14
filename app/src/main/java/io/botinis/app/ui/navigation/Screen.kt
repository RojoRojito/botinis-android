package io.botinis.app.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Worlds : Screen("worlds")
    object Progress : Screen("progress")
    object PreSession : Screen("pre_session/{scenarioId}") {
        fun createRoute(scenarioId: String) = "pre_session/$scenarioId"
    }
    object Conversation : Screen("conversation/{scenarioId}") {
        fun createRoute(scenarioId: String) = "conversation/$scenarioId"
    }
    object SessionComplete : Screen("session_complete")
    object Settings : Screen("settings")
}
