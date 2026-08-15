package com.savingcoach.app.data.repository

import com.savingcoach.app.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChatHistory(userId: String): Flow<List<ChatMessage>>
    suspend fun saveMessage(userId: String, message: ChatMessage)
    suspend fun updateMessage(userId: String, message: ChatMessage)
    suspend fun sendToAi(userId: String, userMessage: String, systemPrompt: String? = null): Result<ChatMessage>
}
