package com.shadow.net

import android.content.Context
import android.net.wifi.WifiManager
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
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger

class LanFragment : Fragment() {

    private lateinit var tvSubnetInfo: TextView
    private lateinit var btnScan: Button
    private lateinit var pbScan: ProgressBar
    private lateinit var tvFoundCount: TextView
    private lateinit var rvHosts: RecyclerView

    private val hostList = mutableListOf<HostItem>()
    private lateinit var hostAdapter: HostAdapter
    private var scanJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_lan, container, false)
        tvSubnetInfo = v.findViewById(R.id.tvLanSubnetInfo)
        btnScan = v.findViewById(R.id.btnScanLan)
        pbScan = v.findViewById(R.id.pbLanScan)
        tvFoundCount = v.findViewById(R.id.tvLanFoundCount)
        rvHosts = v.findViewById(R.id.rvLanHosts)

        hostAdapter = HostAdapter(hostList) { host ->
            (activity as? MainActivity)?.openPortScanner(host.ip)
        }
        rvHosts.layoutManager = LinearLayoutManager(requireContext())
        rvHosts.adapter = hostAdapter

        btnScan.setOnClickListener {
            if (scanJob?.isActive == true) {
                stopScan()
            } else {
                startSubnetScan()
            }
        }

        updateCurrentNetworkInfo()
        return v
    }

    private fun getMyIpAndSubnet(): Pair<String, String>? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress ?: continue
                        val parts = host.split(".")
                        if (parts.size == 4) {
                            return Pair(host, "${parts[0]}.${parts[1]}.${parts[2]}")
                        }
                    }
                }
            }
        } catch (e: Exception) {}
        return null
    }

    private fun updateCurrentNetworkInfo() {
        val info = getMyIpAndSubnet()
        if (info != null) {
            tvSubnetInfo.text = "IP: ${info.first}\nПодсеть: ${info.second}.0/24"
        } else {
            tvSubnetInfo.text = "Wi-Fi: Нет активного соединения"
        }
    }

    private fun stopScan() {
        scanJob?.cancel()
        btnScan.text = "⚡ Сканировать подсеть"
        pbScan.visibility = View.INVISIBLE
    }

    private fun startSubnetScan() {
        val info = getMyIpAndSubnet()
        if (info == null) {
            tvSubnetInfo.text = "Подключите часы к Wi-Fi"
            return
        }

        val myIp = info.first
        val subnet = info.second

        hostList.clear()
        hostAdapter.notifyDataSetChanged()
        tvFoundCount.text = "Сканирование 254 хостов..."
        pbScan.visibility = View.VISIBLE
        pbScan.progress = 0
        btnScan.text = "Остановить"

        val scannedCount = AtomicInteger(0)

        scanJob = CoroutineScope(Dispatchers.IO).launch {
            coroutineScope {
                val jobs = (1..254).map { hostPart ->
                    async {
                        val targetIp = "$subnet.$hostPart"
                        val result = probeHost(targetIp, targetIp == myIp)
                        val done = scannedCount.incrementAndGet()

                        withContext(Dispatchers.Main) {
                            pbScan.progress = ((done * 100) / 254)
                            if (result != null) {
                                hostList.add(result)
                                hostList.sortWith(compareByDescending<HostItem> { it.isCamera }.thenBy { item ->
                                    item.ip.substringAfterLast(".").toIntOrNull() ?: 999
                                })
                                hostAdapter.notifyDataSetChanged()
                                tvFoundCount.text = "Найдено: ${hostList.size} устройств"
                            }
                        }
                    }
                }
                jobs.awaitAll()
            }

            withContext(Dispatchers.Main) {
                stopScan()
                tvFoundCount.text = "Готово: ${hostList.size} активных хостов"
            }
        }
    }

    private fun probeHost(ip: String, isSelf: Boolean): HostItem? {
        val start = System.currentTimeMillis()
        if (isSelf) {
            return HostItem(ip, 0, "Huawei Watch (Текущее устройство)")
        }

        var isReachable = false
        try {
            val addr = InetAddress.getByName(ip)
            if (addr.isReachable(150)) {
                isReachable = true
            }
        } catch (e: Exception) {}

        // Проверяем ключевые порты, в том числе видеопотоки камер (554 RTSP, 8000, 8899 ONVIF)
        var detectedPort = -1
        for (port in listOf(554, 8000, 8899, 80, 443, 5555, 22, 1883, 8080)) {
            try {
                val s = Socket()
                s.connect(InetSocketAddress(ip, port), 90)
                s.close()
                detectedPort = port
                isReachable = true
                break
            } catch (e: Exception) {}
        }

        if (!isReachable) return null

        val latency = maxOf(1, System.currentTimeMillis() - start)

        // Идентификация скрытой камеры
        if (detectedPort == 554 || detectedPort == 8000 || detectedPort == 8899) {
            val details = when (detectedPort) {
                554 -> "RTSP 554"
                8000 -> "Hikvision / Dahua"
                8899 -> "ONVIF Сервис"
                else -> "Видеопоток"
            }
            return HostItem(ip, latency, "Скрытая IP-камера", isCamera = true, cameraDetails = details)
        }

        val hint = when {
            ip.endsWith(".1") -> "Шлюз / Wi-Fi Роутер"
            detectedPort == 22 -> "Linux / SSH Сервер"
            detectedPort == 5555 -> "Android / ADB Устройство"
            detectedPort == 1883 -> "IoT / Умный дом (MQTT)"
            detectedPort == 80 || detectedPort == 443 -> "Веб-сервер / Панель"
            else -> "Устройство сети"
        }
        return HostItem(ip, latency, hint)
    }
}
