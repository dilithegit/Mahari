package com.example.mahari

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.fragment.app.FragmentActivity
import com.example.mahari.data.security.SecurityManager
import com.example.mahari.theme.MahariTheme
import com.example.mahari.ui.dashboard.DashboardScreen
import com.example.mahari.ui.dashboard.DashboardViewModel
import com.example.mahari.ui.onboarding.OnboardingScreen
import com.example.mahari.ui.security.BiometricAuthScreen

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enforce FLAG_SECURE to block screen captures and task switcher data leaks
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()

        val appContainer = (application as MahariApplication).container
        val repository = appContainer.transactionRepository
        val database = appContainer.database
        val securityManager = SecurityManager(this)
        val viewModel = DashboardViewModel(repository)

        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            var isOnboardingFinished by remember { mutableStateOf(securityManager.isOnboardingComplete()) }
            var isUnlocked by remember {
                mutableStateOf(!securityManager.isBiometricEnabled() && securityManager.getPin() == null)
            }

            MahariTheme(darkTheme = isDarkMode) {
                when {
                    !isOnboardingFinished -> {
                        OnboardingScreen(
                            database = database,
                            securityManager = securityManager,
                            onOnboardingFinished = {
                                isOnboardingFinished = true
                                isUnlocked = true
                            }
                        )
                    }
                    !isUnlocked -> {
                        BiometricAuthScreen(
                            securityManager = securityManager,
                            onUnlocked = { isUnlocked = true }
                        )
                    }
                    else -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            securityManager = securityManager,
                            isDarkMode = isDarkMode,
                            onToggleTheme = { isDarkMode = it }
                        )
                    }
                }
            }
        }
    }
}
