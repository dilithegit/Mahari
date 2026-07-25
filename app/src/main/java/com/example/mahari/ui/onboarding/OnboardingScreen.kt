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
import com.example.mahari.MainActivity
import com.example.mahari.data.db.BudgetEntity
import com.example.mahari.data.db.MahariDatabase
import com.example.mahari.data.parser.BackfillResult
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

    var currentStep by remember { mutableStateOf(1) } // 1: Welcome, 2: Profile, 3: SMS Permission, 4: Budget Setup

    // Profile State
    var userName by remember { mutableStateOf("") }
    var userAge by remember { mutableStateOf("") }

    // SMS Permission & Backfill State
    var isPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAttemptedPermission by remember { mutableStateOf(false) }
    var isBackfilling by remember { mutableStateOf(false) }
    var hasBackfillError by remember { mutableStateOf(false) }
    var backfillProgress by remember { mutableStateOf(0 to 0) } // processed to total
    var backfillResult by remember { mutableStateOf<BackfillResult?>(null) }

    // Budget Setup State
    var monthlyBudgetInput by remember { mutableStateOf("30000") }

    fun triggerBackfill() {
        coroutineScope.launch {
            isBackfilling = true
            hasBackfillError = false
            try {
                val result = SmsBackfillManager.performOneTimeBackfill(
                    context = context.applicationContext,
                    transactionDao = database.transactionDao(),
                    mappingDao = database.merchantMappingDao(),
                    onProgress = { processed, total ->
                        backfillProgress = processed to total
                    }
                )
                backfillResult = result
                monthlyBudgetInput = result.suggestedMonthlyBudget.toInt().toString()
            } catch (e: Exception) {
                e.printStackTrace()
                hasBackfillError = true
            } finally {
                isBackfilling = false
            }
        }
    }

    fun requestPermissions() {

        hasAttemptedPermission = true
        val activity = context as? MainActivity
        if (activity != null) {
            activity.requestSmsPermissions { granted ->
                isPermissionGranted = granted
                if (granted) {
                    triggerBackfill()
                }
            }
        } else {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
            isPermissionGranted = granted
            if (granted) {
                triggerBackfill()
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

            // Animated Content per Step
            AnimatedContent(
                targetState = currentStep,
                label = "OnboardingWizard"
            ) { step ->
                when (step) {
                    1 -> WelcomeStep(onNext = { currentStep = 2 })

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

                    3 -> SmsPermissionStep(
                        isGranted = isPermissionGranted,
                        hasAttempted = hasAttemptedPermission,
                        isBackfilling = isBackfilling,
                        hasError = hasBackfillError,
                        backfillProgress = backfillProgress,
                        backfillResult = backfillResult,
                        onRequestPermission = { requestPermissions() },

                        onRetryBackfill = { triggerBackfill() },
                        onNext = { currentStep = 4 }
                    )


                    4 -> BudgetSetupStep(
                        userName = userName,
                        monthlyInput = monthlyBudgetInput,
                        onMonthlyInputChange = { monthlyBudgetInput = it },
                        suggestedBudget = backfillResult?.suggestedMonthlyBudget ?: 30000.0,
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

            // Footer Privacy Note
            Text(
                text = "100% On-Device • Zero Network Calls",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
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
            text = "Mahari automatically turns your M-Pesa SMS messages into a real financial picture — spending by category, budget tracking, and alerts — without manual entry.",
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
                    text = "🔒 100% On-Device Privacy",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryNavy
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your sensitive financial SMS data stays on your phone. Mahari makes zero network calls.",
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
            text = "Setup Local Profile",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tell us your name and age to personalize your dashboard and feed your local ML recap engine.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name or Nickname") },
            placeholder = { Text("e.g. Dili") },
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
            text = "ℹ️ Stored 100% locally. No account, password, or cloud server involved.",
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
private fun SmsPermissionStep(
    isGranted: Boolean,
    hasAttempted: Boolean,
    isBackfilling: Boolean,
    hasError: Boolean,
    backfillProgress: Pair<Int, Int>,
    backfillResult: BackfillResult?,
    onRequestPermission: () -> Unit,
    onRetryBackfill: () -> Unit,
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
            text = "Mahari requires READ_SMS and RECEIVE_SMS permissions to backfill your historical M-Pesa statements and intercept future receipts automatically.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))

        if (!isGranted) {
            if (hasAttempted) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AlertRedContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "⚠️ Permission Required", fontWeight = FontWeight.Bold, color = AlertRed)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Mahari cannot work without SMS access. Your messages remain 100% on your phone.",
                            fontSize = 13.sp,
                            color = AlertRed,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(0.85f).height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text(if (hasAttempted) "Tap to Retry Permission" else "Grant SMS Permission", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            if (isBackfilling) {
                CircularProgressIndicator(color = PrimaryNavy)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Scanning inbox... Found ${backfillProgress.first} transactions, importing...",
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            } else if (hasError) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AlertRedContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "⚠️ Backfill Encountered an Issue", fontWeight = FontWeight.Bold, color = AlertRed)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Something went wrong importing your messages — tap to retry.",
                            fontSize = 13.sp,
                            color = AlertRed,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetryBackfill,
                    modifier = Modifier.fillMaxWidth(0.85f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                ) {
                    Text("Tap to Retry Import", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IncomeMintContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "✅ SMS Backfill Complete!", fontWeight = FontWeight.Bold, color = IncomeMint)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (backfillResult != null) "Successfully imported ${backfillResult.importedCount} historical M-Pesa transactions!" else "Ready to process M-Pesa statements.",
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
    userName: String,
    monthlyInput: String,
    onMonthlyInputChange: (String) -> Unit,
    suggestedBudget: Double,
    onComplete: () -> Unit
) {
    val monthlyValue = monthlyInput.toDoubleOrNull() ?: 0.0
    val dailyLimit = monthlyValue / 30.0

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Text(
            text = if (userName.isNotEmpty()) "Set Budget, $userName" else "Set Monthly Budget",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Define your target monthly spend. Suggested default calculated from your backfilled M-Pesa history.",
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

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Suggested based on history: KES ${String.format("%.0f", suggestedBudget)}",
            fontSize = 12.sp,
            color = PrimaryNavy,
            fontWeight = FontWeight.SemiBold
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
            Text("Complete Setup & Launch Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
