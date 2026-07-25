package com.example.mahari

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.mahari.theme.MahariTheme
import com.example.mahari.ui.dashboard.DashboardScreen
import com.example.mahari.ui.dashboard.DashboardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as MahariApplication).container.transactionRepository
        val viewModel = DashboardViewModel(repository)

        setContent {
            var isDarkMode by remember { mutableStateOf(false) }

            MahariTheme(darkTheme = isDarkMode) {
                DashboardScreen(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onToggleTheme = { isDarkMode = it }
                )
            }
        }
    }
}
