package com.shadow.net

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return
            val device = result.device ?: return
            val name = result.scanRecord?.deviceName ?: device.name ?: "Скрытый BLE маяк"
            val mac = device.address
            val rssi = result.rssi

            val existingIndex = bleList.indexOfFirst { it.mac == mac }
            if (existingIndex >= 0) {
                bleList[existingIndex] = BleItem(name, mac, rssi)
            } else {
                bleList.add(BleItem(name, mac, rssi))
            }
            bleList.sortByDescending { it.rssi }
            bleAdapter.notifyDataSetChanged()
            tvCount.text = "Найдено: ${bleList.size} BLE устройств"
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
        tvStatus.text = "Поиск радиомаяков..."
        tvCount.text = "Сканирование..."

        try {
            val scanner = bluetoothAdapter?.bluetoothLeScanner
            scanner?.startScan(scanCallback)

            // Автоостановка через 15 секунд для экономии батареи часов
            handler.postDelayed({
                if (isScanning) stopBleScan()
            }, 15000)
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
