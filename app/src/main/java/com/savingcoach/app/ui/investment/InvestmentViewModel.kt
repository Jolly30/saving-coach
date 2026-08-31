package com.savingcoach.app.ui.investment

import android.content.Context
import com.savingcoach.app.export.CsvExporter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savingcoach.app.data.model.CoinGeckoCoin
import com.savingcoach.app.data.model.ComputedHolding
import com.savingcoach.app.data.model.CachedPrice
import com.savingcoach.app.data.model.FinnhubNewsResponse
import com.savingcoach.app.data.model.FinnhubResult
import com.savingcoach.app.data.model.PortfolioSummary
import com.savingcoach.app.data.model.UserHolding
import com.savingcoach.app.data.repository.AuthRepository
import com.savingcoach.app.data.repository.InvestmentRepository
import com.savingcoach.app.services.MarketApiService
import com.savingcoach.app.utils.InvestmentCalculations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.savingcoach.app.data.repository.UserRepository

/**
 * UI state for the Investment screen.
 */
data class InvestmentUiState(
    val holdings: List<UserHolding> = emptyList(),
    val computedHoldings: List<ComputedHolding> = emptyList(),
    val portfolioSummary: PortfolioSummary = PortfolioSummary(),
    val selectedTab: InvestmentTab = InvestmentTab.ALL,
    val searchQuery: String = "",
    val filteredHoldings: List<ComputedHolding> = emptyList(),
    val marketNews: List<FinnhubNewsResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val lastSyncTime: Long? = null,
    val errorMessage: String? = null,
    val showAddAssetSheet: Boolean = false,
    val cryptoSearchResults: List<CoinGeckoCoin> = emptyList(),
    val stockSearchResults: List<FinnhubResult> = emptyList(),
    val isSearching: Boolean = false,
    val isSearchScreenOpen: Boolean = false,
    val searchTab: SearchTab = SearchTab.ALL,
    val filteredMarketNews: List<FinnhubNewsResponse> = emptyList(),
    val usdRate: Double = 2100.0,
    val currencyPreference: String = "MMK",
    val exportFile: java.io.File? = null
)

/**
 * Investment tabs for filtering holdings.
 */
enum class InvestmentTab {
    ALL, STOCKS, CRYPTO, NEWS
}

/**
 * Search tabs for the global search screen.
 */
enum class SearchTab {
    ALL, STOCKS, CRYPTO, NEWS
}

/**
 * ViewModel for the Investment screen.
 * Manages portfolio state, API calls, and user interactions.
 */
@HiltViewModel
class InvestmentViewModel @Inject constructor(
    private val investmentRepository: InvestmentRepository,
    private val marketApiService: MarketApiService,
    private val authRepository: AuthRepository,
    private val exchangeRateRepository: com.savingcoach.app.data.repository.ExchangeRateRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvestmentUiState())
    val uiState: StateFlow<InvestmentUiState> = _uiState.asStateFlow()

    init {
        loadHoldings()
        loadMarketNews()
    }

    /**
     * Load holdings from repository and observe changes.
     */
    private fun loadHoldings() {
        val userId = authRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            exchangeRateRepository.fetchLatestRate(force = true)
        }

        viewModelScope.launch {
            combine(
                investmentRepository.getHoldings(userId),
                exchangeRateRepository.usdToMmkRate,
                userRepository.getUserProfileFlow(userId).catch { emit(null) }
            ) { holdings, usdRate, userProfile ->
                Triple(holdings, usdRate, userProfile?.currencyPreference ?: "MMK")
            }.collect { (holdings, usdRate, currencyPref) ->
                _uiState.update { it.copy(holdings = holdings, usdRate = usdRate, currencyPreference = currencyPref) }
                updateComputedHoldingsInstantly(holdings, usdRate, currencyPref)
                fetchPricesForHoldings(holdings, usdRate, currencyPref)
            }
        }
    }

    /**
     * Map holdings to computed holdings instantly using cached/fallback values
     * before API fetch completes to ensure a responsive UI.
     */
    private fun updateComputedHoldingsInstantly(holdings: List<UserHolding>, usdRate: Double, currencyPref: String) {
        val existingMap = _uiState.value.computedHoldings.associateBy { it.holding.id }
        val multiplier = if (currencyPref == "MMK") usdRate else 1.0
        val computedHoldings = holdings.map { holding ->
            val existing = existingMap[holding.id]
            val livePriceVal = if (holding.isStoppedCompat) {
                holding.exitPrice
            } else if (existing != null && existing.livePrice > 0.0) {
                existing.livePrice / multiplier
            } else {
                holding.buyPrice
            }
            
            val change24h = if (holding.isStoppedCompat) 0.0 else (existing?.change24h ?: 0.0)
            
            InvestmentCalculations.computeHolding(
                holding = holding,
                livePrice = livePriceVal * multiplier,
                change24h = change24h,
                currencyRateMultiplier = multiplier
            )
        }
        
        val summary = InvestmentCalculations.computePortfolioSummary(computedHoldings)
        
        _uiState.update { state ->
            state.copy(
                computedHoldings = computedHoldings,
                portfolioSummary = summary
            )
        }
        applyFilter()
    }

    /**
     * Fetch live prices for all holdings.
     * Preserves all user holdings with fallback prices even if individual API calls fail.
     */
    private fun fetchPricesForHoldings(holdings: List<UserHolding>, usdRate: Double, currencyPref: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            try {
                val computedHoldings = mutableListOf<ComputedHolding>()
                val existingMap = _uiState.value.computedHoldings.associateBy { it.holding.id }
                val multiplier = if (currencyPref == "MMK") usdRate else 1.0

                // Separate crypto and stock holdings
                val cryptoHoldings = holdings.filter { it.type == "crypto" && !it.isStoppedCompat }
                val stockHoldings = holdings.filter { it.type == "stock" && !it.isStoppedCompat }
                val stoppedHoldings = holdings.filter { it.isStoppedCompat }

                // Fetch crypto prices in batch
                if (cryptoHoldings.isNotEmpty()) {
                    val cryptoIds = cryptoHoldings.map { it.symbol }
                    val cryptoPricesResult = marketApiService.getCryptoPrices(cryptoIds)
                    val prices = cryptoPricesResult.getOrNull() ?: emptyMap()

                    cryptoHoldings.forEach { holding ->
                        val priceData = prices[holding.symbol]
                        val existing = existingMap[holding.id]

                        val livePriceVal = if (priceData != null && priceData.livePrice > 0.0) {
                            priceData.livePrice
                        } else if (existing != null && existing.livePrice > 0.0) {
                            existing.livePrice / multiplier
                        } else {
                            holding.buyPrice
                        }

                        val change24h = priceData?.change24h ?: existing?.change24h ?: 0.0

                        computedHoldings.add(
                            InvestmentCalculations.computeHolding(
                                holding = holding,
                                livePrice = livePriceVal * multiplier,
                                change24h = change24h,
                                currencyRateMultiplier = multiplier
                            )
                        )
                    }
                }

                // Fetch stock prices individually
                stockHoldings.forEach { holding ->
                    val quoteResult = marketApiService.getStockQuote(holding.symbol)
                    val priceData = quoteResult.getOrNull()
                    val existing = existingMap[holding.id]

                    val livePriceVal = if (priceData != null && priceData.livePrice > 0.0) {
                        priceData.livePrice
                    } else if (existing != null && existing.livePrice > 0.0) {
                        existing.livePrice / multiplier
                    } else {
                        holding.buyPrice
                    }

                    val change24h = priceData?.change24h ?: existing?.change24h ?: 0.0

                    computedHoldings.add(
                        InvestmentCalculations.computeHolding(
                            holding = holding,
                            livePrice = livePriceVal * multiplier,
                            change24h = change24h,
                            currencyRateMultiplier = multiplier
                        )
                    )
                }

                // Compute stopped holdings (no API fetch needed)
                stoppedHoldings.forEach { holding ->
                    computedHoldings.add(
                        InvestmentCalculations.computeHolding(
                            holding = holding,
                            livePrice = holding.exitPrice * multiplier,
                            change24h = 0.0,
                            currencyRateMultiplier = multiplier
                        )
                    )
                }

                // Compute portfolio summary
                val summary = InvestmentCalculations.computePortfolioSummary(computedHoldings)

                _uiState.update { state ->
                    state.copy(
                        computedHoldings = computedHoldings,
                        portfolioSummary = summary,
                        isRefreshing = false,
                        lastSyncTime = System.currentTimeMillis(),
                        errorMessage = null
                    )
                }

                applyFilter()

            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isRefreshing = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    /**
     * Load market news from Finnhub.
     */
    private fun loadMarketNews() {
        viewModelScope.launch {
            marketApiService.getMarketNews()
                .onSuccess { news ->
                    _uiState.update { it.copy(marketNews = news) }
                    applyFilter()
                }
                .onFailure { error ->
                    // News failure is non-critical, just log
                    println("Failed to load market news: ${error.message}")
                }
        }
    }

    /**
     * Refresh all market data.
     */
    fun refresh() {
        viewModelScope.launch {
            exchangeRateRepository.fetchLatestRate(force = true)
            marketApiService.clearCache()
            fetchPricesForHoldings(_uiState.value.holdings, exchangeRateRepository.usdToMmkRate.value, _uiState.value.currencyPreference)
            loadMarketNews()
        }
    }

    /**
     * Set the selected tab and apply filter.
     */
    fun selectTab(tab: InvestmentTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        applyFilter()
    }

    /**
     * Update search query and apply filter.
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilter()
    }

    /**
     * Toggle the search screen overlay.
     */
    fun setSearchScreenOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isSearchScreenOpen = isOpen) }
        if (!isOpen) {
            updateSearchQuery("")
            updateSearchTab(SearchTab.ALL)
        }
    }

    /**
     * Set the selected tab in the search screen.
     */
    fun updateSearchTab(tab: SearchTab) {
        _uiState.update { it.copy(searchTab = tab) }
        applyFilter()
    }

    /**
     * Apply tab and search filters to holdings.
     */
    private fun applyFilter() {
        val state = _uiState.value
        // Only show active holdings in the list
        var filteredHoldings = state.computedHoldings.filter { !it.holding.isStoppedCompat }
        var filteredNews = state.marketNews

        // Apply tab filter based on which screen is active
        if (!state.isSearchScreenOpen) {
            filteredHoldings = when (state.selectedTab) {
                InvestmentTab.ALL -> filteredHoldings
                InvestmentTab.STOCKS -> filteredHoldings.filter { it.holding.type == "stock" }
                InvestmentTab.CRYPTO -> filteredHoldings.filter { it.holding.type == "crypto" }
                InvestmentTab.NEWS -> emptyList()
            }
        } else {
            filteredHoldings = when (state.searchTab) {
                SearchTab.ALL -> filteredHoldings
                SearchTab.STOCKS -> filteredHoldings.filter { it.holding.type == "stock" }
                SearchTab.CRYPTO -> filteredHoldings.filter { it.holding.type == "crypto" }
                SearchTab.NEWS -> emptyList()
            }
            filteredNews = when (state.searchTab) {
                SearchTab.ALL -> filteredNews
                SearchTab.NEWS -> filteredNews
                else -> emptyList()
            }
        }

        // Apply search query filter
        if (state.searchQuery.isNotEmpty()) {
            val query = state.searchQuery.lowercase()
            filteredHoldings = filteredHoldings.filter { holding ->
                holding.holding.displayTicker.lowercase().contains(query) ||
                    holding.holding.name.lowercase().contains(query)
            }
            filteredNews = filteredNews.filter { news ->
                news.headline.lowercase().contains(query) ||
                    news.summary.lowercase().contains(query)
            }
        }

        _uiState.update { 
            it.copy(
                filteredHoldings = filteredHoldings,
                filteredMarketNews = filteredNews
            ) 
        }
    }

    /**
     * Add a new holding to the portfolio.
     */
    fun addHolding(holding: UserHolding) {
        val userId = authRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val rate = _uiState.value.usdRate
                val usdBuyPrice = if (_uiState.value.currencyPreference == "MMK" && rate > 0.0) holding.buyPrice / rate else holding.buyPrice
                
                val dateToUse = holding.date.ifEmpty { java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) }
                val updatedHolding = holding.copy(date = dateToUse, buyPrice = usdBuyPrice)
                investmentRepository.addHolding(userId, updatedHolding)
                _uiState.update { it.copy(isLoading = false, showAddAssetSheet = false) }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    /**
     * Delete a holding from the portfolio.
     */
    fun deleteHolding(holdingId: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                investmentRepository.deleteHolding(userId, holdingId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to delete holding: ${e.message}") }
            }
        }
    }

    fun stopHolding(holdingId: String, exitPrice: Double) {
        val userId = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                val target = _uiState.value.holdings.find { it.id == holdingId }
                if (target != null) {
                    val rate = _uiState.value.usdRate
                    val usdExitPrice = if (_uiState.value.currencyPreference == "MMK" && rate > 0.0) exitPrice / rate else exitPrice
                    val updated = target.copy(isStopped = true, exitPrice = usdExitPrice)
                    investmentRepository.updateHolding(userId, updated)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to stop holding: ${e.message}") }
            }
        }
    }

    /**
     * Show/hide the add asset sheet.
     */
    fun setShowAddAssetSheet(show: Boolean) {
        _uiState.update { it.copy(showAddAssetSheet = show) }
    }

    /**
     * Search for crypto assets.
     */
    fun searchCrypto(query: String) {
        if (query.isEmpty()) {
            _uiState.update { it.copy(cryptoSearchResults = emptyList(), isSearching = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            marketApiService.searchCrypto(query)
                .onSuccess { results ->
                    _uiState.update { it.copy(cryptoSearchResults = results, isSearching = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(cryptoSearchResults = emptyList(), isSearching = false) }
                }
        }
    }

    /**
     * Search for stock assets.
     */
    fun searchStocks(query: String) {
        if (query.isEmpty()) {
            _uiState.update { it.copy(stockSearchResults = emptyList(), isSearching = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            marketApiService.searchStocks(query)
                .onSuccess { results ->
                    _uiState.update { it.copy(stockSearchResults = results, isSearching = false) }
                }
                .onFailure {
                    _uiState.update { it.copy(stockSearchResults = emptyList(), isSearching = false) }
                }
        }
    }

    /**
     * Fetch current price for a crypto asset.
     */
    suspend fun fetchCryptoPrice(coinId: String): Double? {
        return marketApiService.getCryptoPrices(listOf(coinId))
            .getOrNull()?.get(coinId)?.livePrice
    }

    /**
     * Fetch current price for a stock.
     */
    suspend fun fetchStockPrice(symbol: String): Double? {
        return marketApiService.getStockQuote(symbol).getOrNull()?.livePrice
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    suspend fun searchCryptoDirect(query: String): List<CoinGeckoCoin> {
        return marketApiService.searchCrypto(query).getOrDefault(emptyList())
    }

    suspend fun searchStocksDirect(query: String): List<FinnhubResult> {
        return marketApiService.searchStocks(query).getOrDefault(emptyList())
    }
}
