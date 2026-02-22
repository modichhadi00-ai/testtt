package com.wormgpt.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wormgpt.app.data.repository.AuthRepository
import com.wormgpt.app.ui.auth.LoginScreen
import com.wormgpt.app.ui.main.MainScaffold

sealed class Route(val path: String) {
    data object Login : Route("login")
    data object Main : Route("main")
}

@Composable
fun WormGptNavHost(
    authRepository: AuthRepository = AuthRepository(),
    navController: NavHostController = rememberNavController()
) {
    val authState by authRepository.currentUserFlow().collectAsState(initial = authRepository.currentUser)
    val backStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(authState) {
        if (authState != null && backStackEntry?.destination?.route != Route.Main.path) {
            navController.navigate(Route.Main.path) { popUpTo(Route.Login.path) { inclusive = true } }
        }
        if (authState == null && backStackEntry?.destination?.route == Route.Main.path) {
            navController.navigate(Route.Login.path) { popUpTo(Route.Main.path) { inclusive = true } }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Route.Login.path
    ) {
        composable(Route.Login.path) {
            LoginScreen(
                authRepository = authRepository,
                onLoggedIn = {
                    navController.navigate(Route.Main.path) { popUpTo(Route.Login.path) { inclusive = true } }
                }
            )
        }
        composable(Route.Main.path) {
            MainScaffold(
                authRepository = authRepository,
                onSignOut = {
                    authRepository.signOut()
                    navController.navigate(Route.Login.path) { popUpTo(Route.Main.path) { inclusive = true } }
                }
            )
        }
    }
}
