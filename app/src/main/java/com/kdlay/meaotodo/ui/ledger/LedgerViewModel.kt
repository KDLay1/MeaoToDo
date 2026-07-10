package com.kdlay.meaotodo.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import com.kdlay.meaotodo.data.repository.LedgerRepository
import java.math.RoundingMode
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LedgerViewModel(
    private val ledgerRepository: LedgerRepository
) : ViewModel() {
    private val nowMillis = MutableStateFlow(System.currentTimeMillis())

    val uiState = combine(
        ledgerRepository.entries,
        nowMillis
    ) { entries, now ->
        buildLedgerUiState(entries, now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LedgerUiState()
    )

    init {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                nowMillis.value = System.currentTimeMillis()
            }
        }
    }

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun addExpense(amountText: String, category: String, note: String) {
        val amountCents = parseAmountCents(amountText)
        viewModelScope.launch {
            if (amountCents == null || !ledgerRepository.addExpense(amountCents, category, note)) {
                _messages.emit("\u8bf7\u8f93\u5165\u6709\u6548\u91d1\u989d")
            } else {
                _messages.emit("\u5df2\u8bb0\u4e00\u7b14\u652f\u51fa")
            }
        }
    }

    fun removeEntry(entry: LedgerEntryEntity) {
        viewModelScope.launch {
            if (!ledgerRepository.removeEntry(entry.id)) {
                _messages.emit("\u5220\u9664\u5931\u8d25")
            }
        }
    }

    companion object {
        fun factory(ledgerRepository: LedgerRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(LedgerViewModel::class.java))
                return LedgerViewModel(ledgerRepository) as T
            }
        }
    }
}

data class LedgerUiState(
    val entries: List<LedgerEntryEntity> = emptyList(),
    val monthEntries: List<LedgerEntryEntity> = emptyList(),
    val todayExpenseCents: Long = 0,
    val monthExpenseCents: Long = 0,
    val nowMillis: Long = System.currentTimeMillis()
)

internal fun buildLedgerUiState(entries: List<LedgerEntryEntity>, now: Long): LedgerUiState {
    val today = dayRange(now)
    val month = monthRange(now)
    val activeExpenses = entries.filter { it.deletedAt == null && it.type == "expense" }
    val monthEntries = activeExpenses.filter { it.occurredAt in month.first until month.second }
    return LedgerUiState(
        entries = activeExpenses.take(30),
        monthEntries = monthEntries,
        todayExpenseCents = activeExpenses
            .filter { it.occurredAt in today.first until today.second }
            .sumOf { it.amountCents },
        monthExpenseCents = monthEntries.sumOf { it.amountCents },
        nowMillis = now
    )
}

internal fun parseAmountCents(text: String): Long? {
    val normalized = text.trim().replace(',', '.')
    if (normalized.isBlank()) return null
    val amount = normalized.toBigDecimalOrNull() ?: return null
    if (amount.signum() <= 0) return null
    return runCatching {
        amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
    }.getOrNull()?.takeIf { it > 0 }
}

internal fun formatMoney(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val absolute = if (cents == Long.MIN_VALUE) Long.MAX_VALUE else kotlin.math.abs(cents)
    val yuan = absolute / 100
    val fen = absolute % 100
    return "${sign}\u00a5$yuan.${fen.toString().padStart(2, '0')}"
}

internal fun dayRange(timestamp: Long): Pair<Long, Long> {
    val start = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return start to Calendar.getInstance().apply {
        timeInMillis = start
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis
}

internal fun monthRange(timestamp: Long): Pair<Long, Long> {
    val start = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return start to Calendar.getInstance().apply {
        timeInMillis = start
        add(Calendar.MONTH, 1)
    }.timeInMillis
}
