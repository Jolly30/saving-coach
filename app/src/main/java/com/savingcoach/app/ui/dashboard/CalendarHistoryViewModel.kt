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
    val savingTiers: Map<String, com.savingcoach.app.utils.ActivityTier> = emptyMap(),
    val expenseTiers: Map<String, com.savingcoach.app.utils.ActivityTier> = emptyMap(),
    val investmentTiers: Map<String, com.savingcoach.app.utils.ActivityTier> = emptyMap(),
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
    private val investmentRepository: com.savingcoach.app.data.repository.InvestmentRepository,
    private val authRepository: AuthRepository,
    private val exchangeRateRepository: com.savingcoach.app.data.repository.ExchangeRateRepository
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
                dayInvestment = 0.0,
                currency = "MMK"
            )
        )
    }

    fun onDismissTooltip() {
        _uiState.value = _uiState.value.copy(selectedDate = null, tooltipData = null)
    }

    private fun loadHistory() {
        viewModelScope.launch {
            exchangeRateRepository.fetchLatestRate()
        }
        
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

            val allHistoricalSavings = mutableMapOf<String, Double>()
            for (deposit in allDeposits) {
                allHistoricalSavings[deposit.date] = (allHistoricalSavings[deposit.date] ?: 0.0) + deposit.amount
            }
            val sortedHistoricalDates = allHistoricalSavings.keys.sorted()

            val allExpenses = expenseRepository.getAllExpenses(userId)
                .catch { emit(emptyList()) }
                .first()

            val allHistoricalExpenses = mutableMapOf<String, Double>()
            for (expense in allExpenses) {
                allHistoricalExpenses[expense.date] = (allHistoricalExpenses[expense.date] ?: 0.0) + expense.amount
            }
            val sortedExpenseDates = allHistoricalExpenses.keys.sorted()

            val allInvestments = investmentRepository.getHoldingsOnce(userId)
            val usdRate = exchangeRateRepository.usdToMmkRate.value

            val allHistoricalInvestments = mutableMapOf<String, Double>()
            for (holding in allInvestments) {
                val holdingDate = if (holding.date.isNotEmpty()) holding.date else java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                val amount = holding.units * holding.buyPrice * usdRate
                allHistoricalInvestments[holdingDate] = (allHistoricalInvestments[holdingDate] ?: 0.0) + amount
            }
            val sortedInvestmentDates = allHistoricalInvestments.keys.sorted()

            val monthsData = monthsToLoad.map { month ->
                val monthStr = month.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                
                val dailySavingsMap = mutableMapOf<String, Double>()
                for (deposit in allDeposits) {
                    if (deposit.date.startsWith(monthStr)) {
                        dailySavingsMap[deposit.date] = (dailySavingsMap[deposit.date] ?: 0.0) + deposit.amount
                    }
                }
                
                val savingTiersMap = mutableMapOf<String, com.savingcoach.app.utils.ActivityTier>()
                for (date in dailySavingsMap.keys) {
                    val currentAmount = dailySavingsMap[date] ?: 0.0
                    val history = sortedHistoricalDates
                        .filter { it < date }
                        .mapNotNull { allHistoricalSavings[it] }
                    
                    val result = com.savingcoach.app.utils.ActivityTierCalculator.calculateTier(currentAmount, history)
                    savingTiersMap[date] = result.tier
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
                    
                val expenseTiersMap = mutableMapOf<String, com.savingcoach.app.utils.ActivityTier>()
                for (date in dailyMap.keys) {
                    val currentAmount = dailyMap[date] ?: 0.0
                    val history = sortedExpenseDates
                        .filter { it < date }
                        .mapNotNull { allHistoricalExpenses[it] }
                    
                    val result = com.savingcoach.app.utils.ActivityTierCalculator.calculateTier(currentAmount, history)
                    expenseTiersMap[date] = result.tier
                }
                    
                val dailyInvestmentsMap = mutableMapOf<String, Double>()
                for (holding in allInvestments) {
                    val holdingDate = if (holding.date.isNotEmpty()) holding.date else java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    if (holdingDate.startsWith(monthStr)) {
                        val amount = holding.units * holding.buyPrice * usdRate
                        dailyInvestmentsMap[holdingDate] = (dailyInvestmentsMap[holdingDate] ?: 0.0) + amount
                    }
                }
                
                val investmentTiersMap = mutableMapOf<String, com.savingcoach.app.utils.ActivityTier>()
                for (date in dailyInvestmentsMap.keys) {
                    val currentAmount = dailyInvestmentsMap[date] ?: 0.0
                    val history = sortedInvestmentDates
                        .filter { it < date }
                        .mapNotNull { allHistoricalInvestments[it] }
                    
                    val result = com.savingcoach.app.utils.ActivityTierCalculator.calculateTier(currentAmount, history)
                    investmentTiersMap[date] = result.tier
                }
                    
                MonthData(
                    month = month,
                    dailySpending = dailyMap,
                    dailySavings = dailySavingsMap,
                    savingTiers = savingTiersMap,
                    expenseTiers = expenseTiersMap,
                    investmentTiers = investmentTiersMap,
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
                    dayInvestment = 0.0,
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
