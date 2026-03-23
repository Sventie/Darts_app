package com.dartsapp.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object PlayerManagement : Screen("players")
    object GameSetup : Screen("game_setup")
    object Game : Screen("game/{gameId}") {
        fun createRoute(gameId: Long) = "game/$gameId"
    }
    object Stats : Screen("stats")
    object Heatmap : Screen("heatmap/{playerId}") {
        fun createRoute(playerId: Long) = "heatmap/$playerId"
    }
    object TrainingSetup : Screen("training_setup")
    object Training : Screen("training/{mode}/{difficulty}/{playerId}") {
        fun createRoute(mode: String, difficulty: String, playerId: Long) =
            "training/$mode/$difficulty/$playerId"
    }
}
