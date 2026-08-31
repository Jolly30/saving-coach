package com.savingcoach.app.data.model

import kotlinx.serialization.Serializable
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Exclude

enum class ChallengeTemplate { CONSTANT, FLEXI, ENVELOPE, NO_SPEND }
enum class ChallengeStatus { ACTIVE, COMPLETED, FAILED, STOPPED }

@Serializable
data class SavingChallenge(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val startDate: String = "",      // YYYY-MM-DD
    val endDate: String = "",        // YYYY-MM-DD
    
    @get:PropertyName("isActive")
    val isActive: Boolean = true,
    
    @get:PropertyName("isCompleted")
    val isCompleted: Boolean = false,
    
    val createdAt: Long = 0L,
    val template: ChallengeTemplate = ChallengeTemplate.CONSTANT,
    val lastDepositDate: String = "",
    val currency: String = "MMK",
    
    // Backwards compatibility fields for Firestore
    @get:PropertyName("active")
    val active: Boolean? = null,
    
    @get:PropertyName("completed")
    val completed: Boolean? = null
) {
    val progress: Double get() = if (targetAmount > 0) (currentAmount / targetAmount) * 100 else 0.0
    val remaining: Double get() = (targetAmount - currentAmount).coerceAtLeast(0.0)
    
    val isActualActive: Boolean
        @Exclude
        get() = active ?: isActive

    val isActualCompleted: Boolean
        @Exclude
        get() {
            val baseCompleted = completed ?: isCompleted
            if (!baseCompleted) return false
            return when (template) {
                ChallengeTemplate.NO_SPEND -> true
                else -> currentAmount >= targetAmount
            }
        }

    val durationDays: Int
        @Exclude
        get() {
            val parts = lastDepositDate.split("|")
            return if (parts.size > 2) parts[2].toIntOrNull() ?: 30 else {
                try {
                    val start = java.time.LocalDate.parse(startDate)
                    val end = java.time.LocalDate.parse(endDate)
                    java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(1)
                } catch (e: Exception) { 30 }
            }
        }

    val completedDaysCount: Int
        @Exclude
        get() {
            val parts = lastDepositDate.split("|")
            return if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
        }

    val status: ChallengeStatus
        @Exclude
        get() {
            val today = java.time.LocalDate.now()
            val endDateParsed = try { java.time.LocalDate.parse(endDate) } catch (e: Exception) { null }
            val hasTimelineEnded = endDateParsed != null && today.isAfter(endDateParsed)
            val isOutOfDays = completedDaysCount >= durationDays || hasTimelineEnded

            return when {
                isActualCompleted -> ChallengeStatus.COMPLETED
                isOutOfDays -> {
                    if (template == ChallengeTemplate.NO_SPEND) {
                        ChallengeStatus.COMPLETED
                    } else if (currentAmount >= targetAmount) {
                        ChallengeStatus.COMPLETED
                    } else {
                        ChallengeStatus.FAILED
                    }
                }
                isActualActive -> ChallengeStatus.ACTIVE
                else -> ChallengeStatus.STOPPED
            }
        }
}

