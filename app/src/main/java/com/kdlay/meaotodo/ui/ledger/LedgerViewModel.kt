package com.kdlay.meaotodo.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import com.kdlay.meaotodo.data.repository.LedgerRepository
import java.util.Calendar
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LedgerViewModel(
    private val ledgerRepository: LedgerRepository
) : ViewModel() {
    private val todayRange = dayRange(System.currentTimeMillis())
    private val monthRange = monthRange(System.currentTimeMillis())

    val uiState = combine(
        ledgerRepository.entries,
        ledgerRepository.observeExpenseSum(todayRange.first, todayRange.second),
        ledgerRepository.observeExpenseSum(monthRange.first, monthRange.second)
    ) { entries, todayExpenseCents, monthExpenseCents ->
        LedgerUiState(
            entries = entries.take(30),
            todayExpenseCents = todayExpenseCents,
            monthExpenseCents = monthExpenseCents
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LedgerUiState()
    )

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
    val todayExpenseCents: Long = 0,
    val monthExpenseCents: Long = 0
)

internal fun parseAmountCents(text: String): Long? {
    val normalized = text.trim().replace(',', '.')
    if (normalized.isBlank()) return null
    val amount = normalized.toDoubleOrNull() ?: return null
    if (amount <= 0.0) return null
    return (amount * 100).roundToLong().takeIf { it > 0 }
}

internal fun formatMoney(cents: Long): String {
    val yuan = cents / 100
    val fen = kotlin.math.abs(cents % 100)
    return "\u00a5$yuan.${fen.toString().padStart(2, '0')}"
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
