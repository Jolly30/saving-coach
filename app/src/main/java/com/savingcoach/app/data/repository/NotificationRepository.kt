package com.savingcoach.app.data.repository

import com.savingcoach.app.data.model.NotificationItem
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(userId: String): Flow<List<NotificationItem>>
    suspend fun addNotification(userId: String, notification: NotificationItem): String
    suspend fun markAsRead(userId: String, notificationId: String)
    suspend fun deleteNotification(userId: String, notificationId: String)
    suspend fun deleteNotifications(userId: String, notificationIds: List<String>)
    suspend fun clearAllNotifications(userId: String)
}
