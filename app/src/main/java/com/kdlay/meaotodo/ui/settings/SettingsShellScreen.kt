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
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.kdlay.meaotodo.ui.components.MeaoSettingsSwitchRow
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
    val dataTransferStatus by viewModel.dataTransferStatus.collectAsState()
    val pomodoroPreferences by viewModel.pomodoroPreferences.collectAsState()
    val context = LocalContext.current
    var pendingImportUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(viewModel::exportBackup) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> pendingImportUri = uri }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> viewModel.setPomodoroNotificationsEnabled(granted) }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("恢复本地数据？") },
            text = { Text("备份中的记录将按 ID 合并到当前数据库，不会导入 API Key，也不会自动删除当前记录。") },
            confirmButton = {
                Button(onClick = {
                    viewModel.importBackup(uri)
                    pendingImportUri = null
                }) { Text("确认恢复") }
            },
            dismissButton = { OutlinedButton(onClick = { pendingImportUri = null }) { Text("取消") } }
        )
    }

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
                MeaoSettingsSwitchRow(
                    icon = "铃",
                    title = "阶段结束通知",
                    subtitle = "专注或休息结束后发送系统通知",
                    checked = pomodoroPreferences.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (!enabled) {
                            viewModel.setPomodoroNotificationsEnabled(false)
                        } else if (
                            Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setPomodoroNotificationsEnabled(true)
                        }
                    }
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
                    "备份只包含任务、清单、专注和账本记录；不会包含 API Key。恢复采用安全合并，不会先清空当前数据。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                            exportLauncher.launch("MeaoToDo-backup-$date.json")
                        },
                        enabled = dataTransferStatus !is DataTransferStatus.Working
                    ) { Text("导出备份") }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                        enabled = dataTransferStatus !is DataTransferStatus.Working
                    ) { Text("导入恢复") }
                }
                when (val status = dataTransferStatus) {
                    DataTransferStatus.Idle -> Unit
                    is DataTransferStatus.Working -> Text(status.message)
                    is DataTransferStatus.Success -> Text(status.message)
                    is DataTransferStatus.Error -> Text("错误：${status.message}")
                }
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
