package com.dartsapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

import com.dartsapp.ui.screens.game.GameScreen
import com.dartsapp.ui.screens.home.HomeScreen
import com.dartsapp.ui.screens.players.PlayerManagementScreen
import com.dartsapp.ui.screens.setup.GameSetupScreen
import com.dartsapp.ui.screens.setup.GameSetupViewModel
import com.dartsapp.ui.screens.stats.HeatmapScreen
import com.dartsapp.ui.screens.stats.StatsScreen
import com.dartsapp.ui.screens.training.TrainingScreen
import com.dartsapp.ui.screens.training.TrainingSetupScreen
import com.dartsapp.ui.screens.training.TrainingSetupViewModel
import com.dartsapp.ui.screens.training.TrainingViewModel

@Composable
fun DartsNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigatePlayers = { navController.navigate(Screen.PlayerManagement.route) },
                onNavigateSetup = { navController.navigate(Screen.GameSetup.route) },
                onNavigateStats = { navController.navigate(Screen.Stats.route) }
            )
        }

        composable(Screen.PlayerManagement.route) {
            PlayerManagementScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.GameSetup.route) {
            val viewModel: GameSetupViewModel = hiltViewModel()
            val startedGameId by viewModel.startedGameId.collectAsState()

            LaunchedEffect(startedGameId) {
                startedGameId?.let { gameId ->
                    viewModel.clearStartedGame()
                    navController.navigate(Screen.Game.createRoute(gameId)) {
                        popUpTo(Screen.GameSetup.route) { inclusive = true }
                    }
                }
            }

            GameSetupScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onTraining = { navController.navigate(Screen.TrainingSetup.route) }
            )
        }

        composable(Screen.TrainingSetup.route) {
            val viewModel: TrainingSetupViewModel = hiltViewModel()
            TrainingSetupScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onStartTraining = { mode, difficulty, playerId ->
                    navController.navigate(
                        Screen.Training.createRoute(mode.name, difficulty.name, playerId)
                    )
                }
            )
        }

        composable(
            route = Screen.Training.route,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType },
                navArgument("playerId") { type = NavType.LongType }
            )
        ) {
            val viewModel: TrainingViewModel = hiltViewModel()
            TrainingScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onStreuungClick = {
                    navController.navigate(
                        Screen.Heatmap.createRoute(viewModel.playerIdArg, showDispersion = true)
                    )
                }
            )
        }

        composable(
            route = Screen.Game.route,
            arguments = listOf(navArgument("gameId") { type = NavType.LongType })
        ) {
            val goSetup = {
                navController.navigate(Screen.GameSetup.route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                }
            }
            GameScreen(
                onGameOver    = { goSetup() },
                onAbandonGame = { goSetup() }
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(
                onBack         = { navController.popBackStack() },
                onHeatmapClick = { playerId ->
                    navController.navigate(Screen.Heatmap.createRoute(playerId))
                }
            )
        }

        composable(
            route     = Screen.Heatmap.route,
            arguments = listOf(
                navArgument("playerId") { type = NavType.LongType },
                navArgument("showDispersion") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val showDispersion = backStackEntry.arguments?.getBoolean("showDispersion") ?: false
            HeatmapScreen(
                onBack               = { navController.popBackStack() },
                initialShowDispersion = showDispersion
            )
        }
    }
}
