package com.kdlay.meaotodo.ui.record

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.ui.board.BoardUiState
import com.kdlay.meaotodo.ui.board.BoardViewModel
import com.kdlay.meaotodo.ui.board.buildBudgetPace
import com.kdlay.meaotodo.ui.components.MeaoCompactStat
import com.kdlay.meaotodo.ui.components.MeaoPageHeader
import com.kdlay.meaotodo.ui.components.MeaoSectionTitle
import com.kdlay.meaotodo.ui.ledger.formatMoney
import com.kdlay.meaotodo.ui.settings.SettingsViewModel
import kotlin.math.roundToInt

@Composable
internal fun InsightsScreen(
    viewModel: BoardViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val preferences by settingsViewModel.preferences.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MeaoPageHeader(
                title = "洞察",
                subtitle = "所有结论都来自本机任务、专注和账本记录"
            )
        }
        item { ProductivityPulseCard(state) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                MeaoCompactStat(
                    modifier = Modifier.weight(1f),
                    value = "${state.completedTodayCount}",
                    label = "今日完成"
                )
                MeaoCompactStat(
                    modifier = Modifier.weight(1f),
                    value = "${state.todayFocusCount}",
                    label = "今日番茄"
                )
                MeaoCompactStat(
                    modifier = Modifier.weight(1f),
                    value = "${state.focusStreakDays}",
                    label = "连续天数"
                )
            }
        }
        item { MeaoSectionTitle("本周专注") }
        item { WeeklyFocusCard(state) }
        item { MeaoSectionTitle("支出节奏") }
        item {
            SpendingPaceCard(
                state = state,
                monthlyBudgetCents = preferences.monthlyBudgetCents
            )
        }
        if (state.recommendations.isNotEmpty()) {
            item { MeaoSectionTitle("建议优先处理") }
            item { RecommendationCard(state) }
        }
    }
}

@Composable
private fun ProductivityPulseCard(state: BoardUiState) {
    val pulse = state.productivityPulse
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(92.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = pulse.score.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "状态分",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("效率脉搏 · ${pulse.label}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = pulseExplanation(state),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (state.overdueCount > 0) {
                    Text(
                        text = "其中 ${state.overdueCount} 项已经逾期",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyFocusCard(state: BoardUiState) {
    val weeklyTotal = state.weeklyFocusCounts.sum()
    val maxValue = state.weeklyFocusCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("完成番茄", fontWeight = FontWeight.Bold)
                Text(
                    "本周 $weeklyTotal 个",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            if (weeklyTotal == 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(104.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("本周还没有专注记录", fontWeight = FontWeight.SemiBold)
                            Text(
                                "完成第一轮后，这里会显示每天的投入。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(132.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    state.weeklyFocusCounts.forEachIndexed { index, value ->
                        WeekBar(
                            value = value,
                            maxValue = maxValue,
                            label = listOf("一", "二", "三", "四", "五", "六", "日")[index]
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekBar(value: Int, maxValue: Int, label: String) {
    val barHeight = (18 + 78f * value / maxValue).dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(value.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            modifier = Modifier.size(width = 24.dp, height = barHeight),
            shape = RoundedCornerShape(999.dp),
            color = if (value > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        ) {}
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SpendingPaceCard(state: BoardUiState, monthlyBudgetCents: Long) {
    val pace = buildBudgetPace(
        monthSpentCents = state.monthExpenseCents,
        budgetCents = monthlyBudgetCents,
        now = state.nowMillis
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("本月支出", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMoney(pace.spentCents), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("本周", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMoney(state.weekExpenseCents), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            if (pace.budgetCents > 0) {
                LinearProgressIndicator(
                    progress = { pace.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (pace.spentCents > pace.budgetCents) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    "预算 ${formatMoney(pace.budgetCents)} · 已使用 ${(pace.progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "截至今天合理进度约 ${formatMoney(pace.expectedByTodayCents)}，按当前节奏预计月末 ${formatMoney(pace.projectedMonthCents)}。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "按当前节奏预计月末 ${formatMoney(pace.projectedMonthCents)}，可在设置中开启月预算。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                pace.status,
                color = if (pace.status.contains("超") || pace.status.contains("快")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RecommendationCard(state: BoardUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            state.recommendations.take(3).forEachIndexed { index, recommendation ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            recommendation.task.title,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            recommendation.reasons.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        "${recommendation.remainingPomodoros.coerceAtLeast(1)} 番茄",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

private fun pulseExplanation(state: BoardUiState): String = when {
    state.productivityPulse.score >= 80 -> "完成和专注都保持良好，可以继续当前节奏。"
    state.productivityPulse.score >= 60 -> "今天正在稳步推进，再完成一项重点任务会更完整。"
    state.overdueCount > 0 -> "逾期任务正在拉低状态，建议先处理一个最小可完成项。"
    else -> "今天记录还不多，可以从一轮短专注开始。"
}
