package com.jnetai.btkbmouse.ui

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jnetai.btkbmouse.databinding.ItemDiscoveredDeviceBinding

class DiscoveredDeviceAdapter(
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : ListAdapter<BluetoothDevice, DiscoveredDeviceAdapter.DiscoveredDeviceViewHolder>(DiscoveredDeviceDiffCallback()) {

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

    fun clear() {
        submitList(emptyList())
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

        fun bind(device: BluetoothDevice) {
            binding.tvDeviceName.text = device.name ?: "Unknown Device"
            binding.tvDeviceAddress.text = device.address
        }
    }

    class DiscoveredDeviceDiffCallback : DiffUtil.ItemCallback<BluetoothDevice>() {
        override fun areItemsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
            return oldItem.address == newItem.address
        }

        override fun areContentsTheSame(oldItem: BluetoothDevice, newItem: BluetoothDevice): Boolean {
            return oldItem.address == newItem.address && oldItem.name == newItem.name
        }
    }
}
