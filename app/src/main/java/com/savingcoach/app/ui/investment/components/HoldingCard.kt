package com.savingcoach.app.ui.investment.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savingcoach.app.data.model.ComputedHolding
import com.savingcoach.app.ui.theme.AccentGreen
import com.savingcoach.app.ui.theme.CoralRed
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

    val plColor = if (isPositivePL) (if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F)) else CoralRed
    val plBg = if (isPositivePL) {
        if (isDark) Color(0xFF223525) else Color(0xFFE8F5E9)
    } else {
        if (isDark) Color(0xFF382323) else Color(0xFFFFEBEE)
    }
    val plBorder = if (isPositivePL) {
        if (isDark) Color(0xFF335037) else Color(0xFFC8E6C9)
    } else {
        if (isDark) Color(0xFF553232) else Color(0xFFFFCDD2)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(22.dp),
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
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Top Header: Icon + Ticker + Options Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Asset type icon tile
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(
                                    if (isDark) Color(0xFF2C332E) else Color(0xFFEFECE2),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isDark) Color(0xFF3A443D) else Color(0xFFE2DDD0),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (holding.type == "stock") "📈" else "🪙",
                                fontSize = 22.sp
                            )
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = holding.displayTicker,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (holding.isStoppedCompat) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isDark) Color(0xFF2B3236) else Color(0xFFECEFF1),
                                        border = BorderStroke(1.dp, if (isDark) Color(0xFF3E484E) else Color(0xFFCFD8DC))
                                    ) {
                                        Text(
                                            text = strings.soldOut,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = holding.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Options menu
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = if (isDark) Color(0xFF242925) else Color(0xFFFCFBF7),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF38403A) else Color(0xFFE5E0CE))
                        ) {
                            if (!holding.isStoppedCompat) {
                                DropdownMenuItem(
                                    text = { Text(strings.stop, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        menuExpanded = false
                                        showStopDialog = true
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(strings.delete, color = CoralRed, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom section: Values
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Left Column: Units & Buy Price
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = strings.unitsOwned,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        com.savingcoach.app.ui.components.AutoScalingText(
                            text = InvestmentCalculations.formatUnits(holding.units, holding.type),
                            maxTextSize = 15.sp,
                            minTextSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (holding.isStoppedCompat) {
                                "${strings.avgBuyExit} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = true)})"
                            } else {
                                "${strings.avgBuyPrice} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = true)})"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
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
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Right Column: Current value & P/L pill
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (holding.isStoppedCompat) {
                                "${strings.realizedValue} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = true)})"
                            } else {
                                "${strings.currentValue} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = true)})"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                        com.savingcoach.app.ui.components.AutoScalingText(
                            text = InvestmentCalculations.formatValue(computedHolding.liquidValue, currencyPreference, 1.0, isInvestment = true),
                            maxTextSize = 16.sp,
                            minTextSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // P/L pill badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = plBg,
                            border = BorderStroke(1.dp, plBorder)
                        ) {
                            val plPrefix = if (holding.isStoppedCompat) "${strings.realizedValue}: " else ""
                            Text(
                                text = "$plPrefix${InvestmentCalculations.formatPLValue(computedHolding.unrealizedPL, currencyPreference, 1.0, isInvestment = true)} (${InvestmentCalculations.formatPercentage(computedHolding.roiPercentage)})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = plColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
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
