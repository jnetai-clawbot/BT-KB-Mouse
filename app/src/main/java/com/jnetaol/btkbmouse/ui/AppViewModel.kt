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
            try { _activeProfile.value = profileDao.getActiveProfile(); initializeEmulatedDevices(); loadSettingsSnapshot() } catch (_: Exception) {}
        }
    }

    private suspend fun initializeEmulatedDevices() {
        listOf(
            EmulatedDevice("keyboard", true, "Keyboard", "keyboard"),
            EmulatedDevice("mouse", true, "Mouse", "mouse"),
            EmulatedDevice("speaker", false, "Speaker", "speaker"),
            EmulatedDevice("microphone", false, "Microphone", "mic"),
            EmulatedDevice("webcam", false, "Webcam", "videocam"),
            EmulatedDevice("gamepad", false, "Gamepad", "gamepad"),
            EmulatedDevice("scanner", false, "Barcode Scanner", "scanner")
        ).forEach { d ->
            if (emulatedDeviceDao.getAllDevices().first().none { it.deviceType == d.deviceType })
                emulatedDeviceDao.setDevice(d)
        }
    }

    private suspend fun loadSettingsSnapshot() {
        try { _settingsSnapshot.value = settingsDao.getAllSettings().first().associate { it.key to it.value } } catch (_: Exception) {}
    }

    fun createProfile(name: String) { viewModelScope.launch { try { profileDao.insertProfile(Profile(name = name)) } catch (_: Exception) {} } }
    fun deleteProfile(profile: Profile) { viewModelScope.launch { try { profileDao.deleteProfile(profile) } catch (_: Exception) {} } }
    fun setActiveProfile(profile: Profile) { viewModelScope.launch { try { profileDao.deactivateAllProfiles(); profileDao.setActiveProfile(profile.id) } catch (_: Exception) {} } }
    fun updateProfile(profile: Profile) { viewModelScope.launch { try { profileDao.updateProfile(profile) } catch (_: Exception) {} } }
    fun saveSetting(key: String, value: String) { viewModelScope.launch { try { settingsDao.setSetting(AppSetting(key, value)) } catch (_: Exception) {} } }
    fun revertSettings() { viewModelScope.launch { try { loadSettingsSnapshot() } catch (_: Exception) {} } }
    fun addPresetPhrase(label: String, text: String) { viewModelScope.launch { try { presetDao.insertPhrase(PresetPhrase(label = label, text = text)) } catch (_: Exception) {} } }
    fun deletePresetPhrase(phrase: PresetPhrase) { viewModelScope.launch { try { presetDao.deletePhrase(phrase) } catch (_: Exception) {} } }
    fun addClipboardEntry(text: String) { viewModelScope.launch { try { clipboardDao.insertEntry(ClipboardEntry(text = text)) } catch (_: Exception) {} } }
    fun toggleEmulatedDevice(deviceType: String, enabled: Boolean) { viewModelScope.launch { try { emulatedDeviceDao.setDeviceEnabled(deviceType, enabled) } catch (_: Exception) {} } }

    fun sendMouseDelta(dx: Float, dy: Float) { btManager.sendMouseReport(0, dx, dy) }
    fun sendMouseLeftClick(press: Boolean) { btManager.sendMouseLeftClick(press) }
    fun sendMouseRightClick(press: Boolean) { btManager.sendMouseRightClick(press) }
    fun sendMouseMiddleClick(press: Boolean) { btManager.sendMouseMiddleClick(press) }
    fun sendMouseWheel(scroll: Float) { btManager.sendMouseWheel(scroll) }
    fun sendText(text: String) { btManager.sendTextString(text) }

    override fun onCleared() {
        super.onCleared()
        btManager.cleanup()
    }
}
