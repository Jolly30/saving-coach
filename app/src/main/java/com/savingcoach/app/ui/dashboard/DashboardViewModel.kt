package com.savingcoach.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savingcoach.app.data.model.CachedPrice
import com.savingcoach.app.data.model.Expense
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.SavingsDeposit
import com.savingcoach.app.data.model.User
import com.savingcoach.app.data.model.UserHolding
import com.savingcoach.app.data.repository.AuthRepository
import com.savingcoach.app.data.repository.BudgetRepository
import com.savingcoach.app.data.repository.ExpenseRepository
import com.savingcoach.app.data.repository.SavingChallengeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

import com.savingcoach.app.data.repository.UserRepository
import com.savingcoach.app.services.MarketApiService
import com.savingcoach.app.utils.InvestmentCalculations

enum class CalendarFilter { ALL, SAVINGS, EXPENSES, INVESTMENTS }

data class TooltipData(
    val dayBudget: Double,
    val dayExpense: Double,
    val daySaving: Double,
    val dayInvestment: Double,
    val currency: String
)

data class CategorySpending(
    val category: String,
    val amount: Double
)

data class DashboardUiState(
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val remainingBudget: Double = 0.0,
    val totalSavings: Double = 0.0,
    val investmentValue: Double = 0.0,
    val usdToMmkRate: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val currency: String = "MMK",
    val currencyPreference: String = "MMK",
    val isLoading: Boolean = false,
    val spentPercentage: Double = 0.0,
    val dailySpending: Map<String, Double> = emptyMap(),
    val dailySavings: Map<String, Double> = emptyMap(),
    val savingTiers: Map<String, com.savingcoach.app.utils.ActivityTier> = emptyMap(),
    val expenseTiers: Map<String, com.savingcoach.app.utils.ActivityTier> = emptyMap(),
    val dailyInvestments: Map<String, Double> = emptyMap(),
    val investmentTiers: Map<String, com.savingcoach.app.utils.ActivityTier> = emptyMap(),
    val categorySpending: List<CategorySpending> = emptyList(),
    val recentExpenses: List<Expense> = emptyList(),
    val activeChallenges: List<SavingChallenge> = emptyList(),
    val displayChallenges: List<SavingChallenge> = emptyList(),
    val hasCreatedAnyChallenge: Boolean = false,
    val totalSaved: Double = 0.0,
    val investmentTotalCostBasis: Double = 0.0,
    val investmentTotalLiquidValue: Double = 0.0,
    val investmentUnrealizedPL: Double = 0.0,
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
    private val investmentRepository: com.savingcoach.app.data.repository.InvestmentRepository,
    private val marketApiService: MarketApiService,
    private val authRepository: AuthRepository,
    private val exchangeRateRepository: com.savingcoach.app.data.repository.ExchangeRateRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _cryptoPrices = MutableStateFlow<Map<String, CachedPrice>>(emptyMap())
    private val _stockPrices = MutableStateFlow<Map<String, CachedPrice>>(emptyMap())
    private var lastFetchedHoldingsIds = setOf<String>()

    private val userId: String
        get() = authRepository.getCurrentUserId() ?: "unknown"

    private data class RateAndPrices(
        val usdRate: Double,
        val profile: User?,
        val cryptoPrices: Map<String, CachedPrice>,
        val stockPrices: Map<String, CachedPrice>
    )

    init {
        loadDashboard()
    }

    private fun fetchLivePricesAsync(holdings: List<UserHolding>) {
        val currentIds = holdings.map { "${it.type}_${it.symbol}" }.toSet()
        if (currentIds.isEmpty() || (currentIds == lastFetchedHoldingsIds && (_cryptoPrices.value.isNotEmpty() || _stockPrices.value.isNotEmpty()))) return
        lastFetchedHoldingsIds = currentIds
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val cryptoHoldings = holdings.filter { it.type == "crypto" }
            val stockHoldings = holdings.filter { it.type == "stock" }

            if (cryptoHoldings.isNotEmpty()) {
                val cryptoIds = cryptoHoldings.map { it.symbol }
                val prices = marketApiService.getCryptoPrices(cryptoIds).getOrNull()
                if (prices != null) {
                    _cryptoPrices.value = prices
                }
            }

            if (stockHoldings.isNotEmpty()) {
                val quotesMap = mutableMapOf<String, CachedPrice>()
                stockHoldings.forEach { holding ->
                    val quote = marketApiService.getStockQuote(holding.symbol).getOrNull()
                    if (quote != null) {
                        quotesMap[holding.symbol] = quote
                    }
                }
                if (quotesMap.isNotEmpty()) {
                    _stockPrices.value = quotesMap
                }
            }
        }
    }

    fun loadDashboard() {
        val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

        viewModelScope.launch {
            exchangeRateRepository.fetchLatestRate(force = true)
        }

        viewModelScope.launch {
            // Get ALL challenges (including completed) for total saved calculation
            val allChallenges = challengeRepository.getAllChallenges(userId)
                .catch { emit(emptyList()) }
                .first()

            // Get active challenges for display and daily savings
            val activeChallenges = challengeRepository.getActiveChallenges(userId)
                .catch { emit(emptyList()) }
                .first()

            val challengeDepositsList = coroutineScope {
                allChallenges.map { challenge ->
                    async {
                        challengeRepository.getDeposits(userId, challenge.id)
                            .catch { emit(emptyList()) }
                            .first()
                    }
                }.awaitAll()
            }

            val allExpensesFlow = expenseRepository.getAllExpenses(userId)
                .catch { emit(emptyList()) }

            val allChallengesFlow = challengeRepository.getAllChallenges(userId)
                .catch { emit(emptyList()) }

            combine(
                allExpensesFlow,
                budgetRepository.getBudget(userId, currentMonth)
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                        emit(null)
                    },
                investmentRepository.getHoldings(userId)
                    .catch { emit(emptyList()) },
                allChallengesFlow,
                combine(
                    exchangeRateRepository.usdToMmkRate,
                    userRepository.getUserProfileFlow(userId).catch { emit(null) },
                    _cryptoPrices,
                    _stockPrices
                ) { rate, profile, cryptoPrices, stockPrices ->
                    RateAndPrices(rate, profile, cryptoPrices, stockPrices)
                }
            ) { allExpenses, budget, holdings, allChallengesList, rateAndPrices ->
                fetchLivePricesAsync(holdings)

                val usdRate = rateAndPrices.usdRate
                val userProfile = rateAndPrices.profile
                val currencyPref = userProfile?.currencyPreference ?: "MMK"
                val expenseTargetCurrency = com.savingcoach.app.utils.InvestmentCalculations.getTargetCurrency(currencyPref, isInvestment = false)
                val investmentTargetCurrency = com.savingcoach.app.utils.InvestmentCalculations.getTargetCurrency(currencyPref, isInvestment = true)
                val investmentMultiplier = if (investmentTargetCurrency == "MMK") usdRate else 1.0

                // Build daily savings from all challenge deposits
                val dailySavingsMap = mutableMapOf<String, Double>()
                val allHistoricalSavings = mutableMapOf<String, Double>()
                for (deposits in challengeDepositsList) {
                    for (deposit in deposits) {
                        val conv = com.savingcoach.app.utils.InvestmentCalculations.convertAmount(
                            amount = deposit.amount,
                            fromCurrency = deposit.currency,
                            toCurrency = expenseTargetCurrency,
                            usdRate = usdRate
                        )
                        allHistoricalSavings[deposit.date] = (allHistoricalSavings[deposit.date] ?: 0.0) + conv
                        if (deposit.date.startsWith(currentMonth)) {
                            dailySavingsMap[deposit.date] = (dailySavingsMap[deposit.date] ?: 0.0) + conv
                        }
                    }
                }

                // Compute Saving Tiers for the current month
                val savingTiers = mutableMapOf<String, com.savingcoach.app.utils.ActivityTier>()
                val sortedHistoricalDates = allHistoricalSavings.keys.sorted()
                for (date in dailySavingsMap.keys) {
                    val currentAmount = dailySavingsMap[date] ?: 0.0
                    val history = sortedHistoricalDates
                        .filter { it < date }
                        .mapNotNull { allHistoricalSavings[it] }
                    
                    val result = com.savingcoach.app.utils.ActivityTierCalculator.calculateTier(currentAmount, history)
                    savingTiers[date] = result.tier
                }

                val activeList = allChallengesList.filter { 
                    it.isActive && !it.isCompleted && (it.targetAmount == 0.0 || it.currentAmount < it.targetAmount)
                }
                val expensesCurrentMonth = allExpenses.filter { it.date.startsWith(currentMonth) }
                val totalSpent = expensesCurrentMonth.sumOf { 
                    com.savingcoach.app.utils.InvestmentCalculations.convertAmount(it.amount, it.currency, expenseTargetCurrency, usdRate) 
                }
                val limit = if (budget != null) {
                    com.savingcoach.app.utils.InvestmentCalculations.convertAmount(budget.limit, budget.currency, expenseTargetCurrency, usdRate)
                } else {
                    0.0
                }
                val remaining = limit - totalSpent
                val percentage = if (limit > 0) (totalSpent / limit) * 100 else 0.0

                val dailyMap = expensesCurrentMonth
                    .groupBy { it.date }
                    .mapValues { (_, list) -> 
                        list.sumOf { com.savingcoach.app.utils.InvestmentCalculations.convertAmount(it.amount, it.currency, expenseTargetCurrency, usdRate) } 
                    }

                val allHistoricalExpenses = mutableMapOf<String, Double>()
                for (expense in allExpenses) {
                    val conv = com.savingcoach.app.utils.InvestmentCalculations.convertAmount(expense.amount, expense.currency, expenseTargetCurrency, usdRate)
                    allHistoricalExpenses[expense.date] = (allHistoricalExpenses[expense.date] ?: 0.0) + conv
                }
                
                val expenseTiers = mutableMapOf<String, com.savingcoach.app.utils.ActivityTier>()
                val sortedExpenseDates = allHistoricalExpenses.keys.sorted()
                for (date in dailyMap.keys) {
                    val currentAmount = dailyMap[date] ?: 0.0
                    val history = sortedExpenseDates
                        .filter { it < date }
                        .mapNotNull { allHistoricalExpenses[it] }
                    
                    val result = com.savingcoach.app.utils.ActivityTierCalculator.calculateTier(currentAmount, history)
                    expenseTiers[date] = result.tier
                }

                val recent = expensesCurrentMonth.sortedByDescending { it.createdAt }.take(5).map { expense ->
                    val conv = com.savingcoach.app.utils.InvestmentCalculations.convertAmount(expense.amount, expense.currency, expenseTargetCurrency, usdRate)
                    expense.copy(amount = conv, currency = expenseTargetCurrency)
                }

                val categoryMap = expensesCurrentMonth
                    .groupBy { it.category }
                    .map { (cat, list) -> 
                        val catTotal = list.sumOf { com.savingcoach.app.utils.InvestmentCalculations.convertAmount(it.amount, it.currency, expenseTargetCurrency, usdRate) }
                        CategorySpending(cat, catTotal) 
                    }
                    .sortedByDescending { it.amount }

                val currency = expenseTargetCurrency

                // Compute investment metrics
                var costBasis = 0.0
                var liquidValue = 0.0
                var unrealizedPL = 0.0
                val dailyInvestmentsMap = mutableMapOf<String, Double>()
                val investmentTiers = mutableMapOf<String, com.savingcoach.app.utils.ActivityTier>()
                
                if (holdings.isNotEmpty()) {
                    val computedHoldings = mutableListOf<com.savingcoach.app.data.model.ComputedHolding>()
                    val cryptoHoldings = holdings.filter { it.type == "crypto" }
                    val stockHoldings = holdings.filter { it.type == "stock" }

                    cryptoHoldings.forEach { holding ->
                        val priceData = rateAndPrices.cryptoPrices[holding.symbol]
                        val livePrice = priceData?.livePrice ?: holding.buyPrice
                        val change24h = priceData?.change24h ?: 0.0
                        computedHoldings.add(
                            InvestmentCalculations.computeHolding(holding, livePrice * investmentMultiplier, change24h).copy(
                                costBasis = holding.units * holding.buyPrice * investmentMultiplier,
                                liquidValue = holding.units * livePrice * investmentMultiplier,
                                unrealizedPL = (holding.units * livePrice * investmentMultiplier) - (holding.units * holding.buyPrice * investmentMultiplier)
                            )
                        )
                    }

                    stockHoldings.forEach { holding ->
                        val priceData = rateAndPrices.stockPrices[holding.symbol]
                        val livePrice = priceData?.livePrice ?: holding.buyPrice
                        val change24h = priceData?.change24h ?: 0.0
                        computedHoldings.add(
                            InvestmentCalculations.computeHolding(holding, livePrice * investmentMultiplier, change24h).copy(
                                costBasis = holding.units * holding.buyPrice * investmentMultiplier,
                                liquidValue = holding.units * livePrice * investmentMultiplier,
                                unrealizedPL = (holding.units * livePrice * investmentMultiplier) - (holding.units * holding.buyPrice * investmentMultiplier)
                            )
                        )
                    }

                    val stoppedHoldings = holdings.filter { it.isStoppedCompat }
                    stoppedHoldings.forEach { holding ->
                        computedHoldings.add(
                            InvestmentCalculations.computeHolding(holding, holding.exitPrice * investmentMultiplier, 0.0).copy(
                                costBasis = holding.units * holding.buyPrice * investmentMultiplier,
                                liquidValue = holding.units * holding.exitPrice * investmentMultiplier,
                                unrealizedPL = (holding.units * holding.exitPrice * investmentMultiplier) - (holding.units * holding.buyPrice * investmentMultiplier)
                            )
                        )
                    }

                    val summary = InvestmentCalculations.computePortfolioSummary(computedHoldings)
                    costBasis = summary.totalCostBasis
                    liquidValue = summary.totalLiquidValue
                    unrealizedPL = summary.totalUnrealizedPL

                    // Group investments by date
                    for (holding in holdings) {
                        val holdingDate = if (holding.date.isNotEmpty()) holding.date else java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        if (holdingDate.startsWith(currentMonth)) {
                            val amount = holding.units * holding.buyPrice * investmentMultiplier
                            dailyInvestmentsMap[holdingDate] = (dailyInvestmentsMap[holdingDate] ?: 0.0) + amount
                        }
                    }
                    
                    val allHistoricalInvestments = mutableMapOf<String, Double>()
                    for (holding in holdings) {
                        val holdingDate = if (holding.date.isNotEmpty()) holding.date else java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        val amount = holding.units * holding.buyPrice * investmentMultiplier
                        allHistoricalInvestments[holdingDate] = (allHistoricalInvestments[holdingDate] ?: 0.0) + amount
                    }
                    
                    val sortedInvestmentDates = allHistoricalInvestments.keys.sorted()
                    for (date in dailyInvestmentsMap.keys) {
                        val currentAmount = dailyInvestmentsMap[date] ?: 0.0
                        val history = sortedInvestmentDates
                            .filter { it < date }
                            .mapNotNull { allHistoricalInvestments[it] }
                        
                        val result = com.savingcoach.app.utils.ActivityTierCalculator.calculateTier(currentAmount, history)
                        investmentTiers[date] = result.tier
                    }
                }

                // Sum all-time savings across all challenges (in display currency) to match Challenges tab
                val totalSaved = allChallengesList.sumOf { challenge ->
                    com.savingcoach.app.utils.InvestmentCalculations.convertAmount(
                        amount = challenge.currentAmount,
                        fromCurrency = challenge.currency,
                        toCurrency = expenseTargetCurrency,
                        usdRate = usdRate
                    )
                }

                val convertedActiveChallenges = activeList.map { challenge ->
                    val convTarget = com.savingcoach.app.utils.InvestmentCalculations.convertAmount(
                        amount = challenge.targetAmount,
                        fromCurrency = challenge.currency,
                        toCurrency = expenseTargetCurrency,
                        usdRate = usdRate
                    )
                    val convCurrent = com.savingcoach.app.utils.InvestmentCalculations.convertAmount(
                        amount = challenge.currentAmount,
                        fromCurrency = challenge.currency,
                        toCurrency = expenseTargetCurrency,
                        usdRate = usdRate
                    )
                    challenge.copy(
                        currentAmount = convCurrent,
                        targetAmount = convTarget,
                        currency = expenseTargetCurrency
                    )
                }

                // Only take top 3 active challenges with highest saving
                val displayList = convertedActiveChallenges.sortedByDescending { it.currentAmount }.take(3)

                val hasCreatedAnyChallenge = allChallengesList.isNotEmpty()

                DashboardUiState(
                    isLoading = false,
                    totalSpent = totalSpent,
                    monthlyBudget = limit,
                    remainingBudget = remaining,
                    spentPercentage = percentage,
                    dailySpending = dailyMap,
                    dailySavings = dailySavingsMap,
                    savingTiers = savingTiers,
                    expenseTiers = expenseTiers,
                    dailyInvestments = dailyInvestmentsMap,
                    investmentTiers = investmentTiers,
                    categorySpending = categoryMap,
                    recentExpenses = recent,
                    activeChallenges = convertedActiveChallenges,
                    displayChallenges = displayList,
                    hasCreatedAnyChallenge = hasCreatedAnyChallenge,
                    totalSaved = totalSaved,
                    investmentTotalCostBasis = costBasis,
                    investmentTotalLiquidValue = liquidValue,
                    investmentUnrealizedPL = unrealizedPL,
                    usdToMmkRate = usdRate,
                    currency = currency,
                    currencyPreference = currencyPref
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
        val dayInvestment = current.dailyInvestments[date] ?: 0.0

        _uiState.value = current.copy(
            selectedDate = date,
            tooltipData = TooltipData(
                dayBudget = dayBudget,
                dayExpense = dayExpense,
                daySaving = daySaving,
                dayInvestment = dayInvestment,
                currency = current.currencyPreference
            )
        )
    }

    fun dismissTooltip() {
        _uiState.value = _uiState.value.copy(selectedDate = null, tooltipData = null)
    }
}
