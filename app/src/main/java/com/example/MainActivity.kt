package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.core.configureAppCheck
import com.example.ui.AppNavigation
import com.example.ui.theme.KafkaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }
            KafkaTheme(darkTheme = isDarkTheme) {
                AppNavigation(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = it }
                )
            }
        }
    }
}
