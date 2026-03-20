package com.dartsapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.dartsapp.ui.navigation.DartsNavGraph
import com.dartsapp.ui.theme.DartsAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DartsAppTheme {
                val navController = rememberNavController()
                DartsNavGraph(navController = navController)
            }
        }
    }
}
