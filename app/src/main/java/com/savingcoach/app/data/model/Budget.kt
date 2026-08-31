package com.savingcoach.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    val id: String = "",
    val userId: String = "",
    val limit: Double = 0.0,
    val month: String = "",         // YYYY-MM format
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val categories: List<ExpenseCategoryEntity> = emptyList(),
    val deletedCategories: List<String> = emptyList(),
    val currency: String = "MMK"
)
