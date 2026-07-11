package com.kdlay.meaotodo.ai.network

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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
        val firstResponse = execute(
            endpoint = endpoint,
            apiKey = config.apiKey,
            requestBody = encodeRequest(config, request)
        )
        val httpResponse = if (request.requireJsonObject && shouldRetryWithoutResponseFormat(firstResponse.status, firstResponse.body)) {
            execute(
                endpoint = endpoint,
                apiKey = config.apiKey,
                requestBody = encodeRequest(config, request.copy(requireJsonObject = false))
            )
        } else {
            firstResponse
        }
        if (httpResponse.status !in 200..299) {
            throw AiHttpException(
                httpResponse.status,
                "AI 服务请求失败（HTTP ${httpResponse.status}）：${extractProviderError(httpResponse.body)}"
            )
        }
        val response = runCatching { json.decodeFromString<ChatCompletionResponse>(httpResponse.body) }
            .getOrElse { throw AiHttpException(httpResponse.status, "AI 服务返回了无法解析的响应") }
        val content = response.choices.firstOrNull()?.message?.content?.trim().orEmpty()
        if (content.isBlank()) throw AiHttpException(httpResponse.status, "AI 服务没有返回内容")
        AiCompletionResult(
            content = content,
            promptTokens = response.usage?.prompt_tokens,
            completionTokens = response.usage?.completion_tokens,
            totalTokens = response.usage?.total_tokens
        )
    }

    private fun execute(endpoint: String, apiKey: String, requestBody: String): RawHttpResponse {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = connectTimeoutMillis
            readTimeout = readTimeoutMillis
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
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
            return RawHttpResponse(status, body)
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

    internal fun shouldRetryWithoutResponseFormat(status: Int, body: String): Boolean =
        status == 400 && body.contains("response_format", ignoreCase = true)

    internal fun extractProviderError(body: String): String {
        val message = runCatching {
            Json.parseToJsonElement(body).jsonObject["error"]
                ?.jsonObject
                ?.get("message")
                ?.jsonPrimitive
                ?.content
        }.getOrNull()
        return sanitizeError(message ?: body)
    }

    private fun sanitizeError(body: String): String = body
        .replace(Regex("(?i)(api[_ -]?key|authorization|bearer)\\s*[:=]?\\s*[^\\s\"']+"), "$1 [已隐藏]")
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .take(300)
        .ifBlank { "未提供错误详情" }
}

private data class RawHttpResponse(val status: Int, val body: String)
