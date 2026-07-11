package com.kdlay.meaotodo.ai.network

import kotlinx.serialization.Serializable

data class AiProviderConfig(
    val baseUrl: String,
    val model: String,
    val apiKey: String
)

data class AiCompletionRequest(
    val systemPrompt: String,
    val userPrompt: String,
    val temperature: Double = 0.2,
    val maxOutputTokens: Int = 1_200,
    val requireJsonObject: Boolean = true
)

data class AiCompletionResult(
    val content: String,
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null
)

interface AiClient {
    suspend fun complete(config: AiProviderConfig, request: AiCompletionRequest): AiCompletionResult
}

interface AiProviderConfigSource {
    suspend fun getConfig(): AiProviderConfig
}

interface AiUsagePolicy {
    suspend fun beforeRequest()
    suspend fun recordUsage(totalTokens: Int?)
}

object NoOpAiUsagePolicy : AiUsagePolicy {
    override suspend fun beforeRequest() = Unit
    override suspend fun recordUsage(totalTokens: Int?) = Unit
}

@Serializable
internal data class ChatCompletionPayload(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double,
    val max_tokens: Int,
    val stream: Boolean = false,
    val response_format: ResponseFormat? = null
)

@Serializable
internal data class ChatMessage(val role: String, val content: String)

@Serializable
internal data class ResponseFormat(val type: String)

@Serializable
internal data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList(),
    val usage: TokenUsage? = null
)

@Serializable
internal data class ChatChoice(val message: ChatMessage)

@Serializable
internal data class TokenUsage(
    val prompt_tokens: Int? = null,
    val completion_tokens: Int? = null,
    val total_tokens: Int? = null
)

class AiHttpException(
    val statusCode: Int?,
    message: String
) : Exception(message)
