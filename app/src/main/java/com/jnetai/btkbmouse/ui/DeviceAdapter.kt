package com.jnetai.btkbmouse.ui

import android.bluetooth.BluetoothProfile
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jnetai.btkbmouse.R
import com.jnetai.btkbmouse.data.Device
import com.jnetai.btkbmouse.databinding.ItemDeviceBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceAdapter(
    private val onDeviceClick: (Device) -> Unit,
    private val onDeviceLongClick: (Device) -> Unit
) : ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    private var connectionStates: Map<String, Int> = emptyMap()

    fun updateConnectionStates(states: Map<String, Int>) {
        connectionStates = states
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DeviceViewHolder(
        private val binding: ItemDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeviceClick(getItem(position))
                }
            }

            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeviceLongClick(getItem(position))
                }
                true
            }
        }

        fun bind(device: Device) {
            val context = binding.root.context

            binding.tvDeviceName.text = device.name
            binding.tvDeviceAddress.text = device.address
            binding.tvDeviceType.text = device.type

            // Set device type icon
            val iconRes = when (device.type.uppercase()) {
                "MOUSE" -> R.drawable.ic_mouse
                "KEYBOARD" -> R.drawable.ic_keyboard
                else -> R.drawable.ic_devices
            }
            binding.ivDeviceIcon.setImageResource(iconRes)

            // Set last connected time
            binding.tvLastConnected.text = formatLastConnected(device.lastConnected)

            // Set trusted indicator
            if (device.isTrusted) {
                binding.ivTrusted.visibility = View.VISIBLE
                binding.ivTrusted.setColorFilter(ContextCompat.getColor(context, R.color.primary))
            } else {
                binding.ivTrusted.visibility = View.GONE
            }

            // Set battery indicator
            device.batteryLevel?.let { level ->
                binding.tvBattery.visibility = View.VISIBLE
                binding.tvBattery.text = "$level%"
                val batteryColor = when {
                    level > 80 -> R.color.statusConnected
                    level > 20 -> R.color.warning
                    else -> R.color.error
                }
                binding.tvBattery.setTextColor(ContextCompat.getColor(context, batteryColor))
            } ?: run {
                binding.tvBattery.visibility = View.GONE
            }

            // Set connection status
            val isConnected = connectionStates[device.address] == BluetoothProfile.STATE_CONNECTED
            if (isConnected) {
                binding.statusDot.setColorFilter(ContextCompat.getColor(context, R.color.statusConnected))
                binding.tvConnectionStatus.text = "Connected"
                binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(context, R.color.statusConnected))
            } else {
                binding.statusDot.setColorFilter(ContextCompat.getColor(context, R.color.textSecondary))
                binding.tvConnectionStatus.text = "Disconnected"
                binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(context, R.color.textSecondary))
            }
        }

        private fun formatLastConnected(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000} min ago"
                diff < 86400000 -> "${diff / 3600000} hours ago"
                diff < 604800000 -> "${diff / 86400000} days ago"
                else -> {
                    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                    dateFormat.format(Date(timestamp))
                }
            }
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.address == newItem.address
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem == newItem
        }
    }
}
