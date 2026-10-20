package com.shadow.net

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class HostAdapter(
    private val hosts: List<HostItem>,
    private val onHostClick: (HostItem) -> Unit
) : RecyclerView.Adapter<HostAdapter.HostViewHolder>() {

    class HostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view as MaterialCardView
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
        holder.tvPing.text = "${item.latencyMs} ms"

        if (item.isCamera) {
            holder.tvIcon.text = "📹"
            holder.tvHint.text = "⚠️ Скрытая камера (${item.cameraDetails})"
            holder.tvHint.setTextColor(0xFFFF3B30.toInt())
            holder.cardView.strokeColor = 0xFFFF3B30.toInt()
        } else {
            holder.tvHint.text = item.typeHint
            holder.tvHint.setTextColor(0xFF8E9992.toInt())
            holder.cardView.strokeColor = 0xFF1F2B23.toInt()

            if (item.ip.endsWith(".1")) {
                holder.tvIcon.text = "🌐"
            } else if (item.typeHint.contains("Часы", ignoreCase = true)) {
                holder.tvIcon.text = "⌚"
            } else {
                holder.tvIcon.text = "💻"
            }
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
        val cardBle: MaterialCardView = view as MaterialCardView
        val tvIcon: TextView = view.findViewById(R.id.tvBleIcon)
        val tvName: TextView = view.findViewById(R.id.tvBleName)
        val tvMac: TextView = view.findViewById(R.id.tvBleMac)
        val tvRssi: TextView = view.findViewById(R.id.tvBleRssi)
        val tvProximity: TextView = view.findViewById(R.id.tvBleProximity)
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
        holder.tvProximity.text = item.proximityLabel

        if (item.isTracker) {
            holder.cardBle.strokeColor = 0xFFFF3B30.toInt()
            holder.tvName.setTextColor(0xFFFF3B30.toInt())
            holder.tvIcon.text = when {
                item.trackerType.contains("Apple", ignoreCase = true) -> "🍎"
                item.trackerType.contains("Samsung", ignoreCase = true) -> "🔷"
                else -> "⚠️"
            }
        } else {
            holder.cardBle.strokeColor = 0xFF1F2B23.toInt()
            holder.tvName.setTextColor(0xFFFFFFFF.toInt())
            holder.tvIcon.text = "📡"
        }

        val color = when {
            item.rssi >= -55 -> 0xFF00FF66.toInt() // Green (very close)
            item.rssi >= -75 -> 0xFFFFB800.toInt() // Amber (medium)
            else -> 0xFFFF3B30.toInt()             // Red (far)
        }
        holder.tvRssi.setTextColor(color)
    }

    override fun getItemCount(): Int = devices.size
}
