package com.savingcoach.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savingcoach.app.ai.AiChatRepository
import com.savingcoach.app.data.model.ChatMessage
import com.savingcoach.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: AiChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val userId: String
        get() = authRepository.getCurrentUserId() ?: "anonymous"

    init {
        viewModelScope.launch {
            chatRepository.getChatHistory(userId).collect { history ->
                _messages.value = history
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _isLoading.value) return

        val userMessage = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            userId = userId,
            role = "user",
            content = content.trim(),
            timestamp = System.currentTimeMillis(),
            type = "query"
        )

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Save user message
            chatRepository.saveMessage(userId, userMessage)

            // Get AI response
            chatRepository.sendToAi(userId, content)
                .onSuccess { aiMessage ->
                    chatRepository.saveMessage(userId, aiMessage)
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Failed to get AI response"
                }

            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
