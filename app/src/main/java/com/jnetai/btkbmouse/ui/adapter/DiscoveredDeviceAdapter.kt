package com.jnetai.btkbmouse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jnetai.btkbmouse.data.Device
import com.jnetai.btkbmouse.databinding.ItemDiscoveredDeviceBinding

/**
 * RecyclerView Adapter for discovered Bluetooth devices.
 * Uses ListAdapter with DiffUtil for efficient updates.
 */
class DiscoveredDeviceAdapter(
    private val onDeviceClick: (Device) -> Unit
) : ListAdapter<Device, DiscoveredDeviceAdapter.DiscoveredDeviceViewHolder>(DiscoveredDeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscoveredDeviceViewHolder {
        val binding = ItemDiscoveredDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DiscoveredDeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiscoveredDeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DiscoveredDeviceViewHolder(
        private val binding: ItemDiscoveredDeviceBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeviceClick(getItem(position))
                }
            }
        }

        fun bind(device: Device) {
            binding.tvDeviceName.text = device.name
            binding.tvDeviceAddress.text = device.address
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    class DiscoveredDeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.address == newItem.address
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem == newItem
        }
    }
}
