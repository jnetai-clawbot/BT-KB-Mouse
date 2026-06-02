package com.jnetaol.btkbmouse

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jnetaol.btkbmouse.ui.AppViewModel
import com.jnetaol.btkbmouse.ui.screens.*

@Composable
fun BTKBMouseNavHost() {
    val navController = rememberNavController()
    val viewModel: AppViewModel = viewModel()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(viewModel, onNavigate = { route ->
                navController.navigate(route)
            })
        }
        composable("connection") {
            ConnectionScreen(viewModel, onBack = { navController.popBackStack() })
        }
        composable("touchpad") {
            TouchpadScreen(viewModel, onBack = { navController.popBackStack() })
        }
        composable("keyboard") {
            KeyboardScreen(viewModel, onBack = { navController.popBackStack() })
        }
        composable("splitview") {
            SplitViewScreen(viewModel, onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(viewModel, onBack = { navController.popBackStack() })
        }
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
