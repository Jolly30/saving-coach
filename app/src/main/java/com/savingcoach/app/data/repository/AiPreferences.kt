package com.savingcoach.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _geminiApiKey = MutableStateFlow(getSavedGeminiApiKey())
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _openRouterApiKey = MutableStateFlow(getSavedOpenRouterApiKey())
    val openRouterApiKey: StateFlow<String> = _openRouterApiKey.asStateFlow()

    private fun getSavedGeminiApiKey(): String {
        return prefs.getString(KEY_GEMINI_API_KEY, "")?.trim() ?: ""
    }

    private fun getSavedOpenRouterApiKey(): String {
        return prefs.getString(KEY_OPENROUTER_API_KEY, "")?.trim() ?: ""
    }

    fun getGeminiApiKey(): String = _geminiApiKey.value

    fun getOpenRouterApiKey(): String = _openRouterApiKey.value

    fun setGeminiApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_GEMINI_API_KEY, trimmed).apply()
        _geminiApiKey.value = trimmed
    }

    fun setOpenRouterApiKey(key: String) {
        val trimmed = key.trim()
        prefs.edit().putString(KEY_OPENROUTER_API_KEY, trimmed).apply()
        _openRouterApiKey.value = trimmed
    }

    fun saveKeys(geminiKey: String, openRouterKey: String) {
        val trimmedGemini = geminiKey.trim()
        val trimmedOpenRouter = openRouterKey.trim()
        prefs.edit()
            .putString(KEY_GEMINI_API_KEY, trimmedGemini)
            .putString(KEY_OPENROUTER_API_KEY, trimmedOpenRouter)
            .apply()
        _geminiApiKey.value = trimmedGemini
        _openRouterApiKey.value = trimmedOpenRouter
    }

    fun clearKeys() {
        prefs.edit()
            .remove(KEY_GEMINI_API_KEY)
            .remove(KEY_OPENROUTER_API_KEY)
            .apply()
        _geminiApiKey.value = ""
        _openRouterApiKey.value = ""
    }

    companion object {
        private const val PREFS_NAME = "saving_coach_ai_prefs"
        private const val KEY_GEMINI_API_KEY = "key_gemini_api_key"
        private const val KEY_OPENROUTER_API_KEY = "key_openrouter_api_key"
    }
}
