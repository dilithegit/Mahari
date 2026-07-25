package com.example.mahari.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.mahari.data.db.BudgetEntity
import com.example.mahari.data.db.MahariDatabase
import com.example.mahari.data.parser.SmsBackfillManager
import com.example.mahari.data.security.SecurityManager
import com.example.mahari.theme.*
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    database: MahariDatabase,
    securityManager: SecurityManager,
    onOnboardingFinished: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(1) } // 1: Rationale, 2: Profile, 3: SMS Permission & Backfill, 4: Budget Setup

    // Step 2 Profile State
    var userName by remember { mutableStateOf("") }
    var userAge by remember { mutableStateOf("") }

    // Step 3 SMS Backfill State
    var isPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isBackfilling by remember { mutableStateOf(false) }
    var backfillCount by remember { mutableStateOf<Int?>(null) }

    // Step 4 Budget Setup State
    var monthlyBudgetInput by remember { mutableStateOf("30000") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_SMS] == true
        isPermissionGranted = granted

        if (granted) {
            coroutineScope.launch {
                isBackfilling = true
                val count = SmsBackfillManager.performOneTimeBackfill(
                    context = context,
                    transactionDao = database.transactionDao(),
                    mappingDao = database.merchantMappingDao()
                )
                backfillCount = count
                isBackfilling = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Progress Indicator
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MAHARI",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { currentStep / 4.0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = PrimaryNavy,
                    trackColor = PrimaryContainer
                )
            }

            // Animated Step Content
            AnimatedContent(
                targetState = currentStep,
                label = "OnboardingStep"
            ) { step ->
                when (step) {
                    1 -> RationaleStep(onNext = { currentStep = 2 })
                    2 -> ProfileStep(
                        name = userName,
                        onNameChange = { userName = it },
                        age = userAge,
                        onAgeChange = { userAge = it },
                        onNext = {
                            val ageInt = userAge.toIntOrNull() ?: 20
                            securityManager.saveUserProfile(userName, ageInt)
                            currentStep = 3
                        }
                    )
                    3 -> SmsBackfillStep(
                        isGranted = isPermissionGranted,
                        isBackfilling = isBackfilling,
                        backfillCount = backfillCount,
                        onRequestPermission = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_SMS,
                                    Manifest.permission.RECEIVE_SMS
                                )
                            )
                        },
                        onNext = { currentStep = 4 }
                    )
                    4 -> BudgetSetupStep(
                        monthlyInput = monthlyBudgetInput,
                        onMonthlyInputChange = { monthlyBudgetInput = it },
                        onComplete = {
                            val limit = monthlyBudgetInput.toDoubleOrNull() ?: 30000.0
                            coroutineScope.launch {
                                securityManager.setMonthlyBudget(limit)
                                database.budgetDao().setBudget(
                                    BudgetEntity(
                                        id = 1,
                                        monthlyLimit = limit,
                                        monthYear = "2026-07",
                                        dailyLimit = limit / 30.0
                                    )
                                )
                                securityManager.setOnboardingComplete(true)
                                onOnboardingFinished()
                            }
                        }

                    )
                }
            }

            // Footer info
            Text(
                text = "100% Offline • Encrypted On-Device Processing",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun RationaleStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Welcome to Mahari",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Mahari automatically turns your Safaricom M-Pesa SMS messages into a real financial picture — spending by category, running daily budget limits, and smart alerts — without manual entry.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🔒 100% Offline & Private",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNavy
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your sensitive financial SMS messages never leave your phone. Mahari makes zero network calls.",
                    fontSize = 13.sp,
                    color = PrimaryNavy
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(0.85f).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
        ) {
            Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileStep(
    name: String,
    onNameChange: (String) -> Unit,
    age: String,
    onAgeChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val isFormValid = name.isNotBlank() && (age.toIntOrNull() ?: 0) > 0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Your Local Profile",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Provide your name and age to personalize your financial insights and on-device machine learning recap models.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name or Nickname") },
            placeholder = { Text("e.g. Okwudili") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = age,
            onValueChange = { if (it.length <= 3 && it.all { char -> char.isDigit() }) onAgeChange(it) },
            label = { Text("Age") },
            placeholder = { Text("e.g. 21") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "ℹ️ Stored 100% locally in encrypted storage. No password or cloud account required.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNext,
            enabled = isFormValid,
            modifier = Modifier.fillMaxWidth(0.85f).height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
        ) {
            Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SmsBackfillStep(
    isGranted: Boolean,
    isBackfilling: Boolean,
    backfillCount: Int?,
    onRequestPermission: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Text(
            text = "M-Pesa SMS Connection",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Grant SMS permissions so Mahari can perform a one-time scan of existing M-Pesa messages and intercept future receipts automatically.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (!isGranted) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(0.85f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text("Grant SMS Permission", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            if (isBackfilling) {
                CircularProgressIndicator(color = PrimaryNavy)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Scanning & backfilling existing M-Pesa SMS...",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IncomeMintContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "✅ SMS Permission Granted", fontWeight = FontWeight.Bold, color = IncomeMint)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (backfillCount != null) "Successfully backfilled $backfillCount existing M-Pesa transactions!" else "Ready to import M-Pesa transaction history.",
                            fontSize = 13.sp,
                            color = IncomeMint,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(0.85f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                ) {
                    Text("Next: Budget Setup", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BudgetSetupStep(
    monthlyInput: String,
    onMonthlyInputChange: (String) -> Unit,
    onComplete: () -> Unit
) {
    val monthlyValue = monthlyInput.toDoubleOrNull() ?: 0.0
    val dailyLimit = monthlyValue / 30.0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Set Monthly Budget",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Define your monthly target spend. Mahari automatically calculates your safe daily limit to keep you on track.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = monthlyInput,
            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) onMonthlyInputChange(it) },
            label = { Text("Monthly Budget (KES)") },
            prefix = { Text("KES ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("AUTO-COMPUTED SAFE DAILY LIMIT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "KES ${String.format("%.2f", dailyLimit)} / day",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNavy
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onComplete,
            enabled = monthlyValue > 0,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
        ) {
            Text("Complete Setup & Launch Mahari", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
