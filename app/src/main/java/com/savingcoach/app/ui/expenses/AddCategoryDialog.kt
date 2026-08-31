package com.savingcoach.app.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AddCategoryDialog(
    globalLimit: Double = 0.0,
    maxAllowedTarget: Double = Double.MAX_VALUE,
    currencyFormat: NumberFormat = remember { NumberFormat.getNumberInstance(Locale.US) },
    currencyPreference: String = "MMK",
    onDismiss: () -> Unit,
    onConfirm: (emoji: String, name: String, target: Double) -> Unit,
    showTargetField: Boolean = true
) {
    // Fields start empty — user types fresh each time
    var emojiText by remember(Unit) { mutableStateOf("") }
    var nameText by remember(Unit) { mutableStateOf("") }
    var targetText by remember(Unit) { mutableStateOf("") }

    val enteredTarget = if (showTargetField) (targetText.toDoubleOrNull() ?: 0.0) else 0.0
    val isExceedingGlobal = globalLimit > 0 && enteredTarget > maxAllowedTarget
    val isGlobalZero = globalLimit == 0.0 && enteredTarget > 0.0
    val strings = com.savingcoach.app.ui.localization.AppLocale.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addCategory) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = emojiText,
                    onValueChange = { emojiText = it },
                    label = { Text("Emoji") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("${strings.categoryName} *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showTargetField) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = targetText,
                            onValueChange = { newValue ->
                                // Allow empty or valid number: digits with optional single decimal
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    targetText = newValue
                                }
                            },
                            label = { Text("${strings.targetLimit} (${com.savingcoach.app.utils.InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = false)})") },
                            isError = isExceedingGlobal || isGlobalZero,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isGlobalZero) {
                            Text(
                                text = strings.budgetZeroWarning,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else if (isExceedingGlobal) {
                            Text(
                                text = strings.budgetExceedWarning(com.savingcoach.app.utils.InvestmentCalculations.formatValue(maxAllowedTarget, currencyPreference, 1.0, isInvestment = false)),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else if (globalLimit > 0) {
                            Text(
                                text = strings.availableCapacityMsg(com.savingcoach.app.utils.InvestmentCalculations.formatValue(maxAllowedTarget, currencyPreference, 1.0, isInvestment = false)),
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isNotBlank() && !isExceedingGlobal && !isGlobalZero) {
                        onConfirm(
                            emojiText.ifBlank { "🏷️" },
                            nameText.trim(),
                            enteredTarget
                        )
                    }
                },
                enabled = nameText.isNotBlank() && !isExceedingGlobal && !isGlobalZero
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
