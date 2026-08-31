package com.savingcoach.app

import com.savingcoach.app.data.model.FinnhubQuoteResponse
import com.savingcoach.app.data.model.FinnhubResult
import com.savingcoach.app.data.model.FinnhubSearchResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MarketApiSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun testFinnhubSearchResponseWithEtfAndStock() {
        val searchJson = """
            {
                "count": 3,
                "result": [
                    {
                        "description": "APPLE INC",
                        "displaySymbol": "AAPL",
                        "symbol": "AAPL",
                        "type": "Common Stock"
                    },
                    {
                        "description": "VANGUARD S&P 500 ETF",
                        "displaySymbol": "VOO",
                        "symbol": "VOO",
                        "type": "ETP"
                    },
                    {
                        "description": "TAIWAN SEMICONDUCTOR MANUFACTURING CO.",
                        "displaySymbol": "TSM",
                        "symbol": "TSM",
                        "type": "ADR"
                    }
                ]
            }
        """.trimIndent()

        val parsed = json.decodeFromString<FinnhubSearchResponse>(searchJson)
        assertEquals(3, parsed.result.size)

        val filtered = parsed.result.filter { result ->
            result.symbol.isNotBlank() && !result.type.equals("Crypto", ignoreCase = true)
        }
        assertEquals(3, filtered.size)
    }

    @Test
    fun testFinnhubQuoteWithNulls() {
        val quoteJson = """
            {
                "c": 0,
                "d": null,
                "dp": null,
                "h": 0,
                "l": 0,
                "o": 0,
                "pc": 185.5,
                "t": 0
            }
        """.trimIndent()

        val quote = json.decodeFromString<FinnhubQuoteResponse>(quoteJson)
        assertNotNull(quote)
        val livePrice = if (quote.c > 0.0) quote.c else quote.pc
        assertEquals(185.5, livePrice, 0.001)
        assertEquals(0.0, quote.dp ?: 0.0, 0.001)
    }
}
