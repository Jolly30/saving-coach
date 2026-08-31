package com.savingcoach.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val role: String = "",           // "user" or "ai"
    val content: String = "",
    val timestamp: Long = 0L,
    val type: String = "query",      // expense, query, advice
    val parsedExpense: ParsedExpense? = null,
    val parsedExpenses: List<ParsedExpense>? = null,
    val savedExpenseIndices: List<Int> = emptyList(),
    val cancelledExpenseIndices: List<Int> = emptyList(),
    val expenseSaved: Boolean = false,
    val expenseCancelled: Boolean = false
)

@Serializable
data class ParsedExpense(
    val merchant: String = "",
    val amount: Double = 0.0,
    val category: String = "Other",
    val date: String = "",
    val language: String = "en",
    val isChallenge: Boolean = false,
    @SerialName("challenge_title")
    val challengeTitle: String = "",
    val action: String = "log_expense",
    val item: String = "",
    val currency: String = "MMK",
    val choices: List<String> = emptyList(),
    val topic: String = ""
)

