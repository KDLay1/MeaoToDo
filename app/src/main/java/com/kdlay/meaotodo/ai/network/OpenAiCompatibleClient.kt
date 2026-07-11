package com.kdlay.meaotodo.ai.network

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OpenAiCompatibleClient(
    private val connectTimeoutMillis: Int = 15_000,
    private val readTimeoutMillis: Int = 30_000,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = false }
) : AiClient {
    override suspend fun complete(
        config: AiProviderConfig,
        request: AiCompletionRequest
    ): AiCompletionResult = runInterruptible(Dispatchers.IO) {
        require(config.apiKey.isNotBlank()) { "API Key 不能为空" }
        require(config.model.isNotBlank()) { "模型名称不能为空" }
        val endpoint = completionEndpoint(config.baseUrl)
        val requestBody = encodeRequest(config, request)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(requestBody)
            }
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            if (status !in 200..299) {
                throw AiHttpException(status, "AI 服务请求失败（HTTP $status）：${sanitizeError(body)}")
            }
            val response = runCatching { json.decodeFromString<ChatCompletionResponse>(body) }
                .getOrElse { throw AiHttpException(status, "AI 服务返回了无法解析的响应") }
            val content = response.choices.firstOrNull()?.message?.content?.trim().orEmpty()
            if (content.isBlank()) throw AiHttpException(status, "AI 服务没有返回内容")
            AiCompletionResult(
                content = content,
                promptTokens = response.usage?.prompt_tokens,
                completionTokens = response.usage?.completion_tokens,
                totalTokens = response.usage?.total_tokens
            )
        } finally {
            connection.disconnect()
        }
    }

    internal fun completionEndpoint(baseUrl: String): String {
        val clean = baseUrl.trim().trimEnd('/')
        return if (clean.endsWith("/chat/completions")) clean else "$clean/chat/completions"
    }

    internal fun encodeRequest(config: AiProviderConfig, request: AiCompletionRequest): String = json.encodeToString(
        ChatCompletionPayload(
            model = config.model,
            messages = listOf(
                ChatMessage(role = "system", content = request.systemPrompt),
                ChatMessage(role = "user", content = request.userPrompt)
            ),
            temperature = request.temperature.coerceIn(0.0, 2.0),
            max_tokens = request.maxOutputTokens.coerceIn(64, 8_192),
            response_format = if (request.requireJsonObject) ResponseFormat(type = "json_object") else null
        )
    )

    private fun sanitizeError(body: String): String = body
        .replace(Regex("(?i)(api[_ -]?key|authorization|bearer)\\s*[:=]?\\s*[^\\s\"']+"), "$1 [已隐藏]")
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .take(300)
        .ifBlank { "未提供错误详情" }
}
