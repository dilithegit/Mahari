package com.example.mahari.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mahari.AppScreen
import com.example.mahari.theme.WarmMetalDark

data class NavTabItem(
    val screen: AppScreen,
    val label: String,
    val symbol: String
)

@Composable
fun BottomNavBar(
    currentScreen: AppScreen,
    onTabSelected: (AppScreen) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                NavTabItem(AppScreen.Dashboard, "Dashboard", "⌂"),
                NavTabItem(AppScreen.TransactionsLedger, "Ledger", "≡"),
                NavTabItem(AppScreen.RecapHistory, "Insights", "∯"),
                NavTabItem(AppScreen.Settings, "Settings", "⚙")
            )

            tabs.forEach { tab ->
                val isSelected = when (tab.screen) {
                    AppScreen.Dashboard -> currentScreen is AppScreen.Dashboard
                    AppScreen.TransactionsLedger -> currentScreen is AppScreen.TransactionsLedger
                    AppScreen.RecapHistory -> currentScreen is AppScreen.RecapHistory
                    AppScreen.Settings -> currentScreen is AppScreen.Settings
                    else -> false
                }

                val activeColor = WarmMetalDark

                Surface(
                    onClick = { onTabSelected(tab.screen) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) activeColor.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent,
                    contentColor = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = tab.symbol,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isSelected) {
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = activeColor
                            )
                        }
                    }
                }
            }
        }
    }
}
