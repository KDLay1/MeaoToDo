package com.kdlay.meaotodo.sync

data class WifiPeer(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val role: String,
    val lastSeenAt: Long
)
