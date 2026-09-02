package com.savingcoach.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.SavingsDeposit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Firestore implementation of [SavingChallengeRepository].
 *
 * Firestore paths:
 *   users/{userId}/challenges/{challengeId}
 *   users/{userId}/challenges/{challengeId}/deposits/{depositId}
 */
@Singleton
class FirebaseSavingChallengeRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : SavingChallengeRepository {

    private fun challengesCol(userId: String) =
        firestore.collection("users").document(userId).collection("challenges")

    private fun depositsCol(userId: String, challengeId: String) =
        challengesCol(userId).document(challengeId).collection("deposits")

    override fun getActiveChallenges(userId: String): Flow<List<SavingChallenge>> =
        callbackFlow {
            if (userId.isBlank()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listener = challengesCol(userId)
                .whereEqualTo("isActive", true)
                .whereEqualTo("isCompleted", false)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val challenges = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(SavingChallenge::class.java)?.copy(id = doc.id)
                    }?.sortedByDescending { it.createdAt } ?: emptyList()
                    trySend(challenges)
                }
            awaitClose { listener.remove() }
        }

    override fun getAllChallenges(userId: String): Flow<List<SavingChallenge>> =
        callbackFlow {
            if (userId.isBlank()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listener = challengesCol(userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val challenges = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(SavingChallenge::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    trySend(challenges)
                }
            awaitClose { listener.remove() }
        }

    override fun getDeposits(userId: String, challengeId: String): Flow<List<SavingsDeposit>> =
        callbackFlow {
            if (userId.isBlank() || challengeId.isBlank()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listener = depositsCol(userId, challengeId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val deposits = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(SavingsDeposit::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    trySend(deposits)
                }
            awaitClose { listener.remove() }
        }

    override suspend fun createChallenge(challenge: SavingChallenge): String {
        val id = if (challenge.id.isNotBlank()) challenge.id else challengesCol(challenge.userId).document().id
        challengesCol(challenge.userId).document(id).set(challenge.copy(id = id)).await()
        return id
    }

    override suspend fun addDeposit(userId: String, challengeId: String, deposit: SavingsDeposit) {
        // Add deposit to subcollection
        val docRef = depositsCol(userId, challengeId).document()
        docRef.set(deposit.copy(id = docRef.id, challengeId = challengeId)).await()

        // Update challenge's currentAmount
        challengesCol(userId).document(challengeId)
            .update(
                mapOf(
                    "currentAmount" to com.google.firebase.firestore.FieldValue.increment(deposit.amount),
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
    }

    override suspend fun deleteDeposit(userId: String, challengeId: String, depositId: String) {
        depositsCol(userId, challengeId).document(depositId).delete().await()
    }

    override suspend fun completeChallenge(userId: String, challengeId: String) {
        challengesCol(userId).document(challengeId)
            .update(
                mapOf(
                    "isCompleted" to true,
                    "isActive" to false
                )
            ).await()
    }

    override suspend fun deleteChallenge(userId: String, challengeId: String) {
        // Delete all deposits in subcollection first
        val deposits = depositsCol(userId, challengeId).get().await()
        for (doc in deposits.documents) {
            doc.reference.delete().await()
        }
        // Delete the challenge document
        challengesCol(userId).document(challengeId).delete().await()
    }

    override suspend fun initializeDefaultChallengesIfNeeded(userId: String, defaultChallenges: List<SavingChallenge>) {
        val userDocRef = firestore.collection("users").document(userId)
        val userDoc = userDocRef.get().await()
        
        if (userDoc.exists() && userDoc.getBoolean("hasInitializedDefaults") == true) {
            return
        }

        // Use a batch to insert all default challenges
        val batch = firestore.batch()
        for (challenge in defaultChallenges) {
            val id = challengesCol(userId).document().id
            val challengeRef = challengesCol(userId).document(id)
            batch.set(challengeRef, challenge.copy(id = id, userId = userId))
        }
        
        // Update user document
        batch.set(userDocRef, mapOf("hasInitializedDefaults" to true), com.google.firebase.firestore.SetOptions.merge())
        
        batch.commit().await()
    }
}
