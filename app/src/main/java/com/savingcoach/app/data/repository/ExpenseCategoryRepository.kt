package com.savingcoach.app.data.repository

import com.savingcoach.app.data.model.ExpenseCategoryEntity
import kotlinx.coroutines.flow.Flow

interface ExpenseCategoryRepository {
    fun getCategories(userId: String, yearMonth: String): Flow<List<ExpenseCategoryEntity>>
    fun getDeletedCategoryNames(userId: String, yearMonth: String): Flow<Set<String>>
    suspend fun saveCategories(
        userId: String,
        yearMonth: String,
        categories: List<ExpenseCategoryEntity>,
        deletedNames: Set<String> = emptySet()
    )
}
