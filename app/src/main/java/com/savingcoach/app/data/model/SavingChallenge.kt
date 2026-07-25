package com.savingcoach.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SavingChallenge(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val startDate: String = "",      // YYYY-MM-DD
    val endDate: String = "",        // YYYY-MM-DD
    val isActive: Boolean = true,
    val isCompleted: Boolean = false,
    val createdAt: Long = 0L
) {
    val progress: Double get() = if (targetAmount > 0) (currentAmount / targetAmount) * 100 else 0.0
    val remaining: Double get() = (targetAmount - currentAmount).coerceAtLeast(0.0)
}
