package com.jnetai.btkbmouse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jnetai.btkbmouse.data.DeviceSettings
import com.jnetai.btkbmouse.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for app settings management.
 * Uses StateFlow for reactive UI updates as per specification.
 */
class SettingsViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    // StateFlow for settings
    private val _settings = MutableStateFlow(DeviceSettings.default())
    val settingsState: StateFlow<DeviceSettings> = _settings.asStateFlow()
    val settings: StateFlow<DeviceSettings> = _settings.asStateFlow()

    // StateFlow for saving state
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // StateFlow for toast messages
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // StateFlow for error messages
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadSettings()
    }

    /**
     * Load settings from repository
     */
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _settings.value = settings
            }
        }
    }

    /**
     * Update mouse sensitivity setting
     */
    fun updateMouseSensitivity(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateMouseSensitivity(value)
        }
    }

    /**
     * Update scroll speed setting
     */
    fun updateScrollSpeed(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateScrollSpeed(value)
        }
    }

    /**
     * Update left-handed mode setting
     */
    fun updateLeftHandedMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateLeftHandedMode(enabled)
        }
    }

    /**
     * Update smooth acceleration setting
     */
    fun updateSmoothAcceleration(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSmoothAcceleration(enabled)
        }
    }

    /**
     * Update input smoothing level
     */
    fun updateInputSmoothing(value: String) {
        viewModelScope.launch {
            settingsRepository.updateInputSmoothing(value)
        }
    }

    // Keyboard Settings

    /**
     * Update key repeat delay
     */
    fun updateKeyRepeatDelay(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateKeyRepeatDelay(value)
        }
    }

    /**
     * Update key repeat rate
     */
    fun updateKeyRepeatRate(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateKeyRepeatRate(value)
        }
    }

    /**
     * Update keyboard layout
     */
    fun updateKeyboardLayout(value: String) {
        viewModelScope.launch {
            settingsRepository.updateKeyboardLayout(value)
        }
    }

    /**
     * Update function key mode
     */
    fun updateFunctionKeyMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateFunctionKeyMode(enabled)
        }
    }

    /**
     * Update media key support
     */
    fun updateMediaKeySupport(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateMediaKeySupport(enabled)
        }
    }

    // Connection Settings

    /**
     * Update auto-reconnect setting
     */
    fun updateAutoReconnect(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoReconnect(enabled)
        }
    }

    /**
     * Update auto-connect on startup setting
     */
    fun updateAutoConnectStartup(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoConnectStartup(enabled)
        }
    }

    // App Behavior

    /**
     * Update dark theme setting
     */
    fun updateDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDarkTheme(enabled)
        }
    }

    /**
     * Update run in background setting
     */
    fun updateRunInBackground(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateRunInBackground(enabled)
        }
    }

    /**
     * Update prevent screen lock setting
     */
    fun updatePreventScreenLock(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePreventScreenLock(enabled)
        }
    }

    /**
     * Update logging setting
     */
    fun updateLogging(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateLogging(enabled)
        }
    }

    /**
     * Update start on boot setting
     */
    fun updateStartOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateStartOnBoot(enabled)
        }
    }

    // Emulation Settings

    /**
     * Update emulate keyboard setting
     */
    fun updateEmulateKeyboard(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEmulateKeyboard(enabled)
        }
    }

    /**
     * Update emulate mouse setting
     */
    fun updateEmulateMouse(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEmulateMouse(enabled)
        }
    }

    /**
     * Update emulate speakers setting
     */
    fun updateEmulateSpeakers(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEmulateSpeakers(enabled)
        }
    }

    /**
     * Update emulate mic setting
     */
    fun updateEmulateMic(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEmulateMic(enabled)
        }
    }

    // Audio Settings

    /**
     * Update speaker volume setting
     */
    fun updateSpeakerVolume(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateSpeakerVolume(value)
        }
    }

    /**
     * Update mic gain setting
     */
    fun updateMicGain(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateMicGain(value)
        }
    }

    /**
     * Save all settings
     */
    fun saveSettings() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                settingsRepository.saveSettings(_settings.value)
                _toastMessage.value = "Settings saved"
            } catch (e: Exception) {
                _error.value = "Failed to save settings: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
            _settings.value = DeviceSettings.default()
            _toastMessage.value = "Settings reset to defaults"
        }
    }

    /**
     * Update a specific setting
     */
    fun updateSetting(key: String, value: Any) {
        viewModelScope.launch {
            when (value) {
                is Int -> settingsRepository.updateSetting(key, value)
                is Boolean -> settingsRepository.updateSetting(key, value)
                is String -> settingsRepository.updateSetting(key, value)
                is Float -> settingsRepository.updateSetting(key, value)
            }
        }
    }

    /**
     * Clear toast message
     */
    fun clearToast() {
        _toastMessage.value = null
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
}
