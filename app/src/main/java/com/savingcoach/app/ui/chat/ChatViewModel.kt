package com.savingcoach.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savingcoach.app.ai.AiFinanceAssistant
import com.savingcoach.app.ai.SpeechRecognizerManager
import com.savingcoach.app.data.model.ChatMessage
import com.savingcoach.app.data.model.Expense
import com.savingcoach.app.data.repository.AuthRepository
import com.savingcoach.app.data.repository.ChatRepository
import com.savingcoach.app.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val aiFinanceAssistant: AiFinanceAssistant,
    private val speechRecognizerManager: SpeechRecognizerManager,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _savingExpenseMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val savingExpenseMessageIds: StateFlow<Set<String>> = _savingExpenseMessageIds.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val userId: String
        get() = authRepository.getCurrentUserId() ?: "anonymous"

    val isListening: StateFlow<Boolean> = speechRecognizerManager.isListening
    val partialVoiceText: StateFlow<String> = speechRecognizerManager.recognizedText
    val rmsDb: StateFlow<Float> = speechRecognizerManager.rmsDb

    /*val suggestions = listOf(
        "Budget Status",
        "Spending Analysis",
        "Saving Tips",
        "Monthly Report",
        "Food Expense",
        "Compare Expenses"
    )*/

    init {
        viewModelScope.launch {
            chatRepository.getChatHistory(userId).collect { history ->
                _messages.value = history
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank() || _isTyping.value) return

        val userMessage = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            userId = userId,
            role = "user",
            content = content.trim(),
            timestamp = System.currentTimeMillis(),
            type = "query"
        )

        viewModelScope.launch {
            _isTyping.value = true
            _error.value = null

            // Save user message
            chatRepository.saveMessage(userId, userMessage)

            // Get AI response via Assistant
            aiFinanceAssistant.getFinanceAdvice(userId, content)
                .onSuccess { aiMessage ->
                    chatRepository.saveMessage(userId, aiMessage)
                }
                .onFailure { e ->
                    _error.value = e.message ?: "Failed to get AI response"
                }

            _isTyping.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun startVoiceInput() {
        speechRecognizerManager.startListening()
    }

    fun stopVoiceInput() {
        speechRecognizerManager.stopListening()
        // The manager might take a moment to fire onResults, but we can append what we have
        appendVoiceText(speechRecognizerManager.recognizedText.value)
    }

    fun cancelVoiceInput() {
        speechRecognizerManager.cancelListening()
    }

    private fun appendVoiceText(transcription: String) {
        if (transcription.isNotBlank()) {
            val current = _inputText.value
            _inputText.value = if (current.isBlank()) transcription else "$current $transcription"
        }
    }

    fun saveParsedExpense(message: ChatMessage) {
        val parsed = message.parsedExpense ?: return
        if (message.expenseSaved || _savingExpenseMessageIds.value.contains(message.id)) return

        _savingExpenseMessageIds.value = _savingExpenseMessageIds.value + message.id

        viewModelScope.launch {
            try {
                val expense = Expense(
                    amount = parsed.amount,
                    category = parsed.category.ifBlank { "Other" },
                    merchant = parsed.merchant,
                    description = "Added via AI Chat",
                    date = parsed.date.ifBlank { java.time.LocalDate.now().toString() },
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    source = "chat",
                    currency = "MMK",
                    userId = userId
                )
                expenseRepository.addExpense(expense)
                val updatedMessage = message.copy(expenseSaved = true)
                chatRepository.updateMessage(userId, updatedMessage)
            } catch (e: Exception) {
                _error.value = "Failed to add expense: ${e.message}"
            } finally {
                _savingExpenseMessageIds.value = _savingExpenseMessageIds.value - message.id
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager.destroy()
    }
}
