package com.example.mahari.ui.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mahari.theme.FinancialTypography
import com.example.mahari.theme.HeroEmeraldDark
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun CurrentBalanceCard(
    currentBalance: Double?,
    latestTransactionTimestamp: Long?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "CURRENT BALANCE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            if (currentBalance == null || latestTransactionTimestamp == null) {
                Text(
                    text = "Balance unknown",
                    style = FinancialTypography.MoneyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Waiting for your first M-Pesa message",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val formattedBalance = remember(currentBalance) {
                    if (currentBalance >= 0.0) {
                        "Ksh ${"%.2f".format(currentBalance)}"
                    } else {
                        "-Ksh ${"%.2f".format(abs(currentBalance))}"
                    }
                }

                val formattedDate = remember(latestTransactionTimestamp) {
                    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                    dateFormat.format(Date(latestTransactionTimestamp))
                }

                Text(
                    text = formattedBalance,
                    style = FinancialTypography.MoneyDisplay,
                    color = if (currentBalance >= 0.0) HeroEmeraldDark else MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "As of $formattedDate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
