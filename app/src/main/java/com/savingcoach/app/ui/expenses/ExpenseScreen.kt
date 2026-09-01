package com.savingcoach.app.ui.expenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.savingcoach.app.R
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

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
                    // SECTION 1: Monthly Overall Budget Card (Hero Card)
                    // ==========================================
                    item {
                        val limit = uiState.monthlyBudget?.limit ?: 0.0
                        val spent = uiState.totalSpent
                        val rawRemaining = limit.toBigDecimal().subtract(spent.toBigDecimal())
                        val percentage = if (limit > 0) (spent / limit) * 100 else 0.0

                        HeroBudgetCard(
                            budgetAmount = if (limit > 0) strings.formatAmount(limit, uiState.currencyPreference, 1.0, isInvestment = false) else strings.setBudget,
                            currencyLabel = com.savingcoach.app.utils.InvestmentCalculations.getCurrencyLabel(uiState.currencyPreference, isInvestment = false),
                            spentAmount = strings.formatAmount(spent, uiState.currencyPreference, 1.0, isInvestment = false),
                            remainingAmount = strings.formatAmount(rawRemaining.toDouble(), uiState.currencyPreference, 1.0, isInvestment = false),
                            isOverBudget = rawRemaining < java.math.BigDecimal.ZERO,
                            progress = (percentage / 100.0).toFloat().coerceIn(0f, 1f),
                            percentage = percentage,
                            daysLeftText = "${strings.formatNumber(uiState.daysLeftInMonth)} ${strings.days}",
                            isLimitSet = limit > 0,
                            onEditClick = { viewModel.setEditBudgetDialogVisible(true) }
                        )
                    }

                    // ==========================================
                    // SECTION 2: Log New Expense CTA Button
                    // ==========================================
                    item {
                        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                        val buttonBrush = if (isDark) {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF3B6E47),
                                    Color(0xFF2E5A38)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF2D5A38),
                                    Color(0xFF386641),
                                    Color(0xFF2A5234)
                                )
                            )
                        }

                        Surface(
                            onClick = onNavigateToAddExpense,
                            shape = RoundedCornerShape(20.dp),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(buttonBrush),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = strings.addExpense,
                                        modifier = Modifier.size(22.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.addExpense,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                            }
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

                                val backgroundColor = MaterialTheme.colorScheme.background
                                val isDark = backgroundColor.luminance() < 0.5f

                                val bucketBrush = if (isDark) {
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

                                Card(
                                    modifier = Modifier
                                        .width(320.dp)
                                        .height(162.dp),
                                    shape = RoundedCornerShape(22.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.Transparent
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF38403A) else Color(0xFFE5E0CE))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(bucketBrush)
                                            .padding(18.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
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
                                                            text = category.emoji,
                                                            fontSize = 22.sp
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
                                                        tint = if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F),
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
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isOverBudget) CoralRed.copy(alpha = 0.12f) else if (isDark) Color(0xFF2E3830) else Color(0xFFE8EFE8),
                                                    border = BorderStroke(1.dp, if (isOverBudget) CoralRed.copy(alpha = 0.3f) else if (isDark) Color(0xFF3F4E42) else Color(0xFFD0E0D2))
                                                ) {
                                                    Text(
                                                        text = "${strings.formatNumber(catPercentage.toInt())}%",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isOverBudget) CoralRed else if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F),
                                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
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
                                                color = if (isOverBudget) CoralRed else if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F),
                                                trackColor = if (isDark) Color(0xFF2D352F) else Color(0xFFE8ECE8)
                                            )
                                        }
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
                                    color = if (isDark) Color(0xFF242925) else Color(0xFFEFECE2),
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF38403A) else Color(0xFFE2DDD0))
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
                                    containerColor = if (isDark) Color(0xFF242925) else Color(0xFFFCFBF7),
                                    shape = RoundedCornerShape(22.dp),
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF38403A) else Color(0xFFE5E0CE)),
                                    modifier = Modifier.widthIn(min = 190.dp)
                                ) {
                                    val isAllSelected = uiState.filterCategory == null
                                    DropdownMenuItem(
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isAllSelected) (if (isDark) Color(0xFF2F3831) else Color(0xFFE8EFE8)) else Color.Transparent),
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text("📋", fontSize = 17.sp)
                                                Text(
                                                    text = strings.allBuckets,
                                                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isAllSelected) (if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F)) else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectFilterCategory(null)
                                            filterMenuExpanded = false
                                        },
                                        trailingIcon = if (isAllSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else null
                                    )

                                    HorizontalDivider(
                                        color = if (isDark) Color(0xFF38403A) else Color(0xFFE5E0CE),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    uiState.categories.forEach { cat ->
                                        val isSelected = uiState.filterCategory?.equals(cat.name, ignoreCase = true) == true
                                        DropdownMenuItem(
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) (if (isDark) Color(0xFF2F3831) else Color(0xFFE8EFE8)) else Color.Transparent),
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Text(cat.emoji, fontSize = 17.sp)
                                                    Text(
                                                        text = strings.localizeCategory(cat.name),
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = if (isSelected) (if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F)) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.selectFilterCategory(cat.name)
                                                filterMenuExpanded = false
                                            },
                                            trailingIcon = if (isSelected) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // SECTION 5: Recent Expenses List
                    // ==========================================
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
                            val categoryEmoji = uiState.categories.find { it.name.equals(expense.category, ignoreCase = true) }?.emoji ?: "💸"
                            ExpenseItemRow(
                                expense = expense,
                                currencyFormat = currencyFormat,
                                currencyPreference = uiState.currencyPreference,
                                dateTimeFormatter = dateTimeFormatter,
                                categoryEmoji = categoryEmoji,
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
fun HeroBudgetCard(
    budgetAmount: String,
    currencyLabel: String,
    spentAmount: String,
    remainingAmount: String,
    isOverBudget: Boolean,
    progress: Float,
    percentage: Double,
    daysLeftText: String,
    isLimitSet: Boolean,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDark = backgroundColor.luminance() < 0.5f

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

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF38403A) else Color(0xFFE5E0CE)),
        shape = RoundedCornerShape(26.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // 1. Right Visual Area: Big Mascot moved to the right edge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 14.dp, y = 6.dp)
                    .width(175.dp)
                    .height(155.dp)
            ) {
                Image(
                    painter = painterResource(
                        id = if (percentage >= 100) R.drawable.piggy_missed else R.drawable.ic_piggy_hero
                    ),
                    contentDescription = "Budget Mascot",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )

                // Days pill placed directly over the right side of the piggy bank
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF2A302B).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF434E45) else Color(0xFFE2DEC8)),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-12).dp, y = (-12).dp)
                ) {
                    Text(
                        text = daysLeftText,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFE0E0E0) else Color(0xFF333333),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // 2. Main Content Column: Full-width Header at top, lower-left metrics below
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Header: Target Icon + Title on left, Edit Pencil Icon at the top-right corner
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = "🎯",
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${strings.monthlyOverallBudget} ($currencyLabel)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = strings.editBudget,
                            tint = if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Lower-Left Column: Budget Amount, Progress Bar, and Metrics (Constrained to 56% width)
                Column(
                    modifier = Modifier.fillMaxWidth(0.56f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Big Bold Budget Amount Display
                    if (isLimitSet) {
                        com.savingcoach.app.ui.components.AutoScalingText(
                            text = budgetAmount,
                            maxTextSize = 38.sp,
                            minTextSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = budgetAmount,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onEditClick() }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Progress Bar
                    BudgetProgressBar(
                        percentage = percentage,
                        height = 8.dp,
                        showLabel = false
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Metrics: Remaining & Spent
                    com.savingcoach.app.ui.components.AutoScalingText(
                        text = "${strings.remaining}: $remainingAmount",
                        maxTextSize = 14.sp,
                        minTextSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F)
                    )
                    com.savingcoach.app.ui.components.AutoScalingText(
                        text = "${strings.spent}: $spentAmount",
                        maxTextSize = 13.sp,
                        minTextSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
    onDeleteClick: () -> Unit,
    categoryEmoji: String = "💸"
) {
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDark = backgroundColor.luminance() < 0.5f

    val rowBrush = if (isDark) {
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF38403A) else Color(0xFFE5E0CE))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(rowBrush)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Category Emoji Container Tile
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
                            text = categoryEmoji,
                            fontSize = 22.sp
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = strings.localizeCategory(expense.category),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (expense.merchant.isNotBlank()) {
                            Text(
                                text = expense.merchant,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = strings.formatExpenseDateTime(expense.createdAt, expense.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "-" + strings.formatAmount(expense.amount, currencyPreference, 1.0, isInvestment = false),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CoralRed
                    )
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = strings.delete,
                            tint = if (isDark) Color(0xFF758077) else Color(0xFF9E9B8F),
                            modifier = Modifier.size(19.dp)
                        )
                    }
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = if (isDark) Color(0xFF222724) else Color(0xFFFCFBF7),
        title = {
            Text(
                text = "🎯 ${strings.editBudget}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
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
                    shape = RoundedCornerShape(16.dp),
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
                enabled = enteredLimit >= 0,
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val enteredTarget = targetText.toDoubleOrNull() ?: 0.0
    val isExceedingGlobal = globalLimit > 0 && enteredTarget > maxAllowedTarget
    val isGlobalZero = globalLimit == 0.0 && enteredTarget > 0.0

    // Only show one dialog at a time
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = if (isDark) Color(0xFF222724) else Color(0xFFFCFBF7),
            title = { Text(strings.delete, fontWeight = FontWeight.Bold) },
            text = { Text("${category.emoji} ${category.name} - ${strings.delete}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
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
            shape = RoundedCornerShape(26.dp),
            containerColor = if (isDark) Color(0xFF222724) else Color(0xFFFCFBF7),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✏️ ${strings.edit}", fontWeight = FontWeight.Bold)
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showDeleteConfirmation = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(strings.delete, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (!isExceedingGlobal && !isGlobalZero) {
                                onConfirm(enteredTarget)
                            }
                        },
                        enabled = !isExceedingGlobal && !isGlobalZero,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(strings.save, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = null
        )
    }
}
