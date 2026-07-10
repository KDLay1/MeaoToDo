package com.kdlay.meaotodo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SettingsShellScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val preferences by viewModel.preferences.collectAsState()
    val aiSettings by viewModel.aiSettings.collectAsState()
    val aiStatus by viewModel.aiStatus.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SettingsHeader(onBack = onBack) }
        item {
            SettingsSection(title = "助手与洞察", icon = "✦") {
                AiSettingsCard(
                    settings = aiSettings,
                    status = aiStatus,
                    onSave = viewModel::saveAiConfiguration,
                    onTest = viewModel::testAiConnection,
                    onClearKey = viewModel::clearAiApiKey,
                    onAutoDailyBriefChange = viewModel::setAutoDailyBrief,
                    onAutoEveningReviewChange = viewModel::setAutoEveningReview,
                    onUsageLimitsChange = viewModel::setAiUsageLimits
                )
                Text("洞察模块可在记录页的“洞察”中调整，并会自动保存。")
            }
        }
        item {
            SettingsSection(title = "番茄与任务", icon = "⏱") {
                Text("默认时长与轮次请在番茄页直接调整，修改后会自动保存。")
                Text(
                    "结束通知尚未接入 Android 通知渠道；计时状态仍会在专注页与全局专注条实时显示。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            SettingsSection(title = "账本与数据", icon = "▣") {
                Text("账本分类：餐饮、学习、交通、咖啡、生活、其他。")
                Text(
                    if (preferences.monthlyBudgetCents > 0) {
                        "当前月预算：¥${preferences.monthlyBudgetCents / 100}"
                    } else {
                        "当前未设置月预算"
                    },
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0L to "关闭", 200_000L to "¥2000", 300_000L to "¥3000", 500_000L to "¥5000").forEach { (cents, label) ->
                        val selected = preferences.monthlyBudgetCents == cents
                        Surface(
                            modifier = Modifier.clickable { viewModel.setMonthlyBudgetCents(cents) },
                            shape = RoundedCornerShape(999.dp),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Text(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), text = label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Text(
                    "数据导出尚未实现；当前数据保存在本机 Room 数据库中。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("MeaoToDo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Surface(
                modifier = Modifier.clickable(onClick = onBack),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    text = "返回",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text("设置", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(
            text = "本地助手、专注、洞察和账本的统一配置入口。",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        text = icon,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}
