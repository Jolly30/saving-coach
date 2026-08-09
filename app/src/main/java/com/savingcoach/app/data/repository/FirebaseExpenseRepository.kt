package com.savingcoach.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
 * Firestore path: users/{userId}/expenses/{expenseId}
 * Uses server-side date range filtering for month queries.
 */
@Singleton
class FirebaseExpenseRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ExpenseRepository {

    private fun expensesCol(userId: String) = firestore.collection("users").document(userId).collection("expenses")

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    override fun getExpensesForMonth(userId: String, yearMonth: String): Flow<List<Expense>> =
        callbackFlow {
            if (userId.isEmpty()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            // Server-side date range: "2026-08-01" <= date < "2026-09-01"
            // No orderBy on Firestore — sorted client-side to avoid composite index requirement
            val startDate = "$yearMonth-01"
            val parts = yearMonth.split("-")
            val year = parts[0].toIntOrNull() ?: 2026
            val month = parts.getOrElse(1) { "01" }.toIntOrNull() ?: 1
            val (endYear, endMonth) = if (month == 12) Pair(year + 1, 1) else Pair(year, month + 1)
            val endDate = "$endYear-${endMonth.toString().padStart(2, '0')}-01"

            val query = expensesCol(userId)
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThan("date", endDate)

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Expense::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(expenses)
            }
            awaitClose { listener.remove() }
        }

    override fun getExpensesForDate(userId: String, date: String): Flow<List<Expense>> =
        callbackFlow {
            if (userId.isEmpty()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val query = expensesCol(userId).whereEqualTo("date", date)
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Expense::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(expenses)
            }
            awaitClose { listener.remove() }
        }

    override fun getAllExpenses(userId: String): Flow<List<Expense>> =
        callbackFlow {
            if (userId.isEmpty()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val query = expensesCol(userId)
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Expense::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(expenses)
            }
            awaitClose { listener.remove() }
        }

    override suspend fun addExpense(expense: Expense): String {
        val uid = if (expense.userId.isNotEmpty()) expense.userId else currentUserId
        if (uid.isEmpty()) throw Exception("User not authenticated")

        val docRef = if (expense.id.isNotEmpty()) expensesCol(uid).document(expense.id) else expensesCol(uid).document()
        val docId = docRef.id
        docRef.set(expense.copy(id = docId, userId = uid)).await()
        return docId
    }

    override suspend fun updateExpense(expense: Expense) {
        val uid = if (expense.userId.isNotEmpty()) expense.userId else currentUserId
        if (uid.isEmpty()) throw Exception("User not authenticated")

        expensesCol(uid).document(expense.id).set(expense.copy(userId = uid)).await()
    }

    override suspend fun deleteExpense(expenseId: String) {
        val uid = currentUserId
        if (uid.isEmpty()) throw Exception("User not authenticated")

        expensesCol(uid).document(expenseId).delete().await()
    }
}
