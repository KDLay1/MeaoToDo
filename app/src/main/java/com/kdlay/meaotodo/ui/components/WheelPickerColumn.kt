package com.kdlay.meaotodo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
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
    val itemHeight = 46.dp
    val selectedIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LaunchedEffect(listState, values) {
        snapshotFlow { listState.centeredItemIndex() }
            .distinctUntilChanged()
            .collect { centeredIndex ->
                val centeredValue = centeredIndex?.let { values.getOrNull(it) }
                if (centeredValue != null && centeredValue != selectedValue) {
                    onCenteredValueChange(centeredValue)
                }
            }
    }

    LaunchedEffect(selectedValue, values) {
        val targetIndex = values.indexOf(selectedValue)
        if (targetIndex >= 0 && !listState.isScrollInProgress) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Column(
        modifier = modifier.width(columnWidth),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .width(columnWidth)
                    .height(viewportHeight),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
            ) {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 6.dp),
                    state = listState,
                    flingBehavior = flingBehavior,
                    contentPadding = PaddingValues(vertical = (viewportHeight - itemHeight) / 2),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(values, key = { _, value -> value }) { index, value ->
                        WheelPickerItem(
                            index = index,
                            value = value,
                            selected = value == selectedValue,
                            label = itemLabel,
                            itemWidth = itemWidth,
                            itemHeight = itemHeight,
                            listState = listState
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .width(itemWidth + 8.dp)
                    .height(itemHeight),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.36f))
            ) {}
        }
    }
}

@Composable
private fun WheelPickerItem(
    index: Int,
    value: Int,
    selected: Boolean,
    label: (Int) -> String,
    itemWidth: Dp,
    itemHeight: Dp,
    listState: LazyListState
) {
    val itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    val viewportCenter = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
    val distancePx = itemInfo?.let { abs((it.offset + it.size / 2) - viewportCenter) } ?: Int.MAX_VALUE
    val itemSize = itemInfo?.size?.coerceAtLeast(1) ?: 1
    val distanceFraction = (distancePx.toFloat() / (itemSize * 2.2f)).coerceIn(0f, 1f)
    val scale = 1f - distanceFraction * 0.18f
    val textAlpha = 1f - distanceFraction * 0.58f

    Surface(
        modifier = Modifier
            .width(itemWidth)
            .height(itemHeight)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = textAlpha
            },
        shape = RoundedCornerShape(999.dp),
        color = Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label(value),
                style = if (selected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun LazyListState.centeredItemIndex(): Int? {
    val layoutInfo = layoutInfo
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return layoutInfo.visibleItemsInfo
        .minByOrNull { item -> abs((item.offset + item.size / 2) - viewportCenter) }
        ?.index
}
