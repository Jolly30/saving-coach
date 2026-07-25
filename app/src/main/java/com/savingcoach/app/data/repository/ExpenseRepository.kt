package com.savingcoach.app.data.repository

import com.savingcoach.app.data.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getExpensesForMonth(userId: String, yearMonth: String): Flow<List<Expense>>
    fun getExpensesForDate(userId: String, date: String): Flow<List<Expense>>
    fun getAllExpenses(userId: String): Flow<List<Expense>>
    suspend fun addExpense(expense: Expense): String   // returns expenseId
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expenseId: String)
}
