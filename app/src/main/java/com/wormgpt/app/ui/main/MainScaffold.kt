package com.wormgpt.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.wormgpt.app.data.repository.AuthRepository
import com.wormgpt.app.ui.chat.ChatScreen
import com.wormgpt.app.ui.sidebar.DrawerContent
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                authRepository = authRepository,
                onChatSelected = { chatId ->
                    scope.launch { drawerState.close() }
                    navController.navigate("chat/$chatId") { launchSingleTop = true }
                },
                onNewChat = {
                    scope.launch { drawerState.close() }
                    navController.navigate("chat/new") { launchSingleTop = true }
                },
                onManageSubscription = {
                    scope.launch { drawerState.close() }
                    navController.navigate("subscription") { launchSingleTop = true }
                },
                onSignOut = onSignOut
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "WORMGPT",
                            style = MaterialTheme.typography.titleLarge,
                            color = WormRed
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Black,
                        titleContentColor = WormRed,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = Black
        ) { innerPadding ->
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
                    composable("subscription") {
                        SubscriptionScreen(authRepository = authRepository)
                    }
                }
        }
    }
}
