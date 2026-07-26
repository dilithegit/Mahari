package com.example.mahari

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.example.mahari.data.migration.TransactionMigrationManager
import com.example.mahari.data.security.SecurityManager
import com.example.mahari.theme.MahariTheme
import com.example.mahari.ui.category.CategoryDetailScreen
import com.example.mahari.ui.dashboard.DashboardScreen
import com.example.mahari.ui.dashboard.DashboardViewModel
import com.example.mahari.ui.merchant.MerchantDetailScreen
import com.example.mahari.ui.onboarding.OnboardingScreen
import com.example.mahari.ui.recap.RecapHistoryScreen
import com.example.mahari.ui.security.BiometricAuthScreen
import com.example.mahari.ui.settings.DataPrivacyScreen

sealed class AppScreen {
    object Dashboard : AppScreen()
    data class MerchantDetail(val merchantName: String) : AppScreen()
    data class CategoryDetail(val categoryName: String) : AppScreen()
    object RecapHistory : AppScreen()
    object DataPrivacy : AppScreen()
}

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
            var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Dashboard) }

            val uiState by viewModel.uiState.collectAsState()

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
                        when (val screen = currentScreen) {
                            AppScreen.Dashboard -> {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    database = database,
                                    securityManager = securityManager,
                                    isDarkMode = isDarkMode,
                                    onToggleTheme = { isDarkMode = it },
                                    onNavigateToMerchant = { merchant ->
                                        currentScreen = AppScreen.MerchantDetail(merchant)
                                    },
                                    onNavigateToCategory = { category ->
                                        currentScreen = AppScreen.CategoryDetail(category)
                                    },
                                    onNavigateToRecapHistory = {
                                        currentScreen = AppScreen.RecapHistory
                                    },
                                    onNavigateToDataPrivacy = {
                                        currentScreen = AppScreen.DataPrivacy
                                    }
                                )
                            }
                            is AppScreen.MerchantDetail -> {
                                MerchantDetailScreen(
                                    merchantName = screen.merchantName,
                                    transactions = uiState.transactions,
                                    onRecategorizeMerchant = { merchant, newCategory ->
                                        viewModel.recategorizeMerchant(merchant, newCategory)
                                    },
                                    onBack = { currentScreen = AppScreen.Dashboard }
                                )
                            }
                            is AppScreen.CategoryDetail -> {
                                CategoryDetailScreen(
                                    categoryName = screen.categoryName,
                                    periodLabel = uiState.dateScopeMode.label,
                                    transactions = uiState.transactions,
                                    categoryBudgetLimit = null,
                                    onSetCategoryBudget = { /* Category budget target saved */ },
                                    onSelectMerchant = { merchant ->
                                        currentScreen = AppScreen.MerchantDetail(merchant)
                                    },
                                    onBack = { currentScreen = AppScreen.Dashboard }
                                )
                            }
                            AppScreen.RecapHistory -> {
                                RecapHistoryScreen(
                                    database = database,
                                    onBack = { currentScreen = AppScreen.Dashboard }
                                )
                            }
                            AppScreen.DataPrivacy -> {
                                DataPrivacyScreen(
                                    database = database,
                                    securityManager = securityManager,
                                    onBack = { currentScreen = AppScreen.Dashboard }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
