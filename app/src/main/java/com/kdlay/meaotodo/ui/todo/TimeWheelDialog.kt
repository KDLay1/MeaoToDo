package com.kdlay.meaotodo.ui.todo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TimeWheelDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var selectedMinute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }
    val quickTimes = listOf(9 to 0, 12 to 0, 14 to 0, 18 to 0, 21 to 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("选择截止时间", fontWeight = FontWeight.Bold)
                Text(
                    text = "滑动滚轮，让目标时间停在中间。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("截止时间", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = "%02d:%02d".format(selectedHour, selectedMinute),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = if (selectedHour < 12) "上午" else "下午",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickTimes.forEach { (hour, minute) ->
                        val selected = selectedHour == hour && selectedMinute == minute
                        QuickTimePill(
                            text = "%02d:%02d".format(hour, minute),
                            selected = selected,
                            onClick = {
                                selectedHour = hour
                                selectedMinute = minute
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Top
                ) {
                    TimeWheelColumn(
                        title = "小时",
                        values = (0..23).toList(),
                        selectedValue = selectedHour,
                        onCenteredValueChange = { selectedHour = it }
                    )
                    Text(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 72.dp),
                        text = ":",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TimeWheelColumn(
                        title = "分钟",
                        values = (0..59).toList(),
                        selectedValue = selectedMinute,
                        onCenteredValueChange = { selectedMinute = it }
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { onConfirm(selectedHour, selectedMinute) }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun QuickTimePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TimeWheelColumn(
    title: String,
    values: List<Int>,
    selectedValue: Int,
    onCenteredValueChange: (Int) -> Unit
) {
    val selectedIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)

    LaunchedEffect(listState, values) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            layoutInfo.visibleItemsInfo
                .minByOrNull { item -> abs((item.offset + item.size / 2) - viewportCenter) }
                ?.index
        }
            .distinctUntilChanged()
            .collect { centeredIndex ->
                val centeredValue = centeredIndex?.let { values.getOrNull(it) }
                if (centeredValue != null && centeredValue != selectedValue) {
                    onCenteredValueChange(centeredValue)
                }
            }
    }

    LaunchedEffect(selectedValue) {
        val targetIndex = values.indexOf(selectedValue)
        if (targetIndex >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Column(
        modifier = Modifier.width(96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .width(96.dp)
                    .height(180.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
            ) {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 68.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(values, key = { it }) { value ->
                        TimeWheelItem(
                            value = value,
                            selected = value == selectedValue
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .width(82.dp)
                    .height(42.dp),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f))
            ) {}
        }
    }
}

@Composable
private fun TimeWheelItem(
    value: Int,
    selected: Boolean
) {
    Surface(
        modifier = Modifier.width(76.dp),
        shape = RoundedCornerShape(999.dp),
        color = Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier.padding(vertical = if (selected) 9.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "%02d".format(value),
                style = if (selected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
