package com.savingcoach.app.data.repository

import com.savingcoach.app.data.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudget(userId: String, yearMonth: String): Flow<Budget?>
    suspend fun setBudget(userId: String, budget: Budget)
    suspend fun updateLimit(userId: String, yearMonth: String, newLimit: Double)
}
