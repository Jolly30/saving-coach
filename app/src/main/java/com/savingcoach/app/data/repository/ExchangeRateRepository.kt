package com.savingcoach.app.data.repository

import com.savingcoach.app.services.MarketApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRateRepository @Inject constructor(
    private val marketApiService: MarketApiService
) {
    private val _usdToMmkRate = MutableStateFlow<Double>(4500.0) // Default fallback to market rate
    val usdToMmkRate: StateFlow<Double> = _usdToMmkRate.asStateFlow()

    private var lastFetchTime = 0L
    private val CACHE_DURATION_MS = 1000 * 60 * 60 * 12 // 12 hours

    suspend fun fetchLatestRate(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastFetchTime < CACHE_DURATION_MS) {
            return // Use cached rate
        }

        val rate = marketApiService.getUsdToMmkExchangeRate()
        if (rate != null) {
            _usdToMmkRate.value = rate
            lastFetchTime = now
        }
    }
}
