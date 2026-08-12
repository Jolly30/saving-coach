package com.savingcoach.app.ui.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.ChallengeTemplate
import com.savingcoach.app.data.model.SavingsDeposit
import com.savingcoach.app.data.repository.AuthRepository
import com.savingcoach.app.data.repository.SavingChallengeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

val defaultPresets = listOf(
    SavingChallenge(
        id = "preset_1",
        title = "🎯 1K a Day",
        targetAmount = 30000.0,
        currentAmount = 0.0,
        startDate = LocalDate.now().toString(),
        endDate = LocalDate.now().plusDays(30).toString(),
        isActive = true,
        isCompleted = false,
        template = ChallengeTemplate.CONSTANT,
        lastDepositDate = "|0|30"
    ),
    SavingChallenge(
        id = "preset_2",
        title = "✉️ 100 Envelopes",
        targetAmount = 505000.0,
        currentAmount = 0.0,
        startDate = LocalDate.now().toString(),
        endDate = LocalDate.now().plusDays(100).toString(),
        isActive = true,
        isCompleted = false,
        template = ChallengeTemplate.ENVELOPE,
        lastDepositDate = "|0|100"
    ),
    SavingChallenge(
        id = "preset_3",
        title = "⚡ 7-Day Sprint",
        targetAmount = 70000.0,
        currentAmount = 0.0,
        startDate = LocalDate.now().toString(),
        endDate = LocalDate.now().plusDays(7).toString(),
        isActive = true,
        isCompleted = false,
        template = ChallengeTemplate.FLEXI,
        lastDepositDate = "|0|7"
    ),
    SavingChallenge(
        id = "preset_4",
        title = "🚫 No-Spend Week",
        targetAmount = 0.0,
        currentAmount = 0.0,
        startDate = LocalDate.now().toString(),
        endDate = LocalDate.now().plusDays(7).toString(),
        isActive = true,
        isCompleted = false,
        template = ChallengeTemplate.NO_SPEND,
        lastDepositDate = "|0|7"
    )
)

data class ChallengesUiState(
    val challengesList: List<SavingChallenge> = emptyList(),
    val totalSaved: Double = 0.0,
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val selectedChallengeDeposits: List<SavingsDeposit> = emptyList()
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeViewModel @Inject constructor(
    private val repository: SavingChallengeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val userId = authRepository.getCurrentUserId() ?: ""
    private val selectedChallengeId = MutableStateFlow<String?>(null)
    private val completedChallengeIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            repository.initializeDefaultChallengesIfNeeded(userId, defaultPresets)
        }
    }

    val uiState: StateFlow<ChallengesUiState> = combine(
        repository.getAllChallenges(userId),
        selectedChallengeId.flatMapLatest { id ->
            if (id != null) {
                repository.getDeposits(userId, id)
            } else {
                flowOf(emptyList())
            }
        }
    ) { challenges, deposits ->
        val patchedChallenges = challenges.map { challenge ->
            if (!challenge.isCompleted) {
                val percent = if (challenge.template == ChallengeTemplate.NO_SPEND || challenge.targetAmount == 0.0) {
                    if (challenge.durationDays > 0) {
                        ((challenge.completedDaysCount.toFloat() / challenge.durationDays.toFloat()) * 100).toInt()
                    } else 0
                } else {
                    if (challenge.targetAmount > 0) {
                        ((challenge.currentAmount / challenge.targetAmount) * 100).toInt()
                    } else 0
                }
                
                if (percent >= 100 || (challenge.targetAmount == 0.0 && challenge.completedDaysCount >= challenge.durationDays)) {
                    // Sync to Firestore in background (only once per challenge)
                    if (challenge.id !in completedChallengeIds) {
                        completedChallengeIds.add(challenge.id)
                        viewModelScope.launch {
                            repository.completeChallenge(userId, challenge.id)
                        }
                    }
                    // Patch locally instantly
                    challenge.copy(isCompleted = true, isActive = false)
                } else {
                    challenge
                }
            } else {
                challenge
            }
        }
        
        val sortedChallenges = patchedChallenges.sortedWith(
            compareBy<SavingChallenge> { it.isCompleted }.thenByDescending { it.createdAt }
        )
    
        ChallengesUiState(
            challengesList = sortedChallenges,
            totalSaved = patchedChallenges.sumOf { it.currentAmount },
            activeCount = patchedChallenges.count { it.isActive && !it.isCompleted },
            completedCount = patchedChallenges.count { it.isCompleted },
            selectedChallengeDeposits = deposits
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChallengesUiState()
    )

    fun selectChallenge(id: String?) {
        selectedChallengeId.value = id
    }

    fun createChallenge(challenge: SavingChallenge) {
        viewModelScope.launch {
            val finalChallenge = challenge.copy(userId = userId)
            repository.createChallenge(finalChallenge)
        }
    }

    fun createChallenge(name: String, emoji: String, target: Double, duration: Long, template: ChallengeTemplate = ChallengeTemplate.CONSTANT) {
        viewModelScope.launch {
            val challenge = SavingChallenge(
                id = java.util.UUID.randomUUID().toString(),
                userId = userId,
                title = "$emoji $name".trim(),
                targetAmount = target,
                currentAmount = 0.0,
                startDate = LocalDate.now().toString(),
                endDate = LocalDate.now().plusDays(duration).toString(),
                isActive = true,
                isCompleted = false,
                createdAt = System.currentTimeMillis(),
                template = template,
                lastDepositDate = "|0|$duration"
            )
            repository.createChallenge(challenge)
        }
    }

    fun addDeposit(challengeId: String, amount: Double) {
        viewModelScope.launch {
            val deposit = SavingsDeposit(
                id = java.util.UUID.randomUUID().toString(),
                challengeId = challengeId,
                amount = amount,
                date = LocalDate.now().toString(),
                note = "Deposit",
                createdAt = System.currentTimeMillis()
            )
            repository.addDeposit(userId, challengeId, deposit)
        }
    }

    fun addDepositMock(challengeId: String, amount: Double, completedSteps: Int, daysDecremented: Int = 1, note: String = "Deposit") {
        val challenge = uiState.value.challengesList.find { it.id == challengeId } ?: return
        val newAmount = challenge.currentAmount + amount
        val newEndDate = if (daysDecremented > 0) {
            try {
                val parsed = LocalDate.parse(challenge.endDate)
                parsed.minusDays(daysDecremented.toLong()).toString()
            } catch (e: Exception) { challenge.endDate }
        } else challenge.endDate
        
        val parts = challenge.lastDepositDate.split("|")
        val duration = if (parts.size > 2) parts[2] else "30"
        
        val isNowCompleted = when (challenge.template) {
            com.savingcoach.app.data.model.ChallengeTemplate.FLEXI -> newAmount >= challenge.targetAmount
            com.savingcoach.app.data.model.ChallengeTemplate.CONSTANT,
            com.savingcoach.app.data.model.ChallengeTemplate.ENVELOPE,
            com.savingcoach.app.data.model.ChallengeTemplate.NO_SPEND -> completedSteps >= (duration.toIntOrNull() ?: 30)
        }

        val updatedChallenge = challenge.copy(
            endDate = newEndDate,
            lastDepositDate = LocalDate.now().toString() + "|" + completedSteps + "|" + duration,
            isCompleted = challenge.isCompleted || isNowCompleted,
            isActive = challenge.isActive && !isNowCompleted
        )
        
        viewModelScope.launch {
            try {
                // Update challenge state first
                repository.createChallenge(updatedChallenge)

                // Then add the deposit
                val deposit = SavingsDeposit(
                    id = java.util.UUID.randomUUID().toString(),
                    challengeId = challengeId,
                    amount = amount,
                    date = java.time.LocalDate.now().toString(),
                    note = note,
                    createdAt = System.currentTimeMillis()
                )
                repository.addDeposit(userId, challengeId, deposit)
            } catch (e: Exception) {
                // If deposit fails, revert the challenge update
                repository.createChallenge(challenge)
            }
        }
    }

    fun completeChallenge(challengeId: String) {
        viewModelScope.launch {
            repository.completeChallenge(userId, challengeId)
        }
    }

    fun updateChallengeMock(updated: SavingChallenge) {
        viewModelScope.launch {
            repository.createChallenge(updated)
        }
    }

    fun deleteChallenge(challengeId: String) {
        viewModelScope.launch {
            repository.deleteChallenge(userId, challengeId)
            if (selectedChallengeId.value == challengeId) {
                selectedChallengeId.value = null
            }
        }
    }
}
