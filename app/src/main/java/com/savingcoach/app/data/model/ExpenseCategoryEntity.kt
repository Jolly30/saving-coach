package com.savingcoach.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ExpenseCategoryEntity(
    val emoji: String = "🏷️",
    val name: String = "",
    val target: Double = 0.0,
    val isCustom: Boolean = false
)
