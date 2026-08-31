package com.savingcoach.app.ui.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onBackClick: () -> Unit,
    onSaveClick: (amount: Double, category: String, merchant: String, description: String) -> Unit,
    availableCategories: List<ExpenseCategory> = emptyList(),
    currencyPreference: String = "MMK",
    onAddCategory: ((emoji: String, name: String, target: Double) -> Unit)? = null,
    onDeleteCategory: ((categoryName: String) -> Unit)? = null
) {
    // Fresh state on every composition — fields start empty
    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }

    val categoriesList = if (availableCategories.isNotEmpty()) availableCategories else ExpenseCategory.DEFAULT_CATEGORIES
    var selectedCategoryName by remember(categoriesList) {
        mutableStateOf(categoriesList.firstOrNull()?.name ?: "Food & Dining")
    }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    val selectedCatObj = categoriesList.firstOrNull { it.name.equals(selectedCategoryName, ignoreCase = true) }
    val selectedDisplayText = if (selectedCatObj != null) "${selectedCatObj.emoji} ${strings.localizeCategory(selectedCatObj.name)}" else strings.localizeCategory(selectedCategoryName)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(strings.logExpenseTitle) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { newValue ->
                    // Allow empty, or valid number: digits with optional single decimal
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        amountText = newValue
                    }
                },
                label = { Text("${strings.amount} (${com.savingcoach.app.utils.InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = false)}) *") },
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
                    value = selectedDisplayText,
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
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${cat.emoji} ${strings.localizeCategory(cat.name)}",
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (onDeleteCategory != null) {
                                        IconButton(
                                            onClick = { 
                                                categoryToDelete = cat.name
                                                isDropdownExpanded = false
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = strings.delete,
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            },
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

            // Merchant Input
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

            Spacer(modifier = Modifier.height(8.dp))

            val amountVal = amountText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    if (amountVal > 0) {
                        onSaveClick(amountVal, selectedCategoryName, merchantText.trim(), descriptionText.trim())
                    }
                },
                enabled = amountVal > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(strings.saveExpense, style = MaterialTheme.typography.titleMedium)
            }
        }

        if (showAddCategoryDialog) {
            AddCategoryDialog(
                showTargetField = false,
                currencyPreference = currencyPreference,
                onDismiss = { showAddCategoryDialog = false },
                onConfirm = { emoji, name, target ->
                    val cleanName = name.trim()
                    onAddCategory?.invoke(emoji, cleanName, target)
                    selectedCategoryName = cleanName
                    showAddCategoryDialog = false
                }
            )
        }

        if (categoryToDelete != null) {
            val catObj = categoriesList.firstOrNull { it.name.equals(categoryToDelete, ignoreCase = true) }
            val displayName = if (catObj != null) "${catObj.emoji} ${strings.localizeCategory(catObj.name)}" else categoryToDelete!!
            AlertDialog(
                onDismissRequest = { categoryToDelete = null },
                title = { Text(strings.deleteBucketTitle) },
                text = { Text(strings.deleteBucketConfirm(displayName)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteCategory?.invoke(categoryToDelete!!)
                            categoryToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(strings.delete)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToDelete = null }) {
                        Text(strings.cancel)
                    }
                }
            )
        }
    }
}
