package com.savingcoach.app.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val defaultFallbackCategories = ExpenseCategory.DEFAULT_CATEGORIES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogExpenseBottomSheet(
    onDismiss: () -> Unit,
    onSave: (amount: Double, category: String, merchant: String, description: String) -> Unit,
    availableCategories: List<ExpenseCategory> = emptyList(),
    onAddCategory: ((emoji: String, name: String, target: Double) -> Unit)? = null
) {
    // Fresh state on every composition — fields start empty
    var amountText by remember { mutableStateOf("") }
    val categoriesList = if (availableCategories.isNotEmpty()) availableCategories else defaultFallbackCategories

    var selectedCategoryName by remember(categoriesList) {
        mutableStateOf(categoriesList.firstOrNull()?.name ?: "Food & Dining")
    }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var merchantText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    val selectedCategoryObj = categoriesList.firstOrNull { it.name.equals(selectedCategoryName, ignoreCase = true) }
    val selectedCategoryDisplayText = if (selectedCategoryObj != null) {
        "${selectedCategoryObj.emoji} ${strings.localizeCategory(selectedCategoryObj.name)}"
    } else {
        strings.localizeCategory(selectedCategoryName)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "➕ ${strings.logExpenseTitle}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.close)
                }
            }

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { newValue ->
                    // Allow empty, or valid number: digits with optional single decimal
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        amountText = newValue
                    }
                },
                label = { Text("${strings.amount} (MMK) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Category Selection Dropdown
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategoryDisplayText,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.selectSpendingBucketRequired) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )

                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    categoriesList.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.emoji} ${strings.localizeCategory(cat.name)}", fontWeight = FontWeight.Medium) },
                            onClick = {
                                selectedCategoryName = cat.name
                                isDropdownExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("➕", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    strings.addCustomBucket,
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        },
                        onClick = {
                            isDropdownExpanded = false
                            showAddCategoryDialog = true
                        }
                    )
                }
            }

            // Merchant / Store Input
            OutlinedTextField(
                value = merchantText,
                onValueChange = { merchantText = it },
                label = { Text(strings.merchantOptional) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Description Input
            OutlinedTextField(
                value = descriptionText,
                onValueChange = { descriptionText = it },
                label = { Text(strings.noteOptional) },
                modifier = Modifier.fillMaxWidth()
            )

            // Save Button
            val amountValue = amountText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    if (amountValue > 0) {
                        onSave(amountValue, selectedCategoryName, merchantText.trim(), descriptionText.trim())
                    }
                },
                enabled = amountValue > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(strings.saveExpense, style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Inline Add Custom Category Dialog
        if (showAddCategoryDialog) {
            AddCategoryDialog(
                showTargetField = false,
                onDismiss = { showAddCategoryDialog = false },
                onConfirm = { emoji, name, target ->
                    val cleanName = name.trim()
                    onAddCategory?.invoke(emoji, cleanName, target)
                    selectedCategoryName = cleanName
                    showAddCategoryDialog = false
                }
            )
        }
    }
}
