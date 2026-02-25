package com.wormgpt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.wormgpt.app.ui.navigation.WormGptNavHost
import com.wormgpt.app.ui.theme.WormGptTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Let the system resize for keyboard so the input bar moves up quickly
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            WormGptTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WormGptNavHost()
                }
            }
        }
    }
}
