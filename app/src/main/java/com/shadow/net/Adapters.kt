package com.shadow.net

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HostAdapter(
    private val hosts: List<HostItem>,
    private val onHostClick: (HostItem) -> Unit
) : RecyclerView.Adapter<HostAdapter.HostViewHolder>() {

    class HostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView = view.findViewById(R.id.tvHostIcon)
        val tvIp: TextView = view.findViewById(R.id.tvHostIp)
        val tvHint: TextView = view.findViewById(R.id.tvHostHint)
        val tvPing: TextView = view.findViewById(R.id.tvHostPing)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HostViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_host, parent, false)
        return HostViewHolder(v)
    }

    override fun onBindViewHolder(holder: HostViewHolder, position: Int) {
        val item = hosts[position]
        holder.tvIp.text = item.ip
        holder.tvHint.text = item.typeHint
        holder.tvPing.text = "${item.latencyMs} ms"

        if (item.ip.endsWith(".1")) {
            holder.tvIcon.text = "🌐"
        } else if (item.typeHint.contains("Часы", ignoreCase = true)) {
            holder.tvIcon.text = "⌚"
        } else {
            holder.tvIcon.text = "💻"
        }

        holder.itemView.setOnClickListener {
            onHostClick(item)
        }
    }

    override fun getItemCount(): Int = hosts.size
}

class PortAdapter(
    private val ports: List<PortItem>
) : RecyclerView.Adapter<PortAdapter.PortViewHolder>() {

    class PortViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNum: TextView = view.findViewById(R.id.tvPortNum)
        val tvName: TextView = view.findViewById(R.id.tvPortName)
        val tvStatus: TextView = view.findViewById(R.id.tvPortStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PortViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_port, parent, false)
        return PortViewHolder(v)
    }

    override fun onBindViewHolder(holder: PortViewHolder, position: Int) {
        val item = ports[position]
        holder.tvNum.text = item.port.toString()
        holder.tvName.text = item.name

        if (!item.isScanned) {
            holder.tvStatus.text = "..."
            holder.tvStatus.setTextColor(0xFF8E9992.toInt())
        } else if (item.isOpen) {
            holder.tvStatus.text = "OPEN"
            holder.tvStatus.setTextColor(0xFF00FF66.toInt())
        } else {
            holder.tvStatus.text = "CLOSED"
            holder.tvStatus.setTextColor(0xFF556059.toInt())
        }
    }

    override fun getItemCount(): Int = ports.size
}

class BleAdapter(
    private val devices: List<BleItem>
) : RecyclerView.Adapter<BleAdapter.BleViewHolder>() {

    class BleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvBleName)
        val tvMac: TextView = view.findViewById(R.id.tvBleMac)
        val tvRssi: TextView = view.findViewById(R.id.tvBleRssi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BleViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_ble, parent, false)
        return BleViewHolder(v)
    }

    override fun onBindViewHolder(holder: BleViewHolder, position: Int) {
        val item = devices[position]
        holder.tvName.text = item.name
        holder.tvMac.text = item.mac
        holder.tvRssi.text = "${item.rssi} dBm"

        val color = when {
            item.rssi >= -55 -> 0xFF00FF66.toInt() // Green (very close)
            item.rssi >= -75 -> 0xFFFFB800.toInt() // Amber (medium)
            else -> 0xFFFF3B30.toInt()             // Red (far)
        }
        holder.tvRssi.setTextColor(color)
    }

    override fun getItemCount(): Int = devices.size
}
