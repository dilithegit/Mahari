package com.example.mahari

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.mahari.data.db.MahariDatabase
import com.example.mahari.data.migration.TransactionMigrationManager
import com.example.mahari.data.security.SecurityManager
import com.example.mahari.theme.MahariTheme
import com.example.mahari.ui.category.CategoryDetailScreen
import com.example.mahari.ui.dashboard.DashboardScreen
import com.example.mahari.ui.dashboard.DashboardViewModel
import com.example.mahari.ui.ledger.LedgerScreen
import com.example.mahari.ui.merchant.MerchantDetailScreen
import com.example.mahari.ui.navigation.BottomNavBar
import com.example.mahari.ui.onboarding.OnboardingScreen
import com.example.mahari.ui.recap.RecapHistoryScreen
import com.example.mahari.ui.security.BiometricAuthScreen
import com.example.mahari.ui.settings.DataPrivacyScreen
import com.example.mahari.ui.settings.SettingsScreen

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

    override fun onStop() {
        super.onStop()
        MahariDatabase.lockAndEncryptDatabase(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val appContainer = (application as MahariApplication).container
        val repository = appContainer.transactionRepository
        val database = appContainer.database
        val securityManager = SecurityManager(this)
        val viewModel = DashboardViewModel(repository)

        val initialTargetRoute = intent?.getStringExtra("target_route")

        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            var isOnboardingFinished by remember { mutableStateOf(securityManager.isOnboardingComplete()) }
            var isUnlocked by remember {
                mutableStateOf(!securityManager.isBiometricEnabled() && securityManager.getPin() == null)
            }

            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            val uiState by viewModel.uiState.collectAsState()

            LaunchedEffect(Unit) {
                TransactionMigrationManager.runOneTimeDateMigration(
                    context = this@MainActivity,
                    transactionDao = database.transactionDao()
                )

                if (initialTargetRoute != null && isUnlocked && isOnboardingFinished) {
                    navController.navigate(initialTargetRoute)
                }
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
                        val showBottomBar = currentRoute in listOf("dashboard", "ledger", "insights", "settings")

                        Scaffold(
                            bottomBar = {
                                if (showBottomBar) {
                                    BottomNavBar(
                                        currentRoute = currentRoute,
                                        onRouteSelected = { route ->
                                            navController.navigate(route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                NavHost(
                                    navController = navController,
                                    startDestination = "dashboard"
                                ) {
                                    composable("dashboard") {
                                        DashboardScreen(
                                            viewModel = viewModel,
                                            database = database,
                                            securityManager = securityManager,
                                            isDarkMode = isDarkMode,
                                            onToggleTheme = { isDarkMode = it },
                                            onNavigateToMerchant = { merchant ->
                                                navController.navigate("merchant/$merchant")
                                            },
                                            onNavigateToCategory = { category ->
                                                navController.navigate("category/$category")
                                            },
                                            onNavigateToRecapHistory = {
                                                navController.navigate("insights")
                                            },
                                            onNavigateToDataPrivacy = {
                                                navController.navigate("settings/privacy")
                                            }
                                        )
                                    }

                                    composable("ledger") {
                                        LedgerScreen(
                                            transactions = uiState.transactions,
                                            periodLabel = uiState.dateScopeMode.label,
                                            onTransactionClick = { tx ->
                                                navController.navigate("merchant/${tx.merchantOrParty}")
                                            }
                                        )
                                    }

                                    composable("insights") {
                                        RecapHistoryScreen(
                                            database = database,
                                            onBack = { navController.popBackStack() }
                                        )
                                    }

                                    composable("settings") {
                                        SettingsScreen(
                                            securityManager = securityManager,
                                            database = database,
                                            onNavigateToDataPrivacy = { navController.navigate("settings/privacy") },
                                            onNavigateToRecapHistory = { navController.navigate("insights") }
                                        )
                                    }

                                    composable("settings/privacy") {
                                        DataPrivacyScreen(
                                            database = database,
                                            securityManager = securityManager,
                                            onBack = { navController.popBackStack() }
                                        )
                                    }

                                    composable(
                                        route = "merchant/{merchantName}",
                                        arguments = listOf(navArgument("merchantName") { type = NavType.StringType })
                                    ) { backStackEntry ->
                                        val mName = backStackEntry.arguments?.getString("merchantName") ?: ""
                                        MerchantDetailScreen(
                                            merchantName = mName,
                                            transactions = uiState.transactions,
                                            onRecategorizeMerchant = { merchant, newCategory ->
                                                viewModel.recategorizeMerchant(merchant, newCategory)
                                            },
                                            onBack = { navController.popBackStack() }
                                        )
                                    }

                                    composable(
                                        route = "category/{categoryName}",
                                        arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
                                    ) { backStackEntry ->
                                        val cName = backStackEntry.arguments?.getString("categoryName") ?: ""
                                        CategoryDetailScreen(
                                            categoryName = cName,
                                            periodLabel = uiState.dateScopeMode.label,
                                            transactions = uiState.transactions,
                                            categoryBudgetLimit = null,
                                            onSetCategoryBudget = { },
                                            onSelectMerchant = { merchant ->
                                                navController.navigate("merchant/$merchant")
                                            },
                                            onBack = { navController.popBackStack() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
