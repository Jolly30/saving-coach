package com.savingcoach.app.ui.expenses

import com.savingcoach.app.data.model.Budget
import com.savingcoach.app.data.model.Expense

data class ExpenseUiState(
    // Monthly budget
    val monthlyBudget: Budget? = null,
    val totalSpent: Double = 0.0,
    val daysLeftInMonth: Int = 0,
    val currencyPreference: String = "MMK",
    val usdRate: Double = 1.0,


    // Categories / Spending Buckets
    val categories: List<ExpenseCategory> = emptyList(),
    val categorySpending: Map<String, Double> = emptyMap(),
    val selectedCategory: String? = null,  // null = show all

    // Expenses
    val expenses: List<Expense> = emptyList(),
    val filteredExpenses: List<Expense> = emptyList(),
    val filterCategory: String? = null,  // filter dropdown selection (separate from selectedCategory)

    // UI state
    val isBottomSheetOpen: Boolean = false,
    val isEditBudgetDialogOpen: Boolean = false,
    val isAddCategoryDialogOpen: Boolean = false,
    val categoryToEdit: ExpenseCategory? = null,
    val expenseToDelete: Expense? = null,

    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class ExpenseCategory(
    val emoji: String,
    val name: String,
    val target: Double,        // category budget limit in MMK
    val spent: Double = 0.0,   // computed from expenses
    val isCustom: Boolean = false
) {
    companion object {
        val DEFAULT_CATEGORIES = listOf(
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
}
