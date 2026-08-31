package com.savingcoach.app.data.mock

import com.savingcoach.app.data.model.UserHolding
import com.savingcoach.app.data.repository.InvestmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock implementation of InvestmentRepository for development.
 * Uses in-memory storage with sample data.
 */
@Singleton
class MockInvestmentRepository @Inject constructor() : InvestmentRepository {

    private val holdings = MutableStateFlow<Map<String, UserHolding>>(emptyMap())

    init {
        // Pre-populate with sample holdings for development
        val sampleHoldings = listOf(
            UserHolding(
                id = UUID.randomUUID().toString(),
                type = "stock",
                symbol = "AAPL",
                displayTicker = "AAPL",
                name = "Apple Inc.",
                units = 15.0,
                buyPrice = 180.0
            ),
            UserHolding(
                id = UUID.randomUUID().toString(),
                type = "stock",
                symbol = "VOO",
                displayTicker = "VOO",
                name = "Vanguard S&P 500 ETF",
                units = 8.0,
                buyPrice = 480.0
            ),
            UserHolding(
                id = UUID.randomUUID().toString(),
                type = "crypto",
                symbol = "bitcoin",
                displayTicker = "BTC",
                name = "Bitcoin",
                units = 0.05,
                buyPrice = 50000.0
            ),
            UserHolding(
                id = UUID.randomUUID().toString(),
                type = "stock",
                symbol = "TSLA",
                displayTicker = "TSLA",
                name = "Tesla, Inc.",
                units = 10.0,
                buyPrice = 210.0
            ),
            UserHolding(
                id = UUID.randomUUID().toString(),
                type = "crypto",
                symbol = "solana",
                displayTicker = "SOL",
                name = "Solana",
                units = 6.5,
                buyPrice = 120.0
            )
        )

        holdings.value = sampleHoldings.associateBy { it.id }
    }

    override fun getHoldings(userId: String): Flow<List<UserHolding>> {
        return holdings.map { it.values.toList() }
    }

    override suspend fun getHolding(userId: String, holdingId: String): UserHolding? {
        return holdings.value[holdingId]
    }

    override suspend fun addHolding(userId: String, holding: UserHolding): String {
        val id = holding.id.ifEmpty { UUID.randomUUID().toString() }
        val newHolding = holding.copy(id = id)
        holdings.value = holdings.value + (id to newHolding)
        return id
    }

    override suspend fun updateHolding(userId: String, holding: UserHolding) {
        holdings.value = holdings.value + (holding.id to holding)
    }

    override suspend fun deleteHolding(userId: String, holdingId: String) {
        holdings.value = holdings.value - holdingId
    }

    override suspend fun getHoldingsOnce(userId: String): List<UserHolding> {
        return holdings.value.values.toList()
    }
}
