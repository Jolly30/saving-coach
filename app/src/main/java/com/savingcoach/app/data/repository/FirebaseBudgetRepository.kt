package com.savingcoach.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.savingcoach.app.data.model.Budget
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Firestore implementation of [BudgetRepository].
 *
 * Firestore path: users/{userId}/budgets/{YYYY-MM}
 * Document ID is the yearMonth string (e.g. "2026-07") for easy lookup.
 */
@Singleton
class FirebaseBudgetRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : BudgetRepository {

    private fun budgetsCol(userId: String) =
        firestore.collection("users").document(userId).collection("budgets")

    override fun getBudget(userId: String, yearMonth: String): Flow<Budget?> =
        callbackFlow {
            val listener = budgetsCol(userId).document(yearMonth)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val budget = snapshot?.toObject(Budget::class.java)?.copy(id = snapshot.id)
                    trySend(budget)
                }
            awaitClose { listener.remove() }
        }

    override suspend fun setBudget(userId: String, budget: Budget) {
        val docId = budget.month.ifEmpty { System.currentTimeMillis().toString() }
        budgetsCol(userId).document(docId)
            .set(budget.copy(id = docId, userId = userId)).await()
    }

    override suspend fun updateLimit(userId: String, yearMonth: String, newLimit: Double) {
        budgetsCol(userId).document(yearMonth)
            .update("limit", newLimit, "updatedAt", System.currentTimeMillis()).await()
    }
}
