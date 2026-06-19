package com.kdlay.meaotodo.sync

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidNsdWifiDiscoveryService(
    private val context: Context
) : WifiDiscoveryService {
    private val _peers = MutableStateFlow<List<WifiPeer>>(emptyList())
    override val peers: StateFlow<List<WifiPeer>> = _peers

    override fun startAdvertising(deviceName: String, role: String, port: Int) {
        // TODO: 使用 Android NsdManager 注册 _meaotodo._tcp 服务。
        // 第一阶段先保留接口，避免 UI/数据层依赖具体网络实现。
        context.applicationContext
    }

    override fun stopAdvertising() {
        // TODO: 停止 NSD 服务注册。
    }

    override fun startDiscovery() {
        // TODO: 使用 Android NsdManager 发现同一 Wi‑Fi 下的 _meaotodo._tcp 服务。
    }

    override fun stopDiscovery() {
        // TODO: 停止 NSD 服务发现。
        _peers.value = emptyList()
    }
}
