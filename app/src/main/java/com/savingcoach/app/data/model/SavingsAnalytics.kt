package com.savingcoach.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SavingsAnalytics(
    val totalSaved: Double = 0.0,
    val totalTarget: Double = 0.0,
    val activeChallenges: Int = 0,
    val completedChallenges: Int = 0,
    val monthlySavings: Map<String, Double> = emptyMap(),  // YYYY-MM -> amount
    val categoryBreakdown: Map<String, Double> = emptyMap()
)
