package com.savingcoach.app.data.model

import com.google.firebase.firestore.PropertyName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationItem(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "", // BUDGET_BREACH, SEVERE_INACTIVITY, ABANDONED_CHALLENGE, PORTFOLIO_RISK, DAILY_REMINDER
    val timestamp: Long = 0L,
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false
)
