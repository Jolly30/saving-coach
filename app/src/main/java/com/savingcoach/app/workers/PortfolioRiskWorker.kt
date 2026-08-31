package com.savingcoach.app.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.savingcoach.app.core.notification.NotificationHelper
import com.savingcoach.app.data.repository.InvestmentRepository
import com.savingcoach.app.services.MarketApiService
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PortfolioRiskWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val investmentRepository: InvestmentRepository,
    private val marketApiService: MarketApiService,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()
            val holdings = investmentRepository.getHoldingsOnce(userId)
            
            val activeHoldings = holdings.filter { !it.isStoppedCompat }
            if (activeHoldings.isEmpty()) return Result.success()

            val cryptoHoldings = activeHoldings.filter { it.type == "crypto" }
            val stockHoldings = activeHoldings.filter { it.type == "stock" }

            var triggeredRisk = false

            // 1. Check cryptos
            if (cryptoHoldings.isNotEmpty()) {
                val cryptoIds = cryptoHoldings.map { it.symbol }
                marketApiService.getCryptoPrices(cryptoIds).onSuccess { prices ->
                    for (holding in cryptoHoldings) {
                        val priceData = prices[holding.symbol]
                        if (priceData != null && priceData.change24h <= -15.0) {
                            notificationHelper.showPortfolioRiskAlert(holding.displayTicker)
                            triggeredRisk = true
                            break // Only alert on the first major drop to avoid spam
                        }
                    }
                }
            }

            // 2. Check stocks if no crypto drop has triggered yet
            if (!triggeredRisk && stockHoldings.isNotEmpty()) {
                for (holding in stockHoldings) {
                    marketApiService.getStockQuote(holding.symbol).onSuccess { priceData ->
                        if (priceData.change24h <= -15.0) {
                            notificationHelper.showPortfolioRiskAlert(holding.displayTicker)
                            triggeredRisk = true
                        }
                    }.onFailure {
                        // Stock quotes are queried individually, error on one is non-blocking for others
                    }
                    if (triggeredRisk) break
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
