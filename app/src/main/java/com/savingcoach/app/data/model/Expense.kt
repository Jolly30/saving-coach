package com.savingcoach.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val category: String = "Other",
    val merchant: String = "",
    val description: String = "",
    val date: String = "",          // YYYY-MM-DD format
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val source: String = "manual",  // manual, chat, receipt
    val currency: String = "MMK",
    val userId: String = ""
)
