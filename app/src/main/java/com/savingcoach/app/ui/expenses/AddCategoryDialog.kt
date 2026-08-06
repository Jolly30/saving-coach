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
    onDismiss: () -> Unit,
    onConfirm: (emoji: String, name: String, target: Double) -> Unit,
    showTargetField: Boolean = true
) {
    var emojiText by remember { mutableStateOf("🏷️") }
    var nameText by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("0") }

    val enteredTarget = if (showTargetField) (targetText.toDoubleOrNull() ?: 0.0) else 0.0
    val isExceedingGlobal = globalLimit > 0 && enteredTarget > maxAllowedTarget
    val isGlobalZero = globalLimit == 0.0 && enteredTarget > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Category") },
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
                    label = { Text("Category Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showTargetField) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = targetText,
                            onValueChange = { if (it.all { char -> char.isDigit() }) targetText = it },
                            label = { Text("Category Target Limit (MMK)") },
                            isError = isExceedingGlobal || isGlobalZero,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isGlobalZero) {
                            Text(
                                text = "⚠️ Monthly Overall Budget is currently 0 MMK. Please set Global Budget first.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else if (isExceedingGlobal) {
                            Text(
                                text = "⚠️ Cannot exceed Global Budget! Max available capacity: ${currencyFormat.format(maxAllowedTarget.toLong())} MMK.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else if (globalLimit > 0) {
                            Text(
                                text = "Available Global Capacity: ${currencyFormat.format(maxAllowedTarget.toLong())} MMK",
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
                        onConfirm(emojiText.ifBlank { "🏷️" }, nameText.trim(), enteredTarget)
                    }
                },
                enabled = nameText.isNotBlank() && !isExceedingGlobal && !isGlobalZero
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
