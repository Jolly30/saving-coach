package com.savingcoach.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.savingcoach.app.data.model.Expense
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real Firestore implementation of [ExpenseRepository].
 *
 * Uses a top-level "expenses" collection with a "userId" field on each document
 * so that delete/query by expenseId alone is possible without knowing the userId path.
 *
 * Firestore security rules should enforce:
 *   match /expenses/{expenseId} {
 *     allow read, write: if request.auth.uid == resource.data.userId;
 *   }
 */
@Singleton
class FirebaseExpenseRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ExpenseRepository {

    private val expensesCol get() = firestore.collection("expenses")

    override fun getExpensesForMonth(userId: String, yearMonth: String): Flow<List<Expense>> =
        callbackFlow {
            val listener = expensesCol
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", "$yearMonth-01")
                .whereLessThanOrEqualTo("date", "$yearMonth-31")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val expenses = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Expense::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    trySend(expenses)
                }
            awaitClose { listener.remove() }
        }

    override fun getExpensesForDate(userId: String, date: String): Flow<List<Expense>> =
        callbackFlow {
            val listener = expensesCol
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", date)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val expenses = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Expense::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    trySend(expenses)
                }
            awaitClose { listener.remove() }
        }

    override fun getAllExpenses(userId: String): Flow<List<Expense>> =
        callbackFlow {
            val listener = expensesCol
                .whereEqualTo("userId", userId)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val expenses = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Expense::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    trySend(expenses)
                }
            awaitClose { listener.remove() }
        }

    override suspend fun addExpense(expense: Expense): String {
        val docRef = expensesCol.document()
        docRef.set(expense.copy(id = docRef.id)).await()
        return docRef.id
    }

    override suspend fun updateExpense(expense: Expense) {
        expensesCol.document(expense.id).set(expense).await()
    }

    override suspend fun deleteExpense(expenseId: String) {
        expensesCol.document(expenseId).delete().await()
    }
}
