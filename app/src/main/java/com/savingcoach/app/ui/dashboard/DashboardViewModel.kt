package com.savingcoach.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savingcoach.app.data.model.Expense
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.SavingsDeposit
import com.savingcoach.app.data.repository.AuthRepository
import com.savingcoach.app.data.repository.BudgetRepository
import com.savingcoach.app.data.repository.ExpenseRepository
import com.savingcoach.app.data.repository.SavingChallengeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class CalendarFilter { ALL, SAVINGS, EXPENSES }

data class TooltipData(
    val dayBudget: Double,
    val dayExpense: Double,
    val daySaving: Double,
    val currency: String
)

data class CategorySpending(
    val category: String,
    val amount: Double
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val totalSpent: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val spentPercentage: Double = 0.0,
    val dailySpending: Map<String, Double> = emptyMap(),
    val dailySavings: Map<String, Double> = emptyMap(),
    val categorySpending: List<CategorySpending> = emptyList(),
    val recentExpenses: List<Expense> = emptyList(),
    val activeChallenges: List<SavingChallenge> = emptyList(),
    val displayChallenges: List<SavingChallenge> = emptyList(),
    val totalSaved: Double = 0.0,
    val currency: String = "MMK",
    val calendarFilter: CalendarFilter = CalendarFilter.ALL,
    val selectedDate: String? = null,
    val tooltipData: TooltipData? = null,
    val error: String? = null
) {
    companion object {
        /** Default challenge templates — always shown on dashboard */
        val DEFAULT_CHALLENGES = listOf(
            SavingChallenge(
                id = "default_100_envelopes",
                title = "✉️ 100 Envelopes",
                targetAmount = 100.0,
                currentAmount = 0.0,
                isActive = false
            ),
            SavingChallenge(
                id = "default_1_per_day",
                title = "📅 $1/Day Challenge",
                targetAmount = 365.0,
                currentAmount = 0.0,
                isActive = false
            ),
            SavingChallenge(
                id = "default_no_spend_week",
                title = "🚫 No Spend Week",
                targetAmount = 7.0,
                currentAmount = 0.0,
                isActive = false
            ),
            SavingChallenge(
                id = "default_save_20pct",
                title = "💰 Save 20% Income",
                targetAmount = 200.0,
                currentAmount = 0.0,
                isActive = false
            )
        )
    }
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val challengeRepository: SavingChallengeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val userId: String
        get() = authRepository.getCurrentUserId() ?: "unknown"

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

        viewModelScope.launch {
            // First get active challenges so we can query their deposits
            val challenges = challengeRepository.getActiveChallenges(userId)
                .catch { emit(emptyList()) }
                .first()

            // Build daily savings from all active challenge deposits
            val dailySavingsMap = mutableMapOf<String, Double>()
            for (challenge in challenges) {
                val deposits = challengeRepository.getDeposits(userId, challenge.id)
                    .catch { emit(emptyList()) }
                    .first()
                for (deposit in deposits) {
                    if (deposit.date.startsWith(currentMonth)) {
                        dailySavingsMap[deposit.date] =
                            (dailySavingsMap[deposit.date] ?: 0.0) + deposit.amount
                    }
                }
            }

            combine(
                expenseRepository.getExpensesForMonth(userId, currentMonth)
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                        emit(emptyList())
                    },
                budgetRepository.getBudget(userId, currentMonth)
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                        emit(null)
                    }
            ) { expenses, budget ->
                val totalSpent = expenses.sumOf { it.amount }
                val limit = budget?.limit ?: 0.0
                val remaining = limit - totalSpent
                val percentage = if (limit > 0) (totalSpent / limit) * 100 else 0.0

                // Group daily spending by date
                val dailyMap = expenses
                    .groupBy { it.date }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }

                // Group spending by category
                val categoryMap = expenses
                    .groupBy { it.category }
                    .map { (cat, list) -> CategorySpending(cat, list.sumOf { it.amount }) }
                    .sortedByDescending { it.amount }

                // Recent expenses (last 5)
                val recent = expenses.sortedByDescending { it.createdAt }.take(5)

                // Dynamic currency from first expense, fallback to MMK
                val currency = expenses.firstOrNull()?.currency ?: "MMK"

                val totalSaved = challenges.sumOf { it.currentAmount }

                // Merge: active challenges from Firestore + default templates
                // Active ones come first, defaults fill remaining slots
                val activeIds = challenges.map { it.id }.toSet()
                val defaultsOnly = DashboardUiState.DEFAULT_CHALLENGES.filter { it.id !in activeIds }
                val displayList = challenges + defaultsOnly

                DashboardUiState(
                    isLoading = false,
                    totalSpent = totalSpent,
                    monthlyBudget = limit,
                    remainingBudget = remaining.coerceAtLeast(0.0),
                    spentPercentage = percentage,
                    dailySpending = dailyMap,
                    dailySavings = dailySavingsMap,
                    categorySpending = categoryMap,
                    recentExpenses = recent,
                    activeChallenges = challenges,
                    displayChallenges = displayList,
                    totalSaved = totalSaved,
                    currency = currency
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onFilterChange(filter: CalendarFilter) {
        _uiState.value = _uiState.value.copy(calendarFilter = filter)
    }

    fun onDateTap(date: String) {
        val current = _uiState.value
        if (current.selectedDate == date) {
            // Tap same date → dismiss
            _uiState.value = current.copy(selectedDate = null, tooltipData = null)
            return
        }

        val yearMonth = date.substring(0, 7) // "YYYY-MM"
        val dayBudget = current.monthlyBudget / YearMonth.from(
            LocalDate.parse(date)
        ).lengthOfMonth()

        val dayExpense = current.dailySpending[date] ?: 0.0
        val daySaving = current.dailySavings[date] ?: 0.0

        _uiState.value = current.copy(
            selectedDate = date,
            tooltipData = TooltipData(
                dayBudget = dayBudget,
                dayExpense = dayExpense,
                daySaving = daySaving,
                currency = current.currency
            )
        )
    }

    fun dismissTooltip() {
        _uiState.value = _uiState.value.copy(selectedDate = null, tooltipData = null)
    }
}
