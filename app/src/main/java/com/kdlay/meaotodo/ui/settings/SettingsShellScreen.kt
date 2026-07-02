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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdlay.meaotodo.ui.components.MeaoSettingsSwitchRow
import com.kdlay.meaotodo.ui.components.MeaoSettingsValueRow

@Composable
internal fun SettingsShellScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    var keepScreenOn by rememberSaveable { mutableStateOf(true) }
    var autoSync by rememberSaveable { mutableStateOf(false) }
    var darkBoardMode by rememberSaveable { mutableStateOf(true) }
    var focusReminder by rememberSaveable { mutableStateOf(true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SettingsHeader(onBack = onBack) }
        item {
            SettingsSection(title = "设备与同步", icon = "⇄") {
                MeaoSettingsValueRow(
                    icon = "⌂",
                    title = "设备角色",
                    value = "主力机",
                    subtitle = "后续支持主力机 / 备用机看板切换",
                    onClick = {}
                )
                MeaoSettingsSwitchRow(
                    icon = "⟲",
                    title = "Wi‑Fi 自动同步",
                    subtitle = "同一局域网内自动同步任务、番茄和账本",
                    checked = autoSync,
                    onCheckedChange = { autoSync = it }
                )
                MeaoSettingsValueRow(
                    icon = "●",
                    title = "同步状态",
                    value = "未配对",
                    subtitle = "后续接入设备搜索、配对码和最近同步时间",
                    onClick = {}
                )
            }
        }
        item {
            SettingsSection(title = "看板与显示", icon = "▤") {
                MeaoSettingsSwitchRow(
                    icon = "☀",
                    title = "备用机常亮",
                    subtitle = "看板页保持屏幕常亮，适合作为桌面信息面板",
                    checked = keepScreenOn,
                    onCheckedChange = { keepScreenOn = it }
                )
                MeaoSettingsSwitchRow(
                    icon = "◐",
                    title = "看板深色模式",
                    subtitle = "备用机夜间低亮度展示，后续接入全局主题",
                    checked = darkBoardMode,
                    onCheckedChange = { darkBoardMode = it }
                )
                MeaoSettingsValueRow(
                    icon = "▦",
                    title = "看板模块",
                    value = "5 个模块",
                    subtitle = "管理今日重点、番茄、支出、日程、习惯状态",
                    onClick = {}
                )
            }
        }
        item {
            SettingsSection(title = "番茄与任务", icon = "⏱") {
                MeaoSettingsValueRow(
                    icon = "◷",
                    title = "默认番茄",
                    value = "25/5",
                    subtitle = "默认专注 25 分钟，短休息 5 分钟",
                    onClick = {}
                )
                MeaoSettingsSwitchRow(
                    icon = "🔔",
                    title = "结束提醒",
                    subtitle = "专注结束和休息结束时提醒",
                    checked = focusReminder,
                    onCheckedChange = { focusReminder = it }
                )
                MeaoSettingsValueRow(
                    icon = "★",
                    title = "今日重点规则",
                    value = "手动",
                    subtitle = "后续支持按优先级或截止时间自动推荐",
                    onClick = {}
                )
            }
        }
        item {
            SettingsSection(title = "账本与数据", icon = "▣") {
                MeaoSettingsValueRow(
                    icon = "¥",
                    title = "账本分类",
                    value = "6 类",
                    subtitle = "餐饮、学习、交通、咖啡、生活、其他",
                    onClick = {}
                )
                MeaoSettingsValueRow(
                    icon = "⇩",
                    title = "数据导出",
                    value = "CSV",
                    subtitle = "后续支持导出任务、番茄记录和账本流水",
                    onClick = {}
                )
                MeaoSettingsValueRow(
                    icon = "i",
                    title = "关于 MeaoToDo",
                    value = "MVP",
                    subtitle = "单机可用优先，双机同步逐步接入",
                    onClick = {}
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
            text = "设备、同步、看板、番茄和账本的统一配置入口。",
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
