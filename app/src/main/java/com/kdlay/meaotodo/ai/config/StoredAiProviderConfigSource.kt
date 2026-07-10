package com.kdlay.meaotodo.ai.config

import com.kdlay.meaotodo.ai.network.AiProviderConfig
import com.kdlay.meaotodo.ai.network.AiProviderConfigSource
import kotlinx.coroutines.flow.first

class StoredAiProviderConfigSource(
    private val settingsStore: AiSettingsStore
) : AiProviderConfigSource {
    override suspend fun getConfig(): AiProviderConfig {
        val settings = settingsStore.settings.first()
        if (settings.baseUrl.isBlank() || settings.model.isBlank()) {
            throw IllegalStateException("请先配置 API 地址和模型")
        }
        val key = settingsStore.readApiKey()?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("请先配置 API Key")
        return AiProviderConfig(settings.baseUrl, settings.model, key)
    }
}
