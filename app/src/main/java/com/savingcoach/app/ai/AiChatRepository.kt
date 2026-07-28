package com.savingcoach.app.ai

import com.savingcoach.app.data.model.ChatMessage
import com.savingcoach.app.data.repository.ChatRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiChatRepository @Inject constructor(
    private val proxyService: GeminiProxyService,
    private val firestore: FirebaseFirestore
) : ChatRepository {

    private val localHistory = MutableStateFlow<List<ChatMessage>>(emptyList())

    override fun getChatHistory(userId: String): Flow<List<ChatMessage>> {
        return localHistory.map { messages ->
            messages.filter { it.userId == userId }
                .sortedBy { it.timestamp }
        }
    }

    override suspend fun saveMessage(userId: String, message: ChatMessage) {
        // Save to local state
        localHistory.value = localHistory.value + message

        // Save to Firestore
        try {
            firestore.collection("users")
                .document(userId)
                .collection("chatMessages")
                .add(message.copy(userId = userId))
                .await()
        } catch (_: Exception) {
            // Firestore save failed, but local state is updated
        }
    }

    /**
     * Send a message to the AI and get a response.
     * Returns the AI's reply as a ChatMessage.
     */
    suspend fun sendToAi(
        userId: String,
        userMessage: String,
        systemPrompt: String? = null
    ): Result<ChatMessage> {
        // Build conversation context (last 20 messages)
        val recentMessages = localHistory.value
            .filter { it.userId == userId }
            .sortedBy { it.timestamp }
            .takeLast(20)
            .plus(
                ChatMessage(
                    id = "temp_${System.currentTimeMillis()}",
                    userId = userId,
                    role = "user",
                    content = userMessage,
                    timestamp = System.currentTimeMillis()
                )
            )

        return proxyService.chat(recentMessages).map { reply ->
            ChatMessage(
                id = "ai_${System.currentTimeMillis()}",
                userId = userId,
                role = "ai",
                content = reply,
                timestamp = System.currentTimeMillis(),
                type = "advice"
            )
        }
    }
}
