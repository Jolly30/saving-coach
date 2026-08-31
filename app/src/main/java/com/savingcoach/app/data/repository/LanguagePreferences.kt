package com.savingcoach.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class AppLanguage(val code: String, val displayName: String) {
    EN("en", "English"),
    MY("my", "မြန်မာ (Burmese)");

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: EN
        }
    }
}

@Singleton
class LanguagePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(getSavedLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private fun getSavedLanguage(): AppLanguage {
        val saved = prefs.getString(KEY_LANGUAGE, AppLanguage.EN.code) ?: AppLanguage.EN.code
        return AppLanguage.fromCode(saved)
    }

    fun setLanguage(appLanguage: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, appLanguage.code).apply()
        _language.value = appLanguage
    }

    companion object {
        private const val PREFS_NAME = "saving_coach_lang_prefs"
        private const val KEY_LANGUAGE = "key_app_language"
    }
}
