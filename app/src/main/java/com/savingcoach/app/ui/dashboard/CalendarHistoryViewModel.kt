package com.savingcoach.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class MonthData(
    val month: YearMonth,
    val dailySpending: Map<String, Double> = emptyMap(),
    val dailySavings: Map<String, Double> = emptyMap(),
    val monthlyBudget: Double = 0.0
)

data class CalendarHistoryUiState(
    val months: List<MonthData> = emptyList(),
    val selectedDate: String? = null,
    val tooltipData: TooltipData? = null
)

@HiltViewModel
class CalendarHistoryViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val challengeRepository: SavingChallengeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarHistoryUiState())
    val uiState: StateFlow<CalendarHistoryUiState> = _uiState.asStateFlow()

    private val userId: String
        get() = authRepository.getCurrentUserId() ?: "unknown"

    init {
        loadHistory()
    }

    fun onDateTap(date: String, monthData: MonthData) {
        val current = _uiState.value
        if (current.selectedDate == date) {
            _uiState.value = current.copy(selectedDate = null, tooltipData = null)
            return
        }

        val dayBudget = monthData.monthlyBudget / monthData.month.lengthOfMonth()
        val dayExpense = monthData.dailySpending[date] ?: 0.0
        val daySaving = monthData.dailySavings[date] ?: 0.0

        _uiState.value = current.copy(
            selectedDate = date,
            tooltipData = TooltipData(
                dayBudget = dayBudget,
                dayExpense = dayExpense,
                daySaving = daySaving,
                currency = "MMK"
            )
        )
    }

    fun onDismissTooltip() {
        _uiState.value = _uiState.value.copy(selectedDate = null, tooltipData = null)
    }

    private fun loadHistory() {
        viewModelScope.launch {
            val current = YearMonth.now()
            val monthsToLoad = (-12..12).map { current.plusMonths(it.toLong()) }
            
            // Get all deposits for active challenges
            val challenges = challengeRepository.getActiveChallenges(userId)
                .catch { emit(emptyList()) }
                .first()

            val allDeposits = challenges.flatMap { challenge ->
                challengeRepository.getDeposits(userId, challenge.id)
                    .catch { emit(emptyList()) }
                    .first()
            }

            val monthsData = monthsToLoad.map { month ->
                val monthStr = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                
                val dailySavingsMap = mutableMapOf<String, Double>()
                for (deposit in allDeposits) {
                    if (deposit.date.startsWith(monthStr)) {
                        dailySavingsMap[deposit.date] = (dailySavingsMap[deposit.date] ?: 0.0) + deposit.amount
                    }
                }
                
                val expenses = expenseRepository.getExpensesForMonth(userId, monthStr)
                    .catch { emit(emptyList()) }
                    .first()
                    
                val budget = budgetRepository.getBudget(userId, monthStr)
                    .catch { emit(null) }
                    .first()
                    
                val dailyMap = expenses
                    .groupBy { it.date }
                    .mapValues { (_, list) -> list.sumOf { it.amount } }
                    
                MonthData(
                    month = month,
                    dailySpending = dailyMap,
                    dailySavings = dailySavingsMap,
                    monthlyBudget = budget?.limit ?: 0.0
                )
            }
            
            val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val currentMonthData = monthsData.find { it.month == current }
            
            val initialTooltipData = currentMonthData?.let { mData ->
                TooltipData(
                    dayBudget = mData.monthlyBudget / mData.month.lengthOfMonth(),
                    dayExpense = mData.dailySpending[todayStr] ?: 0.0,
                    daySaving = mData.dailySavings[todayStr] ?: 0.0,
                    currency = "MMK"
                )
            }
            
            _uiState.value = _uiState.value.copy(
                months = monthsData,
                selectedDate = todayStr,
                tooltipData = initialTooltipData
            )
        }
    }
}
