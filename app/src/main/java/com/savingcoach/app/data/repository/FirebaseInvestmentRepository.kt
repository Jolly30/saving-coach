package com.savingcoach.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.savingcoach.app.data.model.UserHolding
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of InvestmentRepository.
 * Stores holdings under users/{userId}/investments/{holdingId}.
 */
@Singleton
class FirebaseInvestmentRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : InvestmentRepository {

    private fun holdingsCollection(userId: String) =
        firestore.collection("users").document(userId).collection("investments")

    override fun getHoldings(userId: String): Flow<List<UserHolding>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = holdingsCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val holdings = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(UserHolding::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(holdings)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun getHolding(userId: String, holdingId: String): UserHolding? {
        return holdingsCollection(userId).document(holdingId)
            .get().await()
            .toObject(UserHolding::class.java)?.copy(id = holdingId)
    }

    override suspend fun addHolding(userId: String, holding: UserHolding): String {
        val id = holding.id.ifEmpty { UUID.randomUUID().toString() }
        val holdingWithId = holding.copy(id = id)
        holdingsCollection(userId).document(id).set(holdingWithId).await()
        return id
    }

    override suspend fun updateHolding(userId: String, holding: UserHolding) {
        holdingsCollection(userId).document(holding.id)
            .set(holding, SetOptions.merge()).await()
    }

    override suspend fun deleteHolding(userId: String, holdingId: String) {
        holdingsCollection(userId).document(holdingId).delete().await()
    }

    override suspend fun getHoldingsOnce(userId: String): List<UserHolding> {
        return holdingsCollection(userId).get().await()
            .documents.mapNotNull { doc ->
                doc.toObject(UserHolding::class.java)?.copy(id = doc.id)
            }
    }
}
