package com.savingcoach.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.savingcoach.app.data.model.ExpenseCategoryEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of [ExpenseCategoryRepository].
 *
 * Stores per-user, per-month category configurations nested under the "budgets" subcollection
 * to comply with existing write security rules:
 *   users/{userId}/budgets/{YYYY-MM}
 */
@Singleton
class FirebaseExpenseCategoryRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ExpenseCategoryRepository {

    private fun budgetsDoc(userId: String, yearMonth: String) =
        firestore.collection("users")
            .document(userId)
            .collection("budgets")
            .document(yearMonth)

    override fun getCategories(userId: String, yearMonth: String): Flow<List<ExpenseCategoryEntity>> =
        callbackFlow {
            if (userId.isEmpty()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listener = budgetsDoc(userId, yearMonth)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    @Suppress("UNCHECKED_CAST")
                    val rawList = snapshot.get("categories") as? List<Map<String, Any>>
                    val categories = rawList?.mapNotNull { map ->
                        val name = map["name"] as? String ?: return@mapNotNull null
                        ExpenseCategoryEntity(
                            emoji = map["emoji"] as? String ?: "🏷️",
                            name = name,
                            target = (map["target"] as? Number)?.toDouble() ?: 0.0,
                            isCustom = map["isCustom"] as? Boolean ?: false
                        )
                    } ?: emptyList()
                    trySend(categories)
                }
            awaitClose { listener.remove() }
        }

    override fun getDeletedCategoryNames(userId: String, yearMonth: String): Flow<Set<String>> =
        callbackFlow {
            if (userId.isEmpty()) {
                trySend(emptySet())
                close()
                return@callbackFlow
            }
            val listener = budgetsDoc(userId, yearMonth)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) {
                        trySend(emptySet())
                        return@addSnapshotListener
                    }
                    @Suppress("UNCHECKED_CAST")
                    val deletedList = snapshot.get("deletedCategories") as? List<String>
                    trySend(deletedList?.map { it.lowercase() }?.toSet() ?: emptySet())
                }
            awaitClose { listener.remove() }
        }

    override suspend fun saveCategories(
        userId: String,
        yearMonth: String,
        categories: List<ExpenseCategoryEntity>,
        deletedNames: Set<String>
    ) {
        if (userId.isEmpty()) return
        val data = mapOf(
            "categories" to categories.map { cat ->
                mapOf(
                    "emoji" to cat.emoji,
                    "name" to cat.name,
                    "target" to cat.target,
                    "isCustom" to cat.isCustom
                )
            },
            "deletedCategories" to deletedNames.toList(),
            "updatedAt" to System.currentTimeMillis()
        )
        val docRef = budgetsDoc(userId, yearMonth)
        val doc = docRef.get().await()
        if (doc.exists()) {
            docRef.update(data).await()
        } else {
            val fullData = data + mapOf(
                "id" to yearMonth,
                "userId" to userId,
                "month" to yearMonth,
                "limit" to 0.0,
                "createdAt" to System.currentTimeMillis()
            )
            docRef.set(fullData).await()
        }
    }
}
