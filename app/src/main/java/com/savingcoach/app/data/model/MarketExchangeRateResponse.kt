package com.savingcoach.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MarketExchangeRateResponse(
    val data: List<MarketCurrencyRate>
)

@Serializable
data class MarketCurrencyRate(
    val currency: String,
    val buy: String,
    val sell: String
)
