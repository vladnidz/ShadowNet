package com.shadow.net

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

class PortsFragment : Fragment() {

    private lateinit var tvTargetHost: TextView
    private lateinit var btnScan: Button
    private lateinit var pbScan: ProgressBar
    private lateinit var tvSummary: TextView
    private lateinit var rvPorts: RecyclerView

    private var targetIp: String = "192.168.1.1"
    private var scanJob: Job? = null

    private val defaultPorts = listOf(
        PortItem(21, "FTP (Файловый сервер)"),
        PortItem(22, "SSH (Удаленный шелл)"),
        PortItem(23, "Telnet (Незащищенный шелл)"),
        PortItem(53, "DNS (Служба доменных имен)"),
        PortItem(80, "HTTP (Веб-интерфейс)"),
        PortItem(443, "HTTPS (Защищенный веб)"),
        PortItem(554, "RTSP (Поток IP-камеры)"),
        PortItem(1883, "MQTT (Умный дом / IoT)"),
        PortItem(3389, "RDP (Удаленный рабочий стол)"),
        PortItem(5555, "ADB (Android Отладка)"),
        PortItem(8080, "HTTP-ALT (Панель управления)")
    )
    private val portsList = mutableListOf<PortItem>()
    private lateinit var portAdapter: PortAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_ports, container, false)
        tvTargetHost = v.findViewById(R.id.tvTargetHost)
        btnScan = v.findViewById(R.id.btnScanPorts)
        pbScan = v.findViewById(R.id.pbPortScan)
        tvSummary = v.findViewById(R.id.tvPortsStatusSummary)
        rvPorts = v.findViewById(R.id.rvPorts)

        portsList.clear()
        defaultPorts.forEach { portsList.add(it.copy()) }

        portAdapter = PortAdapter(portsList)
        rvPorts.layoutManager = LinearLayoutManager(requireContext())
        rvPorts.adapter = portAdapter

        tvTargetHost.text = "Цель: $targetIp"

        btnScan.setOnClickListener {
            if (scanJob?.isActive == true) {
                stopScan()
            } else {
                startPortScan()
            }
        }

        return v
    }

    fun setTarget(ip: String) {
        targetIp = ip
        if (::tvTargetHost.isInitialized) {
            tvTargetHost.text = "Цель: $targetIp"
        }
        startPortScan()
    }

    private fun stopScan() {
        scanJob?.cancel()
        btnScan.text = "⚡ Проверить порты"
        pbScan.visibility = View.INVISIBLE
    }

    private fun startPortScan() {
        portsList.forEach {
            it.isScanned = false
            it.isOpen = false
        }
        portAdapter.notifyDataSetChanged()

        pbScan.visibility = View.VISIBLE
        pbScan.progress = 0
        btnScan.text = "Остановить"
        tvSummary.text = "Проверка портов цели $targetIp..."

        val scanned = AtomicInteger(0)
        val openCount = AtomicInteger(0)

        scanJob = CoroutineScope(Dispatchers.IO).launch {
            coroutineScope {
                val jobs = portsList.mapIndexed { index, item ->
                    async {
                        val isOpen = checkPort(targetIp, item.port)
                        item.isOpen = isOpen
                        item.isScanned = true
                        if (isOpen) openCount.incrementAndGet()

                        val done = scanned.incrementAndGet()
                        withContext(Dispatchers.Main) {
                            pbScan.progress = ((done * 100) / portsList.size)
                            portAdapter.notifyItemChanged(index)
                            tvSummary.text = "Открыто: ${openCount.get()} из $done"
                        }
                    }
                }
                jobs.awaitAll()
            }

            withContext(Dispatchers.Main) {
                stopScan()
                tvSummary.text = "Готово: ${openCount.get()} открытых сервисов"
            }
        }
    }

    private fun checkPort(ip: String, port: Int): Boolean {
        return try {
            val s = Socket()
            s.connect(InetSocketAddress(ip, port), 250)
            s.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}
