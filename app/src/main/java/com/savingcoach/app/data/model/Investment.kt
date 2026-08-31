package com.savingcoach.app.data.model

import kotlinx.serialization.Serializable
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.Exclude

/**
 * User's recorded holding — only the 3 manual values they enter.
 * No timestamps or purchase dates stored.
 */
@Serializable
data class UserHolding(
    val id: String = "",              // UUID v4
    val type: String = "stock",       // "stock" | "crypto"
    val symbol: String = "",          // CoinGecko coin ID (e.g., "bitcoin") or stock ticker (e.g., "AAPL")
    val displayTicker: String = "",   // Clean ticker for display (e.g., "BTC", "AAPL")
    val name: String = "",            // Full name (e.g., "Bitcoin", "Apple Inc.")
    val units: Double = 0.0,          // e.g., 15.0 or 0.25
    val buyPrice: Double = 0.0,       // Market price paid per unit at execution ($)
    val date: String = "",            // Purchase date (YYYY-MM-DD)
    @get:PropertyName("isStopped")
    val isStopped: Boolean = false,   // True if user stopped/liquidated this holding
    val exitPrice: Double = 0.0,      // Price at the time it was stopped
    @get:PropertyName("stopped")
    val stopped: Boolean? = null      // Backwards compatibility field
) {
    val isStoppedCompat: Boolean
        @Exclude
        get() = stopped == true || isStopped
}

/**
 * Computed holding with live market data fetched from APIs.
 */
data class ComputedHolding(
    val holding: UserHolding,
    val livePrice: Double = 0.0,       // Fetched from API ($)
    val change24h: Double = 0.0,       // Daily percent change (%)
    val costBasis: Double = 0.0,       // units * buyPrice
    val liquidValue: Double = 0.0,     // units * livePrice
    val unrealizedPL: Double = 0.0,    // liquidValue - costBasis
    val roiPercentage: Double = 0.0    // ((livePrice - buyPrice) / buyPrice) * 100
)

/**
 * Portfolio-level summary statistics.
 */
data class PortfolioSummary(
    val totalLiquidValue: Double = 0.0,
    val totalCostBasis: Double = 0.0,
    val totalUnrealizedPL: Double = 0.0,
    val totalRealizedPL: Double = 0.0,
    val totalROI: Double = 0.0,
    val stockValue: Double = 0.0,
    val cryptoValue: Double = 0.0
)

/**
 * API response models for CoinGecko and Finnhub.
 */
@Serializable
data class CoinGeckoSearchResponse(
    val coins: List<CoinGeckoCoin> = emptyList()
)

@Serializable
data class CoinGeckoCoin(
    val id: String = "",
    val name: String = "",
    val symbol: String = "",
    val thumb: String = ""    // Small thumbnail URL
)

// CoinGecko price response is a dynamic map: {"bitcoin": {"usd": 50000, "usd_24h_change": 2.5}}
// We parse it as Map<String, Map<String, Double>> and convert to our data classes
typealias CoinGeckoPriceResponse = Map<String, Map<String, Double>>

@Serializable
data class CoinGeckoPrice(
    val usd: Double = 0.0,
    val usd_24h_change: Double = 0.0
) {
    companion object {
        fun fromMap(map: Map<String, Double>): CoinGeckoPrice {
            return CoinGeckoPrice(
                usd = map["usd"] ?: 0.0,
                usd_24h_change = map["usd_24h_change"] ?: 0.0
            )
        }
    }
}

@Serializable
data class FinnhubSearchResponse(
    val result: List<FinnhubResult> = emptyList()
)

@Serializable
data class FinnhubResult(
    val description: String = "",
    val displaySymbol: String = "",
    val symbol: String = "",
    val type: String = ""
)

@Serializable
data class FinnhubQuoteResponse(
    val c: Double = 0.0,      // Current price
    val d: Double? = 0.0,     // Change
    val dp: Double? = 0.0,    // Percent change
    val h: Double = 0.0,      // High price of the day
    val l: Double = 0.0,      // Low price of the day
    val o: Double = 0.0,      // Open price of the day
    val pc: Double = 0.0      // Previous close price
)

@Serializable
data class FinnhubNewsResponse(
    val id: Long = 0,
    val category: String = "",
    val datetime: Long = 0,
    val headline: String = "",
    val image: String = "",
    val related: String = "",
    val source: String = "",
    val summary: String = "",
    val url: String = ""
)

/**
 * Market data cache entry with timestamp.
 */
data class CachedPrice(
    val livePrice: Double,
    val change24h: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isFresh: Boolean
        get() = System.currentTimeMillis() - timestamp < 60_000 // 60 seconds TTL
}
