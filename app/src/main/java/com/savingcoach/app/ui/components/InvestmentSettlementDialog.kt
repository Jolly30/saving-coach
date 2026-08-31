package com.savingcoach.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.savingcoach.app.utils.InvestmentCalculations
import java.text.DecimalFormat

@Composable
fun InvestmentSettlementDialog(
    holdingName: String,
    units: Double,
    buyPrice: Double, // in display currency
    usdRate: Double,  // exchange rate
    currencyPreference: String = "MMK",
    onDismiss: () -> Unit,
    onStopInvestment: (exitPrice: Double) -> Unit,
    onDeleteInvestment: () -> Unit
) {
    var exitPriceText by remember { mutableStateOf("") }
    val exitPrice = exitPriceText.toDoubleOrNull() ?: 0.0
    
    val realizedPL = (exitPrice - buyPrice) * units
    val isPositive = realizedPL >= 0
    val plColor = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    
    val formatter = DecimalFormat("#,##0.00")
    val isMmk = currencyPreference == "MMK"
    val buyPriceUsd = if (isMmk && usdRate > 0) buyPrice / usdRate else buyPrice
    val exitPriceUsd = if (isMmk && usdRate > 0) exitPrice / usdRate else exitPrice
    val realizedPlUsd = if (isMmk && usdRate > 0) realizedPL / usdRate else realizedPL
    val strings = com.savingcoach.app.ui.localization.AppLocale.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "${strings.settlementDialogTitle} - $holdingName", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val displayBuyPrice = InvestmentCalculations.formatValue(buyPrice, currencyPreference, 1.0, isInvestment = true)
                val displayBuyPriceUsd = if (isMmk) " (~$${formatter.format(buyPriceUsd)})" else ""
                Text(
                    text = "Holding ${formatter.format(units)} units ($displayBuyPrice$displayBuyPriceUsd)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = exitPriceText,
                    onValueChange = { exitPriceText = it.filter { char -> char.isDigit() || char == '.' }.take(15) },
                    label = { Text("${strings.exitMarketPrice} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = true)})") },
                    supportingText = {
                        if (exitPrice > 0 && isMmk) {
                            Text("Equivalent exit price: $${formatter.format(exitPriceUsd)}")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = strings.realizedPL,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val displayPL = InvestmentCalculations.formatValue(realizedPL, currencyPreference, 1.0, isInvestment = true)
                        val displayPLUsd = if (isMmk) " (~$${formatter.format(realizedPlUsd)})" else ""
                        Text(
                            text = "${if (isPositive && realizedPL != 0.0) "+" else ""}$displayPL$displayPLUsd",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = plColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info, 
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "\"Confirm\" finalizes your transaction. \"Delete\" removes the record.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (exitPrice > 0) {
                        onStopInvestment(exitPrice)
                    }
                },
                enabled = exitPrice > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(strings.confirmSettlement)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDeleteInvestment,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(strings.delete)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
