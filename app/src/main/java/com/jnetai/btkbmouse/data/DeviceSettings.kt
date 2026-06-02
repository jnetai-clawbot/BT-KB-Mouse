package com.jnetai.btkbmouse.data

/**
 * Data class containing all configurable application settings.
 * Used with DataStore for persistent preferences storage.
 */
data class DeviceSettings(
    // Mouse settings
    val mouseSensitivity: Int = 50,          // 1-100 range
    val scrollSpeed: Int = 50,              // 1-100 range
    val leftHanded: Boolean = false,
    val smoothAcceleration: Boolean = true,
    val inputSmoothing: Float = 0.5f,      // 0.0 to 1.0

    // Keyboard settings
    val keyboardLayout: String = "US",      // US, UK, ISO
    val fnMode: String = "standard",        // standard, media, function
    val mediaKeys: Boolean = true,
    val keyRepeatDelay: Int = 500,          // milliseconds
    val keyRepeatRate: Int = 100,           // milliseconds

    // Connection settings
    val autoReconnect: Boolean = true,
    val autoConnect: Boolean = false,
    val disconnectNotification: Boolean = true,

    // System integration
    val preventLock: Boolean = true,
    val backgroundRun: Boolean = true,
    val startOnBoot: Boolean = false,

    // UI settings
    val darkTheme: Boolean = true,

    // Emulation settings
    val emulateKeyboard: Boolean = true,
    val emulateMouse: Boolean = true,
    val emulateSpeaker: Boolean = false,
    val emulateMic: Boolean = false,

    // Audio settings
    val speakerVolume: Float = 1.0f,       // 0.0 to 1.0
    val micGain: Float = 1.0f,             // 0.0 to 1.0

    // Debug/Logging
    val logging: Boolean = false
) {
    companion object {
        const val KEYBOARD_LAYOUT_US = "US"
        const val KEYBOARD_LAYOUT_UK = "UK"
        const val KEYBOARD_LAYOUT_ISO = "ISO"

        const val FN_MODE_STANDARD = "standard"
        const val FN_MODE_MEDIA = "media"
        const val FN_MODE_FUNCTION = "function"

        const val MOUSE_SENSITIVITY_MIN = 1
        const val MOUSE_SENSITIVITY_MAX = 100
        const val MOUSE_SENSITIVITY_DEFAULT = 50

        const val SCROLL_SPEED_MIN = 1
        const val SCROLL_SPEED_MAX = 100
        const val SCROLL_SPEED_DEFAULT = 50

        const val KEY_REPEAT_DELAY_MIN = 100
        const val KEY_REPEAT_DELAY_MAX = 1000
        const val KEY_REPEAT_DELAY_DEFAULT = 500

        const val KEY_REPEAT_RATE_MIN = 50
        const val KEY_REPEAT_RATE_MAX = 500
        const val KEY_REPEAT_RATE_DEFAULT = 100

        /**
         * Factory method to create default settings
         */
        fun default(): DeviceSettings = DeviceSettings()

        /**
         * Create settings with only specific values changed
         */
        fun create(
            mouseSensitivity: Int = MOUSE_SENSITIVITY_DEFAULT,
            scrollSpeed: Int = SCROLL_SPEED_DEFAULT,
            leftHanded: Boolean = false,
            smoothAcceleration: Boolean = true,
            inputSmoothing: Float = 0.5f,
            keyboardLayout: String = KEYBOARD_LAYOUT_US,
            fnMode: String = FN_MODE_STANDARD,
            mediaKeys: Boolean = true,
            keyRepeatDelay: Int = KEY_REPEAT_DELAY_DEFAULT,
            keyRepeatRate: Int = KEY_REPEAT_RATE_DEFAULT,
            autoReconnect: Boolean = true,
            autoConnect: Boolean = false,
            disconnectNotification: Boolean = true,
            preventLock: Boolean = true,
            backgroundRun: Boolean = true,
            startOnBoot: Boolean = false,
            darkTheme: Boolean = true,
            emulateKeyboard: Boolean = true,
            emulateMouse: Boolean = true,
            emulateSpeaker: Boolean = false,
            emulateMic: Boolean = false,
            speakerVolume: Float = 1.0f,
            micGain: Float = 1.0f,
            logging: Boolean = false
        ): DeviceSettings = DeviceSettings(
            mouseSensitivity = mouseSensitivity,
            scrollSpeed = scrollSpeed,
            leftHanded = leftHanded,
            smoothAcceleration = smoothAcceleration,
            inputSmoothing = inputSmoothing,
            keyboardLayout = keyboardLayout,
            fnMode = fnMode,
            mediaKeys = mediaKeys,
            keyRepeatDelay = keyRepeatDelay,
            keyRepeatRate = keyRepeatRate,
            autoReconnect = autoReconnect,
            autoConnect = autoConnect,
            disconnectNotification = disconnectNotification,
            preventLock = preventLock,
            backgroundRun = backgroundRun,
            startOnBoot = startOnBoot,
            darkTheme = darkTheme,
            emulateKeyboard = emulateKeyboard,
            emulateMouse = emulateMouse,
            emulateSpeaker = emulateSpeaker,
            emulateMic = emulateMic,
            speakerVolume = speakerVolume,
            micGain = micGain,
            logging = logging
        )
    }

    /**
     * Validate and clamp mouse sensitivity to valid range
     */
    fun withClampedMouseSensitivity(): DeviceSettings {
        return copy(mouseSensitivity = mouseSensitivity.coerceIn(MOUSE_SENSITIVITY_MIN, MOUSE_SENSITIVITY_MAX))
    }

    /**
     * Validate and clamp scroll speed to valid range
     */
    fun withClampedScrollSpeed(): DeviceSettings {
        return copy(scrollSpeed = scrollSpeed.coerceIn(SCROLL_SPEED_MIN, SCROLL_SPEED_MAX))
    }

    /**
     * Validate and clamp key repeat delay to valid range
     */
    fun withClampedKeyRepeatDelay(): DeviceSettings {
        return copy(keyRepeatDelay = keyRepeatDelay.coerceIn(KEY_REPEAT_DELAY_MIN, KEY_REPEAT_DELAY_MAX))
    }

    /**
     * Validate and clamp key repeat rate to valid range
     */
    fun withClampedKeyRepeatRate(): DeviceSettings {
        return copy(keyRepeatRate = keyRepeatRate.coerceIn(KEY_REPEAT_RATE_MIN, KEY_REPEAT_RATE_MAX))
    }

    /**
     * Check if any emulation is enabled
     */
    fun hasAnyEmulationEnabled(): Boolean {
        return emulateKeyboard || emulateMouse || emulateSpeaker || emulateMic
    }

    /**
     * Check if mouse emulation is enabled
     */
    fun isMouseEmulationEnabled(): Boolean = emulateMouse

    /**
     * Check if keyboard emulation is enabled
     */
    fun isKeyboardEmulationEnabled(): Boolean = emulateKeyboard
}
