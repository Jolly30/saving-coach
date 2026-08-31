package com.savingcoach.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SavingsDeposit(
    val id: String = "",
    val challengeId: String = "",
    val amount: Double = 0.0,
    val date: String = "",           // YYYY-MM-DD
    val note: String = "",
    val createdAt: Long = 0L,
    val currency: String = "MMK"
)
