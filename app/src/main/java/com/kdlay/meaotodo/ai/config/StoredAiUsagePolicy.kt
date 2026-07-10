package com.kdlay.meaotodo.ai.config

import com.kdlay.meaotodo.ai.network.AiUsagePolicy
import java.util.Calendar
import kotlinx.coroutines.flow.first

class StoredAiUsagePolicy(
    private val settingsStore: AiSettingsStore,
    private val calendarProvider: () -> Calendar = Calendar::getInstance
) : AiUsagePolicy {
    override suspend fun beforeRequest() {
        val calendar = calendarProvider()
        val dayKey = dayKey(calendar)
        val monthKey = monthKey(calendar)
        val settings = settingsStore.settings.first()
        val requests = if (settings.usageDay == dayKey) settings.requestsToday else 0
        val tokens = if (settings.usageMonth == monthKey) settings.tokensThisMonth else 0
        check(requests < settings.dailyRequestLimit) { "已达到今日 AI 请求上限（${settings.dailyRequestLimit} 次）" }
        check(tokens < settings.monthlyTokenLimit) { "已达到本月 AI Token 上限（${settings.monthlyTokenLimit}）" }
        settingsStore.consumeRequest(dayKey, monthKey)
    }

    override suspend fun recordUsage(totalTokens: Int?) {
        settingsStore.recordTokens(monthKey(calendarProvider()), totalTokens ?: 0)
    }

    private fun dayKey(calendar: Calendar): String = "%04d-%02d-%02d".format(
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)
    )

    private fun monthKey(calendar: Calendar): String = "%04d-%02d".format(
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1
    )
}
