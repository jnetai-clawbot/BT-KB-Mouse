package com.jnetai.btkbmouse.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
        // Mouse Settings
        binding.seekbarMouseSensitivity.setOnSeekBarChangeListener(createSeekBarListener { value ->
            viewModel.updateMouseSensitivity(value)
            binding.tvMouseSensitivityValue.text = "$value%"
        })

        binding.seekbarScrollSpeed.setOnSeekBarChangeListener(createSeekBarListener { value ->
            viewModel.updateScrollSpeed(value)
            binding.tvScrollSpeedValue.text = "$value%"
        })

        binding.switchLeftHanded.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateLeftHandedMode(isChecked)
        }

        binding.switchSmoothAcceleration.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateSmoothAcceleration(isChecked)
        }

        // Keyboard Settings
        binding.seekbarKeyRepeatDelay.setOnSeekBarChangeListener(createSeekBarListener { value ->
            viewModel.updateKeyRepeatDelay(value)
            binding.tvKeyRepeatDelayValue.text = "${value}ms"
        })

        binding.seekbarKeyRepeatRate.setOnSeekBarChangeListener(createSeekBarChangeListener { value ->
            viewModel.updateKeyRepeatRate(value)
            binding.tvKeyRepeatRateValue.text = "${value}ms"
        })

        binding.switchFunctionKeys.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateFunctionKeyMode(isChecked)
        }

        binding.switchMediaKeys.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateMediaKeySupport(isChecked)
        }

        // Connection Settings
        binding.switchAutoReconnect.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateAutoReconnect(isChecked)
        }

        binding.switchAutoConnectStartup.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateAutoConnectStartup(isChecked)
        }

        // App Behavior
        binding.switchDarkTheme.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateDarkTheme(isChecked)
        }

        binding.switchRunInBackground.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateRunInBackground(isChecked)
        }

        binding.switchPreventScreenLock.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updatePreventScreenLock(isChecked)
        }

        binding.switchLogging.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateLogging(isChecked)
        }

        binding.switchStartOnBoot.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateStartOnBoot(isChecked)
        }

        // Emulation Settings
        binding.switchEmulateKeyboard.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateEmulateKeyboard(isChecked)
        }

        binding.switchEmulateMouse.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateEmulateMouse(isChecked)
        }

        binding.switchEmulateSpeakers.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateEmulateSpeakers(isChecked)
        }

        binding.switchEmulateMic.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateEmulateMic(isChecked)
        }

        // Audio Settings
        binding.seekbarSpeakerVolume.setOnSeekBarChangeListener(createSeekBarListener { value ->
            viewModel.updateSpeakerVolume(value)
            binding.tvSpeakerVolumeValue.text = "$value%"
        })

        binding.seekbarMicGain.setOnSeekBarChangeListener(createSeekBarListener { value ->
            viewModel.updateMicGain(value)
            binding.tvMicGainValue.text = "$value%"
        })

        // Keyboard Layout Spinner
        val keyboardLayouts = arrayOf("US", "UK", "ISO")
        val layoutAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, keyboardLayouts)
        layoutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerKeyboardLayout.adapter = layoutAdapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe settings
                launch {
                    viewModel.settings.collect { settings ->
                        // Mouse Settings
                        binding.seekbarMouseSensitivity.progress = settings.mouseSensitivity
                        binding.tvMouseSensitivityValue.text = "${settings.mouseSensitivity}%"

                        binding.seekbarScrollSpeed.progress = settings.scrollSpeed
                        binding.tvScrollSpeedValue.text = "${settings.scrollSpeed}%"

                        binding.switchLeftHanded.isChecked = settings.leftHandedMode
                        binding.switchSmoothAcceleration.isChecked = settings.smoothAcceleration

                        // Keyboard Settings
                        binding.seekbarKeyRepeatDelay.progress = settings.keyRepeatDelay
                        binding.tvKeyRepeatDelayValue.text = "${settings.keyRepeatDelay}ms"

                        binding.seekbarKeyRepeatRate.progress = settings.keyRepeatRate
                        binding.tvKeyRepeatRateValue.text = "${settings.keyRepeatRate}ms"

                        binding.switchFunctionKeys.isChecked = settings.functionKeyMode
                        binding.switchMediaKeys.isChecked = settings.mediaKeySupport

                        // Connection Settings
                        binding.switchAutoReconnect.isChecked = settings.autoReconnect
                        binding.switchAutoConnectStartup.isChecked = settings.autoConnectStartup

                        // App Behavior
                        binding.switchDarkTheme.isChecked = settings.darkTheme
                        binding.switchRunInBackground.isChecked = settings.runInBackground
                        binding.switchPreventScreenLock.isChecked = settings.preventScreenLock
                        binding.switchLogging.isChecked = settings.logging
                        binding.switchStartOnBoot.isChecked = settings.startOnBoot

                        // Emulation Settings
                        binding.switchEmulateKeyboard.isChecked = settings.emulateKeyboard
                        binding.switchEmulateMouse.isChecked = settings.emulateMouse
                        binding.switchEmulateSpeakers.isChecked = settings.emulateSpeakers
                        binding.switchEmulateMic.isChecked = settings.emulateMic

                        // Audio Settings
                        binding.seekbarSpeakerVolume.progress = settings.speakerVolume
                        binding.tvSpeakerVolumeValue.text = "${settings.speakerVolume}%"

                        binding.seekbarMicGain.progress = settings.micGain
                        binding.tvMicGainValue.text = "${settings.micGain}%"

                        // Select keyboard layout
                        val layoutIndex = when (settings.keyboardLayout) {
                            "US" -> 0
                            "UK" -> 1
                            "ISO" -> 2
                            else -> 0
                        }
                        binding.spinnerKeyboardLayout.setSelection(layoutIndex)
                    }
                }

                // Observe saving state
                launch {
                    viewModel.isSaving.collect { isSaving ->
                        binding.progressSaving.visibility = if (isSaving) android.view.View.VISIBLE else android.view.View.GONE
                    }
                }

                // Observe toast messages
                launch {
                    viewModel.toastMessage.collect { message ->
                        message?.let {
                            Toast.makeText(this@SettingsActivity, it, Toast.LENGTH_SHORT).show()
                            viewModel.clearToast()
                        }
                    }
                }

                // Observe errors
                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            Toast.makeText(this@SettingsActivity, it, Toast.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun createSeekBarListener(onProgressChanged: (Int) -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    onProgressChanged(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
    }

    private fun createSeekBarChangeListener(onProgressChanged: (Int) -> Unit): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    onProgressChanged(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reset -> {
                showResetConfirmDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showResetConfirmDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.reset_settings)
            .setMessage(R.string.confirm_reset_settings)
            .setPositiveButton(R.string.reset) { _, _ ->
                viewModel.resetToDefaults()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
