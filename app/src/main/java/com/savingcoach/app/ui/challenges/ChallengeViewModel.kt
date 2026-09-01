package com.savingcoach.app.ui.challenges

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.ChallengeTemplate
import com.savingcoach.app.data.model.ChallengeStatus
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
import com.savingcoach.app.data.repository.UserRepository
import com.savingcoach.app.data.repository.ExchangeRateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
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
    val failedCount: Int = 0,
    val stoppedCount: Int = 0,
    val selectedChallengeDeposits: List<SavingsDeposit> = emptyList(),
    val currencyPreference: String = "MMK",
    val usdRate: Double = 1.0
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ChallengeViewModel @Inject constructor(
    private val repository: SavingChallengeRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val notificationHelper: com.savingcoach.app.core.notification.NotificationHelper
) : ViewModel() {

    private val userId = authRepository.getCurrentUserId() ?: ""
    private val selectedChallengeId = MutableStateFlow<String?>(null)
    private val completedChallengeIds = mutableSetOf<String>()
    private var currencyPreference = "MMK"
    private var usdRate = 1.0

    init {
        reconcileAllActiveChallenges()
    }

    fun reconcileAllActiveChallenges() {
        viewModelScope.launch {
            try {
                val challenges = repository.getAllChallenges(userId).first()
                for (challenge in challenges) {
                    if (challenge.isActive && !challenge.isCompleted) {
                        autoSkipMissedDays(challenge.id)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ChallengeViewModel", "Failed to reconcile active challenges", e)
            }
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
        },
        userRepository.getUserProfileFlow(userId).catch { emit(null) },
        exchangeRateRepository.usdToMmkRate
    ) { challenges, deposits, userProfile, usdRateVal ->
        val currencyPref = userProfile?.currencyPreference ?: "MMK"
        this.currencyPreference = currencyPref
        this.usdRate = usdRateVal

        val targetCurrency = com.savingcoach.app.utils.InvestmentCalculations.getTargetCurrency(
            currencyPref, 
            isInvestment = false
        )

        // Collect state updates that need to be applied after the map
        data class StateUpdate(val challengeId: String, val updates: SavingChallenge)
        val stateUpdates = mutableListOf<StateUpdate>()

        val patchedChallenges = challenges.map { challenge ->
            var isNowCompleted = challenge.isCompleted
            var isNowActive = challenge.isActive

            val today = LocalDate.now()
            val endDateParsed = try { LocalDate.parse(challenge.endDate) } catch (e: Exception) { null }
            val hasTimelineEnded = endDateParsed != null && today.isAfter(endDateParsed)

            if (!challenge.isCompleted && challenge.isActive) {
                val isActuallyCompleted = when (challenge.template) {
                    ChallengeTemplate.FLEXI -> {
                        challenge.targetAmount > 0 && challenge.currentAmount >= challenge.targetAmount
                    }
                    else -> {
                        challenge.durationDays > 0 && challenge.completedDaysCount >= challenge.durationDays
                    }
                }

                if (isActuallyCompleted) {
                    if (challenge.id !in completedChallengeIds) {
                        completedChallengeIds.add(challenge.id)
                        // Queue update to be applied after the map completes
                        stateUpdates.add(StateUpdate(challenge.id, challenge.copy(isCompleted = true, isActive = false)))
                    }
                    isNowCompleted = true
                    isNowActive = false
                } else if (hasTimelineEnded || challenge.completedDaysCount >= challenge.durationDays) {
                    isNowActive = false
                    stateUpdates.add(StateUpdate(challenge.id, challenge.copy(isActive = false, isCompleted = false)))
                }
            }

            // Convert target and current to active display currency for UI state
            val displayTarget = com.savingcoach.app.utils.InvestmentCalculations.convertAmount(
                amount = challenge.targetAmount,
                fromCurrency = challenge.currency,
                toCurrency = targetCurrency,
                usdRate = usdRateVal
            )

            val selId = selectedChallengeId.value
            val displayCurrent = if (challenge.id == selId) {
                val totalDepositInChallengeCurr = deposits.sumOf { 
                    com.savingcoach.app.utils.InvestmentCalculations.convertAmount(
                        amount = it.amount,
                        fromCurrency = it.currency,
                        toCurrency = challenge.currency,
                        usdRate = usdRateVal
                    )
                }
                // Queue update if currentAmount differs from deposit sum
                if (challenge.currentAmount != totalDepositInChallengeCurr) {
                    stateUpdates.add(StateUpdate(challenge.id, challenge.copy(currentAmount = totalDepositInChallengeCurr)))
                }
                deposits.sumOf { 
                    com.savingcoach.app.utils.InvestmentCalculations.convertAmount(
                        amount = it.amount,
                        fromCurrency = it.currency,
                        toCurrency = targetCurrency,
                        usdRate = usdRateVal
                    )
                }
            } else {
                com.savingcoach.app.utils.InvestmentCalculations.convertAmount(
                    amount = challenge.currentAmount,
                    fromCurrency = challenge.currency,
                    toCurrency = targetCurrency,
                    usdRate = usdRateVal
                )
            }

            challenge.copy(
                targetAmount = displayTarget,
                currentAmount = displayCurrent,
                currency = targetCurrency,
                isCompleted = isNowCompleted,
                isActive = isNowActive
            )
        }

        // Apply queued state updates after the map completes
        if (stateUpdates.isNotEmpty()) {
            viewModelScope.launch {
                for (update in stateUpdates) {
                    try {
                        repository.createChallenge(update.updates)
                    } catch (e: Exception) {
                        android.util.Log.e("ChallengeViewModel", "Failed to update challenge ${update.challengeId}", e)
                    }
                }
            }
        }

        val sortedChallenges = patchedChallenges.sortedWith(
            compareBy<SavingChallenge> { it.isCompleted }.thenByDescending { it.createdAt }
        )

        val displayDeposits = deposits.map { deposit ->
            val convertedAmount = com.savingcoach.app.utils.InvestmentCalculations.convertAmount(
                amount = deposit.amount,
                fromCurrency = deposit.currency,
                toCurrency = targetCurrency,
                usdRate = usdRateVal
            )
            deposit.copy(
                amount = convertedAmount,
                currency = targetCurrency
            )
        }

        ChallengesUiState(
            challengesList = sortedChallenges,
            totalSaved = patchedChallenges.sumOf { it.currentAmount },
            activeCount = patchedChallenges.count { it.status == ChallengeStatus.ACTIVE },
            completedCount = patchedChallenges.count { it.status == ChallengeStatus.COMPLETED },
            failedCount = patchedChallenges.count { it.status == ChallengeStatus.FAILED },
            stoppedCount = patchedChallenges.count { it.status == ChallengeStatus.STOPPED },
            selectedChallengeDeposits = displayDeposits,
            currencyPreference = currencyPref,
            usdRate = usdRateVal
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
            val targetCurrency = com.savingcoach.app.utils.InvestmentCalculations.getTargetCurrency(
                currencyPreference, 
                isInvestment = false
            )
            val toSave = if (challenge.currency.isEmpty()) challenge.copy(currency = targetCurrency, userId = userId) else challenge.copy(userId = userId)
            repository.createChallenge(toSave)
        }
    }

    fun createChallenge(name: String, emoji: String, target: Double, duration: Long, template: ChallengeTemplate = ChallengeTemplate.CONSTANT) {
        viewModelScope.launch {
            val targetCurrency = com.savingcoach.app.utils.InvestmentCalculations.getTargetCurrency(
                currencyPreference, 
                isInvestment = false
            )
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
                lastDepositDate = "|0|$duration",
                currency = targetCurrency
            )
            repository.createChallenge(challenge)
        }
    }

    fun addDeposit(challengeId: String, amount: Double) {
        viewModelScope.launch {
            val targetCurrency = com.savingcoach.app.utils.InvestmentCalculations.getTargetCurrency(
                currencyPreference, 
                isInvestment = false
            )
            val deposit = SavingsDeposit(
                id = java.util.UUID.randomUUID().toString(),
                challengeId = challengeId,
                amount = amount,
                date = LocalDate.now().toString(),
                note = "Deposit",
                createdAt = System.currentTimeMillis(),
                currency = targetCurrency
            )
            repository.addDeposit(userId, challengeId, deposit)
        }
    }

    fun addDepositMock(challengeId: String, amount: Double, completedStepsParam: Int, daysDecremented: Int = 1, note: String = "Deposit") {
        val targetCurrency = com.savingcoach.app.utils.InvestmentCalculations.getTargetCurrency(
            currencyPreference, 
            isInvestment = false
        )

        viewModelScope.launch {
            try {
                // Fetch the original raw challenge from repository
                val challenges = repository.getAllChallenges(userId).first()
                val challenge = challenges.find { it.id == challengeId } ?: return@launch

                val parts = challenge.lastDepositDate.split("|")
                val currentCompletedSteps = if (parts.size > 1) (parts[1].toIntOrNull() ?: 0) else 0
                val lastDepositDateOnly = if (parts.isNotEmpty()) parts[0] else ""
                val todayStr = LocalDate.now().toString()
                if (currentCompletedSteps > 0 && lastDepositDateOnly == todayStr) {
                    return@launch
                }

                // Increment completed steps BEFORE checking completion
                val newCompletedSteps = currentCompletedSteps + 1
                val amountInChallengeCurrency = com.savingcoach.app.utils.InvestmentCalculations.convertAmount(
                    amount = amount,
                    fromCurrency = targetCurrency,
                    toCurrency = challenge.currency,
                    usdRate = usdRate
                )
                val newAmount = challenge.currentAmount + amountInChallengeCurrency
                val newEndDate = challenge.endDate

                val duration = if (parts.size > 2) parts[2] else "30"
                val totalSteps = duration.toIntOrNull() ?: 30

                val isNowCompleted = when (challenge.template) {
                    ChallengeTemplate.FLEXI -> newAmount >= challenge.targetAmount
                    ChallengeTemplate.CONSTANT,
                    ChallengeTemplate.ENVELOPE,
                    ChallengeTemplate.NO_SPEND -> newCompletedSteps >= totalSteps
                }

                val updatedChallenge = challenge.copy(
                    currentAmount = newAmount,
                    endDate = newEndDate,
                    lastDepositDate = LocalDate.now().toString() + "|" + newCompletedSteps + "|" + duration,
                    isCompleted = challenge.isCompleted || isNowCompleted,
                    isActive = challenge.isActive && !isNowCompleted
                )

                // Update challenge state in its native currency
                repository.createChallenge(updatedChallenge)

                // Add deposit in active target currency
                val deposit = SavingsDeposit(
                    id = java.util.UUID.randomUUID().toString(),
                    challengeId = challengeId,
                    amount = amount,
                    date = java.time.LocalDate.now().toString(),
                    note = note,
                    createdAt = System.currentTimeMillis(),
                    currency = targetCurrency
                )
                repository.addDeposit(userId, challengeId, deposit)

                // Trigger saving milestone / completion notification
                val percentage = if (challenge.template == ChallengeTemplate.FLEXI) {
                    if (challenge.targetAmount > 0) ((newAmount / challenge.targetAmount) * 100).toInt() else 0
                } else {
                    if (totalSteps > 0) ((newCompletedSteps.toDouble() / totalSteps) * 100).toInt() else 0
                }
                if (isNowCompleted || percentage >= 100) {
                    notificationHelper.showSavingMilestone(challenge.title, 100)
                } else if (percentage in listOf(25, 50, 75)) {
                    notificationHelper.showSavingMilestone(challenge.title, percentage)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChallengeViewModel", "Failed to add deposit mock", e)
            }
        }
    }

    private val reconcilingChallenges = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    fun autoSkipMissedDays(challengeId: String) {
        if (!reconcilingChallenges.add(challengeId)) return // Prevent concurrent runs for the same challenge
        viewModelScope.launch {
            try {
                val challenges = repository.getAllChallenges(userId).first()
                val challenge = challenges.find { it.id == challengeId } ?: return@launch
                if (challenge.isCompleted) return@launch

                val deposits = repository.getDeposits(userId, challengeId).first()

                // 1. Deduplicate any existing duplicate skipped deposits on the same date
                val depositsByDate = deposits.groupBy { it.date }
                val validDeposits = mutableListOf<SavingsDeposit>()
                for ((date, dateDeposits) in depositsByDate) {
                    if (dateDeposits.size > 1) {
                        val realDeposits = dateDeposits.filter { !it.note.contains("Skipped", ignoreCase = true) }
                        if (realDeposits.isNotEmpty()) {
                            validDeposits.addAll(realDeposits)
                            val skippedToDelete = dateDeposits.filter { it.note.contains("Skipped", ignoreCase = true) }
                            for (s in skippedToDelete) {
                                repository.deleteDeposit(userId, challengeId, s.id)
                            }
                        } else {
                            // All are skipped: keep the first one, delete the rest
                            validDeposits.add(dateDeposits.first())
                            for (dup in dateDeposits.drop(1)) {
                                repository.deleteDeposit(userId, challengeId, dup.id)
                            }
                        }
                    } else {
                        validDeposits.addAll(dateDeposits)
                    }
                }

                // 2. Identify challenge timeline dates
                val startDate = try { LocalDate.parse(challenge.startDate) } catch (e: Exception) { LocalDate.now() }
                val duration = challenge.durationDays.coerceAtLeast(1)
                val today = LocalDate.now()
                val recordedDates = validDeposits.map { it.date }.toSet()

                var updatedDepositsCount = validDeposits.size
                for (dayOffset in 0 until duration) {
                    val targetDay = startDate.plusDays(dayOffset.toLong())
                    // Only past days (strictly before today) are marked as missed
                    if (targetDay.isBefore(today)) {
                        val dateStr = targetDay.toString()
                        if (dateStr !in recordedDates) {
                            val skipEpochMilli = targetDay.atTime(23, 59, 59)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()

                            val skipDeposit = SavingsDeposit(
                                id = java.util.UUID.randomUUID().toString(),
                                challengeId = challengeId,
                                amount = 0.0,
                                date = dateStr,
                                note = "Skipped",
                                createdAt = skipEpochMilli
                            )
                            repository.addDeposit(userId, challengeId, skipDeposit)
                            validDeposits.add(skipDeposit)
                            updatedDepositsCount++
                        }
                    }
                }

                // 3. Update challenge metadata
                val sortedDeposits = validDeposits.sortedBy { it.createdAt }
                val latestDepositDate = sortedDeposits.lastOrNull()?.date ?: challenge.startDate
                val newLastDepositDate = "$latestDepositDate|$updatedDepositsCount|$duration"

                val endDateParsed = try { LocalDate.parse(challenge.endDate) } catch (e: Exception) { null }
                val hasTimelineEnded = endDateParsed != null && today.isAfter(endDateParsed)
                val isNowActive = updatedDepositsCount < duration && !hasTimelineEnded

                val updatedChallenge = challenge.copy(
                    lastDepositDate = newLastDepositDate,
                    isActive = isNowActive
                )
                repository.createChallenge(updatedChallenge)
            } catch (e: Exception) {
                android.util.Log.e("ChallengeViewModel", "Failed to auto skip missed days", e)
            } finally {
                reconcilingChallenges.remove(challengeId)
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

    fun stopChallenge(challengeId: String) {
        viewModelScope.launch {
            val challenges = repository.getAllChallenges(userId).first()
            val challenge = challenges.find { it.id == challengeId } ?: return@launch
            val stoppedChallenge = challenge.copy(isActive = false, isCompleted = false)
            repository.createChallenge(stoppedChallenge)
            // Deselect if this was the selected challenge
            if (selectedChallengeId.value == challengeId) {
                selectedChallengeId.value = null
            }
        }
    }
}
