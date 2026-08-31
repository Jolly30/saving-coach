package com.savingcoach.app.ui.investment.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savingcoach.app.data.model.PortfolioSummary
import com.savingcoach.app.ui.theme.AccentGreen
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Label
            Text(
                text = "${strings.portfolioValue.uppercase()} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = true)})",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Main Balance
            Text(
                text = InvestmentCalculations.formatValue(summary.totalLiquidValue, currencyPreference, 1.0, isInvestment = true),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Active return (gain/loss)
            Text(
                text = "${InvestmentCalculations.formatPLValue(summary.totalUnrealizedPL, currencyPreference, 1.0, isInvestment = true)} (${InvestmentCalculations.formatPercentage(summary.totalROI)})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (isPositiveActive) AccentGreen else com.savingcoach.app.ui.theme.Red,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


