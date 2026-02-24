package com.wormgpt.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
    var navigated by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        val user = authState
        if (user != null && !navigated) {
            navigated = true
            navController.navigate(Route.Main.path) {
                popUpTo(Route.Login.path) { inclusive = true }
                launchSingleTop = true
            }
        }
        if (user == null && navigated) {
            navigated = false
            navController.navigate(Route.Login.path) {
                popUpTo(Route.Main.path) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (authRepository.currentUser != null) Route.Main.path else Route.Login.path
    ) {
        composable(Route.Login.path) {
            LoginScreen(
                authRepository = authRepository,
                onLoggedIn = {
                    navigated = true
                    navController.navigate(Route.Main.path) {
                        popUpTo(Route.Login.path) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Route.Main.path) {
            MainScaffold(
                authRepository = authRepository,
                onSignOut = {
                    authRepository.signOut()
                }
            )
        }
    }
}
