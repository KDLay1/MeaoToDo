package com.kdlay.meaotodo.ui.ledger

import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LedgerViewModelTest {
    @Test
    fun parseAmountCents_acceptsDecimalAmounts() {
        assertEquals(1234L, parseAmountCents("12.34"))
        assertEquals(1200L, parseAmountCents("12"))
        assertEquals(999L, parseAmountCents("9,99"))
    }

    @Test
    fun parseAmountCents_rejectsInvalidAmounts() {
        assertNull(parseAmountCents(""))
        assertNull(parseAmountCents("abc"))
        assertNull(parseAmountCents("0"))
        assertNull(parseAmountCents("-1"))
        assertNull(parseAmountCents("999999999999999999999999"))
    }

    @Test
    fun parseAmountCents_roundsDecimalInputWithoutDoubleDrift() {
        assertEquals(1L, parseAmountCents("0.005"))
        assertEquals(10L, parseAmountCents("0.10"))
    }

    @Test
    fun buildLedgerUiState_usesAllCurrentMonthEntriesButLimitsRecentList() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 10, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val entries = (1..40).map { index ->
            LedgerEntryEntity(
                id = index.toString(),
                amountCents = 100,
                type = "expense",
                category = "餐饮",
                occurredAt = now,
                createdAt = now,
                updatedAt = now
            )
        }

        val state = buildLedgerUiState(entries, now)

        assertEquals(30, state.entries.size)
        assertEquals(40, state.monthEntries.size)
        assertEquals(4_000L, state.monthExpenseCents)
        assertEquals(4_000L, state.todayExpenseCents)
    }

    @Test
    fun formatMoney_keepsNegativeSubYuanSign() {
        assertEquals("-¥0.50", formatMoney(-50))
    }
}
