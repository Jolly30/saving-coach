package com.savingcoach.app.ui.investment.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savingcoach.app.data.model.PortfolioSummary
import com.savingcoach.app.ui.theme.AccentGreen
import com.savingcoach.app.ui.theme.CoralRed
import com.savingcoach.app.utils.InvestmentCalculations

/**
 * Hero summary card displaying total portfolio value and active performance.
 */
@Composable
fun PortfolioSummaryCard(
    summary: PortfolioSummary,
    currencyPreference: String = "MMK",
    modifier: Modifier = Modifier
) {
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    val isPositiveActive = summary.totalUnrealizedPL >= 0
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val cardBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF242925),
                Color(0xFF1D211E),
                Color(0xFF161917)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFBF9F2),
                Color(0xFFF5F1E6)
            )
        )
    }

    val returnColor = if (isPositiveActive) (if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F)) else CoralRed
    val returnBg = if (isPositiveActive) {
        if (isDark) Color(0xFF223525) else Color(0xFFE8F5E9)
    } else {
        if (isDark) Color(0xFF382323) else Color(0xFFFFEBEE)
    }
    val returnBorder = if (isPositiveActive) {
        if (isDark) Color(0xFF335037) else Color(0xFFC8E6C9)
    } else {
        if (isDark) Color(0xFF553232) else Color(0xFFFFCDD2)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF38403A) else Color(0xFFE5E0CE))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Header Row: Label & Return pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.portfolioValue.uppercase()} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = true)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = returnBg,
                        border = BorderStroke(1.dp, returnBorder)
                    ) {
                        Text(
                            text = "${if (isPositiveActive) "▲ +" else "▼ "}${InvestmentCalculations.formatPercentage(summary.totalROI)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = returnColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Main Portfolio Value
                com.savingcoach.app.ui.components.AutoScalingText(
                    text = InvestmentCalculations.formatValue(summary.totalLiquidValue, currencyPreference, 1.0, isInvestment = true),
                    maxTextSize = 36.sp,
                    minTextSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Total gain / loss amount
                Text(
                    text = "${InvestmentCalculations.formatPLValue(summary.totalUnrealizedPL, currencyPreference, 1.0, isInvestment = true)} (${InvestmentCalculations.formatPercentage(summary.totalROI)})",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = returnColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


