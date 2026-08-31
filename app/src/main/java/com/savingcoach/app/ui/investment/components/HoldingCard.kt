package com.savingcoach.app.ui.investment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savingcoach.app.data.model.ComputedHolding
import com.savingcoach.app.ui.theme.AccentGreen
import com.savingcoach.app.ui.theme.PrimaryBlue
import com.savingcoach.app.utils.InvestmentCalculations

/**
 * Individual holding card displaying asset info, current value, and P/L.
 * Layout: Left side (icon + ticker + name), Right side (units, buy price, current value, P/L badge).
 */
@Composable
fun HoldingCard(
    computedHolding: ComputedHolding,
    usdRate: Double,
    currencyPreference: String = "MMK",
    onDeleteClick: () -> Unit,
    onStopClick: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var stopPriceInput by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    val holding = computedHolding.holding
    val isPositivePL = computedHolding.unrealizedPL >= 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left section: Icon + Ticker + Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Asset type icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (holding.type == "stock") PrimaryBlue.copy(alpha = 0.1f)
                            else Color(0xFFF97316).copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (holding.type == "stock") "📈" else "🪙",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = holding.displayTicker,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (holding.isStoppedCompat) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Gray.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = strings.soldOut,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                    Text(
                        text = holding.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Right section: Actions menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (!holding.isStoppedCompat) {
                        DropdownMenuItem(
                            text = { Text(strings.stop) },
                            onClick = {
                                menuExpanded = false
                                showStopDialog = true
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(strings.delete, color = Color(0xFFEF4444)) },
                        onClick = {
                            menuExpanded = false
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        // Bottom section: Values
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Units and buy price
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = strings.unitsOwned,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                com.savingcoach.app.ui.components.AutoScalingText(
                    text = InvestmentCalculations.formatUnits(holding.units, holding.type),
                    maxTextSize = 14.sp,
                    minTextSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (holding.isStoppedCompat) {
                        "${strings.avgBuyExit} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = true)})"
                    } else {
                        "${strings.avgBuyPrice} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = true)})"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val avgBuyPriceConverted = if (holding.units > 0) computedHolding.costBasis / holding.units else 0.0
                val displayPriceText = if (holding.isStoppedCompat) {
                    "${InvestmentCalculations.formatValue(avgBuyPriceConverted, currencyPreference, 1.0, isInvestment = true)} / ${InvestmentCalculations.formatValue(computedHolding.livePrice, currencyPreference, 1.0, isInvestment = true)}"
                } else {
                    InvestmentCalculations.formatValue(avgBuyPriceConverted, currencyPreference, 1.0, isInvestment = true)
                }
                com.savingcoach.app.ui.components.AutoScalingText(
                    text = displayPriceText,
                    maxTextSize = 14.sp,
                    minTextSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Current value and P/L
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Text(
                    text = if (holding.isStoppedCompat) {
                        "${strings.realizedValue} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = true)})"
                    } else {
                        "${strings.currentValue} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = true)})"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                com.savingcoach.app.ui.components.AutoScalingText(
                    text = InvestmentCalculations.formatValue(computedHolding.liquidValue, currencyPreference, 1.0, isInvestment = true),
                    maxTextSize = 14.sp,
                    minTextSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // P/L badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isPositivePL) AccentGreen.copy(alpha = 0.1f)
                            else com.savingcoach.app.ui.theme.Red.copy(alpha = 0.1f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val plPrefix = if (holding.isStoppedCompat) "${strings.realizedValue}: " else ""
                    com.savingcoach.app.ui.components.AutoScalingText(
                        text = "$plPrefix${InvestmentCalculations.formatPLValue(computedHolding.unrealizedPL, currencyPreference, 1.0, isInvestment = true)} (${InvestmentCalculations.formatPercentage(computedHolding.roiPercentage)})",
                        maxTextSize = 11.sp,
                        minTextSize = 7.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isPositivePL) AccentGreen else com.savingcoach.app.ui.theme.Red
                    )
                }
            }
        }

        // Delete confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(strings.deleteAssetTitle) },
                text = { Text(strings.deleteAssetConfirmMsg) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDeleteClick()
                        }
                    ) {
                        Text(strings.delete, color = Color(0xFFEF4444))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(strings.cancel)
                    }
                }
            )
        }

        if (showStopDialog) {
            val avgBuyPriceConverted = if (holding.units > 0) computedHolding.costBasis / holding.units else 0.0
            com.savingcoach.app.ui.components.InvestmentSettlementDialog(
                holdingName = holding.name,
                units = holding.units,
                buyPrice = avgBuyPriceConverted,
                usdRate = usdRate,
                currencyPreference = currencyPreference,
                onDismiss = { showStopDialog = false },
                onStopInvestment = { exitPrice ->
                    showStopDialog = false
                    onStopClick(exitPrice)
                },
                onDeleteInvestment = {
                    showStopDialog = false
                    onDeleteClick()
                }
            )
        }
    }
}
