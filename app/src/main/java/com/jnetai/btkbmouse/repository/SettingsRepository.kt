package com.jnetai.btkbmouse.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    // Mouse Settings
    private val MOUSE_SENSITIVITY = intPreferencesKey("mouse_sensitivity")
    private val SCROLL_SPEED = intPreferencesKey("scroll_speed")
    private val LEFT_HANDED_MODE = booleanPreferencesKey("left_handed_mode")
    private val SMOOTH_ACCELERATION = booleanPreferencesKey("smooth_acceleration")
    private val INPUT_SMOOTHING = stringPreferencesKey("input_smoothing")

    // Keyboard Settings
    private val KEY_REPEAT_DELAY = intPreferencesKey("key_repeat_delay")
    private val KEY_REPEAT_RATE = intPreferencesKey("key_repeat_rate")
    private val KEYBOARD_LAYOUT = stringPreferencesKey("keyboard_layout")
    private val FUNCTION_KEY_MODE = booleanPreferencesKey("function_key_mode")
    private val MEDIA_KEY_SUPPORT = booleanPreferencesKey("media_key_support")

    // Connection Settings
    private val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
    private val AUTO_CONNECT_STARTUP = booleanPreferencesKey("auto_connect_startup")

    // App Behavior
    private val DARK_THEME = booleanPreferencesKey("dark_theme")
    private val RUN_IN_BACKGROUND = booleanPreferencesKey("run_in_background")
    private val PREVENT_SCREEN_LOCK = booleanPreferencesKey("prevent_screen_lock")
    private val LOGGING = booleanPreferencesKey("logging")
    private val START_ON_BOOT = booleanPreferencesKey("start_on_boot")

    // Emulation Settings
    private val EMULATE_KEYBOARD = booleanPreferencesKey("emulate_keyboard")
    private val EMULATE_MOUSE = booleanPreferencesKey("emulate_mouse")
    private val EMULATE_SPEAKERS = booleanPreferencesKey("emulate_speakers")
    private val EMULATE_MIC = booleanPreferencesKey("emulate_mic")

    // Audio Settings
    private val SPEAKER_VOLUME = intPreferencesKey("speaker_volume")
    private val MIC_GAIN = intPreferencesKey("mic_gain")

    // Data class for all settings
    data class AppSettings(
        val mouseSensitivity: Int = 50,
        val scrollSpeed: Int = 50,
        val leftHandedMode: Boolean = false,
        val smoothAcceleration: Boolean = true,
        val inputSmoothing: String = "medium",
        val keyRepeatDelay: Int = 500,
        val keyRepeatRate: Int = 100,
        val keyboardLayout: String = "US",
        val functionKeyMode: Boolean = false,
        val mediaKeySupport: Boolean = true,
        val autoReconnect: Boolean = true,
        val autoConnectStartup: Boolean = false,
        val darkTheme: Boolean = true,
        val runInBackground: Boolean = true,
        val preventScreenLock: Boolean = true,
        val logging: Boolean = false,
        val startOnBoot: Boolean = false,
        val emulateKeyboard: Boolean = true,
        val emulateMouse: Boolean = true,
        val emulateSpeakers: Boolean = true,
        val emulateMic: Boolean = true,
        val speakerVolume: Int = 80,
        val micGain: Int = 70
    )

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                mouseSensitivity = preferences[MOUSE_SENSITIVITY] ?: 50,
                scrollSpeed = preferences[SCROLL_SPEED] ?: 50,
                leftHandedMode = preferences[LEFT_HANDED_MODE] ?: false,
                smoothAcceleration = preferences[SMOOTH_ACCELERATION] ?: true,
                inputSmoothing = preferences[INPUT_SMOOTHING] ?: "medium",
                keyRepeatDelay = preferences[KEY_REPEAT_DELAY] ?: 500,
                keyRepeatRate = preferences[KEY_REPEAT_RATE] ?: 100,
                keyboardLayout = preferences[KEYBOARD_LAYOUT] ?: "US",
                functionKeyMode = preferences[FUNCTION_KEY_MODE] ?: false,
                mediaKeySupport = preferences[MEDIA_KEY_SUPPORT] ?: true,
                autoReconnect = preferences[AUTO_RECONNECT] ?: true,
                autoConnectStartup = preferences[AUTO_CONNECT_STARTUP] ?: false,
                darkTheme = preferences[DARK_THEME] ?: true,
                runInBackground = preferences[RUN_IN_BACKGROUND] ?: true,
                preventScreenLock = preferences[PREVENT_SCREEN_LOCK] ?: true,
                logging = preferences[LOGGING] ?: false,
                startOnBoot = preferences[START_ON_BOOT] ?: false,
                emulateKeyboard = preferences[EMULATE_KEYBOARD] ?: true,
                emulateMouse = preferences[EMULATE_MOUSE] ?: true,
                emulateSpeakers = preferences[EMULATE_SPEAKERS] ?: true,
                emulateMic = preferences[EMULATE_MIC] ?: true,
                speakerVolume = preferences[SPEAKER_VOLUME] ?: 80,
                micGain = preferences[MIC_GAIN] ?: 70
            )
        }

    suspend fun updateMouseSensitivity(value: Int) {
        context.dataStore.edit { it[MOUSE_SENSITIVITY] = value.coerceIn(1, 100) }
    }

    suspend fun updateScrollSpeed(value: Int) {
        context.dataStore.edit { it[SCROLL_SPEED] = value.coerceIn(1, 100) }
    }

    suspend fun updateLeftHandedMode(enabled: Boolean) {
        context.dataStore.edit { it[LEFT_HANDED_MODE] = enabled }
    }

    suspend fun updateSmoothAcceleration(enabled: Boolean) {
        context.dataStore.edit { it[SMOOTH_ACCELERATION] = enabled }
    }

    suspend fun updateInputSmoothing(value: String) {
        context.dataStore.edit { it[INPUT_SMOOTHING] = value }
    }

    suspend fun updateKeyRepeatDelay(value: Int) {
        context.dataStore.edit { it[KEY_REPEAT_DELAY] = value.coerceIn(100, 1000) }
    }

    suspend fun updateKeyRepeatRate(value: Int) {
        context.dataStore.edit { it[KEY_REPEAT_RATE] = value.coerceIn(50, 500) }
    }

    suspend fun updateKeyboardLayout(value: String) {
        context.dataStore.edit { it[KEYBOARD_LAYOUT] = value }
    }

    suspend fun updateFunctionKeyMode(enabled: Boolean) {
        context.dataStore.edit { it[FUNCTION_KEY_MODE] = enabled }
    }

    suspend fun updateMediaKeySupport(enabled: Boolean) {
        context.dataStore.edit { it[MEDIA_KEY_SUPPORT] = enabled }
    }

    suspend fun updateAutoReconnect(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_RECONNECT] = enabled }
    }

    suspend fun updateAutoConnectStartup(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_CONNECT_STARTUP] = enabled }
    }

    suspend fun updateDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[DARK_THEME] = enabled }
    }

    suspend fun updateRunInBackground(enabled: Boolean) {
        context.dataStore.edit { it[RUN_IN_BACKGROUND] = enabled }
    }

    suspend fun updatePreventScreenLock(enabled: Boolean) {
        context.dataStore.edit { it[PREVENT_SCREEN_LOCK] = enabled }
    }

    suspend fun updateLogging(enabled: Boolean) {
        context.dataStore.edit { it[LOGGING] = enabled }
    }

    suspend fun updateStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { it[START_ON_BOOT] = enabled }
    }

    suspend fun updateEmulateKeyboard(enabled: Boolean) {
        context.dataStore.edit { it[EMULATE_KEYBOARD] = enabled }
    }

    suspend fun updateEmulateMouse(enabled: Boolean) {
        context.dataStore.edit { it[EMULATE_MOUSE] = enabled }
    }

    suspend fun updateEmulateSpeakers(enabled: Boolean) {
        context.dataStore.edit { it[EMULATE_SPEAKERS] = enabled }
    }

    suspend fun updateEmulateMic(enabled: Boolean) {
        context.dataStore.edit { it[EMULATE_MIC] = enabled }
    }

    suspend fun updateSpeakerVolume(value: Int) {
        context.dataStore.edit { it[SPEAKER_VOLUME] = value.coerceIn(0, 100) }
    }

    suspend fun updateMicGain(value: Int) {
        context.dataStore.edit { it[MIC_GAIN] = value.coerceIn(0, 100) }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[MOUSE_SENSITIVITY] = settings.mouseSensitivity
            preferences[SCROLL_SPEED] = settings.scrollSpeed
            preferences[LEFT_HANDED_MODE] = settings.leftHandedMode
            preferences[SMOOTH_ACCELERATION] = settings.smoothAcceleration
            preferences[INPUT_SMOOTHING] = settings.inputSmoothing
            preferences[KEY_REPEAT_DELAY] = settings.keyRepeatDelay
            preferences[KEY_REPEAT_RATE] = settings.keyRepeatRate
            preferences[KEYBOARD_LAYOUT] = settings.keyboardLayout
            preferences[FUNCTION_KEY_MODE] = settings.functionKeyMode
            preferences[MEDIA_KEY_SUPPORT] = settings.mediaKeySupport
            preferences[AUTO_RECONNECT] = settings.autoReconnect
            preferences[AUTO_CONNECT_STARTUP] = settings.autoConnectStartup
            preferences[DARK_THEME] = settings.darkTheme
            preferences[RUN_IN_BACKGROUND] = settings.runInBackground
            preferences[PREVENT_SCREEN_LOCK] = settings.preventScreenLock
            preferences[LOGGING] = settings.logging
            preferences[START_ON_BOOT] = settings.startOnBoot
            preferences[EMULATE_KEYBOARD] = settings.emulateKeyboard
            preferences[EMULATE_MOUSE] = settings.emulateMouse
            preferences[EMULATE_SPEAKERS] = settings.emulateSpeakers
            preferences[EMULATE_MIC] = settings.emulateMic
            preferences[SPEAKER_VOLUME] = settings.speakerVolume
            preferences[MIC_GAIN] = settings.micGain
        }
    }

    suspend fun updateSetting(key: String, value: Any) {
        context.dataStore.edit { preferences ->
            when (key) {
                "mouseSensitivity" -> preferences[MOUSE_SENSITIVITY] = (value as Int).coerceIn(1, 100)
                "scrollSpeed" -> preferences[SCROLL_SPEED] = (value as Int).coerceIn(1, 100)
                "leftHandedMode" -> preferences[LEFT_HANDED_MODE] = value as Boolean
                "smoothAcceleration" -> preferences[SMOOTH_ACCELERATION] = value as Boolean
                "inputSmoothing" -> preferences[INPUT_SMOOTHING] = value as String
                "keyRepeatDelay" -> preferences[KEY_REPEAT_DELAY] = (value as Int).coerceIn(100, 1000)
                "keyRepeatRate" -> preferences[KEY_REPEAT_RATE] = (value as Int).coerceIn(50, 500)
                "keyboardLayout" -> preferences[KEYBOARD_LAYOUT] = value as String
                "functionKeyMode" -> preferences[FUNCTION_KEY_MODE] = value as Boolean
                "mediaKeySupport" -> preferences[MEDIA_KEY_SUPPORT] = value as Boolean
                "autoReconnect" -> preferences[AUTO_RECONNECT] = value as Boolean
                "autoConnectStartup" -> preferences[AUTO_CONNECT_STARTUP] = value as Boolean
                "darkTheme" -> preferences[DARK_THEME] = value as Boolean
                "runInBackground" -> preferences[RUN_IN_BACKGROUND] = value as Boolean
                "preventScreenLock" -> preferences[PREVENT_SCREEN_LOCK] = value as Boolean
                "logging" -> preferences[LOGGING] = value as Boolean
                "startOnBoot" -> preferences[START_ON_BOOT] = value as Boolean
                "emulateKeyboard" -> preferences[EMULATE_KEYBOARD] = value as Boolean
                "emulateMouse" -> preferences[EMULATE_MOUSE] = value as Boolean
                "emulateSpeakers" -> preferences[EMULATE_SPEAKERS] = value as Boolean
                "emulateMic" -> preferences[EMULATE_MIC] = value as Boolean
                "speakerVolume" -> preferences[SPEAKER_VOLUME] = (value as Int).coerceIn(0, 100)
                "micGain" -> preferences[MIC_GAIN] = (value as Int).coerceIn(0, 100)
            }
        }
    }
}
