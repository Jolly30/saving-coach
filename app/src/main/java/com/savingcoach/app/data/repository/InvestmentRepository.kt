package com.savingcoach.app.data.repository

import com.savingcoach.app.data.model.UserHolding
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for investment holdings management.
 * Implementations handle Firestore persistence and local caching.
 */
interface InvestmentRepository {
    /**
     * Get all holdings for the current user as a reactive Flow.
     */
    fun getHoldings(userId: String): Flow<List<UserHolding>>

    /**
     * Get a single holding by ID.
     */
    suspend fun getHolding(userId: String, holdingId: String): UserHolding?

    /**
     * Add a new holding to the portfolio.
     * Returns the generated holding ID.
     */
    suspend fun addHolding(userId: String, holding: UserHolding): String

    /**
     * Update an existing holding.
     */
    suspend fun updateHolding(userId: String, holding: UserHolding)

    /**
     * Delete a holding by ID.
     */
    suspend fun deleteHolding(userId: String, holdingId: String)

    /**
     * Get all holdings once (non-Flow) for batch operations.
     */
    suspend fun getHoldingsOnce(userId: String): List<UserHolding>
}
