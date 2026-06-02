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
 * RecyclerView Adapter for device list.
 * Uses ListAdapter with DiffUtil for efficient updates.
 */
class DeviceAdapter(
    private val onDeviceClick: (Device) -> Unit,
    private val onDeviceLongClick: (Device) -> Unit
) : ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    private var connectionStates: Map<String, Boolean> = emptyMap()

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
            super.onBindViewHolder(holder, position, payloads)
        } else {
            // Partial update for connection state changes
            holder.updateConnectionState(getItem(position))
        }
    }

    fun updateConnectionStates(states: Map<String, Boolean>) {
        connectionStates = states
        notifyItemRangeChanged(0, itemCount, PAYLOAD_CONNECTION_STATE)
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
            binding.ivDeviceType.setImageResource(iconRes)

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
        }

        fun updateConnectionState(device: Device) {
            val isConnected = connectionStates[device.address] == true

            val statusColor = if (isConnected) {
                R.color.statusConnected
            } else {
                R.color.statusDisconnected
            }
            binding.viewStatusDot.setBackgroundResource(
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

    /**
     * DiffUtil callback for efficient list updates
     */
    class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: Device, newItem: Device): Any? {
            return PAYLOAD_CONNECTION_STATE
        }
    }

    companion object {
        private const val PAYLOAD_CONNECTION_STATE = "connection_state"
    }
}
