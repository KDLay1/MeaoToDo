package com.kdlay.meaotodo.ui.ledger

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val ledgerCategories = listOf("餐饮", "学习", "交通", "咖啡", "生活", "其他")
private val ledgerCategoryIcons = mapOf(
    "餐饮" to "餐",
    "学习" to "学",
    "交通" to "行",
    "咖啡" to "饮",
    "生活" to "家",
    "其他" to "其"
)
private val ledgerChartColors = listOf(
    Color(0xFF147E82),
    Color(0xFFB57631),
    Color(0xFF527FA3),
    Color(0xFF65A28B),
    Color(0xFFC97860),
    Color(0xFFA6AAA4)
)

@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var amountText by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(ledgerCategories.first()) }
    var note by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { LedgerTopHeader(uiState = uiState) }
            item { CategorySummaryRow(uiState = uiState) }
            item { MonthOverviewCard(uiState = uiState) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("最近流水", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("支出 ${formatMoney(uiState.todayExpenseCents)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                }
            }
            if (uiState.entries.isEmpty()) {
                item { EmptyLedgerCard() }
            } else {
                items(uiState.entries, key = { it.id }) { entry ->
                    LedgerEntryRow(entry = entry, onRemove = { viewModel.removeEntry(entry) })
                }
            }
            item {
                QuickLedgerEntryCard(
                    amountText = amountText,
                    selectedCategory = selectedCategory,
                    note = note,
                    onAmountChange = { amountText = it },
                    onCategoryChange = { selectedCategory = it },
                    onNoteChange = { note = it },
                    onSave = {
                        val validAmount = parseAmountCents(amountText) != null
                        viewModel.addExpense(amountText, selectedCategory, note)
                        if (validAmount) {
                            amountText = ""
                            note = ""
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LedgerTopHeader(uiState: LedgerUiState) {
    val monthExpense = uiState.monthExpenseCents
    val incomeCents = 0L
    val balance = incomeCents - monthExpense
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("记录每一笔，也看清生活节奏", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("⌕", fontSize = 30.sp, color = MaterialTheme.colorScheme.onBackground)
                Text("⋮", fontSize = 28.sp, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        Text("账本", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("本月支出 ${formatMoney(monthExpense)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("收入 ${formatMoney(incomeCents)}", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("结余 ${formatMoney(balance)}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CategorySummaryRow(uiState: LedgerUiState) {
    val monthTotal = uiState.monthExpenseCents.coerceAtLeast(1)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ledgerCategories.take(4).forEach { category ->
            val total = uiState.monthEntries.filter { it.category == category }.sumOf { it.amountCents }
            val percent = ((total * 100f) / monthTotal).roundToInt().coerceAtLeast(0)
            CategorySummaryCard(
                modifier = Modifier.weight(1f),
                category = category,
                amount = total,
                percent = percent
            )
        }
    }
}

@Composable
private fun CategorySummaryCard(modifier: Modifier, category: String, amount: Long, percent: Int) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)) {
                Text(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), text = ledgerCategoryIcons[category] ?: "•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Text(category, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(formatMoney(amount), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text("$percent%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MonthOverviewCard(uiState: LedgerUiState) {
    val totals = ledgerCategories.map { category -> uiState.monthEntries.filter { it.category == category }.sumOf { it.amountCents } }
    val total = totals.sum().takeIf { it > 0 } ?: uiState.monthExpenseCents
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${formatLedgerMonth(uiState.nowMillis)}概览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))) {
                    Text(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), text = formatLedgerYearMonth(uiState.nowMillis), style = MaterialTheme.typography.labelLarge)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                    DonutChart(totals = totals, total = total)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("支出总计", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatMoney(total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    ledgerCategories.take(5).forEachIndexed { index, category ->
                        val amount = totals.getOrElse(index) { 0L }
                        val percent = if (total > 0) amount * 100f / total else 0f
                        LegendLine(color = ledgerChartColors[index % ledgerChartColors.size], category = category, percent = percent, amount = amount)
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            ) {
                Text(modifier = Modifier.padding(14.dp), text = "查看分类详情  ›", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DonutChart(totals: List<Long>, total: Long) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stroke = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt)
        drawCircle(color = Color(0xFFF0F1F7), style = stroke)
        var startAngle = -90f
        totals.forEachIndexed { index, value ->
            val sweep = if (total > 0) (value.toFloat() / total.toFloat()) * 360f else 0f
            if (sweep > 0f) {
                drawArc(
                    color = ledgerChartColors[index % ledgerChartColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = stroke
                )
                startAngle += sweep
            }
        }
    }
}

@Composable
private fun LegendLine(color: Color, category: String, percent: Float, amount: Long) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(9.dp), shape = CircleShape, color = color) {}
        Text(category, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text("%.1f%%".format(percent), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatMoney(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickLedgerEntryCard(
    amountText: String,
    selectedCategory: String,
    note: String,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Box(contentAlignment = Alignment.Center) { Text("+", fontSize = 28.sp, fontWeight = FontWeight.Light) }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("记录一笔收支", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("快速记账，轻松管理", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = amountText,
                onValueChange = onAmountChange,
                label = { Text("金额") },
                prefix = { Text("¥") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ledgerCategories.forEach { category ->
                    CategoryChip(text = category, selected = selectedCategory == category, onClick = { onCategoryChange(category) })
                }
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = note,
                onValueChange = onNoteChange,
                label = { Text("备注，可选") },
                singleLine = true
            )
            Button(modifier = Modifier.fillMaxWidth(), onClick = onSave) {
                Text("保存记录", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.54f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Text(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), text = text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyLedgerCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("▣", fontSize = 34.sp, color = MaterialTheme.colorScheme.primary)
            Text("暂无流水", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("从下方记录第一笔收支。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LedgerEntryRow(entry: LedgerEntryEntity, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(ledgerCategoryIcons[entry.category] ?: "•", color = MaterialTheme.colorScheme.primary, fontSize = 22.sp)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.note.ifBlank { entry.category }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatLedgerDate(entry.occurredAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("- ${formatMoney(entry.amountCents)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.76f)) {
                    Text(modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp), text = entry.category, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Text(modifier = Modifier.clickable(onClick = onRemove), text = "删除", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatLedgerDate(timestamp: Long): String = SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(timestamp))
private fun formatLedgerMonth(timestamp: Long): String = SimpleDateFormat("M月", Locale.getDefault()).format(Date(timestamp))
private fun formatLedgerYearMonth(timestamp: Long): String = SimpleDateFormat("yyyy年M月", Locale.getDefault()).format(Date(timestamp))
