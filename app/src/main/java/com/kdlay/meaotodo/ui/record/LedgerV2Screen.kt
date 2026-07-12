package com.kdlay.meaotodo.ui.record

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import com.kdlay.meaotodo.ui.components.MeaoPageHeader
import com.kdlay.meaotodo.ui.components.MeaoSectionTitle
import com.kdlay.meaotodo.ui.ledger.LedgerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ledgerV2Categories = listOf("餐饮", "交通", "学习", "生活", "数码", "娱乐", "其他")

@Composable
internal fun LedgerV2Screen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(ledgerV2Categories.first()) }
    var note by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MeaoPageHeader(
                    title = "账本",
                    subtitle = "${formatMonth(state.nowMillis)} · 只记录真正需要回看的信息"
                )
            }
            item {
                LedgerSummaryCard(
                    monthExpense = state.monthExpenseCents,
                    todayExpense = state.todayExpenseCents
                )
            }
            item {
                QuickExpenseCard(
                    amount = amount,
                    category = category,
                    note = note,
                    onAmountChange = { amount = it },
                    onCategoryChange = { category = it },
                    onNoteChange = { note = it },
                    onSave = {
                        val shouldClear = amountIsValid(amount)
                        viewModel.addExpense(amount, category, note)
                        if (shouldClear) {
                            amount = ""
                            note = ""
                        }
                    }
                )
            }
            item {
                MeaoSectionTitle(
                    title = "最近流水",
                    actionText = "本月 ${formatMoney(state.monthExpenseCents)}"
                )
            }
            if (state.entries.isEmpty()) {
                item { EmptyLedgerCard() }
            } else {
                items(state.entries, key = { it.id }) { entry ->
                    LedgerEntryCard(entry = entry, onRemove = { viewModel.removeEntry(entry) })
                }
            }
        }
    }
}

@Composable
private fun LedgerSummaryCard(monthExpense: Long, todayExpense: Long) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("本月支出", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formatMoney(monthExpense),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("今天", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    formatMoney(todayExpense),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun QuickExpenseCard(
    amount: String,
    category: String,
    note: String,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("快速记一笔", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = amount,
                onValueChange = onAmountChange,
                singleLine = true,
                label = { Text("金额") },
                prefix = { Text("¥ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ledgerV2Categories) { option ->
                    val selected = option == category
                    Surface(
                        modifier = Modifier.clickable { onCategoryChange(option) },
                        shape = RoundedCornerShape(999.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                            text = option,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = note,
                onValueChange = onNoteChange,
                singleLine = true,
                label = { Text("备注（可选）") }
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSave,
                enabled = amount.isNotBlank()
            ) {
                Text("保存支出", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LedgerEntryCard(entry: LedgerEntryEntity, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(entry.category.take(1), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    entry.note.ifBlank { entry.category },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${entry.category} · ${formatEntryTime(entry.occurredAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatMoney(entry.amountCents), fontWeight = FontWeight.Bold)
                TextButton(onClick = onRemove) {
                    Text("删除", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun EmptyLedgerCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("还没有支出记录", fontWeight = FontWeight.Bold)
            Text("第一笔记录会出现在这里。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun amountIsValid(value: String): Boolean =
    value.trim().replace(',', '.').toBigDecimalOrNull()?.signum()?.let { it > 0 } == true

private fun formatMoney(cents: Long): String {
    val yuan = cents / 100
    val fen = kotlin.math.abs(cents % 100)
    return "¥$yuan.${fen.toString().padStart(2, '0')}"
}

private fun formatMonth(timestamp: Long): String =
    SimpleDateFormat("yyyy 年 M 月", Locale.CHINESE).format(Date(timestamp))

private fun formatEntryTime(timestamp: Long): String =
    SimpleDateFormat("M-d HH:mm", Locale.getDefault()).format(Date(timestamp))
