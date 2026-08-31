package com.savingcoach.app.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.savingcoach.app.data.model.Budget
import com.savingcoach.app.data.model.Expense
import com.savingcoach.app.data.model.ExpenseCategoryEntity
import com.savingcoach.app.data.repository.BudgetRepository
import com.savingcoach.app.data.repository.ExpenseCategoryRepository
import com.savingcoach.app.data.repository.ExpenseRepository
import com.savingcoach.app.data.repository.UserRepository
import com.savingcoach.app.data.repository.ExchangeRateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val expenseCategoryRepository: ExpenseCategoryRepository,
    private val userRepository: UserRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val notificationHelper: com.savingcoach.app.core.notification.NotificationHelper,
    private val auth: FirebaseAuth
) : ViewModel() {

    companion object {
        private val EMOJI_STRIP_REGEX =
            Regex("^[\\u2600-\\u27BF\\uFE00-\\uFE0F\\u200D\\u20E3\\uD83C-\\uDBFF\\uDC00-\\uDFFF\\u2000-\\u2BFF\\u2300-\\u23FF\\u2B50-\\u2B55\\u2934-\\u2935\\u25AA-\\u25FE\\u2190-\\u21FF\\u2122\\u00A9\\u00AE]+\\s*")

        private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
        private data class Sextuple<A, B, C, D, E, F>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E, val sixth: F)
    }

    private val sessionExpenses = java.util.Collections.synchronizedMap(LinkedHashMap<String, Expense>())
    private val activeCategories = java.util.Collections.synchronizedList(mutableListOf<ExpenseCategory>())
    private val deletedCategoryNames = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private var loadJob: Job? = null
    private var currencyPreference = "MMK"
    private var usdRate = 1.0

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            exchangeRateRepository.fetchLatestRate(force = false)
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userId = auth.currentUser?.uid ?: ""
                val currentMonth = LocalDate.now().toString().substring(0, 7)

                combine(
                    budgetRepository.getBudget(userId, currentMonth),
                    expenseRepository.getExpensesForMonth(userId, currentMonth),
                    expenseCategoryRepository.getCategories(userId, currentMonth),
                    expenseCategoryRepository.getDeletedCategoryNames(userId, currentMonth),
                    combine(
                        userRepository.getUserProfileFlow(userId).catch { emit(null) },
                        exchangeRateRepository.usdToMmkRate
                    ) { profile, rate -> Pair(profile, rate) }
                ) { budgetObj, expenseList, savedCategories, persistedDeleted, profileRate ->
                    val userProfile = profileRate.first
                    val usdRateVal = profileRate.second
                    val currencyPref = userProfile?.currencyPreference ?: "MMK"

                    this@ExpenseViewModel.currencyPreference = currencyPref
                    this@ExpenseViewModel.usdRate = usdRateVal

                    // Filter session cache to current month only
                    val filteredSession = sessionExpenses.values.filter { it.date.startsWith(currentMonth) }
                    val combinedList = (expenseList + filteredSession)
                        .distinctBy { it.id }
                        .sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.createdAt })

                    // Reconcile: remove session entries that match Firestore entries (offline→online sync)
                    if (expenseList.isNotEmpty()) {
                        val firestoreKeys = expenseList.map { it.id }.toSet()
                        val sessionToRemove = sessionExpenses.keys.filter { key ->
                            key !in firestoreKeys && sessionExpenses[key]?.let { s ->
                                expenseList.any { it.amount == s.amount && it.date == s.date && it.category == s.category }
                            } == true
                        }
                        sessionToRemove.forEach { sessionExpenses.remove(it) }
                    }

                    // Load persisted deleted names into memory set
                    deletedCategoryNames.clear()
                    deletedCategoryNames.addAll(persistedDeleted)

                    // Merge saved categories with defaults
                    val mergedCategories = mergeCategories(ExpenseCategory.DEFAULT_CATEGORIES, savedCategories)
                    activeCategories.clear()
                    activeCategories.addAll(mergedCategories)

                    // Calculate days left fresh on each emission
                    val now = LocalDate.now()
                    val lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth())
                    val daysLeft = ChronoUnit.DAYS.between(now, lastDayOfMonth).toInt() + 1

                    updateStateFromData(
                        budget = budgetObj,
                        expenses = combinedList,
                        daysLeft = daysLeft,
                        userId = userId,
                        currentMonth = currentMonth
                    )
                }.collect()
            } catch (e: Exception) {
                val currentMonth = LocalDate.now().toString().substring(0, 7)
                val filteredSession = sessionExpenses.values.filter { it.date.startsWith(currentMonth) }
                val combinedList = (_uiState.value.expenses + filteredSession)
                    .distinctBy { it.id }
                    .sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.createdAt })
                updateStateFromData(
                    budget = _uiState.value.monthlyBudget,
                    expenses = combinedList,
                    daysLeft = 0,
                    userId = auth.currentUser?.uid ?: "",
                    currentMonth = currentMonth,
                    error = e.message
                )
            }
        }
    }

    private fun updateStateFromData(
        budget: Budget?,
        expenses: List<Expense>,
        daysLeft: Int,
        userId: String,
        currentMonth: String,
        error: String? = null
    ) {
        val targetCurrency = com.savingcoach.app.utils.InvestmentCalculations.getTargetCurrency(
            this.currencyPreference, 
            isInvestment = false
        )

        val totalSpent = expenses.sumOf { 
            com.savingcoach.app.utils.InvestmentCalculations.convertAmount(
                amount = it.amount,
                fromCurrency = it.currency,
                toCurrency = targetCurrency,
                usdRate = this.usdRate
            )
        }

        val categorySpendingMap = expenses
            .groupBy { extractCleanCategoryName(it.category) }
            .mapValues { entry -> 
                entry.value.sumOf { 
                    com.savingcoach.app.utils.InvestmentCalculations.convertAmount(
                        amount = it.amount,
                        fromCurrency = it.currency,
                        toCurrency = targetCurrency,
                        usdRate = this.usdRate
                    )
                } 
            }

        val budgetCurrency = budget?.currency ?: "MMK"
        val updatedCategories = activeCategories.map { cat ->
            val spentForCat = calculateSpentForCategory(cat.name, categorySpendingMap)
            val convertedTarget = com.savingcoach.app.utils.InvestmentCalculations.convertAmount(
                amount = cat.target,
                fromCurrency = budgetCurrency,
                toCurrency = targetCurrency,
                usdRate = this.usdRate
            )
            cat.copy(
                spent = spentForCat,
                target = convertedTarget
            )
        }

        val displayExpenses = expenses.map { expense ->
            val converted = com.savingcoach.app.utils.InvestmentCalculations.convertAmount(
                amount = expense.amount,
                fromCurrency = expense.currency,
                toCurrency = targetCurrency,
                usdRate = this.usdRate
            )
            expense.copy(
                amount = converted,
                currency = targetCurrency
            )
        }

        val convertedBudgetLimit = if (budget != null) {
            com.savingcoach.app.utils.InvestmentCalculations.convertAmount(
                amount = budget.limit,
                fromCurrency = budget.currency,
                toCurrency = targetCurrency,
                usdRate = this.usdRate
            )
        } else {
            0.0
        }

        _uiState.update { state ->
            val filterCat = state.filterCategory
            state.copy(
                monthlyBudget = (budget ?: Budget(
                    userId = userId,
                    month = currentMonth,
                    limit = 0.0,
                    currency = targetCurrency
                )).copy(
                    limit = convertedBudgetLimit,
                    currency = targetCurrency
                ),
                totalSpent = totalSpent,
                daysLeftInMonth = daysLeft,
                categories = updatedCategories,
                categorySpending = categorySpendingMap,
                expenses = displayExpenses,
                filteredExpenses = filterExpenses(displayExpenses, filterCat),
                currencyPreference = this.currencyPreference,
                usdRate = this.usdRate,
                isLoading = false,
                errorMessage = error
            )
        }
    }

    fun selectCategory(categoryName: String?) {
        val cleanName = categoryName?.let { extractCleanCategoryName(it) }
        val newSelection = if (cleanName == _uiState.value.selectedCategory) null else cleanName
        _uiState.update { state ->
            state.copy(selectedCategory = newSelection)
        }
    }

    fun selectFilterCategory(categoryName: String?) {
        val cleanName = categoryName?.let { extractCleanCategoryName(it) }
        val newFilter = if (cleanName == _uiState.value.filterCategory) null else cleanName
        _uiState.update { state ->
            state.copy(
                filterCategory = newFilter,
                filteredExpenses = filterExpenses(state.expenses, newFilter)
            )
        }
    }

    private fun filterExpenses(list: List<Expense>, categoryName: String?): List<Expense> {
        if (categoryName.isNullOrEmpty() || categoryName.equals("All", ignoreCase = true)) return list
        val cleanTarget = extractCleanCategoryName(categoryName)
        return list.filter { expense ->
            val cleanExpenseCat = extractCleanCategoryName(expense.category)
            cleanExpenseCat.equals(cleanTarget, ignoreCase = true) ||
            cleanExpenseCat.contains(cleanTarget, ignoreCase = true)
        }
    }

    fun extractCleanCategoryName(rawCategory: String): String {
        if (rawCategory.isBlank()) return rawCategory
        val cleaned = rawCategory.replace(EMOJI_STRIP_REGEX, "").trim()
        return if (cleaned.isNotEmpty()) cleaned else rawCategory.trim()
    }

    private fun calculateSpentForCategory(catName: String, categorySpendingMap: Map<String, Double>): Double {
        val cleanCatName = extractCleanCategoryName(catName)
        return categorySpendingMap.entries.filter { (key, _) ->
            val cleanKey = extractCleanCategoryName(key)
            cleanKey.equals(cleanCatName, ignoreCase = true)
        }.sumOf { it.value }
    }

    fun addExpense(amount: Double, category: String, merchant: String, description: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: ""
            val todayStr = LocalDate.now().toString()
            val cleanCategoryName = extractCleanCategoryName(category)
            val targetCurrency = com.savingcoach.app.utils.InvestmentCalculations.getTargetCurrency(
                currencyPreference, 
                isInvestment = false
            )

            val newExpense = Expense(
                id = UUID.randomUUID().toString(),
                userId = userId,
                amount = amount,
                category = cleanCategoryName,
                merchant = merchant,
                description = description,
                date = todayStr,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                source = "manual",
                currency = targetCurrency
            )

            try {
                // Persist to Firestore — use returned doc ID as the canonical ID
                val firestoreId = expenseRepository.addExpense(newExpense)
                val savedExpense = newExpense.copy(id = firestoreId)
                sessionExpenses[firestoreId] = savedExpense

                val currentBudget = _uiState.value.monthlyBudget
                if (currentBudget != null && currentBudget.limit > 0) {
                    val newTotalSpent = _uiState.value.totalSpent + amount
                    val percentage = ((newTotalSpent / currentBudget.limit) * 100).toInt()
                    if (percentage >= 100) {
                        notificationHelper.showBudgetAlert(percentage, newTotalSpent - currentBudget.limit)
                    } else if (percentage >= 90) {
                        notificationHelper.showBudgetAlert(percentage)
                    }
                }
            } catch (e: Exception) {
                // If Firestore fails, keep in session cache with original ID as fallback
                sessionExpenses[newExpense.id] = newExpense
                val currentMonth = LocalDate.now().toString().substring(0, 7)
                val filteredSession = sessionExpenses.values.filter { it.date.startsWith(currentMonth) }
                val combinedList = (_uiState.value.expenses + filteredSession)
                    .distinctBy { it.id }
                    .sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.createdAt })
                updateStateFromData(
                    budget = _uiState.value.monthlyBudget,
                    expenses = combinedList,
                    daysLeft = _uiState.value.daysLeftInMonth,
                    userId = userId,
                    currentMonth = currentMonth
                )
                _uiState.update { it.copy(isBottomSheetOpen = false, errorMessage = "Failed to save: ${e.message}") }
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: ""
            try {
                expenseRepository.deleteExpense(expenseId)
                sessionExpenses.remove(expenseId)
                // Immediately update UI
                val remaining = _uiState.value.expenses.filterNot { it.id == expenseId }
                updateStateFromData(
                    budget = _uiState.value.monthlyBudget,
                    expenses = remaining,
                    daysLeft = _uiState.value.daysLeftInMonth,
                    userId = userId,
                    currentMonth = LocalDate.now().toString().substring(0, 7)
                )
            } catch (e: Exception) {
                sessionExpenses.remove(expenseId)
                val combinedList = _uiState.value.expenses.filterNot { it.id == expenseId }
                updateStateFromData(
                    budget = _uiState.value.monthlyBudget,
                    expenses = combinedList,
                    daysLeft = _uiState.value.daysLeftInMonth,
                    userId = userId,
                    currentMonth = LocalDate.now().toString().substring(0, 7)
                )
            } finally {
                _uiState.update { it.copy(expenseToDelete = null) }
            }
        }
    }

    fun updateBudgetLimit(newLimit: Double) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: ""
            val currentMonth = LocalDate.now().toString().substring(0, 7)
            val targetCurrency = com.savingcoach.app.utils.InvestmentCalculations.getTargetCurrency(
                currencyPreference, 
                isInvestment = false
            )
            val previousBudget = _uiState.value.monthlyBudget
            val currentBudget = previousBudget
            val updatedBudget = (currentBudget ?: Budget(userId = userId, month = currentMonth)).copy(
                limit = newLimit,
                currency = targetCurrency
            )

            // Optimistically set display budget
            val displayUpdatedBudget = updatedBudget.copy(limit = newLimit, currency = targetCurrency)
            _uiState.update { it.copy(monthlyBudget = displayUpdatedBudget, isEditBudgetDialogOpen = false) }

            try {
                if (currentBudget != null && currentBudget.id.isNotEmpty()) {
                    budgetRepository.setBudget(userId, updatedBudget)
                } else {
                    budgetRepository.setBudget(userId, updatedBudget)
                }
            } catch (e: Exception) {
                // Roll back to previous budget on failure
                _uiState.update { it.copy(monthlyBudget = previousBudget, errorMessage = "Failed to save budget: ${e.message}") }
            }
        }
    }

    fun addCustomCategory(emoji: String, name: String, target: Double) {
        val globalLimit = _uiState.value.monthlyBudget?.limit ?: 0.0
        val existingSum = _uiState.value.categories.sumOf { it.target }
        if (globalLimit == 0.0 && target > 0) {
            _uiState.update { it.copy(errorMessage = "Please set a Monthly Overall Budget before setting category targets.") }
            return
        }
        if (existingSum + target > globalLimit && globalLimit > 0) {
            val available = (globalLimit - existingSum).coerceAtLeast(0.0)
            _uiState.update { it.copy(errorMessage = "Category budget target cannot exceed remaining Global Budget capacity (${available.toLong()} MMK).") }
            return
        }
        val cleanName = extractCleanCategoryName(name)
        val cleanEmoji = if (emoji.isBlank()) "🏷️" else emoji.trim()

        if (cleanName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Category name cannot be empty.") }
            return
        }

        if (activeCategories.any { it.name.equals(cleanName, ignoreCase = true) }) {
            _uiState.update { it.copy(errorMessage = "Category \"$cleanName\" already exists.") }
            return
        }

        val newCategory = ExpenseCategory(
            emoji = cleanEmoji,
            name = cleanName,
            target = target,
            isCustom = true
        )
        activeCategories.add(newCategory)
        persistCategories()
        refreshCategoryState()
        setAddCategoryDialogVisible(false)
    }

    fun deleteCategory(categoryName: String) {
        val cleanName = extractCleanCategoryName(categoryName)
        // Snapshot for rollback
        val previousCategories = activeCategories.toList()
        val previousDeleted = deletedCategoryNames.toSet()

        activeCategories.removeAll { it.name.equals(cleanName, ignoreCase = true) }
        deletedCategoryNames.add(cleanName.lowercase())

        val currentSelected = _uiState.value.selectedCategory
        val newSelected = if (currentSelected?.equals(cleanName, ignoreCase = true) == true) null else currentSelected
        val currentFilter = _uiState.value.filterCategory
        val newFilter = if (currentFilter?.equals(cleanName, ignoreCase = true) == true) null else currentFilter
        _uiState.update { state ->
            state.copy(
                categories = activeCategories.map { cat ->
                    cat.copy(spent = calculateSpentForCategory(cat.name, state.categorySpending))
                },
                selectedCategory = newSelected,
                filterCategory = newFilter,
                filteredExpenses = filterExpenses(state.expenses, newFilter),
                categoryToEdit = null
            )
        }

        // Persist and rollback on failure
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: ""
                val currentMonth = LocalDate.now().toString().substring(0, 7)
                val entities = activeCategories.map { cat ->
                    ExpenseCategoryEntity(emoji = cat.emoji, name = cat.name, target = cat.target, isCustom = cat.isCustom)
                }
                expenseCategoryRepository.saveCategories(userId, currentMonth, entities, deletedCategoryNames)
            } catch (e: Exception) {
                // Rollback
                activeCategories.clear()
                activeCategories.addAll(previousCategories)
                deletedCategoryNames.clear()
                deletedCategoryNames.addAll(previousDeleted)
                refreshCategoryState()
                _uiState.update { it.copy(errorMessage = "Failed to delete category: ${e.message}") }
            }
        }
    }

    fun updateCategoryTarget(categoryName: String, newTarget: Double) {
        val globalLimit = _uiState.value.monthlyBudget?.limit ?: 0.0
        val cleanName = extractCleanCategoryName(categoryName)
        val otherSum = activeCategories.filterNot { it.name.equals(cleanName, ignoreCase = true) }.sumOf { it.target }
        if (globalLimit == 0.0 && newTarget > 0) {
            _uiState.update { it.copy(errorMessage = "Please set a Monthly Overall Budget before setting category targets.") }
            return
        }
        if (otherSum + newTarget > globalLimit && globalLimit > 0) {
            val available = (globalLimit - otherSum).coerceAtLeast(0.0)
            _uiState.update { it.copy(errorMessage = "Category budget target cannot exceed remaining Global Budget capacity (${available.toLong()} MMK).") }
            return
        }
        val index = activeCategories.indexOfFirst { it.name.equals(cleanName, ignoreCase = true) }
        if (index != -1) {
            activeCategories[index] = activeCategories[index].copy(target = newTarget)
        }
        persistCategories()
        refreshCategoryState()
    }

    private fun refreshCategoryState() {
        val categorySpendingMap = _uiState.value.categorySpending
        val updatedCategories = activeCategories.map { cat ->
            cat.copy(spent = calculateSpentForCategory(cat.name, categorySpendingMap))
        }
        _uiState.update { it.copy(categories = updatedCategories, categoryToEdit = null) }
    }

    private fun persistCategories() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: ""
                val currentMonth = LocalDate.now().toString().substring(0, 7)
                val entities = activeCategories.map { cat ->
                    ExpenseCategoryEntity(
                        emoji = cat.emoji,
                        name = cat.name,
                        target = cat.target,
                        isCustom = cat.isCustom
                    )
                }
                expenseCategoryRepository.saveCategories(
                    userId, currentMonth, entities, deletedCategoryNames
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save categories: ${e.message}") }
            }
        }
    }

    private fun mergeCategories(
        defaults: List<ExpenseCategory>,
        saved: List<ExpenseCategoryEntity>
    ): List<ExpenseCategory> {
        if (saved.isEmpty()) {
            return defaults.filterNot { deletedCategoryNames.contains(it.name.lowercase()) }.toMutableList()
        }

        val savedMap = saved.associateBy { it.name.lowercase() }
        val result = mutableListOf<ExpenseCategory>()

        for (default in defaults) {
            if (deletedCategoryNames.contains(default.name.lowercase())) continue

            val savedEntity = savedMap[default.name.lowercase()]
            if (savedEntity != null) {
                result.add(default.copy(target = savedEntity.target, emoji = savedEntity.emoji))
            } else {
                result.add(default)
            }
        }

        for (savedEntity in saved) {
            if (defaults.none { it.name.equals(savedEntity.name, ignoreCase = true) }) {
                result.add(
                    ExpenseCategory(
                        emoji = savedEntity.emoji,
                        name = savedEntity.name,
                        target = savedEntity.target,
                        isCustom = savedEntity.isCustom
                    )
                )
            }
        }

        return result
    }

    fun setCategoryToEdit(category: ExpenseCategory?) {
        _uiState.update { it.copy(categoryToEdit = category) }
    }

    fun setBottomSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isBottomSheetOpen = visible) }
    }

    fun setEditBudgetDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isEditBudgetDialogOpen = visible) }
    }

    fun setAddCategoryDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(isAddCategoryDialogOpen = visible) }
    }

    fun setExpenseToDelete(expense: Expense?) {
        _uiState.update { it.copy(expenseToDelete = expense) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
