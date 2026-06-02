package com.jnetai.btkbmouse.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jnetai.btkbmouse.BTKBMouseApp
import com.jnetai.btkbmouse.R
import com.jnetai.btkbmouse.databinding.ActivitySettingsBinding
import com.jnetai.btkbmouse.ui.viewmodel.SettingsViewModel
import com.jnetai.btkbmouse.ui.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.launch

/**
 * Activity for app settings management.
 * Uses StateFlow for reactive UI updates.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory((application as BTKBMouseApp))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupUI()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupUI() {
        // Mouse Sensitivity
        binding.sliderMouseSensitivity.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.updateMouseSensitivity(value.toInt())
                binding.tvMouseSensitivityValue.text = "${value.toInt()}%"
            }
        }

        // Scroll Speed
        binding.sliderScrollSpeed.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.updateScrollSpeed(value.toInt())
                binding.tvScrollSpeedValue.text = "${value.toInt()}%"
            }
        }

        // Left Handed Mode
        binding.switchLeftHanded.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateLeftHandedMode(isChecked)
        }

        // Smooth Acceleration
        binding.switchSmoothAcceleration.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateSmoothAcceleration(isChecked)
        }

        // Key Repeat Delay
        binding.sliderKeyRepeatDelay.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.updateKeyRepeatDelay(value.toInt())
                binding.tvKeyRepeatDelayValue.text = "${value.toInt()}ms"
            }
        }

        // Key Repeat Rate
        binding.sliderKeyRepeatRate.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                viewModel.updateKeyRepeatRate(value.toInt())
                binding.tvKeyRepeatRateValue.text = "${value.toInt()}ms"
            }
        }

        // Auto Reconnect
        binding.switchAutoReconnect.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateAutoReconnect(isChecked)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.settingsState.collect { settings ->
                // Update UI with current settings
                binding.sliderMouseSensitivity.value = settings.mouseSensitivity.toFloat()
                binding.tvMouseSensitivityValue.text = "${settings.mouseSensitivity}%"

                binding.sliderScrollSpeed.value = settings.scrollSpeed.toFloat()
                binding.tvScrollSpeedValue.text = "${settings.scrollSpeed}%"

                binding.switchLeftHanded.isChecked = settings.leftHandedMode
                binding.switchSmoothAcceleration.isChecked = settings.smoothAcceleration

                binding.sliderKeyRepeatDelay.value = settings.keyRepeatDelay.toFloat()
                binding.tvKeyRepeatDelayValue.text = "${settings.keyRepeatDelay}ms"

                binding.sliderKeyRepeatRate.value = settings.keyRepeatRate.toFloat()
                binding.tvKeyRepeatRateValue.text = "${settings.keyRepeatRate}ms"

                binding.switchAutoReconnect.isChecked = settings.autoReconnect
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reset -> {
                showResetConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showResetConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.reset_settings)
            .setMessage(R.string.confirm_reset_settings)
            .setPositiveButton(R.string.reset) { _, _ ->
                viewModel.resetToDefaults()
                Toast.makeText(this, R.string.settings_reset, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
