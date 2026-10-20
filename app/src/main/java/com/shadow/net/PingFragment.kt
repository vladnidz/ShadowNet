package com.shadow.net

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class PingFragment : Fragment() {

    private lateinit var tvTarget: TextView
    private lateinit var tvCurrentPing: TextView
    private lateinit var tvStats: TextView
    private lateinit var btnToggle: Button

    private var targetHost = "1.1.1.1"
    private var pingJob: Job? = null
    private var sent = 0
    private var received = 0
    private var minPing = Long.MAX_VALUE
    private var maxPing = 0L
    private var totalPing = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_ping, container, false)
        tvTarget = v.findViewById(R.id.tvPingTarget)
        tvCurrentPing = v.findViewById(R.id.tvCurrentPing)
        tvStats = v.findViewById(R.id.tvPingStats)
        btnToggle = v.findViewById(R.id.btnTogglePing)

        tvTarget.text = "Цель: $targetHost (Cloudflare DNS)"

        btnToggle.setOnClickListener {
            if (pingJob?.isActive == true) {
                stopPing()
            } else {
                startPing()
            }
        }

        return v
    }

    private fun startPing() {
        btnToggle.text = "Остановить пинг"
        sent = 0
        received = 0
        minPing = Long.MAX_VALUE
        maxPing = 0L
        totalPing = 0L

        pingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                sent++
                val start = System.currentTimeMillis()
                val success = try {
                    val s = Socket()
                    s.connect(InetSocketAddress(targetHost, 53), 1000)
                    s.close()
                    true
                } catch (e: Exception) {
                    try {
                        InetAddress.getByName(targetHost).isReachable(1000)
                    } catch (ex: Exception) {
                        false
                    }
                }

                val latency = System.currentTimeMillis() - start

                withContext(Dispatchers.Main) {
                    if (success) {
                        received++
                        totalPing += latency
                        minPing = minOf(minPing, latency)
                        maxPing = maxOf(maxPing, latency)
                        val avg = totalPing / received

                        tvCurrentPing.text = "$latency ms"
                        val loss = ((sent - received) * 100) / sent
                        tvStats.text = "Мин: $minPing | Ср: $avg | Макс: $maxPing ms\nПотери: $loss% ($received/$sent)"
                    } else {
                        tvCurrentPing.text = "TIMEOUT"
                        val loss = ((sent - received) * 100) / sent
                        tvStats.text = "Потери: $loss% ($received/$sent)"
                    }
                }

                delay(1000)
            }
        }
    }

    private fun stopPing() {
        pingJob?.cancel()
        btnToggle.text = "Запустить пинг"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopPing()
    }
}
