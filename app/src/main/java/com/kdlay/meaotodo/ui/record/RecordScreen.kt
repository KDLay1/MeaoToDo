package com.kdlay.meaotodo.ui.record

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.domain.assistant.DailyTimelineEvent
import com.kdlay.meaotodo.domain.assistant.DailyTimelineEventType
import com.kdlay.meaotodo.ui.assistant.AssistantViewModel
import com.kdlay.meaotodo.ui.board.BoardViewModel
import com.kdlay.meaotodo.ui.components.MeaoCompactStat
import com.kdlay.meaotodo.ui.components.MeaoPageHeader
import com.kdlay.meaotodo.ui.components.MeaoSegmentedControl
import com.kdlay.meaotodo.ui.components.MeaoSectionTitle
import com.kdlay.meaotodo.ui.ledger.LedgerScreen
import com.kdlay.meaotodo.ui.ledger.LedgerViewModel
import com.kdlay.meaotodo.ui.settings.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordScreen(
    ledgerViewModel: LedgerViewModel,
    boardViewModel: BoardViewModel,
    settingsViewModel: SettingsViewModel,
    assistantViewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        MeaoSegmentedControl(
            options = listOf("时间线", "账本", "洞察"),
            selectedIndex = selectedSection,
            onSelect = { selectedSection = it },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
        when (selectedSection) {
            0 -> DailyTimelineScreen(
                viewModel = assistantViewModel,
                modifier = Modifier.weight(1f)
            )
            1 -> LedgerScreen(
                viewModel = ledgerViewModel,
                modifier = Modifier.weight(1f)
            )
            else -> InsightsScreen(
                viewModel = boardViewModel,
                settingsViewModel = settingsViewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DailyTimelineScreen(
    viewModel: AssistantViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = state.context
    val events = context?.timelineEvents.orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MeaoPageHeader(
                title = "今天的记录",
                subtitle = "任务、专注和支出按发生时间汇总"
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                MeaoCompactStat(
                    modifier = Modifier.weight(1f),
                    value = "${context?.completedTodayCount ?: 0}",
                    label = "完成任务"
                )
                MeaoCompactStat(
                    modifier = Modifier.weight(1f),
                    value = "${context?.focusedMinutes ?: 0}",
                    label = "专注分钟"
                )
                MeaoCompactStat(
                    modifier = Modifier.weight(1f),
                    value = formatMoney(context?.todayExpenseCents ?: 0),
                    label = "今日支出"
                )
            }
        }
        item { MeaoSectionTitle("时间线") }
        if (events.isEmpty()) {
            item { EmptyTimelineCard() }
        } else {
            items(events, key = { it.id }) { event ->
                TimelineEventRow(event)
            }
        }
    }
}

@Composable
private fun TimelineEventRow(event: DailyTimelineEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = eventContainerColor(event.type)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = eventIcon(event.type),
                        color = eventAccentColor(event.type),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Surface(
                modifier = Modifier.size(width = 2.dp, height = 38.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            ) {}
        }
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        event.title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        event.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        formatTime(event.occurredAt),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    event.amountCents?.let {
                        Text(formatMoney(it), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyTimelineCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("今天还没有完成记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "完成任务、结束一轮专注或记录支出后，会自动出现在这里。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun eventContainerColor(type: DailyTimelineEventType) = when (type) {
    DailyTimelineEventType.TASK_COMPLETED -> MaterialTheme.colorScheme.tertiaryContainer
    DailyTimelineEventType.FOCUS_COMPLETED -> MaterialTheme.colorScheme.primaryContainer
    DailyTimelineEventType.EXPENSE -> MaterialTheme.colorScheme.secondaryContainer
}

@Composable
private fun eventAccentColor(type: DailyTimelineEventType) = when (type) {
    DailyTimelineEventType.TASK_COMPLETED -> MaterialTheme.colorScheme.tertiary
    DailyTimelineEventType.FOCUS_COMPLETED -> MaterialTheme.colorScheme.primary
    DailyTimelineEventType.EXPENSE -> MaterialTheme.colorScheme.secondary
}

private fun eventIcon(type: DailyTimelineEventType): String = when (type) {
    DailyTimelineEventType.TASK_COMPLETED -> "✓"
    DailyTimelineEventType.FOCUS_COMPLETED -> "专"
    DailyTimelineEventType.EXPENSE -> "¥"
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun formatMoney(cents: Long): String =
    if (cents % 100L == 0L) "¥${cents / 100}" else "¥%.2f".format(cents / 100.0)
