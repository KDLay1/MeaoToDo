package com.kdlay.meaotodo.sync

import kotlinx.coroutines.flow.Flow

interface WifiDiscoveryService {
    val peers: Flow<List<WifiPeer>>

    fun startAdvertising(deviceName: String, role: String, port: Int)

    fun stopAdvertising()

    fun startDiscovery()

    fun stopDiscovery()
}
