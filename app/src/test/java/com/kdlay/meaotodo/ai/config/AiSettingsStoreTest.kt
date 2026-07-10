package com.kdlay.meaotodo.ai.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AiSettingsStoreTest {
    @Test
    fun normalizeBaseUrl_acceptsHttpsAndTrimsSlash() {
        assertEquals("https://example.com/v1", AiSettingsStore.normalizeBaseUrl(" https://example.com/v1/ "))
    }

    @Test
    fun normalizeBaseUrl_rejectsPublicHttp() {
        assertThrows(IllegalArgumentException::class.java) {
            AiSettingsStore.normalizeBaseUrl("http://example.com/v1")
        }
    }

    @Test
    fun mask_neverReturnsFullSecret() {
        assertEquals("sk-••••••7890", AiSettingsStore.mask("sk-1234567890"))
        assertEquals("••••", AiSettingsStore.mask("abcd"))
    }
}
