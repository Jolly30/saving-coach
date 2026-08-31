package com.savingcoach.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savingcoach.app.data.model.NotificationItem
import com.savingcoach.app.data.repository.AuthRepository
import com.savingcoach.app.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val userId = authRepository.getCurrentUserId() ?: ""

    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<NotificationsUiState> = if (userId.isEmpty()) {
        flowOf(NotificationsUiState())
    } else {
        repository.getNotifications(userId)
            .map { list -> NotificationsUiState(notifications = list) }
            .catch { e -> emit(NotificationsUiState(errorMessage = e.message)) }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotificationsUiState(isLoading = true)
        )

    fun markAsRead(notificationId: String) {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            try {
                repository.markAsRead(userId, notificationId)
            } catch (e: Exception) {
                // Handle or log error
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            try {
                repository.deleteNotification(userId, notificationId)
            } catch (e: Exception) {
                // Handle or log error
            }
        }
    }

    fun deleteNotifications(notificationIds: Set<String>) {
        if (userId.isEmpty() || notificationIds.isEmpty()) return
        viewModelScope.launch {
            try {
                repository.deleteNotifications(userId, notificationIds.toList())
            } catch (e: Exception) {
                // Handle or log error
            }
        }
    }

    fun clearAll() {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            try {
                repository.clearAllNotifications(userId)
            } catch (e: Exception) {
                // Handle or log error
            }
        }
    }
}
