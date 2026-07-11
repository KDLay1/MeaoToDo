package com.kdlay.meaotodo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "ledger_entries")
@Serializable
data class LedgerEntryEntity(
    @PrimaryKey val id: String,
    val amountCents: Long,
    val type: String,
    val category: String,
    val note: String = "",
    val occurredAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null
)
