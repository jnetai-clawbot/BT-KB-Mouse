package com.jnetai.btkbmouse.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jnetai.btkbmouse.BTKBMouseApp
import com.jnetai.btkbmouse.R
import com.jnetai.btkbmouse.data.Device
import com.jnetai.btkbmouse.data.DeviceType
import com.jnetai.btkbmouse.data.Profile
import com.jnetai.btkbmouse.databinding.ActivityConnectionBinding
import com.jnetai.btkbmouse.ui.viewmodel.ConnectionState
import com.jnetai.btkbmouse.ui.viewmodel.ConnectionViewModel
import com.jnetai.btkbmouse.ui.viewmodel.ConnectionViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity for device connection management.
 * Uses StateFlow for reactive UI updates.
 */
class ConnectionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DEVICE_ADDRESS = "device_address"
    }

    private lateinit var binding: ActivityConnectionBinding

    private val viewModel: ConnectionViewModel by viewModels {
        ConnectionViewModelFactory((application as BTKBMouseApp))
    }

    private var connectionTimer: Runnable? = null
    private var connectionStartTime: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
        if (deviceAddress == null) {
            Toast.makeText(this, "Invalid device", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupUI()
        observeViewModel()
        viewModel.loadDevice(deviceAddress)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    startActivity(android.content.Intent(this, SettingsActivity::class.java))
                    true
                }
            }
        }
    }

    private fun setupUI() {
        binding.btnConnect.setOnClickListener {
            when (viewModel.connectionState.value) {
                ConnectionState.DISCONNECTED -> viewModel.connectDevice()
                ConnectionState.CONNECTING -> viewModel.disconnectDevice()
                ConnectionState.CONNECTED -> viewModel.disconnectDevice()
            }
        }

        binding.btnForget.setOnClickListener {
            showConfirmForgetDialog()
        }

        binding.switchTrusted.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setTrusted(isChecked)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe device
                launch {
                    viewModel.device.collect { device ->
                        device?.let { updateDeviceUI(it) }
                    }
                }

                // Observe connection state
                launch {
                    viewModel.connectionState.collect { state ->
                        updateConnectionStateUI(state)
                    }
                }

                // Observe profiles
                launch {
                    viewModel.profiles.collect { profiles ->
                        updateProfileSpinner(profiles)
                    }
                }

                // Observe battery level
                launch {
                    viewModel.batteryLevel.collect { level ->
                        updateBatteryUI(level)
                    }
                }

                // Observe connection time
                launch {
                    viewModel.connectionTime.collect { time ->
                        updateConnectionTime(time)
                    }
                }

                // Observe toast messages
                launch {
                    viewModel.toastMessage.collect { message ->
                        message?.let {
                            Toast.makeText(this@ConnectionActivity, it, Toast.LENGTH_SHORT).show()
                            viewModel.clearToast()
                        }
                    }
                }

                // Observe errors
                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            Toast.makeText(this@ConnectionActivity, it, Toast.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun updateDeviceUI(device: Device) {
        binding.tvDeviceName.text = device.name
        binding.tvDeviceAddress.text = device.address
        binding.tvDeviceType.text = getString(R.string.device_type, device.type.name)

        // Set device icon based on type
        val iconRes = when (device.type) {
            DeviceType.MOUSE -> R.drawable.ic_mouse
            DeviceType.KEYBOARD -> R.drawable.ic_keyboard
            else -> R.drawable.ic_devices
        }
        binding.ivDeviceIcon.setImageResource(iconRes)

        // Set trusted switch
        binding.switchTrusted.isChecked = device.isTrusted
    }

    private fun updateConnectionStateUI(state: ConnectionState) {
        when (state) {
            ConnectionState.DISCONNECTED -> {
                binding.statusDot.setBackgroundResource(R.drawable.bg_status_dot_disconnected)
                binding.tvConnectionStatus.text = getString(R.string.status_disconnected)
                binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.textSecondary))
                binding.btnConnect.text = getString(R.string.connect)
                binding.btnConnect.isEnabled = true
                binding.progressConnection.visibility = View.GONE
                binding.btnConnect.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
                stopConnectionTimer()
            }
            ConnectionState.CONNECTING -> {
                binding.statusDot.setBackgroundResource(R.drawable.bg_status_dot_connecting)
                binding.tvConnectionStatus.text = getString(R.string.connecting)
                binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.accent))
                binding.btnConnect.text = getString(R.string.cancel)
                binding.btnConnect.isEnabled = true
                binding.progressConnection.visibility = View.VISIBLE
                binding.btnConnect.backgroundTintList = ContextCompat.getColorStateList(this, R.color.accent)
                startConnectionTimer()
            }
            ConnectionState.CONNECTED -> {
                binding.statusDot.setBackgroundResource(R.drawable.bg_status_dot_connected)
                binding.tvConnectionStatus.text = getString(R.string.status_connected)
                binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, R.color.statusConnected))
                binding.btnConnect.text = getString(R.string.disconnect)
                binding.btnConnect.isEnabled = true
                binding.progressConnection.visibility = View.GONE
                binding.btnConnect.backgroundTintList = ContextCompat.getColorStateList(this, R.color.error)
                playConnectedAnimation()
            }
        }
    }

    private fun updateBatteryUI(level: Int?) {
        if (level != null) {
            binding.tvBattery.visibility = View.VISIBLE
            binding.tvBattery.text = getString(R.string.battery_level_format, level)

            val colorRes = when {
                level >= 60 -> R.color.batteryHigh
                level >= 30 -> R.color.batteryMedium
                level >= 15 -> R.color.batteryLow
                else -> R.color.batteryCritical
            }
            binding.tvBattery.setTextColor(ContextCompat.getColor(this, colorRes))
        } else {
            binding.tvBattery.visibility = View.GONE
        }
    }

    private fun updateConnectionTime(time: Long?) {
        if (time != null && viewModel.connectionState.value == ConnectionState.CONNECTED) {
            val seconds = (time / 1000) % 60
            val minutes = (time / 1000) / 60
            binding.tvConnectionTime.text = getString(R.string.connection_time_format, minutes, seconds)
            binding.tvConnectionTime.visibility = View.VISIBLE
        } else {
            binding.tvConnectionTime.visibility = View.GONE
        }
    }

    private fun updateProfileSpinner(profiles: List<Profile>) {
        val profileNames = profiles.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, profileNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerProfile.adapter = adapter

        binding.spinnerProfile.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position < profiles.size) {
                    viewModel.selectProfile(profiles[position])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun startConnectionTimer() {
        connectionStartTime = System.currentTimeMillis()
        connectionTimer = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - connectionStartTime
                val seconds = (elapsed / 1000) % 60
                val minutes = (elapsed / 1000) / 60
                binding.tvConnectionTime.text = getString(R.string.connection_time_format, minutes, seconds)
                binding.tvConnectionTime.visibility = View.VISIBLE
                binding.root.postDelayed(this, 1000)
            }
        }
        binding.root.post(connectionTimer!!)
    }

    private fun stopConnectionTimer() {
        connectionTimer?.let { binding.root.removeCallbacks(it) }
        connectionTimer = null
        binding.tvConnectionTime.visibility = View.GONE
    }

    private fun playConnectedAnimation() {
        val scaleX = ObjectAnimator.ofFloat(binding.ivDeviceIcon, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.ivDeviceIcon, "scaleY", 1f, 1.2f, 1f)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 300
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()
    }

    private fun showConfirmForgetDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.forget_device)
            .setMessage(R.string.confirm_forget_device)
            .setPositiveButton(R.string.forget) { _, _ ->
                viewModel.forgetDevice()
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopConnectionTimer()
    }
}
