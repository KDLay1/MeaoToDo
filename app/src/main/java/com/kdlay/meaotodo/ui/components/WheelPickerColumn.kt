package com.kdlay.meaotodo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

@Composable
internal fun WheelPickerColumn(
    title: String,
    values: List<Int>,
    selectedValue: Int,
    onCenteredValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: (Int) -> String = { "%02d".format(it) },
    columnWidth: Dp = 96.dp,
    itemWidth: Dp = 76.dp,
    viewportHeight: Dp = 180.dp
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
        modifier = modifier.width(columnWidth),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .width(columnWidth)
                    .height(viewportHeight),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
            ) {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 68.dp),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(values, key = { it }) { value ->
                        WheelPickerItem(
                            value = value,
                            selected = value == selectedValue,
                            label = itemLabel,
                            itemWidth = itemWidth
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .width(itemWidth + 6.dp)
                    .height(42.dp),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f))
            ) {}
        }
    }
}

@Composable
private fun WheelPickerItem(
    value: Int,
    selected: Boolean,
    label: (Int) -> String,
    itemWidth: Dp
) {
    Surface(
        modifier = Modifier.width(itemWidth),
        shape = RoundedCornerShape(999.dp),
        color = Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier.padding(vertical = if (selected) 9.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label(value),
                style = if (selected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
