package com.savingcoach.app.ui.expenses

import com.savingcoach.app.data.model.Budget
import com.savingcoach.app.data.model.Expense

data class ExpenseUiState(
    // Monthly budget
    val monthlyBudget: Budget? = null,
    val totalSpent: Double = 0.0,
    val daysLeftInMonth: Int = 0,

    // Categories / Spending Buckets
    val categories: List<ExpenseCategory> = emptyList(),
    val categorySpending: Map<String, Double> = emptyMap(),
    val selectedCategory: String? = null,  // null = show all

    // Expenses
    val expenses: List<Expense> = emptyList(),
    val filteredExpenses: List<Expense> = emptyList(),

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
)
