package com.shadow.net

data class HostItem(
    val ip: String,
    val latencyMs: Long,
    val typeHint: String
)

data class PortItem(
    val port: Int,
    val name: String,
    var isOpen: Boolean = false,
    var isScanned: Boolean = false
)

data class BleItem(
    val name: String,
    val mac: String,
    val rssi: Int
)
