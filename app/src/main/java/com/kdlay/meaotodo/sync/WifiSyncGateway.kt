package com.kdlay.meaotodo.sync

import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao

class WifiSyncGateway(
    private val syncOutboxDao: SyncOutboxDao
) {
    suspend fun buildPendingBatch(fromDeviceId: String, limit: Int = 100): SyncBatch {
        val pending = syncOutboxDao.pending(limit)
        return SyncBatch(
            fromDeviceId = fromDeviceId,
            changes = pending.map { change ->
                SyncChange(
                    id = change.id,
                    entityType = change.entityType,
                    entityId = change.entityId,
                    operation = change.operation,
                    payloadJson = change.payloadJson,
                    createdAt = change.createdAt
                )
            }
        )
    }

    suspend fun markAccepted(ack: SyncAck) {
        if (ack.acceptedChangeIds.isNotEmpty()) {
            syncOutboxDao.markDelivered(
                ids = ack.acceptedChangeIds,
                deliveredAt = System.currentTimeMillis()
            )
        }
    }
}
