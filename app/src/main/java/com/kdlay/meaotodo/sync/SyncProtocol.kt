package com.kdlay.meaotodo.sync

data class SyncHello(
    val deviceId: String,
    val deviceName: String,
    val role: String,
    val protocolVersion: Int = 1
)

data class SyncChange(
    val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payloadJson: String,
    val createdAt: Long
)

data class SyncBatch(
    val fromDeviceId: String,
    val changes: List<SyncChange>
)

data class SyncAck(
    val acceptedChangeIds: List<String>,
    val rejectedChangeIds: List<String> = emptyList()
)
