package com.savingcoach.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CbmExchangeRateResponse(
    val info: String,
    val description: String,
    val timestamp: String,
    val rates: Map<String, String>
)
