package com.savingcoach.app.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = if (isDark) Color(0xFF222724) else Color(0xFFFCFBF7),
        title = {
            Text(
                text = "🏷️ ${strings.addCategory}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = emojiText,
                    onValueChange = { emojiText = it },
                    label = { Text("Emoji") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("${strings.categoryName} *") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showTargetField) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = targetText,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    targetText = newValue
                                }
                            },
                            label = { Text("${strings.targetLimit} (${com.savingcoach.app.utils.InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = false)})") },
                            isError = isExceedingGlobal || isGlobalZero,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(16.dp),
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
                enabled = nameText.isNotBlank() && !isExceedingGlobal && !isGlobalZero,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(strings.save, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
