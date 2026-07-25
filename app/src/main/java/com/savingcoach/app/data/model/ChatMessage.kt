package com.savingcoach.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val role: String = "",           // "user" or "ai"
    val content: String = "",
    val timestamp: Long = 0L,
    val type: String = "query",      // expense, query, advice
    val parsedExpense: ParsedExpense? = null
)

@Serializable
data class ParsedExpense(
    val merchant: String = "",
    val amount: Double = 0.0,
    val category: String = "Other",
    val date: String = ""
)
