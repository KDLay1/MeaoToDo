package com.kdlay.meaotodo.ai.config

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
    val lastEveningReviewDay: String = "",
    val dailyRequestLimit: Int = 20,
    val monthlyTokenLimit: Int = 200_000,
    val usageDay: String = "",
    val requestsToday: Int = 0,
    val usageMonth: String = "",
    val tokensThisMonth: Int = 0
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
            lastEveningReviewDay = preferences[LAST_EVENING_REVIEW_DAY].orEmpty(),
            dailyRequestLimit = (preferences[DAILY_REQUEST_LIMIT] ?: 20).coerceIn(1, 200),
            monthlyTokenLimit = (preferences[MONTHLY_TOKEN_LIMIT] ?: 200_000).coerceIn(10_000, 5_000_000),
            usageDay = preferences[USAGE_DAY].orEmpty(),
            requestsToday = (preferences[REQUESTS_TODAY] ?: 0).coerceAtLeast(0),
            usageMonth = preferences[USAGE_MONTH].orEmpty(),
            tokensThisMonth = (preferences[TOKENS_THIS_MONTH] ?: 0).coerceAtLeast(0)
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

    suspend fun setUsageLimits(dailyRequests: Int, monthlyTokens: Int) {
        dataStore.edit { preferences ->
            preferences[DAILY_REQUEST_LIMIT] = dailyRequests.coerceIn(1, 200)
            preferences[MONTHLY_TOKEN_LIMIT] = monthlyTokens.coerceIn(10_000, 5_000_000)
        }
    }

    suspend fun consumeRequest(dayKey: String, monthKey: String) {
        dataStore.edit { preferences ->
            if (preferences[USAGE_DAY] != dayKey) {
                preferences[USAGE_DAY] = dayKey
                preferences[REQUESTS_TODAY] = 0
            }
            if (preferences[USAGE_MONTH] != monthKey) {
                preferences[USAGE_MONTH] = monthKey
                preferences[TOKENS_THIS_MONTH] = 0
            }
            preferences[REQUESTS_TODAY] = (preferences[REQUESTS_TODAY] ?: 0) + 1
        }
    }

    suspend fun recordTokens(monthKey: String, tokens: Int) {
        if (tokens <= 0) return
        dataStore.edit { preferences ->
            if (preferences[USAGE_MONTH] != monthKey) {
                preferences[USAGE_MONTH] = monthKey
                preferences[TOKENS_THIS_MONTH] = 0
            }
            preferences[TOKENS_THIS_MONTH] = (preferences[TOKENS_THIS_MONTH] ?: 0) + tokens
        }
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
        private val DAILY_REQUEST_LIMIT = intPreferencesKey("daily_request_limit")
        private val MONTHLY_TOKEN_LIMIT = intPreferencesKey("monthly_token_limit")
        private val USAGE_DAY = stringPreferencesKey("usage_day")
        private val REQUESTS_TODAY = intPreferencesKey("requests_today")
        private val USAGE_MONTH = stringPreferencesKey("usage_month")
        private val TOKENS_THIS_MONTH = intPreferencesKey("tokens_this_month")
    }
}
