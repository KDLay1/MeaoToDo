package com.kdlay.meaotodo.ui.ledger

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
    }
}
