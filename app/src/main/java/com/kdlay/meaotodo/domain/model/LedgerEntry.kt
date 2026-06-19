package com.kdlay.meaotodo.domain.model

data class LedgerEntry(
    val id: String,
    val amountCents: Long,
    val type: String,
    val category: String,
    val note: String,
    val occurredAt: Long
)
