package com.jnetai.btkbmouse.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repository for managing application settings using DataStore
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val database: AppDatabase
) {
    private val _settingsFlow = MutableStateFlow(DeviceSettings.default())
    val settingsFlow: StateFlow<DeviceSettings> = _settingsFlow.asStateFlow()

    init {
        // Initialize with default values
        _settingsFlow.value = DeviceSettings.default()
    }

    companion object {
        // Mouse settings keys
        private val MOUSE_SENSITIVITY = intPreferencesKey("mouse_sensitivity")
        private val SCROLL_SPEED = intPreferencesKey("scroll_speed")
        private val LEFT_HANDED = booleanPreferencesKey("left_handed")
        private val SMOOTH_ACCELERATION = booleanPreferencesKey("smooth_acceleration")
        private val INPUT_SMOOTHING = floatPreferencesKey("input_smoothing")

        // Keyboard settings keys
        private val KEYBOARD_LAYOUT = stringPreferencesKey("keyboard_layout")
        private val FN_MODE = stringPreferencesKey("fn_mode")
        private val MEDIA_KEYS = booleanPreferencesKey("media_keys")
        private val KEY_REPEAT_DELAY = intPreferencesKey("key_repeat_delay")
        private val KEY_REPEAT_RATE = intPreferencesKey("key_repeat_rate")

        // Connection settings keys
        private val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        private val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        private val DISCONNECT_NOTIFICATION = booleanPreferencesKey("disconnect_notification")

        // System integration keys
        private val PREVENT_LOCK = booleanPreferencesKey("prevent_lock")
        private val BACKGROUND_RUN = booleanPreferencesKey("background_run")
        private val START_ON_BOOT = booleanPreferencesKey("start_on_boot")

        // UI settings keys
        private val DARK_THEME = booleanPreferencesKey("dark_theme")

        // Emulation settings keys
        private val EMULATE_KEYBOARD = booleanPreferencesKey("emulate_keyboard")
        private val EMULATE_MOUSE = booleanPreferencesKey("emulate_mouse")
        private val EMULATE_SPEAKER = booleanPreferencesKey("emulate_speaker")
        private val EMULATE_MIC = booleanPreferencesKey("emulate_mic")

        // Audio settings keys
        private val SPEAKER_VOLUME = floatPreferencesKey("speaker_volume")
        private val MIC_GAIN = floatPreferencesKey("mic_gain")

        // Debug/Logging keys
        private val LOGGING = booleanPreferencesKey("logging")
    }

    /**
     * Observe settings changes as a Flow
     */
    fun observeSettings(): Flow<DeviceSettings> {
        return dataStore.data.map { preferences ->
            mapPreferencesToSettings(preferences)
        }
    }

    /**
     * Get current settings synchronously (first emission)
     */
    suspend fun getSettings(): DeviceSettings {
        return dataStore.data.first().let { mapPreferencesToSettings(it) }
    }

    /**
     * Update settings with the provided DeviceSettings object
     */
    suspend fun updateSettings(settings: DeviceSettings) {
        dataStore.edit { preferences ->
            applySettingsToPreferences(settings, preferences)
        }
        _settingsFlow.value = settings
    }

    /**
     * Update a single setting value
     */
    suspend fun updateMouseSensitivity(value: Int) {
        updateSettings(_settingsFlow.value.copy(mouseSensitivity = value))
    }

    suspend fun updateScrollSpeed(value: Int) {
        updateSettings(_settingsFlow.value.copy(scrollSpeed = value))
    }

    suspend fun updateLeftHanded(value: Boolean) {
        updateSettings(_settingsFlow.value.copy(leftHanded = value))
    }

    suspend fun updateSmoothAcceleration(value: Boolean) {
        updateSettings(_settingsFlow.value.copy(smoothAcceleration = value))
    }

    suspend fun updateKeyboardLayout(value: String) {
        updateSettings(_settingsFlow.value.copy(keyboardLayout = value))
    }

    suspend fun updateFnMode(value: String) {
        updateSettings(_settingsFlow.value.copy(fnMode = value))
    }

    suspend fun updateMediaKeys(value: Boolean) {
        updateSettings(_settingsFlow.value.copy(mediaKeys = value))
    }

    suspend fun updateKeyRepeatDelay(value: Int) {
        updateSettings(_settingsFlow.value.copy(keyRepeatDelay = value))
    }

    suspend fun updateKeyRepeatRate(value: Int) {
        updateSettings(_settingsFlow.value.copy(keyRepeatRate = value))
    }

    suspend fun updateAutoReconnect(value: Boolean) {
        updateSettings(_settingsFlow.value.copy(autoReconnect = value))
    }

    suspend fun updateAutoConnect(value: Boolean) {
        updateSettings(_settingsFlow.value.copy(autoConnect = value))
    }

    suspend fun updateDarkTheme(value: Boolean) {
        updateSettings(_settingsFlow.value.copy(darkTheme = value))
    }

    suspend fun updateEmulateKeyboard(value: Boolean) {
        updateSettings(_settingsFlow.value.copy(emulateKeyboard = value))
    }

    suspend fun updateEmulateMouse(value: Boolean) {
        updateSettings(_settingsFlow.value.copy(emulateMouse = value))
    }

    suspend fun updateEmulateSpeaker(value: Boolean) {
        updateSettings(_settingsFlow.value.copy(emulateSpeaker = value))
    }

    suspend fun updateEmulateMic(value: Boolean) {
        updateSettings(_settingsFlow.value.copy(emulateMic = value))
    }

    suspend fun updateSpeakerVolume(value: Float) {
        updateSettings(_settingsFlow.value.copy(speakerVolume = value))
    }

    suspend fun updateMicGain(value: Float) {
        updateSettings(_settingsFlow.value.copy(micGain = value))
    }

    suspend fun updateLogging(value: Boolean) {
        updateSettings(_settingsFlow.value.copy(logging = value))
    }

    suspend fun updateInputSmoothing(value: Float) {
        updateSettings(_settingsFlow.value.copy(inputSmoothing = value))
    }

    /**
     * Reset all settings to defaults
     */
    suspend fun resetToDefaults() {
        updateSettings(DeviceSettings.default())
    }

    private fun mapPreferencesToSettings(preferences: Preferences): DeviceSettings {
        return DeviceSettings(
            mouseSensitivity = preferences[MOUSE_SENSITIVITY] ?: DeviceSettings.MOUSE_SENSITIVITY_DEFAULT,
            scrollSpeed = preferences[SCROLL_SPEED] ?: DeviceSettings.SCROLL_SPEED_DEFAULT,
            leftHanded = preferences[LEFT_HANDED] ?: false,
            smoothAcceleration = preferences[SMOOTH_ACCELERATION] ?: true,
            inputSmoothing = preferences[INPUT_SMOOTHING] ?: 0.5f,
            keyboardLayout = preferences[KEYBOARD_LAYOUT] ?: DeviceSettings.KEYBOARD_LAYOUT_US,
            fnMode = preferences[FN_MODE] ?: DeviceSettings.FN_MODE_STANDARD,
            mediaKeys = preferences[MEDIA_KEYS] ?: true,
            keyRepeatDelay = preferences[KEY_REPEAT_DELAY] ?: DeviceSettings.KEY_REPEAT_DELAY_DEFAULT,
            keyRepeatRate = preferences[KEY_REPEAT_RATE] ?: DeviceSettings.KEY_REPEAT_RATE_DEFAULT,
            autoReconnect = preferences[AUTO_RECONNECT] ?: true,
            autoConnect = preferences[AUTO_CONNECT] ?: false,
            disconnectNotification = preferences[DISCONNECT_NOTIFICATION] ?: true,
            preventLock = preferences[PREVENT_LOCK] ?: true,
            backgroundRun = preferences[BACKGROUND_RUN] ?: true,
            startOnBoot = preferences[START_ON_BOOT] ?: false,
            darkTheme = preferences[DARK_THEME] ?: true,
            emulateKeyboard = preferences[EMULATE_KEYBOARD] ?: true,
            emulateMouse = preferences[EMULATE_MOUSE] ?: true,
            emulateSpeaker = preferences[EMULATE_SPEAKER] ?: false,
            emulateMic = preferences[EMULATE_MIC] ?: false,
            speakerVolume = preferences[SPEAKER_VOLUME] ?: 1.0f,
            micGain = preferences[MIC_GAIN] ?: 1.0f,
            logging = preferences[LOGGING] ?: false
        )
    }

    private fun applySettingsToPreferences(settings: DeviceSettings, preferences: androidx.datastore.preferences.core.MutablePreferences) {
        preferences[MOUSE_SENSITIVITY] = settings.mouseSensitivity
        preferences[SCROLL_SPEED] = settings.scrollSpeed
        preferences[LEFT_HANDED] = settings.leftHanded
        preferences[SMOOTH_ACCELERATION] = settings.smoothAcceleration
        preferences[INPUT_SMOOTHING] = settings.inputSmoothing
        preferences[KEYBOARD_LAYOUT] = settings.keyboardLayout
        preferences[FN_MODE] = settings.fnMode
        preferences[MEDIA_KEYS] = settings.mediaKeys
        preferences[KEY_REPEAT_DELAY] = settings.keyRepeatDelay
        preferences[KEY_REPEAT_RATE] = settings.keyRepeatRate
        preferences[AUTO_RECONNECT] = settings.autoReconnect
        preferences[AUTO_CONNECT] = settings.autoConnect
        preferences[DISCONNECT_NOTIFICATION] = settings.disconnectNotification
        preferences[PREVENT_LOCK] = settings.preventLock
        preferences[BACKGROUND_RUN] = settings.backgroundRun
        preferences[START_ON_BOOT] = settings.startOnBoot
        preferences[DARK_THEME] = settings.darkTheme
        preferences[EMULATE_KEYBOARD] = settings.emulateKeyboard
        preferences[EMULATE_MOUSE] = settings.emulateMouse
        preferences[EMULATE_SPEAKER] = settings.emulateSpeaker
        preferences[EMULATE_MIC] = settings.emulateMic
        preferences[SPEAKER_VOLUME] = settings.speakerVolume
        preferences[MIC_GAIN] = settings.micGain
        preferences[LOGGING] = settings.logging
    }
}
