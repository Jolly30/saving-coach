package com.savingcoach.app.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.savingcoach.app.data.model.Budget
import com.savingcoach.app.data.model.Expense
import com.savingcoach.app.data.repository.BudgetRepository
import com.savingcoach.app.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val auth: FirebaseAuth
) : ViewModel() {

    companion object {
        private val sessionExpenses = LinkedHashMap<String, Expense>()
        private val defaultCategories = mutableListOf(
            ExpenseCategory("🍔", "Food & Dining", 0.0),
            ExpenseCategory("🚗", "Transportation", 0.0),
            ExpenseCategory("🛍️", "Shopping", 0.0),
            ExpenseCategory("📱", "Bills & Utilities", 0.0),
            ExpenseCategory("🎬", "Entertainment", 0.0),
            ExpenseCategory("📚", "Education", 0.0),
            ExpenseCategory("💊", "Health", 0.0),
            ExpenseCategory("📦", "Other", 0.0)
        )
    }

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userId = auth.currentUser?.uid ?: ""
                val now = LocalDate.now()
                val currentMonth = now.toString().substring(0, 7) // "YYYY-MM"
                
                val lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth())
                val daysLeft = ChronoUnit.DAYS.between(now, lastDayOfMonth).toInt() + 1

                combine(
                    budgetRepository.getBudget(userId, currentMonth),
                    expenseRepository.getExpensesForMonth(userId, currentMonth)
                ) { budgetObj, expenseList ->
                    Pair(budgetObj, expenseList)
                }.collect { (budgetObj, expenseList) ->
                    val combinedList = (expenseList + sessionExpenses.values)
                        .distinctBy { it.id }
                        .sortedByDescending { if (it.createdAt > 0) it.createdAt else System.currentTimeMillis() }

                    val totalSpent = combinedList.sumOf { it.amount }

                    val categorySpendingMap = combinedList
                        .groupBy { extractCleanCategoryName(it.category) }
                        .mapValues { entry -> entry.value.sumOf { it.amount } }

                    val updatedCategories = defaultCategories.map { cat ->
                        val spentForCat = calculateSpentForCategory(cat.name, categorySpendingMap)
                        cat.copy(spent = spentForCat)
                    }

                    val selectedCat = _uiState.value.selectedCategory
                    _uiState.update { state ->
                        state.copy(
                            monthlyBudget = budgetObj ?: Budget(
                                userId = userId,
                                month = currentMonth,
                                limit = 1000000.0,
                                totalSpent = totalSpent
                            ),
                            totalSpent = totalSpent,
                            daysLeftInMonth = daysLeft,
                            categories = updatedCategories,
                            categorySpending = categorySpendingMap,
                            expenses = combinedList,
                            filteredExpenses = filterCategory(combinedList, selectedCat),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                // In case of error, still emit session cached expenses so user never loses data
                val combinedList = sessionExpenses.values
                    .sortedByDescending { if (it.createdAt > 0) it.createdAt else System.currentTimeMillis() }
                val totalSpent = combinedList.sumOf { it.amount }
                _uiState.update { 
                    it.copy(
                        expenses = combinedList,
                        filteredExpenses = filterCategory(combinedList, it.selectedCategory),
                        totalSpent = totalSpent,
                        isLoading = false,
                        errorMessage = e.message
                    ) 
                }
            }
        }
    }

    fun selectCategory(categoryName: String?) {
        val cleanName = categoryName?.let { extractCleanCategoryName(it) }
        val newSelection = if (cleanName == _uiState.value.selectedCategory) null else cleanName
        _uiState.update { state ->
            state.copy(
                selectedCategory = newSelection,
                filteredExpenses = filterCategory(state.expenses, newSelection)
            )
        }
    }

    private fun filterCategory(list: List<Expense>, categoryName: String?): List<Expense> {
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
        val cleaned = rawCategory.replace(Regex("^[\\p{So}\\p{Sk}\\p{Sm}\\p{Sc}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+\\s*"), "").trim()
        val finalName = if (cleaned.isNotEmpty()) cleaned else rawCategory.trim()
        return if (finalName.equals("& Dining", ignoreCase = true)) "Food & Dining" else finalName
    }

    private fun calculateSpentForCategory(catName: String, categorySpendingMap: Map<String, Double>): Double {
        val cleanCatName = extractCleanCategoryName(catName)
        return categorySpendingMap.entries.filter { (key, _) ->
            val cleanKey = extractCleanCategoryName(key)
            cleanKey.equals(cleanCatName, ignoreCase = true) ||
            (cleanCatName.equals("Food & Dining", ignoreCase = true) && cleanKey.contains("Dining", ignoreCase = true))
        }.sumOf { it.value }
    }

    fun addExpense(amount: Double, category: String, merchant: String, description: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: ""
            val todayStr = LocalDate.now().toString() // YYYY-MM-DD
            val cleanCategoryName = extractCleanCategoryName(category)

            val generatedId = UUID.randomUUID().toString()
            val newExpense = Expense(
                id = generatedId,
                userId = userId,
                amount = amount,
                category = cleanCategoryName, // Save category name only
                merchant = merchant,
                description = description,
                date = todayStr,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                source = "manual",
                currency = "MMK"
            )

            // 1. Save in session cache
            sessionExpenses[generatedId] = newExpense

            // 2. Immediately update UI State
            val combinedList = sessionExpenses.values
                .sortedByDescending { if (it.createdAt > 0) it.createdAt else System.currentTimeMillis() }

            val totalSpent = combinedList.sumOf { it.amount }
            val categorySpendingMap = combinedList.groupBy { extractCleanCategoryName(it.category) }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val updatedCategories = defaultCategories.map { cat ->
                val spentForCat = calculateSpentForCategory(cat.name, categorySpendingMap)
                cat.copy(spent = spentForCat)
            }

            val selectedCat = _uiState.value.selectedCategory
            _uiState.update { state ->
                state.copy(
                    expenses = combinedList,
                    filteredExpenses = filterCategory(combinedList, selectedCat),
                    totalSpent = totalSpent,
                    categories = updatedCategories,
                    categorySpending = categorySpendingMap,
                    isBottomSheetOpen = false
                )
            }

            // 3. Async save to repository
            try {
                expenseRepository.addExpense(newExpense)
            } catch (e: Exception) {
                // Session copy retains local entry
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            sessionExpenses.remove(expenseId)

            val combinedList = sessionExpenses.values
                .sortedByDescending { if (it.createdAt > 0) it.createdAt else System.currentTimeMillis() }
            val totalSpent = combinedList.sumOf { it.amount }
            val categorySpendingMap = combinedList.groupBy { extractCleanCategoryName(it.category) }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val updatedCategories = defaultCategories.map { cat ->
                val spent = calculateSpentForCategory(cat.name, categorySpendingMap)
                cat.copy(spent = spent)
            }

            val selectedCat = _uiState.value.selectedCategory
            _uiState.update { state ->
                state.copy(
                    expenses = combinedList,
                    filteredExpenses = filterCategory(combinedList, selectedCat),
                    totalSpent = totalSpent,
                    categories = updatedCategories,
                    categorySpending = categorySpendingMap,
                    expenseToDelete = null
                )
            }

            try {
                expenseRepository.deleteExpense(expenseId)
            } catch (e: Exception) {
                // Session deletion retained
            }
        }
    }

    fun updateBudgetLimit(newLimit: Double) {
        val currentCategoryTargetsSum = defaultCategories.sumOf { it.target }
        if (newLimit < currentCategoryTargetsSum && currentCategoryTargetsSum > 0.0) {
            _uiState.update { it.copy(errorMessage = "Global budget cannot be less than total assigned category targets (${currentCategoryTargetsSum.toLong()} MMK).") }
            return
        }
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: ""
            val currentMonth = LocalDate.now().toString().substring(0, 7)
            val currentBudget = _uiState.value.monthlyBudget
            val updatedBudget = (currentBudget ?: Budget(userId = userId, month = currentMonth)).copy(limit = newLimit)
            
            _uiState.update { it.copy(monthlyBudget = updatedBudget, isEditBudgetDialogOpen = false) }

            try {
                if (currentBudget != null && currentBudget.id.isNotEmpty()) {
                    budgetRepository.updateLimit(userId, currentMonth, newLimit)
                } else {
                    budgetRepository.setBudget(userId, updatedBudget)
                }
            } catch (e: Exception) {
                // Ignore sync errors
            }
        }
    }

    fun addCustomCategory(emoji: String, name: String, target: Double) {
        val globalLimit = _uiState.value.monthlyBudget?.limit ?: 0.0
        val existingSum = defaultCategories.sumOf { it.target }
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
        val newCategory = ExpenseCategory(
            emoji = cleanEmoji,
            name = cleanName,
            target = target
        )
        if (!defaultCategories.any { it.name.equals(cleanName, ignoreCase = true) }) {
            defaultCategories.add(newCategory)
            val updatedList = defaultCategories.map { cat ->
                val spent = _uiState.value.categorySpending[cat.name] ?: 0.0
                cat.copy(spent = spent)
            }
            _uiState.update { it.copy(categories = updatedList) }
        }
        setAddCategoryDialogVisible(false)
    }

    fun deleteCategory(categoryName: String) {
        val cleanName = extractCleanCategoryName(categoryName)
        defaultCategories.removeAll { it.name.equals(cleanName, ignoreCase = true) }
        val updatedCategories = defaultCategories.map { cat ->
            val spent = _uiState.value.categorySpending[cat.name] ?: 0.0
            cat.copy(spent = spent)
        }
        val currentSelected = _uiState.value.selectedCategory
        val newSelected = if (currentSelected?.equals(cleanName, ignoreCase = true) == true) null else currentSelected
        _uiState.update { state ->
            state.copy(
                categories = updatedCategories,
                selectedCategory = newSelected,
                filteredExpenses = filterCategory(state.expenses, newSelected),
                categoryToEdit = null
            )
        }
    }

    fun updateCategoryTarget(categoryName: String, newTarget: Double) {
        val globalLimit = _uiState.value.monthlyBudget?.limit ?: 0.0
        val cleanName = extractCleanCategoryName(categoryName)
        val otherSum = defaultCategories.filterNot { it.name.equals(cleanName, ignoreCase = true) }.sumOf { it.target }
        if (globalLimit == 0.0 && newTarget > 0) {
            _uiState.update { it.copy(errorMessage = "Please set a Monthly Overall Budget before setting category targets.") }
            return
        }
        if (otherSum + newTarget > globalLimit && globalLimit > 0) {
            val available = (globalLimit - otherSum).coerceAtLeast(0.0)
            _uiState.update { it.copy(errorMessage = "Category budget target cannot exceed remaining Global Budget capacity (${available.toLong()} MMK).") }
            return
        }
        val index = defaultCategories.indexOfFirst { it.name.equals(cleanName, ignoreCase = true) }
        if (index != -1) {
            defaultCategories[index] = defaultCategories[index].copy(target = newTarget)
        }
        val updatedCategories = defaultCategories.map { cat ->
            val spent = _uiState.value.categorySpending[cat.name] ?: 0.0
            cat.copy(spent = spent)
        }
        _uiState.update { it.copy(categories = updatedCategories, categoryToEdit = null) }
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
}
