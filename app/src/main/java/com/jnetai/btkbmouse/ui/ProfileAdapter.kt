package com.jnetai.btkbmouse.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jnetai.btkbmouse.R
import com.jnetai.btkbmouse.data.Profile
import com.jnetai.btkbmouse.databinding.ItemProfileBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileAdapter(
    private val onProfileClick: (Profile) -> Unit,
    private val onProfileLongClick: (Profile) -> Unit
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
            binding.root.setOnLongClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onProfileLongClick(getItem(position))
                }
                true
            }
        }

        fun bind(profile: Profile) {
            val context = binding.root.context

            binding.tvProfileName.text = profile.name
            binding.tvMouseSensitivity.text = "Mouse: ${profile.mouseSensitivity}%"
            binding.tvScrollSpeed.text = "Scroll: ${profile.scrollSpeed}%"

            // Show active indicator
            if (profile.isActive) {
                binding.ivActiveIndicator.visibility = View.VISIBLE
                binding.ivActiveIndicator.setColorFilter(ContextCompat.getColor(context, R.color.primary))
            } else {
                binding.ivActiveIndicator.visibility = View.GONE
            }

            // Show last updated time
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            binding.tvLastUpdated.text = "Updated: ${dateFormat.format(Date(profile.updatedAt))}"

            // Set feature indicators
            binding.tvLeftHandedIndicator.visibility = if (profile.leftHandedMode) View.VISIBLE else View.GONE
            binding.tvSmoothAccelIndicator.visibility = if (profile.smoothAcceleration) View.VISIBLE else View.GONE
            binding.tvAutoReconnectIndicator.visibility = if (profile.autoReconnect) View.VISIBLE else View.GONE
        }
    }

    class ProfileDiffCallback : DiffUtil.ItemCallback<Profile>() {
        override fun areItemsTheSame(oldItem: Profile, newItem: Profile): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Profile, newItem: Profile): Boolean {
            return oldItem == newItem
        }
    }
}
