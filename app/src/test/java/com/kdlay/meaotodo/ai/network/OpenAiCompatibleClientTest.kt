package com.kdlay.meaotodo.ai.network

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
