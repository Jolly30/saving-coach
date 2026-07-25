package com.savingcoach.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Budget(
    val id: String = "",
    val userId: String = "",
    val limit: Double = 0.0,
    val totalSpent: Double = 0.0,
    val month: String = "",         // YYYY-MM format
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val remaining: Double get() = limit - totalSpent
    val spentPercentage: Double get() = if (limit > 0) (totalSpent / limit) * 100 else 0.0
}
