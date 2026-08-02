package com.savingcoach.app.ai

import com.savingcoach.app.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ChatRequest(
    val messages: List<MessageDto>,
    val systemPrompt: String? = null
)

@Serializable
data class MessageDto(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val reply: String
)

@Serializable
data class ErrorResponse(
    val error: String
)

@Singleton
class GeminiProxyService @Inject constructor(
    private val client: OkHttpClient,
    private val proxyUrl: String
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun chat(messages: List<ChatMessage>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val requestMessages = messages.map { msg ->
                    MessageDto(
                        role = if (msg.role == "ai") "model" else "user",
                        content = msg.content
                    )
                }

                val body = Json.encodeToString(ChatRequest(messages = requestMessages))
                    .toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("$proxyUrl/api/chat")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    val error = try {
                        json.decodeFromString<ErrorResponse>(errorBody).error
                    } catch (_: Exception) {
                        errorBody
                    }
                    return@withContext Result.failure(Exception(error))
                }

                val responseBody = response.body?.string() ?: return@withContext Result.failure(
                    Exception("Empty response from proxy")
                )

                val chatResponse = json.decodeFromString<ChatResponse>(responseBody)
                Result.success(chatResponse.reply)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
