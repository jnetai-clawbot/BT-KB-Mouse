package com.jnetai.btkbmouse.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jnetai.btkbmouse.R
import com.jnetai.btkbmouse.data.Device
import com.jnetai.btkbmouse.data.DeviceType
import com.jnetai.btkbmouse.databinding.ItemDeviceBinding

/**
 * RecyclerView adapter for displaying Bluetooth devices.
 * Uses ListAdapter with DiffUtil for efficient updates.
 */
class DeviceAdapter(
    private val onDeviceClick: (Device) -> Unit,
    private val onDeviceLongClick: (Device) -> Unit
) : ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    private val connectionStates = mutableMapOf<String, Boolean>()

    fun updateConnectionState(address: String, isConnected: Boolean) {
        connectionStates[address] = isConnected
        val position = currentList.indexOfFirst { it.address == address }
        if (position != -1) {
            notifyItemChanged(position, "connection_state")
        }
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

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position)
        } else {
            // Partial update for connection state changes
            holder.updateConnectionState(getItem(position))
        }
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
                    true
                } else {
                    false
                }
            }
        }

        fun bind(device: Device) {
            binding.tvDeviceName.text = device.name
            binding.tvDeviceAddress.text = device.address

            // Set device type icon
            val iconRes = when (device.type) {
                DeviceType.MOUSE -> R.drawable.ic_mouse
                DeviceType.KEYBOARD -> R.drawable.ic_keyboard
                else -> R.drawable.ic_devices
            }
            binding.ivDeviceIcon.setImageResource(iconRes)

            // Set connection status
            updateConnectionState(device)

            // Set trusted indicator
            binding.ivTrusted.visibility = if (device.isTrusted) View.VISIBLE else View.GONE

            // Set battery level
            if (device.batteryLevel != null) {
                binding.tvBattery.visibility = View.VISIBLE
                binding.tvBattery.text = "${device.batteryLevel}%"

                val batteryColor = when {
                    device.batteryLevel >= 60 -> R.color.batteryHigh
                    device.batteryLevel >= 30 -> R.color.batteryMedium
                    device.batteryLevel >= 15 -> R.color.batteryLow
                    else -> R.color.batteryCritical
                }
                binding.tvBattery.setTextColor(binding.root.context.getColor(batteryColor))
            } else {
                binding.tvBattery.visibility = View.GONE
            }

            // Set device type text
            val typeText = when (device.type) {
                DeviceType.MOUSE -> binding.root.context.getString(R.string.device_type_mouse)
                DeviceType.KEYBOARD -> binding.root.context.getString(R.string.device_type_keyboard)
                DeviceType.COMBO -> binding.root.context.getString(R.string.device_type_combo)
                else -> binding.root.context.getString(R.string.device_type_unknown)
            }
            binding.tvDeviceType.text = typeText
        }

        fun updateConnectionState(device: Device) {
            val isConnected = connectionStates[device.address] == true

            val statusColor = if (isConnected) {
                R.color.statusConnected
            } else {
                R.color.statusDisconnected
            }
            binding.statusDot.setBackgroundResource(
                if (isConnected) R.drawable.bg_status_dot_connected
                else R.drawable.bg_status_dot_disconnected
            )

            binding.tvConnectionStatus.text = if (isConnected) {
                binding.root.context.getString(R.string.connected)
            } else {
                binding.root.context.getString(R.string.disconnected)
            }
            binding.tvConnectionStatus.setTextColor(binding.root.context.getColor(statusColor))
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
