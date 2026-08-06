package com.savingcoach.app.data.repository

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
 */
@Singleton
class FirebaseExpenseRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ExpenseRepository {

    private val expensesCol get() = firestore.collection("expenses")

    override fun getExpensesForMonth(userId: String, yearMonth: String): Flow<List<Expense>> =
        callbackFlow {
            val query = if (userId.isNotEmpty()) {
                expensesCol.whereEqualTo("userId", userId)
            } else {
                expensesCol
            }
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Expense::class.java)?.copy(id = doc.id)
                }?.filter { 
                    it.date.isEmpty() || it.date.startsWith(yearMonth)
                }?.sortedByDescending { 
                    if (it.createdAt > 0) it.createdAt else System.currentTimeMillis()
                } ?: emptyList()
                trySend(expenses)
            }
            awaitClose { listener.remove() }
        }

    override fun getExpensesForDate(userId: String, date: String): Flow<List<Expense>> =
        callbackFlow {
            val query = if (userId.isNotEmpty()) {
                expensesCol.whereEqualTo("userId", userId)
            } else {
                expensesCol
            }
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Expense::class.java)?.copy(id = doc.id)
                }?.filter { it.date == date }
                ?.sortedByDescending { it.createdAt }
                ?: emptyList()
                trySend(expenses)
            }
            awaitClose { listener.remove() }
        }

    override fun getAllExpenses(userId: String): Flow<List<Expense>> =
        callbackFlow {
            val query = if (userId.isNotEmpty()) {
                expensesCol.whereEqualTo("userId", userId)
            } else {
                expensesCol
            }
            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val expenses = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Expense::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt }
                ?: emptyList()
                trySend(expenses)
            }
            awaitClose { listener.remove() }
        }

    override suspend fun addExpense(expense: Expense): String {
        val docRef = if (expense.id.isNotEmpty()) expensesCol.document(expense.id) else expensesCol.document()
        val docId = docRef.id
        docRef.set(expense.copy(id = docId)).await()
        return docId
    }

    override suspend fun updateExpense(expense: Expense) {
        expensesCol.document(expense.id).set(expense).await()
    }

    override suspend fun deleteExpense(expenseId: String) {
        expensesCol.document(expenseId).delete().await()
    }
}
