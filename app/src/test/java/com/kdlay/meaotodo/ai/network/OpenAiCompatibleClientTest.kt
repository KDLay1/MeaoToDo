package com.kdlay.meaotodo.ai.network

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OpenAiCompatibleClientTest {
    private val client = OpenAiCompatibleClient()

    @Test
    fun completionEndpoint_appendsCompatiblePath() {
        assertEquals(
            "https://example.com/v1/chat/completions",
            client.completionEndpoint("https://example.com/v1/")
        )
    }

    @Test
    fun completionEndpoint_keepsFullEndpoint() {
        assertEquals(
            "https://example.com/v1/chat/completions",
            client.completionEndpoint("https://example.com/v1/chat/completions")
        )
    }

    @Test
    fun encodeRequest_includesRequiredResponseFormatType() {
        val body = client.encodeRequest(
            config = AiProviderConfig("https://example.com/v1", "test-model", "secret"),
            request = AiCompletionRequest("system", "user", requireJsonObject = true)
        )
        val root = Json.parseToJsonElement(body).jsonObject

        assertEquals("json_object", root.getValue("response_format").jsonObject.getValue("type").jsonPrimitive.content)
    }

    @Test
    fun encodeRequest_omitsResponseFormatWhenDisabled() {
        val body = client.encodeRequest(
            config = AiProviderConfig("https://example.com/v1", "test-model", "secret"),
            request = AiCompletionRequest("system", "user", requireJsonObject = false)
        )

        assertEquals(false, Json.parseToJsonElement(body).jsonObject.containsKey("response_format"))
    }
}
