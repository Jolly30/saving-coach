package com.savingcoach.app

import com.savingcoach.app.data.model.MarketExchangeRateResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiTest {

    @Test
    fun testMarketApi() = runBlocking {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://myanmar-currency-api.github.io/api/latest.json")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertTrue(response.isSuccessful)
            val bodyStr = response.body?.string()
            assertNotNull(bodyStr)
            
            val json = Json { ignoreUnknownKeys = true }
            val result = json.decodeFromString<MarketExchangeRateResponse>(bodyStr!!)
            
            val usdData = result.data.find { it.currency == "USD" }
            assertNotNull(usdData)
            assertTrue(usdData!!.buy.isNotEmpty())
        }
    }
}
