package com.savingcoach.app.ui.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import com.savingcoach.app.data.model.Expense
import com.savingcoach.app.ui.components.BudgetProgressBar
import com.savingcoach.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    viewModel: ExpenseViewModel,
    onNavigateToAddExpense: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Show Snackbar when errorMessage changes
    val errorMessage = uiState.errorMessage
    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrEmpty()) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Budget & Expense Hub",
                        fontWeight = FontWeight.Bold
                    )
                },
                windowInsets = WindowInsets(0.dp)
            )
        },
        contentWindowInsets = WindowInsets(0.dp)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading && uiState.expenses.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // ==========================================
                    // SECTION 1: Monthly Overall Budget Card
                    // ==========================================
                    item {
                        val limit = uiState.monthlyBudget?.limit ?: 0.0
                        val spent = uiState.totalSpent
                        val rawRemaining = limit - spent
                        val percentage = if (limit > 0) (spent / limit) * 100 else 0.0

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🎯 Monthly Overall Budget",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { viewModel.setEditBudgetDialogVisible(true) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Budget",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "${currencyFormat.format(spent.toLong())} MMK Spent / ${currencyFormat.format(limit.toLong())} MMK Target",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                BudgetProgressBar(
                                    percentage = percentage,
                                    height = 22.dp,
                                    showLabel = false
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${percentage.toInt()}% Used",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (percentage >= 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Remaining: ${currencyFormat.format(rawRemaining.toLong())} MMK",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (rawRemaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${uiState.daysLeftInMonth} Days Left",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }

                    // ==========================================
                    // SECTION 2: Log New Expense CTA Button
                    // ==========================================
                    item {
                        Button(
                            onClick = onNavigateToAddExpense,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 4.dp,
                                pressedElevation = 1.dp
                            )
                        ) {
                            Text(
                                text = "Log New Expense",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // ==========================================
                    // SECTION 3: Spending Buckets Horizontal Big Cards
                    // ==========================================
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🏷️ SPENDING BUCKETS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { viewModel.setAddCategoryDialogVisible(true) }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Bucket")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.categories) { category ->
                                val globalLimit = uiState.monthlyBudget?.limit ?: 0.0
                                val effectiveTarget = if (category.target > 0) category.target else globalLimit
                                val catPercentage = when {
                                    effectiveTarget > 0 -> (category.spent / effectiveTarget) * 100.0
                                    category.spent > 0 -> 100.0
                                    else -> 0.0
                                }
                                val isOverBudget = when {
                                    category.target > 0 -> category.spent > category.target
                                    globalLimit > 0 -> category.spent > globalLimit
                                    else -> false
                                }

                                Card(
                                    modifier = Modifier
                                        .widthIn(max = 330.dp)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isOverBudget)
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    ),
                                    border = if (isOverBudget)
                                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                                    else null
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // Header Row: Emoji + Category Name | Edit Icon Button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = if (isOverBudget) "🚨" else category.emoji,
                                                    style = MaterialTheme.typography.titleLarge
                                                )
                                                Text(
                                                    text = category.name,
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.setCategoryToEdit(category) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Target",
                                                    tint = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        // Spending Metrics Row: spent X / Y mmk | Percentage
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (category.target > 0)
                                                    "spent ${currencyFormat.format(category.spent.toLong())}/${currencyFormat.format(category.target.toLong())} mmk"
                                                else
                                                    "spent ${currencyFormat.format(category.spent.toLong())} mmk",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Text(
                                                text = "${catPercentage.toInt()}%",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        // Progress Bar (Always visible for uniform card height)
                                        val progressVal = (catPercentage / 100.0).toFloat().coerceIn(0f, 1f)
                                        LinearProgressIndicator(
                                            progress = { progressVal },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp)),
                                            color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // SECTION 4: Recent Expenses Header with Dropdown Filter
                    // ==========================================
                    item {
                        var filterDropdownExpanded by remember { mutableStateOf(false) }

                        val filterCategoryObj = uiState.categories.firstOrNull { it.name.equals(uiState.filterCategory, ignoreCase = true) }
                        val currentFilterText = if (filterCategoryObj != null) {
                            "${filterCategoryObj.emoji} ${filterCategoryObj.name}"
                        } else if (!uiState.filterCategory.isNullOrEmpty()) {
                            uiState.filterCategory!!
                        } else {
                            "All"
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🧾 RECENT EXPENSES",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Box {
                                    OutlinedButton(
                                        onClick = { filterDropdownExpanded = true },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text(currentFilterText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Filter Categories", modifier = Modifier.size(18.dp))
                                    }

                                    DropdownMenu(
                                        expanded = filterDropdownExpanded,
                                        onDismissRequest = { filterDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("All", fontWeight = if (uiState.filterCategory == null) FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                viewModel.selectFilterCategory(null)
                                                filterDropdownExpanded = false
                                            }
                                        )
                                        uiState.categories.forEach { cat ->
                                            DropdownMenuItem(
                                                text = { Text("${cat.emoji} ${cat.name}", fontWeight = if (uiState.filterCategory.equals(cat.name, ignoreCase = true)) FontWeight.Bold else FontWeight.Normal) },
                                                onClick = {
                                                    viewModel.selectFilterCategory(cat.name)
                                                    filterDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "Select category filter to refine this list",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Recent Expenses Items
                    if (uiState.filteredExpenses.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (uiState.filterCategory != null && uiState.expenses.isNotEmpty())
                                        "No expenses in this category."
                                    else
                                        "No expenses yet. Tap + to log your first expense!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        items(uiState.filteredExpenses, key = { it.id }) { expense ->
                            ExpenseItemRow(
                                expense = expense,
                                currencyFormat = currencyFormat,
                                onDeleteClick = { viewModel.setExpenseToDelete(expense) }
                            )
                        }
                    }
                }
            }

            // Edit Budget Dialog
            if (uiState.isEditBudgetDialogOpen) {
                val globalLimit = uiState.monthlyBudget?.limit ?: 0.0
                EditBudgetDialog(
                    currentLimit = globalLimit,
                    currencyFormat = currencyFormat,
                    onDismiss = { viewModel.setEditBudgetDialogVisible(false) },
                    onConfirm = { newLimit -> viewModel.updateBudgetLimit(newLimit) }
                )
            }

            // Add Category Dialog (Main screen mode: includes target limit field)
            if (uiState.isAddCategoryDialogOpen) {
                val globalLimit = uiState.monthlyBudget?.limit ?: 0.0
                val categoryTargetsSum = uiState.categories.sumOf { it.target }
                val maxAllowedForNew = (globalLimit - categoryTargetsSum).coerceAtLeast(0.0)
                AddCategoryDialog(
                    globalLimit = globalLimit,
                    maxAllowedTarget = maxAllowedForNew,
                    currencyFormat = currencyFormat,
                    showTargetField = true,
                    onDismiss = { viewModel.setAddCategoryDialogVisible(false) },
                    onConfirm = { emoji, name, target -> viewModel.addCustomCategory(emoji, name, target) }
                )
            }

            // Edit Category Target Dialog
            uiState.categoryToEdit?.let { category ->
                val globalLimit = uiState.monthlyBudget?.limit ?: 0.0
                val otherSum = uiState.categories.filterNot { it.name.equals(category.name, ignoreCase = true) }.sumOf { it.target }
                val maxAllowedForEdit = (globalLimit - otherSum).coerceAtLeast(0.0)
                EditCategoryTargetDialog(
                    category = category,
                    globalLimit = globalLimit,
                    maxAllowedTarget = maxAllowedForEdit,
                    currencyFormat = currencyFormat,
                    onDismiss = { viewModel.setCategoryToEdit(null) },
                    onConfirm = { newTarget -> viewModel.updateCategoryTarget(category.name, newTarget) },
                    onDelete = { viewModel.deleteCategory(category.name) }
                )
            }

            // Delete Expense Dialog
            uiState.expenseToDelete?.let { expense ->
                AlertDialog(
                    onDismissRequest = { viewModel.setExpenseToDelete(null) },
                    title = { Text("Delete Expense") },
                    text = { Text("Are you sure you want to delete this expense of ${currencyFormat.format(expense.amount.toLong())} MMK?") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.deleteExpense(expense.id) }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.setExpenseToDelete(null) }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ExpenseItemRow(
    expense: Expense,
    currencyFormat: NumberFormat,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.category,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (expense.merchant.isNotEmpty()) {
                    Text(
                        text = expense.merchant,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = try {
                        val parsed = java.time.LocalDate.parse(expense.date)
                        parsed.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
                    } catch (e: Exception) {
                        expense.date.ifEmpty { "Today" }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "-${currencyFormat.format(expense.amount.toLong())} MMK",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun EditBudgetDialog(
    currentLimit: Double,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit,
    onConfirm: (newLimit: Double) -> Unit
) {
    var limitText by remember { mutableStateOf(if (currentLimit > 0) currentLimit.toBigDecimal().stripTrailingZeros().toPlainString() else "") }
    val enteredLimit = limitText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Monthly Overall Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            limitText = newValue
                        }
                    },
                    label = { Text("Global Budget Limit (MMK)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (enteredLimit >= 0) onConfirm(enteredLimit)
                },
                enabled = enteredLimit >= 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditCategoryTargetDialog(
    category: ExpenseCategory,
    globalLimit: Double,
    maxAllowedTarget: Double,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit,
    onConfirm: (newTarget: Double) -> Unit,
    onDelete: () -> Unit = {}
) {
    var targetText by remember { mutableStateOf(if (category.target > 0) category.target.toBigDecimal().stripTrailingZeros().toPlainString() else "") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val enteredTarget = targetText.toDoubleOrNull() ?: 0.0
    val isExceedingGlobal = globalLimit > 0 && enteredTarget > maxAllowedTarget
    val isGlobalZero = globalLimit == 0.0 && enteredTarget > 0.0

    // Only show one dialog at a time
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Category?") },
            text = { Text("Are you sure you want to delete '${category.emoji} ${category.name}'? This spending bucket will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit Category Budget")
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${category.emoji} ${category.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                targetText = newValue
                            }
                        },
                        label = { Text("Monthly Target Limit (MMK)") },
                        isError = isExceedingGlobal || isGlobalZero,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                            text = "⚠️ Cannot exceed Global Budget! Max available for this category: ${currencyFormat.format(maxAllowedTarget.toLong())} MMK.",
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
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete separated from Save by Spacer, styled distinctly
                    TextButton(
                        onClick = { showDeleteConfirmation = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete", fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.width(32.dp))
                    TextButton(
                        onClick = {
                            if (!isExceedingGlobal && !isGlobalZero) {
                                onConfirm(enteredTarget)
                            }
                        },
                        enabled = !isExceedingGlobal && !isGlobalZero
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = null
        )
    }
}
