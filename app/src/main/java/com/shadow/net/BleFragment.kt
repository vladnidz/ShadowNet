package com.shadow.net

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BleFragment : Fragment() {

    private lateinit var tvStatus: TextView
    private lateinit var btnScan: Button
    private lateinit var pbScan: ProgressBar
    private lateinit var tvCount: TextView
    private lateinit var rvDevices: RecyclerView

    private val bleList = mutableListOf<BleItem>()
    private lateinit var bleAdapter: BleAdapter

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())
    private var lastVibrateTime = 0L

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val device = result.device ?: return
            val record = result.scanRecord
            val mac = device.address
            val rssi = result.rssi

            var isTracker = false
            var trackerType = ""

            // 1. Детекция Apple AirTag / Find My сети
            val appleData = record?.getManufacturerSpecificData(0x004C)
            if (appleData != null) {
                if (appleData.size >= 2 && (appleData[0].toInt() and 0xFF) == 0x12) {
                    isTracker = true
                    trackerType = "Apple AirTag (FindMy)"
                } else if (appleData.size >= 2 && (appleData[0].toInt() and 0xFF) == 0x07) {
                    trackerType = "Apple AirPods / Beats"
                } else {
                    trackerType = "Устройство Apple"
                }
            }

            // 2. Детекция Samsung SmartTag
            val samsungData = record?.getManufacturerSpecificData(0x0075)
            if (samsungData != null) {
                if (samsungData.size >= 2 && (samsungData[0].toInt() and 0xFF) == 0x42) {
                    isTracker = true
                    trackerType = "Samsung SmartTag"
                } else {
                    trackerType = "Устройство Samsung"
                }
            }

            // 3. Детекция Tile Tracker
            val serviceUuids = record?.serviceUuids
            if (serviceUuids != null) {
                for (u in serviceUuids) {
                    val s = u.uuid.toString().uppercase()
                    if (s.contains("FEED") || s.contains("FEEC")) {
                        isTracker = true
                        trackerType = "Tile Tracker"
                        break
                    }
                }
            }

            val displayName = when {
                trackerType.isNotBlank() -> trackerType
                !record?.deviceName.isNullOrBlank() -> record!!.deviceName!!
                !device.name.isNullOrBlank() -> device.name!!
                else -> "BLE Маяк"
            }

            val proximity = when {
                rssi >= -50 -> "В упор (< 0.5м)"
                rssi >= -65 -> "Рядом (1–2м)"
                rssi >= -80 -> "В комнате (3–5м)"
                else -> "Вдали (> 5м)"
            }

            if (isTracker && rssi >= -65) {
                triggerHapticAlert()
            }

            val item = BleItem(
                name = displayName,
                mac = mac,
                rssi = rssi,
                isTracker = isTracker,
                trackerType = trackerType,
                proximityLabel = proximity
            )

            val existingIndex = bleList.indexOfFirst { it.mac == mac }
            if (existingIndex >= 0) {
                bleList[existingIndex] = item
            } else {
                bleList.add(item)
            }

            // Трекеры всегда первыми в списке, затем по мощности сигнала
            bleList.sortWith(compareByDescending<BleItem> { it.isTracker }.thenByDescending { it.rssi })
            bleAdapter.notifyDataSetChanged()

            val trackersCount = bleList.count { it.isTracker }
            if (trackersCount > 0) {
                tvCount.text = "⚠️ Найдено меток: $trackersCount (всего ${bleList.size})"
                tvCount.setTextColor(0xFFFF3B30.toInt())
            } else {
                tvCount.text = "Найдено: ${bleList.size} устройств"
                tvCount.setTextColor(0xFFFFB800.toInt())
            }
        }
    }

    private fun triggerHapticAlert() {
        val now = System.currentTimeMillis()
        if (now - lastVibrateTime > 1500) {
            lastVibrateTime = now
            try {
                val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator?.vibrate(70)
                }
            } catch (e: Exception) {}
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_ble, container, false)
        tvStatus = v.findViewById(R.id.tvBleStatus)
        btnScan = v.findViewById(R.id.btnScanBle)
        pbScan = v.findViewById(R.id.pbBleScan)
        tvCount = v.findViewById(R.id.tvBleCount)
        rvDevices = v.findViewById(R.id.rvBleDevices)

        val bm = requireContext().getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bm?.adapter

        bleAdapter = BleAdapter(bleList)
        rvDevices.layoutManager = LinearLayoutManager(requireContext())
        rvDevices.adapter = bleAdapter

        btnScan.setOnClickListener {
            if (isScanning) {
                stopBleScan()
            } else {
                startBleScan()
            }
        }

        return v
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        if (bluetoothAdapter?.isEnabled != true) {
            tvStatus.text = "Bluetooth выключен"
            return
        }

        bleList.clear()
        bleAdapter.notifyDataSetChanged()
        isScanning = true
        btnScan.text = "Остановить"
        pbScan.visibility = View.VISIBLE
        tvStatus.text = "Поиск маяков и AirTag..."
        tvCount.text = "Сканирование..."

        try {
            val scanner = bluetoothAdapter?.bluetoothLeScanner
            scanner?.startScan(scanCallback)

            handler.postDelayed({
                if (isScanning) stopBleScan()
            }, 20000)
        } catch (e: Exception) {
            tvStatus.text = "Ошибка сканера: ${e.message}"
            stopBleScan()
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopBleScan() {
        isScanning = false
        btnScan.text = "📡 Сканировать BLE"
        pbScan.visibility = View.INVISIBLE
        tvStatus.text = "Сканирование завершено"
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: Exception) {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopBleScan()
    }
}
