package com.shadow.net

data class HostItem(
    val ip: String,
    val latencyMs: Long,
    val typeHint: String,
    val isCamera: Boolean = false,
    val cameraDetails: String = ""
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
    val rssi: Int,
    val isTracker: Boolean = false,
    val trackerType: String = "",
    val proximityLabel: String = ""
)
