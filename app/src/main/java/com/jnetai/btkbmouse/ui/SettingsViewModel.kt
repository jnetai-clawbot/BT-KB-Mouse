package com.jnetai.btkbmouse.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.jnetai.btkbmouse.repository.SettingsRepository
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)

    val settings = settingsRepository.settingsFlow.asLiveData()

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    // Mouse Settings
    fun updateMouseSensitivity(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateMouseSensitivity(value)
        }
    }

    fun updateScrollSpeed(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateScrollSpeed(value)
        }
    }

    fun updateLeftHandedMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateLeftHandedMode(enabled)
        }
    }

    fun updateSmoothAcceleration(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSmoothAcceleration(enabled)
        }
    }

    fun updateInputSmoothing(value: String) {
        viewModelScope.launch {
            settingsRepository.updateInputSmoothing(value)
        }
    }

    // Keyboard Settings
    fun updateKeyRepeatDelay(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateKeyRepeatDelay(value)
        }
    }

    fun updateKeyRepeatRate(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateKeyRepeatRate(value)
        }
    }

    fun updateKeyboardLayout(value: String) {
        viewModelScope.launch {
            settingsRepository.updateKeyboardLayout(value)
        }
    }

    fun updateFunctionKeyMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateFunctionKeyMode(enabled)
        }
    }

    fun updateMediaKeySupport(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateMediaKeySupport(enabled)
        }
    }

    // Connection Settings
    fun updateAutoReconnect(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoReconnect(enabled)
        }
    }

    fun updateAutoConnectStartup(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoConnectStartup(enabled)
        }
    }

    // App Behavior
    fun updateDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDarkTheme(enabled)
        }
    }

    fun updateRunInBackground(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateRunInBackground(enabled)
        }
    }

    fun updatePreventScreenLock(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePreventScreenLock(enabled)
        }
    }

    fun updateLogging(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateLogging(enabled)
        }
    }

    fun updateStartOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateStartOnBoot(enabled)
        }
    }

    // Emulation Settings
    fun updateEmulateKeyboard(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEmulateKeyboard(enabled)
        }
    }

    fun updateEmulateMouse(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEmulateMouse(enabled)
        }
    }

    fun updateEmulateSpeakers(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEmulateSpeakers(enabled)
        }
    }

    fun updateEmulateMic(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateEmulateMic(enabled)
        }
    }

    // Audio Settings
    fun updateSpeakerVolume(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateSpeakerVolume(value)
        }
    }

    fun updateMicGain(value: Int) {
        viewModelScope.launch {
            settingsRepository.updateMicGain(value)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
            _toastMessage.value = "Settings reset to defaults"
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
