package com.example.mahari

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.example.mahari.data.migration.TransactionMigrationManager
import com.example.mahari.data.security.SecurityManager
import com.example.mahari.theme.MahariTheme
import com.example.mahari.ui.dashboard.DashboardScreen
import com.example.mahari.ui.dashboard.DashboardViewModel
import com.example.mahari.ui.onboarding.OnboardingScreen
import com.example.mahari.ui.security.BiometricAuthScreen

class MainActivity : FragmentActivity() {

    private var permissionCallback: ((Boolean) -> Unit)? = null

    fun requestSmsPermissions(callback: (Boolean) -> Unit) {
        this.permissionCallback = callback
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.READ_SMS,
                android.Manifest.permission.RECEIVE_SMS
            ),
            1001
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            permissionCallback?.invoke(granted)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

            LaunchedEffect(Unit) {
                TransactionMigrationManager.runOneTimeDateMigration(
                    context = this@MainActivity,
                    transactionDao = database.transactionDao()
                )
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
                            database = database,
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
