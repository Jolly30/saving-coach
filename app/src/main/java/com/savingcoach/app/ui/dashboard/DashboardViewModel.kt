package com.savingcoach.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savingcoach.app.data.repository.BudgetRepository
import com.savingcoach.app.data.repository.ExpenseRepository
import com.savingcoach.app.data.repository.SavingChallengeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val totalSpent: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val spentPercentage: Double = 0.0,
    val dailySpending: Map<String, Double> = emptyMap(),  // date -> amount
    val activeChallenges: Int = 0,
    val totalSaved: Double = 0.0,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val challengeRepository: SavingChallengeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val userId: String
        get() = "default_user" // TODO: Replace with actual auth user ID

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

        viewModelScope.launch {
            combine(
                expenseRepository.getExpensesForMonth(userId, currentMonth),
                budgetRepository.getBudget(userId, currentMonth),
                challengeRepository.getActiveChallenges(userId)
            ) { expenses, budget, challenges ->
                val totalSpent = expenses.sumOf { it.amount }
                val limit = budget?.limit ?: 0.0
                val remaining = limit - totalSpent
                val percentage = if (limit > 0) (totalSpent / limit) * 100 else 0.0

                // Group daily spending by date
                val dailyMap = expenses
                    .groupBy { it.date }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }

                val totalSaved = challenges.sumOf { it.currentAmount }

                DashboardUiState(
                    isLoading = false,
                    totalSpent = totalSpent,
                    monthlyBudget = limit,
                    remainingBudget = remaining.coerceAtLeast(0.0),
                    spentPercentage = percentage,
                    dailySpending = dailyMap,
                    activeChallenges = challenges.size,
                    totalSaved = totalSaved
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
