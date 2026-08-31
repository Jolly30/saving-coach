package com.savingcoach.app.data.repository

import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.SavingsDeposit
import kotlinx.coroutines.flow.Flow

interface SavingChallengeRepository {
    fun getActiveChallenges(userId: String): Flow<List<SavingChallenge>>
    fun getAllChallenges(userId: String): Flow<List<SavingChallenge>>
    fun getDeposits(userId: String, challengeId: String): Flow<List<SavingsDeposit>>
    suspend fun createChallenge(challenge: SavingChallenge): String
    suspend fun addDeposit(userId: String, challengeId: String, deposit: SavingsDeposit)
    suspend fun deleteDeposit(userId: String, challengeId: String, depositId: String)
    suspend fun completeChallenge(userId: String, challengeId: String)
    suspend fun deleteChallenge(userId: String, challengeId: String)
    suspend fun initializeDefaultChallengesIfNeeded(userId: String, defaultChallenges: List<SavingChallenge>)
}
