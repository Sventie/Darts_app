package com.dartsapp.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object PlayerManagement : Screen("players")
    object GameSetup : Screen("game_setup")
    object Game : Screen("game/{gameId}") {
        fun createRoute(gameId: Long) = "game/$gameId"
    }
    object Stats : Screen("stats")
    object StatsDetail : Screen("stats/{playerId}") {
        fun createRoute(playerId: Long) = "stats/$playerId"
    }
    object Heatmap : Screen("heatmap/{playerId}") {
        fun createRoute(playerId: Long) = "heatmap/$playerId"
    }
}
