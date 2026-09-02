package com.savingcoach.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.savingcoach.app.data.model.NotificationItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of [NotificationRepository].
 *
 * Path: users/{userId}/notifications/{notificationId}
 */
@Singleton
class FirebaseNotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : NotificationRepository {

    private fun notificationsCol(userId: String) =
        firestore.collection("users").document(userId).collection("notifications")

    override fun getNotifications(userId: String): Flow<List<NotificationItem>> =
        callbackFlow {
            if (userId.isBlank()) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }
            val listener = notificationsCol(userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val items = snapshot?.documents?.mapNotNull { doc ->
                        val obj = doc.toObject(NotificationItem::class.java)?.copy(id = doc.id)
                        if (obj != null) {
                            val isRead = doc.getBoolean("isRead") ?: doc.getBoolean("read") ?: false
                            obj.copy(isRead = isRead)
                        } else null
                    } ?: emptyList()
                    trySend(items)
                }
            awaitClose { listener.remove() }
        }

    override suspend fun addNotification(userId: String, notification: NotificationItem): String {
        val docRef = notificationsCol(userId).document()
        val itemWithId = notification.copy(
            id = docRef.id,
            userId = userId,
            timestamp = if (notification.timestamp == 0L) System.currentTimeMillis() else notification.timestamp
        )
        docRef.set(itemWithId).await()
        return docRef.id
    }

    override suspend fun markAsRead(userId: String, notificationId: String) {
        notificationsCol(userId).document(notificationId)
            .update(mapOf("isRead" to true, "read" to true))
            .await()
    }

    override suspend fun deleteNotification(userId: String, notificationId: String) {
        notificationsCol(userId).document(notificationId).delete().await()
    }

    override suspend fun deleteNotifications(userId: String, notificationIds: List<String>) {
        if (notificationIds.isEmpty()) return
        val batch = firestore.batch()
        for (id in notificationIds) {
            batch.delete(notificationsCol(userId).document(id))
        }
        batch.commit().await()
    }

    override suspend fun clearAllNotifications(userId: String) {
        val querySnapshot = notificationsCol(userId).get().await()
        val batch = firestore.batch()
        for (doc in querySnapshot.documents) {
            batch.delete(doc.reference)
        }
        batch.commit().await()
    }
}
