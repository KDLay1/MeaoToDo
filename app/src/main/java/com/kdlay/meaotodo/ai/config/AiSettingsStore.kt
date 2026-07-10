package com.kdlay.meaotodo.ai.config

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.aiSettingsDataStore by preferencesDataStore(name = "ai_settings")

data class AiProviderSettings(
    val baseUrl: String = "",
    val model: String = "",
    val hasApiKey: Boolean = false,
    val maskedApiKey: String = "",
    val autoDailyBrief: Boolean = false,
    val autoEveningReview: Boolean = false,
    val lastDailyBriefDay: String = "",
    val lastEveningReviewDay: String = ""
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && model.isNotBlank() && hasApiKey
}

class AiSettingsStore(
    context: Context,
    private val cipher: ApiKeyCipher = ApiKeyCipher()
) {
    private val dataStore = context.applicationContext.aiSettingsDataStore

    val settings: Flow<AiProviderSettings> = dataStore.data.map { preferences ->
        val cipherText = preferences[API_KEY_CIPHER_TEXT].orEmpty()
        AiProviderSettings(
            baseUrl = preferences[BASE_URL].orEmpty(),
            model = preferences[MODEL].orEmpty(),
            hasApiKey = cipherText.isNotBlank(),
            maskedApiKey = preferences[API_KEY_MASK].orEmpty(),
            autoDailyBrief = preferences[AUTO_DAILY_BRIEF] ?: false,
            autoEveningReview = preferences[AUTO_EVENING_REVIEW] ?: false,
            lastDailyBriefDay = preferences[LAST_DAILY_BRIEF_DAY].orEmpty(),
            lastEveningReviewDay = preferences[LAST_EVENING_REVIEW_DAY].orEmpty()
        )
    }

    suspend fun saveProvider(baseUrl: String, model: String) {
        val normalizedUrl = normalizeBaseUrl(baseUrl)
        require(model.isNotBlank()) { "模型名称不能为空" }
        dataStore.edit { preferences ->
            preferences[BASE_URL] = normalizedUrl
            preferences[MODEL] = model.trim()
        }
    }

    suspend fun saveApiKey(apiKey: String) {
        val cleanKey = apiKey.trim()
        require(cleanKey.isNotBlank()) { "API Key 不能为空" }
        val encrypted = cipher.encrypt(cleanKey)
        dataStore.edit { preferences ->
            preferences[API_KEY_CIPHER_TEXT] = encrypted.cipherText
            preferences[API_KEY_IV] = encrypted.initializationVector
            preferences[API_KEY_MASK] = mask(cleanKey)
        }
    }

    suspend fun readApiKey(): String? {
        val preferences = dataStore.data.first()
        val cipherText = preferences[API_KEY_CIPHER_TEXT]?.takeIf { it.isNotBlank() } ?: return null
        val iv = preferences[API_KEY_IV]?.takeIf { it.isNotBlank() } ?: return null
        return cipher.decrypt(EncryptedApiKey(cipherText, iv))
    }

    suspend fun clearApiKey() {
        dataStore.edit { preferences ->
            preferences.remove(API_KEY_CIPHER_TEXT)
            preferences.remove(API_KEY_IV)
            preferences.remove(API_KEY_MASK)
        }
        cipher.deleteKey()
    }

    suspend fun setAutoDailyBrief(enabled: Boolean) {
        dataStore.edit { it[AUTO_DAILY_BRIEF] = enabled }
    }

    suspend fun setAutoEveningReview(enabled: Boolean) {
        dataStore.edit { it[AUTO_EVENING_REVIEW] = enabled }
    }

    suspend fun markDailyBriefGenerated(dayKey: String) {
        dataStore.edit { it[LAST_DAILY_BRIEF_DAY] = dayKey }
    }

    suspend fun markEveningReviewGenerated(dayKey: String) {
        dataStore.edit { it[LAST_EVENING_REVIEW_DAY] = dayKey }
    }

    companion object {
        fun normalizeBaseUrl(value: String): String {
            val clean = value.trim().trimEnd('/')
            require(clean.startsWith("https://")) {
                "API 地址必须使用 HTTPS"
            }
            return clean
        }

        fun mask(value: String): String = when {
            value.length <= 4 -> "••••"
            value.length <= 8 -> "${value.take(2)}••••${value.takeLast(2)}"
            else -> "${value.take(3)}••••••${value.takeLast(4)}"
        }

        private val BASE_URL = stringPreferencesKey("base_url")
        private val MODEL = stringPreferencesKey("model")
        private val API_KEY_CIPHER_TEXT = stringPreferencesKey("api_key_cipher_text")
        private val API_KEY_IV = stringPreferencesKey("api_key_iv")
        private val API_KEY_MASK = stringPreferencesKey("api_key_mask")
        private val AUTO_DAILY_BRIEF = booleanPreferencesKey("auto_daily_brief")
        private val AUTO_EVENING_REVIEW = booleanPreferencesKey("auto_evening_review")
        private val LAST_DAILY_BRIEF_DAY = stringPreferencesKey("last_daily_brief_day")
        private val LAST_EVENING_REVIEW_DAY = stringPreferencesKey("last_evening_review_day")
    }
}
