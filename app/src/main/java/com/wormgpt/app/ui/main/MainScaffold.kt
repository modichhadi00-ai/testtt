package com.wormgpt.app.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wormgpt.app.data.repository.AuthRepository
import com.wormgpt.app.ui.chat.ChatScreen
import com.wormgpt.app.ui.sidebar.DrawerContent
import com.wormgpt.app.ui.settings.SettingsScreen
import com.wormgpt.app.ui.subscription.SubscriptionScreen
import com.wormgpt.app.ui.theme.Black
import com.wormgpt.app.ui.theme.WormRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    authRepository: AuthRepository,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentBackStackEntry = navController.currentBackStackEntry
    val currentChatId = currentBackStackEntry?.arguments?.getString("chatId")

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            DrawerContent(
                authRepository = authRepository,
                currentChatId = currentChatId,
                onChatSelected = { chatId ->
                    scope.launch { drawerState.close() }
                    navController.navigate("chat/$chatId") { launchSingleTop = true }
                },
                onNewChat = {
                    scope.launch { drawerState.close() }
                    navController.navigate("chat/new") {
                        popUpTo("chat/new") { inclusive = true; saveState = false }
                        launchSingleTop = true
                    }
                },
                onDeleteChat = { deletedId ->
                    if (deletedId == currentChatId) {
                        navController.navigate("chat/new") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false; saveState = false }
                            launchSingleTop = true
                        }
                    }
                },
                onSettings = {
                    scope.launch { drawerState.close() }
                    navController.navigate("settings") { launchSingleTop = true }
                },
                onManageSubscription = {
                    scope.launch { drawerState.close() }
                    navController.navigate("subscription") { launchSingleTop = true }
                },
                onSignOut = {
                    scope.launch { drawerState.close() }
                    onSignOut()
                }
            )
        },
        scrimColor = Black.copy(alpha = 0.6f)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { WormGptTitleGlow() },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Black)
                )
            },
            containerColor = Black
        ) { innerPadding ->
            val focusManager = LocalFocusManager.current
            LaunchedEffect(drawerState.isOpen) {
                if (drawerState.isOpen) focusManager.clearFocus()
            }
            NavHost(
                navController = navController,
                startDestination = "chat/new",
                modifier = Modifier
                    .padding(innerPadding)
                    .background(Black)
            ) {
                composable("chat/new") {
                    ChatScreen(chatId = null, authRepository = authRepository)
                }
                composable(
                    route = "chat/{chatId}",
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                    ChatScreen(chatId = chatId, authRepository = authRepository)
                }
                composable("settings") {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
                composable("subscription") {
                    SubscriptionScreen(authRepository = authRepository)
                }
            }
        }
    }
}

@Composable
private fun WormGptTitleGlow() {
    val infiniteTransition = rememberInfiniteTransition(label = "titleGlow")
    val shine by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shine"
    )
    Text(
        text = "WORMGPT",
        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
        color = WormRed,
        modifier = Modifier.alpha(shine)
    )
}
