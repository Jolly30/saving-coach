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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

val defaultPresets = listOf(
    SavingChallenge(
        id = "preset_1",
        title = "🎯 1K a Day",
        targetAmount = 30000.0,
        currentAmount = 18000.0,
        startDate = LocalDate.now().minusDays(18).toString(),
        endDate = LocalDate.now().plusDays(12).toString(),
        isActive = true,
        isCompleted = false,
        template = ChallengeTemplate.CONSTANT,
        lastDepositDate = "|18|30"
    ),
    SavingChallenge(
        id = "preset_2",
        title = "✉️ 100 Envelopes",
        targetAmount = 505000.0,
        currentAmount = 142500.0,
        startDate = LocalDate.now().minusDays(2).toString(),
        endDate = LocalDate.now().plusDays(58).toString(),
        isActive = true,
        isCompleted = false,
        template = ChallengeTemplate.ENVELOPE,
        lastDepositDate = "|34|100"
    ),
    SavingChallenge(
        id = "preset_3",
        title = "⚡ 7-Day Sprint",
        targetAmount = 70000.0,
        currentAmount = 50000.0,
        startDate = LocalDate.now().minusDays(5).toString(),
        endDate = LocalDate.now().plusDays(2).toString(),
        isActive = true,
        isCompleted = false,
        template = ChallengeTemplate.FLEXI,
        lastDepositDate = "|5|7"
    ),
    SavingChallenge(
        id = "preset_4",
        title = "🚫 No-Spend Week",
        targetAmount = 50000.0,
        currentAmount = 50000.0,
        startDate = LocalDate.now().minusDays(7).toString(),
        endDate = LocalDate.now().minusDays(1).toString(),
        isActive = false,
        isCompleted = true,
        template = ChallengeTemplate.NO_SPEND,
        lastDepositDate = "|4|7"
    )
)

data class ChallengesUiState(
    val challengesList: List<SavingChallenge> = defaultPresets,
    val totalSaved: Double = defaultPresets.sumOf { it.currentAmount },
    val activeCount: Int = defaultPresets.count { it.isActive && !it.isCompleted },
    val completedCount: Int = defaultPresets.count { it.isCompleted },
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

    private val _mockChallenges = MutableStateFlow(defaultPresets)

    init {
        viewModelScope.launch {
            repository.getAllChallenges(userId).collect { dbChallenges ->
                val dbPresets = dbChallenges.filter { it.id.startsWith("preset_") }
                val currentMock = _mockChallenges.value
                val updatedMock = currentMock.map { mockChan ->
                    dbPresets.find { it.id == mockChan.id } ?: mockChan
                }
                _mockChallenges.value = updatedMock
            }
        }
    }

    val uiState: StateFlow<ChallengesUiState> = combine(
        repository.getAllChallenges(userId),
        _mockChallenges,
        selectedChallengeId.flatMapLatest { id ->
            if (id != null && !id.startsWith("preset_")) {
                repository.getDeposits(userId, id)
            } else {
                flowOf(emptyList())
            }
        }
    ) { challenges, mockChallenges, deposits ->
        val customChallenges = challenges.filter { dbChallenge ->
            dbChallenge.id != "preset_1" &&
            dbChallenge.id != "preset_2" &&
            dbChallenge.id != "preset_3" &&
            dbChallenge.id != "preset_4" &&
            mockChallenges.none { it.id == dbChallenge.id }
        }
        val displayList = mockChallenges + customChallenges
        ChallengesUiState(
            challengesList = displayList,
            totalSaved = displayList.sumOf { it.currentAmount },
            activeCount = displayList.count { it.isActive && !it.isCompleted },
            completedCount = displayList.count { it.isCompleted },
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

    fun activatePresetAndSelect(presetId: String) {
        val preset = defaultPresets.find { it.id == presetId } ?: return
        val newId = UUID.randomUUID().toString()
        val realChallenge = preset.copy(
            id = newId,
            userId = userId,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.createChallenge(realChallenge)
            selectedChallengeId.value = newId
        }
    }

    fun createChallenge(challenge: SavingChallenge) {
        viewModelScope.launch {
            val finalChallenge = challenge.copy(userId = userId)
            repository.createChallenge(finalChallenge)
            _mockChallenges.value = _mockChallenges.value + finalChallenge
        }
    }

    fun createChallenge(name: String, emoji: String, target: Double, duration: Long) {
        viewModelScope.launch {
            val challenge = SavingChallenge(
                id = UUID.randomUUID().toString(),
                userId = userId,
                title = "$emoji $name".trim(),
                targetAmount = target,
                currentAmount = 0.0,
                startDate = LocalDate.now().toString(),
                endDate = LocalDate.now().plusDays(duration).toString(),
                isActive = true,
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            )
            repository.createChallenge(challenge)
        }
    }

    fun addDeposit(challengeId: String, amount: Double) {
        viewModelScope.launch {
            val deposit = SavingsDeposit(
                id = UUID.randomUUID().toString(),
                challengeId = challengeId,
                amount = amount,
                date = LocalDate.now().toString(),
                note = "Deposit",
                createdAt = System.currentTimeMillis()
            )
            repository.addDeposit(userId, challengeId, deposit)
        }
    }

    fun addDepositMock(challengeId: String, amount: Double, completedSteps: Int, daysDecremented: Int = 1) {
        val currentList = _mockChallenges.value
        val hasChallenge = currentList.any { it.id == challengeId }
        
        val listToMap = if (hasChallenge) {
            currentList
        } else {
            val customChan = uiState.value.challengesList.find { it.id == challengeId }
            if (customChan != null) {
                currentList + customChan
            } else {
                currentList
            }
        }
        
        val updatedList = listToMap.map { challenge ->
            if (challenge.id == challengeId) {
                val newAmount = challenge.currentAmount + amount
                val newEndDate = if (daysDecremented > 0) {
                    try {
                        val parsed = LocalDate.parse(challenge.endDate)
                        parsed.minusDays(daysDecremented.toLong()).toString()
                    } catch (e: Exception) { challenge.endDate }
                } else challenge.endDate
                
                val parts = challenge.lastDepositDate.split("|")
                val duration = if (parts.size > 2) parts[2] else "30"
                
                val updatedChallenge = challenge.copy(
                    currentAmount = newAmount,
                    endDate = newEndDate,
                    lastDepositDate = LocalDate.now().toString() + "|" + completedSteps + "|" + duration
                )
                
                viewModelScope.launch {
                    repository.createChallenge(updatedChallenge)
                }
                
                updatedChallenge
            } else challenge
        }
        _mockChallenges.value = updatedList
    }

    fun completeChallenge(challengeId: String) {
        viewModelScope.launch {
            repository.completeChallenge(userId, challengeId)
        }
    }

    fun updateChallengeMock(updated: SavingChallenge) {
        val currentList = _mockChallenges.value
        val hasChallenge = currentList.any { it.id == updated.id }
        val updatedList = if (hasChallenge) {
            currentList.map { challenge ->
                if (challenge.id == updated.id) updated else challenge
            }
        } else {
            currentList + updated
        }
        _mockChallenges.value = updatedList
        
        viewModelScope.launch {
            repository.createChallenge(updated)
        }
    }

    fun deleteChallenge(challengeId: String) {
        viewModelScope.launch {
            repository.deleteChallenge(userId, challengeId)
            _mockChallenges.value = _mockChallenges.value.filter { it.id != challengeId }
            if (selectedChallengeId.value == challengeId) {
                selectedChallengeId.value = null
            }
        }
    }
}
