package com.savingcoach.app.ai

import com.savingcoach.app.data.model.ChatMessage
import com.savingcoach.app.data.repository.ChatRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import com.savingcoach.app.data.model.ParsedExpense

@Singleton
class AiChatRepository @Inject constructor(
    private val proxyService: GeminiProxyService,
    private val firestore: FirebaseFirestore
) : ChatRepository {

    private val localHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    private var hasLoadedFromFirestore = false

    override fun getChatHistory(userId: String): Flow<List<ChatMessage>> {
        // Load from Firestore once
        if (!hasLoadedFromFirestore) {
            hasLoadedFromFirestore = true
            loadFromFirestore(userId)
        }
        return localHistory.map { messages ->
            messages.filter { it.userId == userId }
                .sortedBy { it.timestamp }
        }
    }

    private fun loadFromFirestore(userId: String) {
        // Load in background - will update localHistory when complete
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("chatMessages")
                    .orderBy("timestamp")
                    .get()
                    .await()

                val messages = snapshot.toObjects(ChatMessage::class.java)
                localHistory.value = messages
            } catch (_: Exception) {
                // Firestore load failed, start with empty history
            }
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

    override suspend fun updateMessage(userId: String, message: ChatMessage) {
        // Update local state
        localHistory.value = localHistory.value.map { if (it.id == message.id) message else it }

        // Update in Firestore
        try {
            val querySnapshot = firestore.collection("users")
                .document(userId)
                .collection("chatMessages")
                .whereEqualTo("id", message.id)
                .get()
                .await()
                
            for (doc in querySnapshot.documents) {
                doc.reference.set(message).await()
            }
        } catch (_: Exception) {
            // Firestore update failed, but local state is updated
        }
    }

    /**
     * Send a message to the AI and get a response.
     * Returns the AI's reply as a ChatMessage.
     */
    override suspend fun sendToAi(
        userId: String,
        userMessage: String,
        systemPrompt: String?
    ): Result<ChatMessage> {
        // Build conversation context (last 20 messages)
        val history = localHistory.value
            .filter { it.userId == userId && !it.content.contains("{") }
            .sortedBy { it.timestamp }
            .takeLast(20)

        val messagesWithPrompt = if (!systemPrompt.isNullOrBlank()) {
            listOf(
                ChatMessage(
                    id = "system_${System.currentTimeMillis()}",
                    userId = userId,
                    role = "user", // Pass as user because proxy expects user/ai
                    content = systemPrompt,
                    timestamp = 0L,
                    type = "system"
                )
            ) + history
        } else {
            history
        }

        val recentMessages = messagesWithPrompt.plus(
            ChatMessage(
                id = "temp_${System.currentTimeMillis()}",
                userId = userId,
                role = "user",
                content = userMessage,
                timestamp = System.currentTimeMillis()
            )
        )

        return proxyService.chat(recentMessages).map { reply ->
            var finalReply = reply
            var parsedExpense: ParsedExpense? = null
            var msgType = "advice"

            val expenseRegex = "\\[EXPENSE_DATA\\](.*?)\\[/EXPENSE_DATA\\]".toRegex(RegexOption.DOT_MATCHES_ALL)
            val match = expenseRegex.find(reply)
            if (match != null) {
                val jsonStr = match.groupValues[1].trim()
                try {
                    parsedExpense = Json { ignoreUnknownKeys = true }.decodeFromString<ParsedExpense>(jsonStr)
                    val hasMyanmarText = userMessage.contains(Regex("[\\u1000-\\u109F]"))
                    val detectedLang = if (hasMyanmarText) "my" else "en"
                    parsedExpense = parsedExpense?.copy(language = detectedLang)
                    finalReply = reply.replace(match.value, "").trim()
                    msgType = "expense"
                } catch (e: Exception) {
                    // Ignore parse error
                }
            }

            ChatMessage(
                id = "ai_${System.currentTimeMillis()}",
                userId = userId,
                role = "ai",
                content = finalReply,
                timestamp = System.currentTimeMillis(),
                type = msgType,
                parsedExpense = parsedExpense
            )
        }
    }
}
