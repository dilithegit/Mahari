package com.example.mahari.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mahari.AppScreen
import com.example.mahari.theme.WarmCharcoalBgDark
import com.example.mahari.theme.WarmCharcoalElevatedDark
import com.example.mahari.theme.WarmMetalDark

@Composable
fun BottomNavBar(
    currentScreen: AppScreen,
    onTabSelected: (AppScreen) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = WarmCharcoalBgDark,
        tonalElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, WarmMetalDark.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                Triple(AppScreen.Dashboard, "Dashboard", "🏠"),
                Triple(AppScreen.TransactionsLedger, "Ledger", "📜"),
                Triple(AppScreen.RecapHistory, "Insights", "📊"),
                Triple(AppScreen.Settings, "Settings", "⚙️")
            )

            tabs.forEach { (screen, label, icon) ->
                val isSelected = when (screen) {
                    AppScreen.Dashboard -> currentScreen is AppScreen.Dashboard
                    AppScreen.TransactionsLedger -> currentScreen is AppScreen.TransactionsLedger
                    AppScreen.RecapHistory -> currentScreen is AppScreen.RecapHistory
                    AppScreen.Settings -> currentScreen is AppScreen.Settings
                    else -> false
                }

                Surface(
                    onClick = { onTabSelected(screen) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) WarmMetalDark.copy(alpha = 0.2f) else androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = if (isSelected) WarmMetalDark else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(icon, fontSize = 16.sp)
                        if (isSelected) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmMetalDark
                            )
                        }
                    }
                }
            }
        }
    }
}
