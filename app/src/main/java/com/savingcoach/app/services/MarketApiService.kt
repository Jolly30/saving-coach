package com.savingcoach.app.services

import com.savingcoach.app.data.model.CachedPrice
import com.savingcoach.app.data.model.CoinGeckoCoin
import com.savingcoach.app.data.model.CoinGeckoPrice
import com.savingcoach.app.data.model.CoinGeckoSearchResponse
import com.savingcoach.app.data.model.FinnhubNewsResponse
import com.savingcoach.app.data.model.FinnhubQuoteResponse
import com.savingcoach.app.data.model.FinnhubResult
import com.savingcoach.app.data.model.FinnhubSearchResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Market data API service for CoinGecko (Crypto) and Finnhub (Stocks/ETFs).
 * Uses Vercel proxy to hide API keys server-side.
 * Features: 60-second TTL cache, rate limit protection, graceful fallbacks.
 */
@Singleton
class MarketApiService @Inject constructor(
    private val client: OkHttpClient,
    @Named("coingecko_proxy_url") private val coingeckoProxyUrl: String,
    @Named("finnhub_proxy_url") private val finnhubProxyUrl: String
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
        isLenient = true
    }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    // In-memory price cache with 60-second TTL
    private val priceCache = mutableMapOf<String, CachedPrice>()

    private val defaultPopularStocksAndEtfs = listOf(
        // Popular ETFs
        FinnhubResult(description = "Vanguard S&P 500 ETF", displaySymbol = "VOO", symbol = "VOO", type = "ETP"),
        FinnhubResult(description = "SPDR S&P 500 ETF Trust", displaySymbol = "SPY", symbol = "SPY", type = "ETP"),
        FinnhubResult(description = "Invesco QQQ Trust Series 1", displaySymbol = "QQQ", symbol = "QQQ", type = "ETP"),
        FinnhubResult(description = "Vanguard Total Stock Market ETF", displaySymbol = "VTI", symbol = "VTI", type = "ETP"),
        FinnhubResult(description = "iShares Core S&P 500 ETF", displaySymbol = "IVV", symbol = "IVV", type = "ETP"),
        FinnhubResult(description = "Schwab U.S. Dividend Equity ETF", displaySymbol = "SCHD", symbol = "SCHD", type = "ETP"),
        FinnhubResult(description = "ARK Innovation ETF", displaySymbol = "ARKK", symbol = "ARKK", type = "ETP"),
        FinnhubResult(description = "Vanguard Total International Stock ETF", displaySymbol = "VXUS", symbol = "VXUS", type = "ETP"),
        FinnhubResult(description = "Vanguard Total Bond Market ETF", displaySymbol = "BND", symbol = "BND", type = "ETP"),
        FinnhubResult(description = "Vanguard Total World Stock ETF", displaySymbol = "VT", symbol = "VT", type = "ETP"),
        FinnhubResult(description = "SPDR Dow Jones Industrial Average ETF", displaySymbol = "DIA", symbol = "DIA", type = "ETP"),
        FinnhubResult(description = "iShares Russell 2000 ETF", displaySymbol = "IWM", symbol = "IWM", type = "ETP"),
        FinnhubResult(description = "Vanguard Growth ETF", displaySymbol = "VUG", symbol = "VUG", type = "ETP"),
        FinnhubResult(description = "Vanguard Value ETF", displaySymbol = "VTV", symbol = "VTV", type = "ETP"),
        FinnhubResult(description = "Technology Select Sector SPDR Fund", displaySymbol = "XLK", symbol = "XLK", type = "ETP"),
        FinnhubResult(description = "Financial Select Sector SPDR Fund", displaySymbol = "XLF", symbol = "XLF", type = "ETP"),
        FinnhubResult(description = "Energy Select Sector SPDR Fund", displaySymbol = "XLE", symbol = "XLE", type = "ETP"),
        FinnhubResult(description = "iShares Semiconductor ETF", displaySymbol = "SOXX", symbol = "SOXX", type = "ETP"),
        FinnhubResult(description = "VanEck Semiconductor ETF", displaySymbol = "SMH", symbol = "SMH", type = "ETP"),
        FinnhubResult(description = "SPDR Gold Shares", displaySymbol = "GLD", symbol = "GLD", type = "ETP"),
        FinnhubResult(description = "iShares Silver Trust", displaySymbol = "SLV", symbol = "SLV", type = "ETP"),
        FinnhubResult(description = "iShares 20+ Year Treasury Bond ETF", displaySymbol = "TLT", symbol = "TLT", type = "ETP"),
        FinnhubResult(description = "JPMorgan Equity Premium Income ETF", displaySymbol = "JEPI", symbol = "JEPI", type = "ETP"),
        FinnhubResult(description = "JPMorgan Nasdaq Equity Premium Income ETF", displaySymbol = "JEPQ", symbol = "JEPQ", type = "ETP"),
        // Popular Stocks
        FinnhubResult(description = "Apple Inc.", displaySymbol = "AAPL", symbol = "AAPL", type = "Common Stock"),
        FinnhubResult(description = "Microsoft Corporation", displaySymbol = "MSFT", symbol = "MSFT", type = "Common Stock"),
        FinnhubResult(description = "Alphabet Inc. (Class A)", displaySymbol = "GOOGL", symbol = "GOOGL", type = "Common Stock"),
        FinnhubResult(description = "Alphabet Inc. (Class C)", displaySymbol = "GOOG", symbol = "GOOG", type = "Common Stock"),
        FinnhubResult(description = "Amazon.com Inc.", displaySymbol = "AMZN", symbol = "AMZN", type = "Common Stock"),
        FinnhubResult(description = "NVIDIA Corporation", displaySymbol = "NVDA", symbol = "NVDA", type = "Common Stock"),
        FinnhubResult(description = "Tesla, Inc.", displaySymbol = "TSLA", symbol = "TSLA", type = "Common Stock"),
        FinnhubResult(description = "Meta Platforms, Inc.", displaySymbol = "META", symbol = "META", type = "Common Stock"),
        FinnhubResult(description = "Berkshire Hathaway Inc. Class B", displaySymbol = "BRK.B", symbol = "BRK.B", type = "Common Stock"),
        FinnhubResult(description = "Taiwan Semiconductor Manufacturing Co.", displaySymbol = "TSM", symbol = "TSM", type = "ADR"),
        FinnhubResult(description = "Broadcom Inc.", displaySymbol = "AVGO", symbol = "AVGO", type = "Common Stock"),
        FinnhubResult(description = "JPMorgan Chase & Co.", displaySymbol = "JPM", symbol = "JPM", type = "Common Stock"),
        FinnhubResult(description = "Visa Inc.", displaySymbol = "V", symbol = "V", type = "Common Stock"),
        FinnhubResult(description = "UnitedHealth Group Inc.", displaySymbol = "UNH", symbol = "UNH", type = "Common Stock"),
        FinnhubResult(description = "Walmart Inc.", displaySymbol = "WMT", symbol = "WMT", type = "Common Stock"),
        FinnhubResult(description = "Exxon Mobil Corporation", displaySymbol = "XOM", symbol = "XOM", type = "Common Stock"),
        FinnhubResult(description = "Mastercard Incorporated", displaySymbol = "MA", symbol = "MA", type = "Common Stock"),
        FinnhubResult(description = "Johnson & Johnson", displaySymbol = "JNJ", symbol = "JNJ", type = "Common Stock"),
        FinnhubResult(description = "Procter & Gamble Co.", displaySymbol = "PG", symbol = "PG", type = "Common Stock"),
        FinnhubResult(description = "Costco Wholesale Corporation", displaySymbol = "COST", symbol = "COST", type = "Common Stock"),
        FinnhubResult(description = "The Home Depot, Inc.", displaySymbol = "HD", symbol = "HD", type = "Common Stock"),
        FinnhubResult(description = "Advanced Micro Devices, Inc.", displaySymbol = "AMD", symbol = "AMD", type = "Common Stock"),
        FinnhubResult(description = "Netflix, Inc.", displaySymbol = "NFLX", symbol = "NFLX", type = "Common Stock"),
        FinnhubResult(description = "The Walt Disney Company", displaySymbol = "DIS", symbol = "DIS", type = "Common Stock"),
        FinnhubResult(description = "Adobe Inc.", displaySymbol = "ADBE", symbol = "ADBE", type = "Common Stock"),
        FinnhubResult(description = "Salesforce, Inc.", displaySymbol = "CRM", symbol = "CRM", type = "Common Stock"),
        FinnhubResult(description = "Bank of America Corp.", displaySymbol = "BAC", symbol = "BAC", type = "Common Stock"),
        FinnhubResult(description = "The Coca-Cola Company", displaySymbol = "KO", symbol = "KO", type = "Common Stock"),
        FinnhubResult(description = "PepsiCo, Inc.", displaySymbol = "PEP", symbol = "PEP", type = "Common Stock"),
        FinnhubResult(description = "Alibaba Group Holding Ltd.", displaySymbol = "BABA", symbol = "BABA", type = "ADR"),
        FinnhubResult(description = "Intel Corporation", displaySymbol = "INTC", symbol = "INTC", type = "Common Stock"),
        FinnhubResult(description = "Cisco Systems, Inc.", displaySymbol = "CSCO", symbol = "CSCO", type = "Common Stock"),
        FinnhubResult(description = "QUALCOMM Incorporated", displaySymbol = "QCOM", symbol = "QCOM", type = "Common Stock"),
        FinnhubResult(description = "Palantir Technologies Inc.", displaySymbol = "PLTR", symbol = "PLTR", type = "Common Stock"),
        FinnhubResult(description = "Uber Technologies, Inc.", displaySymbol = "UBER", symbol = "UBER", type = "Common Stock"),
        FinnhubResult(description = "PayPal Holdings, Inc.", displaySymbol = "PYPL", symbol = "PYPL", type = "Common Stock"),
        FinnhubResult(description = "NIKE, Inc.", displaySymbol = "NKE", symbol = "NKE", type = "Common Stock"),
        FinnhubResult(description = "Starbucks Corporation", displaySymbol = "SBUX", symbol = "SBUX", type = "Common Stock"),
        FinnhubResult(description = "Sony Group Corporation", displaySymbol = "SONY", symbol = "SONY", type = "ADR"),
        FinnhubResult(description = "Realty Income Corporation", displaySymbol = "O", symbol = "O", type = "REIT"),
        FinnhubResult(description = "The Boeing Company", displaySymbol = "BA", symbol = "BA", type = "Common Stock"),
        FinnhubResult(description = "Pfizer Inc.", displaySymbol = "PFE", symbol = "PFE", type = "Common Stock"),
        FinnhubResult(description = "AbbVie Inc.", displaySymbol = "ABBV", symbol = "ABBV", type = "Common Stock"),
        FinnhubResult(description = "Eli Lilly and Company", displaySymbol = "LLY", symbol = "LLY", type = "Common Stock")
    )

    // ─────────────────────────────────────────────
    // COINGECKO (Crypto) - via proxy
    // ─────────────────────────────────────────────

    /**
     * Search for cryptocurrencies by query string.
     * Returns matching coins with id, symbol, name, and thumbnail.
     */
    suspend fun searchCrypto(query: String): Result<List<CoinGeckoCoin>> {
        return withContext(Dispatchers.IO) {
            try {
                val body = mapOf(
                    "action" to "search",
                    "query" to query
                )
                val requestBody = Json.encodeToString(body).toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(coingeckoProxyUrl)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("CoinGecko search failed: ${response.code}")
                    )
                }

                val responseBody = response.body?.string() ?: return@withContext Result.failure(
                    Exception("Empty response from CoinGecko proxy")
                )

                val searchResponse = json.decodeFromString<CoinGeckoSearchResponse>(responseBody)
                Result.success(searchResponse.coins)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Fetch current prices for multiple crypto assets in a single batch request.
     * Uses CoinGecko's simple/price endpoint for efficiency.
     */
    suspend fun getCryptoPrices(coinIds: List<String>): Result<Map<String, CachedPrice>> {
        return withContext(Dispatchers.IO) {
            try {
                if (coinIds.isEmpty()) return@withContext Result.success(emptyMap())

                // Check cache first
                val cachedResults = mutableMapOf<String, CachedPrice>()
                val uncachedIds = coinIds.filter { id ->
                    val cached = priceCache[id]
                    if (cached != null && cached.isFresh) {
                        cachedResults[id] = cached
                        false
                    } else {
                        true
                    }
                }

                if (uncachedIds.isEmpty()) {
                    return@withContext Result.success(cachedResults)
                }

                // Fetch uncached prices via proxy
                val idsParam = uncachedIds.joinToString(",")
                val body = mapOf(
                    "action" to "price",
                    "ids" to idsParam,
                    "vs_currencies" to "usd",
                    "include_24hr_change" to "true"
                )
                val requestBody = Json.encodeToString(body).toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(coingeckoProxyUrl)
                    .post(requestBody)
                    .build()

                val response = try { client.newCall(request).execute() } catch (_: Exception) { null }
                if (response != null && response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank()) {
                        try {
                            val priceMap = json.decodeFromString<Map<String, Map<String, Double>>>(responseBody)
                            priceMap.forEach { (id, priceData) ->
                                val coinGeckoPrice = CoinGeckoPrice.fromMap(priceData)
                                val cached = CachedPrice(
                                    livePrice = coinGeckoPrice.usd,
                                    change24h = coinGeckoPrice.usd_24h_change
                                )
                                priceCache[id] = cached
                                cachedResults[id] = cached
                            }
                            if (cachedResults.isNotEmpty()) {
                                return@withContext Result.success(cachedResults)
                            }
                        } catch (_: Exception) {
                            // Non-JSON or error format from proxy
                        }
                    }
                }

                // Fallback to direct CoinGecko API
                val directResult = fetchDirectCoinGeckoPrices(uncachedIds)
                if (directResult.isSuccess) {
                    cachedResults.putAll(directResult.getOrThrow())
                    return@withContext Result.success(cachedResults)
                }

                if (cachedResults.isNotEmpty()) {
                    Result.success(cachedResults)
                } else {
                    Result.failure(Exception("CoinGecko price fetch failed"))
                }
            } catch (e: Exception) {
                val directResult = fetchDirectCoinGeckoPrices(coinIds)
                if (directResult.isSuccess) {
                    return@withContext directResult
                }
                // Return any cached data on failure
                val fallbackResults = coinIds.mapNotNull { id ->
                    priceCache[id]?.let { id to it }
                }.toMap()
                if (fallbackResults.isNotEmpty()) {
                    Result.success(fallbackResults)
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    private suspend fun fetchDirectCoinGeckoPrices(ids: List<String>): Result<Map<String, CachedPrice>> {
        return withContext(Dispatchers.IO) {
            try {
                if (ids.isEmpty()) return@withContext Result.success(emptyMap())
                val idsParam = ids.joinToString(",")
                val url = "https://api.coingecko.com/api/v3/simple/price?ids=$idsParam&vs_currencies=usd&include_24hr_change=true"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) Gecko/125.0 Firefox/125.0")
                    .header("Accept", "application/json")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Direct CoinGecko failed: ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val priceMap = json.decodeFromString<Map<String, Map<String, Double>>>(body)
                val results = mutableMapOf<String, CachedPrice>()
                priceMap.forEach { (id, priceData) ->
                    val coinGeckoPrice = CoinGeckoPrice.fromMap(priceData)
                    val cached = CachedPrice(
                        livePrice = coinGeckoPrice.usd,
                        change24h = coinGeckoPrice.usd_24h_change
                    )
                    priceCache[id] = cached
                    results[id] = cached
                }
                Result.success(results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ─────────────────────────────────────────────
    // FINNHUB (Stocks & ETFs) - via proxy with Yahoo Finance fallback
    // ─────────────────────────────────────────────

    /**
     * Search for stocks and ETFs by query string.
     * Searches via Finnhub proxy with broad asset-class filtering (Common Stock, ETFs, ADRs, REITs),
     * and seamlessly falls back to a curated local dataset of top global stocks & ETFs.
     */
    suspend fun searchStocks(query: String): Result<List<FinnhubResult>> {
        return withContext(Dispatchers.IO) {
            val cleanQuery = query.trim()
            if (cleanQuery.isEmpty()) return@withContext Result.success(emptyList())

            val fallbackMatches = defaultPopularStocksAndEtfs.filter { item ->
                item.symbol.contains(cleanQuery, ignoreCase = true) ||
                item.description.contains(cleanQuery, ignoreCase = true)
            }

            try {
                val body = mapOf(
                    "action" to "search",
                    "q" to cleanQuery
                )
                val requestBody = Json.encodeToString(body).toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(finnhubProxyUrl)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext if (fallbackMatches.isNotEmpty()) {
                        Result.success(fallbackMatches)
                    } else {
                        Result.failure(
                            Exception("Finnhub search failed: ${response.code}")
                        )
                    }
                }

                val responseBody = response.body?.string() ?: return@withContext if (fallbackMatches.isNotEmpty()) {
                    Result.success(fallbackMatches)
                } else {
                    Result.failure(Exception("Empty response from Finnhub proxy"))
                }

                val searchResponse = json.decodeFromString<FinnhubSearchResponse>(responseBody)
                // Filter for equities and ETFs (exclude crypto which has its own search tab)
                val apiResults = searchResponse.result.filter { result ->
                    result.symbol.isNotBlank() && !result.type.equals("Crypto", ignoreCase = true)
                }

                // Merge API results and fallback matches, deduplicated by ticker symbol
                val combined = (apiResults + fallbackMatches).distinctBy { it.symbol.uppercase() }
                val resultsToReturn = if (combined.isNotEmpty()) combined else fallbackMatches
                Result.success(resultsToReturn)
            } catch (e: Exception) {
                if (fallbackMatches.isNotEmpty()) {
                    Result.success(fallbackMatches)
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    /**
     * Fetch current quote for a single stock/ETF.
     * Returns price, change, and percent change.
     * Robust failover: Finnhub proxy -> Yahoo Finance chart API.
     */
    suspend fun getStockQuote(symbol: String): Result<CachedPrice> {
        return withContext(Dispatchers.IO) {
            try {
                // Check cache first
                val cached = priceCache[symbol]
                if (cached != null && cached.isFresh) {
                    return@withContext Result.success(cached)
                }

                val body = mapOf(
                    "action" to "quote",
                    "symbol" to symbol
                )
                val requestBody = Json.encodeToString(body).toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(finnhubProxyUrl)
                    .post(requestBody)
                    .build()

                val response = try { client.newCall(request).execute() } catch (_: Exception) { null }
                if (response != null && response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank()) {
                        try {
                            val quote = json.decodeFromString<FinnhubQuoteResponse>(responseBody)
                            val livePrice = if (quote.c > 0.0) quote.c else quote.pc
                            if (livePrice > 0.0) {
                                val result = CachedPrice(
                                    livePrice = livePrice,
                                    change24h = quote.dp ?: 0.0
                                )
                                priceCache[symbol] = result
                                return@withContext Result.success(result)
                            }
                        } catch (_: Exception) {
                            // Non-JSON or 429 HTML response from proxy
                        }
                    }
                }

                // Fallback to Yahoo Finance quote
                val yahooQuote = fetchYahooFinanceQuote(symbol)
                if (yahooQuote.isSuccess) {
                    return@withContext yahooQuote
                }

                // Return stale cache if available
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.failure(Exception("Could not fetch quote for $symbol"))
                }
            } catch (e: Exception) {
                val yahooQuote = fetchYahooFinanceQuote(symbol)
                if (yahooQuote.isSuccess) {
                    return@withContext yahooQuote
                }
                val cached = priceCache[symbol]
                if (cached != null) {
                    Result.success(cached)
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    suspend fun fetchYahooFinanceQuote(symbol: String): Result<CachedPrice> {
        return withContext(Dispatchers.IO) {
            try {
                val clean = symbol.trim().uppercase(Locale.US)
                val yahooSymbol = when (clean) {
                    "BTC", "BITCOIN" -> "BTC-USD"
                    "ETH", "ETHEREUM" -> "ETH-USD"
                    "SOL", "SOLANA" -> "SOL-USD"
                    "GOLD" -> "GC=F"
                    else -> clean
                }

                val url = "https://query1.finance.yahoo.com/v8/finance/chart/$yahooSymbol?interval=1d&range=1d"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:125.0) Gecko/125.0 Firefox/125.0")
                    .header("Accept", "application/json")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Yahoo Finance failed: ${response.code}"))
                }

                val responseBody = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val jsonElement = json.parseToJsonElement(responseBody)
                val chartObj = jsonElement.jsonObject["chart"]?.jsonObject
                val resultArr = chartObj?.get("result")?.jsonArray
                val metaObj = resultArr?.firstOrNull()?.jsonObject?.get("meta")?.jsonObject
                    ?: return@withContext Result.failure(Exception("No meta object in Yahoo Finance response"))

                val regularPrice = metaObj["regularMarketPrice"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val previousClose = metaObj["previousClose"]?.jsonPrimitive?.doubleOrNull
                    ?: metaObj["chartPreviousClose"]?.jsonPrimitive?.doubleOrNull
                    ?: regularPrice

                if (regularPrice <= 0.0) {
                    return@withContext Result.failure(Exception("Invalid price from Yahoo Finance: $regularPrice"))
                }

                val change24h = if (previousClose > 0.0) {
                    ((regularPrice - previousClose) / previousClose) * 100.0
                } else 0.0

                val cached = CachedPrice(livePrice = regularPrice, change24h = change24h)
                priceCache[symbol] = cached
                priceCache[clean] = cached
                Result.success(cached)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Fetch current prices for multiple stocks.
     * Queries each stock individually (Finnhub doesn't support batch quotes).
     */
    suspend fun getStockPrices(symbols: List<String>): Result<Map<String, CachedPrice>> {
        val results = mutableMapOf<String, CachedPrice>()
        val errors = mutableListOf<String>()

        for (symbol in symbols) {
            getStockQuote(symbol)
                .onSuccess { results[symbol] = it }
                .onFailure { errors.add(symbol) }
        }

        return if (results.isNotEmpty()) {
            Result.success(results)
        } else {
            Result.failure(Exception("Failed to fetch prices for: ${errors.joinToString(", ")}"))
        }
    }

    // ─────────────────────────────────────────────
    // FINNHUB NEWS - via proxy
    // ─────────────────────────────────────────────

    /**
     * Fetch general market and crypto news.
     * Tries Finnhub proxy first, and gracefully falls back to live CoinTelegraph (crypto) and Yahoo Finance (markets) RSS feeds.
     */
    suspend fun getMarketNews(): Result<List<FinnhubNewsResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val body = mapOf(
                    "action" to "news",
                    "category" to "general"
                )
                val requestBody = Json.encodeToString(body).toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(finnhubProxyUrl)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    if (responseBody.trim().startsWith("[")) {
                        val news = json.decodeFromString<List<FinnhubNewsResponse>>(responseBody)
                        if (news.isNotEmpty()) {
                            return@withContext Result.success(news.take(20))
                        }
                    }
                }
                // Fallback to live RSS feeds if proxy failed, returned non-array, or returned empty
                fetchRssMarketNews()
            } catch (e: Exception) {
                fetchRssMarketNews()
            }
        }
    }

    /**
     * Fallback to fetch live crypto and financial market news from CoinTelegraph and Yahoo Finance RSS.
     */
    private fun fetchRssMarketNews(): Result<List<FinnhubNewsResponse>> {
        val newsList = mutableListOf<FinnhubNewsResponse>()
        val now = System.currentTimeMillis() / 1000

        // 1. CoinTelegraph for Crypto News
        try {
            val cryptoReq = Request.Builder()
                .url("https://cointelegraph.com/rss")
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()
            val res = client.newCall(cryptoReq).execute()
            if (res.isSuccessful) {
                val xml = res.body?.string() ?: ""
                val items = parseRssItems(xml, "CoinTelegraph", "crypto", now)
                newsList.addAll(items.take(8))
            }
        } catch (_: Exception) {}

        // 2. Yahoo Finance for General Financial News
        try {
            val stockReq = Request.Builder()
                .url("https://finance.yahoo.com/news/rssindex")
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()
            val res = client.newCall(stockReq).execute()
            if (res.isSuccessful) {
                val xml = res.body?.string() ?: ""
                val items = parseRssItems(xml, "Yahoo Finance", "general", now)
                newsList.addAll(items.take(8))
            }
        } catch (_: Exception) {}

        return if (newsList.isNotEmpty()) {
            Result.success(newsList)
        } else {
            Result.failure(Exception("All news feeds unavailable"))
        }
    }

    private fun parseRssItems(xml: String, sourceName: String, category: String, timestamp: Long): List<FinnhubNewsResponse> {
        val result = mutableListOf<FinnhubNewsResponse>()
        val itemRegex = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
        val titleRegex = Regex("<title>(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?</title>", RegexOption.DOT_MATCHES_ALL)
        val descRegex = Regex("<description>(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?</description>", RegexOption.DOT_MATCHES_ALL)
        val linkRegex = Regex("<link>(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?</link>", RegexOption.DOT_MATCHES_ALL)

        val itemMatches = itemRegex.findAll(xml)
        for (match in itemMatches) {
            val itemContent = match.groupValues[1]
            val titleMatch = titleRegex.find(itemContent)
            val descMatch = descRegex.find(itemContent)
            val linkMatch = linkRegex.find(itemContent)

            val rawTitle = titleMatch?.groupValues?.get(1)?.trim() ?: ""
            val rawDesc = descMatch?.groupValues?.get(1)?.replace(Regex("<.*?>"), "")?.trim() ?: ""
            val link = linkMatch?.groupValues?.get(1)?.trim() ?: ""

            if (rawTitle.isNotBlank()) {
                val cleanTitle = rawTitle.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
                val cleanDesc = rawDesc.replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").take(250)
                result.add(
                    FinnhubNewsResponse(
                        id = (cleanTitle.hashCode().toLong() and 0x7fffffffL),
                        category = category,
                        datetime = timestamp,
                        headline = cleanTitle,
                        source = sourceName,
                        summary = cleanDesc,
                        url = link
                    )
                )
            }
        }
        return result
    }

    /**
     * Clear the price cache (useful for manual refresh).
     */
    fun clearCache() {
        priceCache.clear()
    }

    /**
     * Get the last time prices were fetched for a given symbol.
     */
    fun getLastFetchTime(symbol: String): Long? {
        return priceCache[symbol]?.timestamp
    }

    /**
     * Fetches the market USD to MMK exchange rate (black market/trading rate).
     * @return The exchange rate as a Double (e.g., 4480.0), or null if the request fails.
     */
    suspend fun getUsdToMmkExchangeRate(): Double? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://myanmar-currency-api.github.io/api/latest.json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                
                val bodyStr = response.body?.string() ?: return@withContext null
                val result = json.decodeFromString<com.savingcoach.app.data.model.MarketExchangeRateResponse>(bodyStr)
                
                val usdData = result.data.find { it.currency == "USD" } ?: return@withContext null
                // We'll use the "buy" rate as the baseline market rate
                return@withContext usdData.buy.replace(",", "").toDoubleOrNull()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
