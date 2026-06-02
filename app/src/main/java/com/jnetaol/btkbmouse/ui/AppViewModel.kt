package com.jnetaol.btkbmouse.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jnetaol.btkbmouse.bluetooth.BluetoothManager
import com.jnetaol.btkbmouse.data.db.AppDatabase
import com.jnetaol.btkbmouse.data.model.*
import com.jnetaol.btkbmouse.logger.DebugLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    val btManager = BluetoothManager(application)
    private val db = AppDatabase.getInstance(application)
    private val profileDao = db.profileDao()
    private val settingsDao = db.settingsDao()
    private val presetDao = db.presetPhraseDao()
    private val clipboardDao = db.clipboardDao()
    private val emulatedDeviceDao = db.emulatedDeviceDao()

    val profiles: StateFlow<List<Profile>> = profileDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val presets: StateFlow<List<PresetPhrase>> = presetDao.getAllPhrases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clipboardEntries: StateFlow<List<ClipboardEntry>> = clipboardDao.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emulatedDevices: StateFlow<List<EmulatedDevice>> = emulatedDeviceDao.getAllDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeProfile = MutableStateFlow<Profile?>(null)
    val activeProfile: StateFlow<Profile?> = _activeProfile.asStateFlow()

    private val _settingsSnapshot = MutableStateFlow<Map<String, String>>(emptyMap())
    val settingsSnapshot: StateFlow<Map<String, String>> = _settingsSnapshot.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _activeProfile.value = profileDao.getActiveProfile()
                initializeEmulatedDevices()
                loadSettingsSnapshot()
                DebugLogger.i("AppVM", "BK-001 ViewModel initialized")
            } catch (e: Exception) {
                DebugLogger.e("AppVM", "BK-002 Init error", e)
            }
        }
    }

    private suspend fun initializeEmulatedDevices() {
        val defaults = listOf(
            EmulatedDevice("keyboard", true, "Keyboard", "keyboard"),
            EmulatedDevice("mouse", true, "Mouse", "mouse"),
            EmulatedDevice("speaker", false, "Speaker", "speaker"),
            EmulatedDevice("microphone", false, "Microphone", "mic"),
            EmulatedDevice("webcam", false, "Webcam", "videocam"),
            EmulatedDevice("gamepad", false, "Gamepad", "gamepad"),
            EmulatedDevice("scanner", false, "Barcode Scanner", "scanner")
        )
        defaults.forEach { device ->
            val existing = emulatedDeviceDao.getAllDevices().first().find { it.deviceType == device.deviceType }
            if (existing == null) {
                emulatedDeviceDao.setDevice(device)
            }
        }
    }

    private suspend fun loadSettingsSnapshot() {
        try {
            val all = settingsDao.getAllSettings().first()
            _settingsSnapshot.value = all.associate { it.key to it.value }
        } catch (e: Exception) {
            DebugLogger.e("AppVM", "BK-003 Settings load error", e)
        }
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            try {
                val profile = Profile(name = name, deviceName = btManager.connectionState.value.deviceName,
                    deviceAddress = btManager.connectionState.value.deviceAddress)
                profileDao.insertProfile(profile)
                DebugLogger.i("AppVM", "BK-010 Profile created: $name")
            } catch (e: Exception) {
                DebugLogger.e("AppVM", "BK-004 Profile create error", e)
            }
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            try {
                profileDao.deleteProfile(profile)
                DebugLogger.i("AppVM", "BK-011 Profile deleted: ${profile.name}")
            } catch (e: Exception) {
                DebugLogger.e("AppVM", "BK-005 Profile delete error", e)
            }
        }
    }

    fun setActiveProfile(profile: Profile) {
        viewModelScope.launch {
            try {
                profileDao.deactivateAllProfiles()
                profileDao.setActiveProfile(profile.id)
                _activeProfile.value = profileDao.getProfileById(profile.id)
                DebugLogger.i("AppVM", "BK-012 Active profile: ${profile.name}")
            } catch (e: Exception) {
                DebugLogger.e("AppVM", "BK-006 Set active profile error", e)
            }
        }
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            try {
                profileDao.updateProfile(profile.copy(updatedTimestamp = System.currentTimeMillis()))
                if (_activeProfile.value?.id == profile.id) {
                    _activeProfile.value = profile
                }
                DebugLogger.i("AppVM", "BK-013 Profile updated: ${profile.name}")
            } catch (e: Exception) {
                DebugLogger.e("AppVM", "BK-007 Profile update error", e)
            }
        }
    }

    fun saveSetting(key: String, value: String) {
        viewModelScope.launch {
            try {
                settingsDao.setSetting(AppSetting(key, value))
                _settingsSnapshot.value = _settingsSnapshot.value.toMutableMap().apply { put(key, value) }
            } catch (e: Exception) {
                DebugLogger.e("AppVM", "BK-008 Setting save error", e)
            }
        }
    }

    fun revertSettings() {
        viewModelScope.launch {
            try {
                loadSettingsSnapshot()
                DebugLogger.i("AppVM", "BK-014 Settings reverted")
            } catch (e: Exception) {
                DebugLogger.e("AppVM", "BK-009 Revert settings error", e)
            }
        }
    }

    fun addPresetPhrase(label: String, text: String) {
        viewModelScope.launch {
            try {
                presetDao.insertPhrase(PresetPhrase(label = label, text = text))
                DebugLogger.i("AppVM", "BK-015 Preset added: $label")
            } catch (e: Exception) {
                DebugLogger.e("AppVM", "BK-010 Preset add error", e)
            }
        }
    }

    fun deletePresetPhrase(phrase: PresetPhrase) {
        viewModelScope.launch {
            try {
                presetDao.deletePhrase(phrase)
                DebugLogger.i("AppVM", "BK-016 Preset deleted: ${phrase.label}")
            } catch (e: Exception) {
                DebugLogger.e("AppVM", "BK-011 Preset delete error", e)
            }
        }
    }

    fun addClipboardEntry(text: String) {
        viewModelScope.launch {
            try {
                clipboardDao.insertEntry(ClipboardEntry(text = text))
            } catch (e: Exception) {
                DebugLogger.e("AppVM", "BK-012 Clipboard add error", e)
            }
        }
    }

    fun toggleEmulatedDevice(deviceType: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                emulatedDeviceDao.setDeviceEnabled(deviceType, enabled)
                DebugLogger.i("AppVM", "BK-017 Device $deviceType enabled=$enabled")
            } catch (e: Exception) {
                DebugLogger.e("AppVM", "BK-013 Device toggle error", e)
            }
        }
    }

    fun sendMouseDelta(dx: Float, dy: Float) {
        btManager.sendMouseReport(0, dx, dy)
    }

    fun sendMouseLeftClick(press: Boolean) {
        btManager.sendMouseLeftClick(press)
    }

    fun sendMouseRightClick(press: Boolean) {
        btManager.sendMouseRightClick(press)
    }

    fun sendMouseMiddleClick(press: Boolean) {
        btManager.sendMouseMiddleClick(press)
    }

    fun sendMouseWheel(scroll: Float) {
        btManager.sendMouseWheel(scroll)
    }

    fun sendKeyEvent(keyCode: Int, isPress: Boolean) {
        btManager.sendKeyPress(keyCode.toByte())
    }

    fun sendText(text: String, delayMultiplier: Float = 1.0f) {
        btManager.sendTextString(text, delayMultiplier)
    }

    fun sendMediaKey(keyName: String) {
        btManager.sendMediaKey(keyName)
    }

    override fun onCleared() {
        super.onCleared()
        btManager.cleanup()
    }
}
