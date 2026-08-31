package com.savingcoach.app.ui.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    val currencyFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val dateTimeFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a") }
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        strings.expensesTitle,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
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
                        val rawRemaining = limit.toBigDecimal().subtract(spent.toBigDecimal())
                        val percentage = if (limit > 0) (spent / limit) * 100 else 0.0

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(22.dp)
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "🎯",
                                            fontSize = 20.sp
                                        )
                                        Text(
                                            text = "${strings.monthlyOverallBudget} (${com.savingcoach.app.utils.InvestmentCalculations.getCurrencyLabel(uiState.currencyPreference, isInvestment = false)})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.setEditBudgetDialogVisible(true) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = strings.editBudget,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (limit > 0) {
                                        com.savingcoach.app.ui.components.AutoScalingText(
                                            text = strings.formatAmount(limit, uiState.currencyPreference, 1.0, isInvestment = false),
                                            maxTextSize = 32.sp,
                                            minTextSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                BudgetProgressBar(
                                    percentage = percentage,
                                    height = 8.dp,
                                    showLabel = false
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        com.savingcoach.app.ui.components.AutoScalingText(
                                            text = "${strings.remaining}: ${strings.formatAmount(rawRemaining.toDouble(), uiState.currencyPreference, 1.0, isInvestment = false)}",
                                            maxTextSize = 13.sp,
                                            minTextSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (rawRemaining < java.math.BigDecimal.ZERO) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                        com.savingcoach.app.ui.components.AutoScalingText(
                                            text = "${strings.spent}: ${strings.formatAmount(spent, uiState.currencyPreference, 1.0, isInvestment = false)}",
                                            maxTextSize = 13.sp,
                                            minTextSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text(
                                            text = "${strings.formatNumber(uiState.daysLeftInMonth)} ${strings.days}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
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
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = strings.addExpense,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.addExpense,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
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
                                text = "🏷️ ${strings.spendingBuckets}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { viewModel.setAddCategoryDialogVisible(true) }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(strings.newBucket)
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
                                        .width(320.dp)
                                        .height(160.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Header Row: Emoji Tile + Category Name | Edit Icon Button
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
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .background(
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                            RoundedCornerShape(12.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = category.emoji,
                                                        fontSize = 20.sp
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = strings.localizeCategory(category.name),
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (category.target > 0) {
                                                        Text(
                                                            text = "${strings.target}: ${strings.formatAmount(category.target, uiState.currencyPreference, 1.0, isInvestment = false)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }

                                            IconButton(
                                                onClick = { viewModel.setCategoryToEdit(category) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Target",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // Metrics Row: Spent amount + percentage badge
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "${strings.spent}:",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = strings.formatAmount(category.spent, uiState.currencyPreference, 1.0, isInvestment = false),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOverBudget) CoralRed else MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isOverBudget) CoralRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                border = BorderStroke(1.dp, if (isOverBudget) CoralRed.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                            ) {
                                                Text(
                                                    text = "${strings.formatNumber(catPercentage.toInt())}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOverBudget) CoralRed else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        // Progress Bar
                                        val progressVal = (catPercentage / 100.0).toFloat().coerceIn(0f, 1f)
                                        LinearProgressIndicator(
                                            progress = { progressVal },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = if (isOverBudget) CoralRed else MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
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
                        var filterMenuExpanded by remember { mutableStateOf(false) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🧾 ${strings.recentExpenses.uppercase()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Box {
                                val currentFilterName = uiState.filterCategory
                                val currentCategory = uiState.categories.firstOrNull { it.name.equals(currentFilterName, ignoreCase = true) }
                                val filterText = if (currentCategory != null) {
                                    "${currentCategory.emoji} ${strings.localizeCategory(currentCategory.name)}"
                                } else {
                                    strings.allBuckets
                                }

                                Surface(
                                    onClick = { filterMenuExpanded = true },
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = filterText,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Filter",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = filterMenuExpanded,
                                    onDismissRequest = { filterMenuExpanded = false },
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.widthIn(min = 180.dp)
                                ) {
                                    val isAllSelected = uiState.filterCategory == null
                                    DropdownMenuItem(
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isAllSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent),
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text("🏷️", fontSize = 15.sp)
                                                    Text(
                                                        text = strings.allBuckets,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                if (isAllSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectFilterCategory(null)
                                            filterMenuExpanded = false
                                        }
                                    )
                                    uiState.categories.forEach { cat ->
                                        val isSelected = uiState.filterCategory.equals(cat.name, ignoreCase = true)
                                        DropdownMenuItem(
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent),
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Text(cat.emoji, fontSize = 15.sp)
                                                        Text(
                                                            text = strings.localizeCategory(cat.name),
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                viewModel.selectFilterCategory(cat.name)
                                                filterMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
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
                                        strings.noExpensesInCategory
                                    else
                                        strings.noExpensesYet,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(uiState.filteredExpenses, key = { it.id }) { expense ->
                            ExpenseItemRow(
                                expense = expense,
                                currencyFormat = currencyFormat,
                                currencyPreference = uiState.currencyPreference,
                                dateTimeFormatter = dateTimeFormatter,
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
                    currencyPreference = uiState.currencyPreference,
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
                    currencyPreference = uiState.currencyPreference,
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
                    currencyPreference = uiState.currencyPreference,
                    onDismiss = { viewModel.setCategoryToEdit(null) },
                    onConfirm = { newTarget ->
                        viewModel.updateCategoryTarget(category.name, newTarget)
                        viewModel.setCategoryToEdit(null)
                    },
                    onDelete = {
                        viewModel.deleteCategory(category.name)
                        viewModel.setCategoryToEdit(null)
                    }
                )
            }

            // Delete Expense Dialog
            uiState.expenseToDelete?.let { expense ->
                AlertDialog(
                    onDismissRequest = { viewModel.setExpenseToDelete(null) },
                    title = { Text(strings.deleteExpenseConfirmTitle) },
                    text = { Text(strings.deleteExpenseConfirmMsg(com.savingcoach.app.utils.InvestmentCalculations.formatValue(expense.amount, uiState.currencyPreference, 1.0, isInvestment = false))) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.deleteExpense(expense.id) }) {
                            Text(strings.delete, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.setExpenseToDelete(null) }) {
                            Text(strings.cancel)
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
    currencyPreference: String,
    dateTimeFormatter: java.time.format.DateTimeFormatter,
    onDeleteClick: () -> Unit
) {
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
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
                    text = strings.localizeCategory(expense.category),
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
                    text = strings.formatExpenseDateTime(expense.createdAt, expense.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "-" + strings.formatAmount(expense.amount, currencyPreference, 1.0, isInvestment = false),
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
                        contentDescription = strings.delete,
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
    currencyPreference: String = "MMK",
    onDismiss: () -> Unit,
    onConfirm: (newLimit: Double) -> Unit
) {
    var limitText by remember { mutableStateOf(if (currentLimit > 0) currentLimit.toBigDecimal().stripTrailingZeros().toPlainString() else "") }
    val enteredLimit = limitText.toDoubleOrNull() ?: 0.0
    val strings = com.savingcoach.app.ui.localization.AppLocale.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.editBudget) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            limitText = newValue
                        }
                    },
                    label = { Text("${strings.globalBudgetLimit} (${com.savingcoach.app.utils.InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = false)})") },
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

@Composable
fun EditCategoryTargetDialog(
    category: ExpenseCategory,
    globalLimit: Double,
    maxAllowedTarget: Double,
    currencyFormat: NumberFormat,
    currencyPreference: String = "MMK",
    onDismiss: () -> Unit,
    onConfirm: (newTarget: Double) -> Unit,
    onDelete: () -> Unit = {}
) {
    var targetText by remember { mutableStateOf(if (category.target > 0) category.target.toBigDecimal().stripTrailingZeros().toPlainString() else "") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val strings = com.savingcoach.app.ui.localization.AppLocale.current

    val enteredTarget = targetText.toDoubleOrNull() ?: 0.0
    val isExceedingGlobal = globalLimit > 0 && enteredTarget > maxAllowedTarget
    val isGlobalZero = globalLimit == 0.0 && enteredTarget > 0.0

    // Only show one dialog at a time
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(strings.delete) },
            text = { Text("${category.emoji} ${category.name} - ${strings.delete}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(strings.cancel)
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
                    Text(strings.edit)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.close,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        Text(strings.delete, fontWeight = FontWeight.Medium)
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
                        Text(strings.save, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = null
        )
    }
}
