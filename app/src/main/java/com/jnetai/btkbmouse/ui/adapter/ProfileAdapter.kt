package com.jnetai.btkbmouse.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jnetai.btkbmouse.data.Profile
import com.jnetai.btkbmouse.databinding.ItemProfileBinding

/**
 * RecyclerView Adapter for profile list.
 * Uses ListAdapter with DiffUtil for efficient updates.
 */
class ProfileAdapter(
    private val onProfileClick: (Profile) -> Unit,
    private val onProfileEditClick: (Profile) -> Unit,
    private val onProfileDeleteClick: (Profile) -> Unit
) : ListAdapter<Profile, ProfileAdapter.ProfileViewHolder>(ProfileDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        val binding = ItemProfileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProfileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProfileViewHolder(
        private val binding: ItemProfileBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onProfileClick(getItem(position))
                }
            }

            binding.btnEdit.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onProfileEditClick(getItem(position))
                }
            }

            binding.btnDelete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onProfileDeleteClick(getItem(position))
                }
            }
        }

        fun bind(profile: Profile) {
            binding.tvProfileName.text = profile.name
            binding.tvMouseSensitivity.text = "Mouse: ${profile.mouseSensitivity}%"
            binding.tvScrollSpeed.text = "Scroll: ${profile.scrollSpeed}%"

            // Show device address if available
            if (profile.deviceAddress.isNotEmpty()) {
                binding.tvDeviceAddress.text = profile.deviceAddress
            } else {
                binding.tvDeviceAddress.text = "Global Profile"
            }
        }
    }

    /**
     * DiffUtil callback for efficient list updates
     */
    class ProfileDiffCallback : DiffUtil.ItemCallback<Profile>() {
        override fun areItemsTheSame(oldItem: Profile, newItem: Profile): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Profile, newItem: Profile): Boolean {
            return oldItem == newItem
        }
    }
}
