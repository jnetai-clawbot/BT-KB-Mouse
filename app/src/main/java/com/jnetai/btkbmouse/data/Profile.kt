package com.jnetai.btkbmouse.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a device configuration profile
 */
@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val deviceAddress: String,

    // Mouse settings
    val mouseSensitivity: Int = 50,          // 1-100 range
    val scrollSpeed: Int = 50,              // 1-100 range
    val leftHandedMode: Boolean = false,
    val smoothAcceleration: Boolean = true,

    // Keyboard settings
    val keyRepeatDelay: Int = 500,          // milliseconds (100-1000)
    val keyRepeatRate: Int = 100,           // milliseconds (50-500)

    // Connection settings
    val autoReconnect: Boolean = true,
    val isActive: Boolean = false,

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Create a copy with updated timestamp
     */
    fun withUpdatedTimestamp(): Profile {
        return copy(updatedAt = System.currentTimeMillis())
    }

    /**
     * Create a copy with new values
     */
    fun update(
        name: String = this.name,
        mouseSensitivity: Int = this.mouseSensitivity,
        scrollSpeed: Int = this.scrollSpeed,
        leftHandedMode: Boolean = this.leftHandedMode,
        smoothAcceleration: Boolean = this.smoothAcceleration,
        keyRepeatDelay: Int = this.keyRepeatDelay,
        keyRepeatRate: Int = this.keyRepeatRate,
        autoReconnect: Boolean = this.autoReconnect
    ): Profile {
        return copy(
            name = name,
            mouseSensitivity = mouseSensitivity,
            scrollSpeed = scrollSpeed,
            leftHandedMode = leftHandedMode,
            smoothAcceleration = smoothAcceleration,
            keyRepeatDelay = keyRepeatDelay,
            keyRepeatRate = keyRepeatRate,
            autoReconnect = autoReconnect,
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        const val TABLE_NAME = "profiles"

        /**
         * Create a new profile with current timestamp
         */
        fun create(
            name: String,
            deviceAddress: String,
            mouseSensitivity: Int = DeviceSettings.MOUSE_SENSITIVITY_DEFAULT,
            scrollSpeed: Int = DeviceSettings.SCROLL_SPEED_DEFAULT,
            keyRepeatDelay: Int = DeviceSettings.KEY_REPEAT_DELAY_DEFAULT,
            keyRepeatRate: Int = DeviceSettings.KEY_REPEAT_RATE_DEFAULT,
            leftHandedMode: Boolean = false,
            smoothAcceleration: Boolean = true,
            autoReconnect: Boolean = true
        ): Profile {
            val now = System.currentTimeMillis()
            return Profile(
                name = name,
                deviceAddress = deviceAddress,
                mouseSensitivity = mouseSensitivity,
                scrollSpeed = scrollSpeed,
                leftHandedMode = leftHandedMode,
                smoothAcceleration = smoothAcceleration,
                keyRepeatDelay = keyRepeatDelay,
                keyRepeatRate = keyRepeatRate,
                autoReconnect = autoReconnect,
                createdAt = now,
                updatedAt = now
            )
        }

        /**
         * Create profile from current device settings
         */
        fun fromDeviceSettings(
            name: String,
            deviceAddress: String,
            settings: DeviceSettings
        ): Profile {
            return create(
                name = name,
                deviceAddress = deviceAddress,
                mouseSensitivity = settings.mouseSensitivity,
                scrollSpeed = settings.scrollSpeed,
                leftHandedMode = settings.leftHanded,
                smoothAcceleration = settings.smoothAcceleration,
                keyRepeatDelay = settings.keyRepeatDelay,
                keyRepeatRate = settings.keyRepeatRate,
                autoReconnect = settings.autoReconnect
            )
        }
    }

    /**
     * Convert profile to DeviceSettings
     */
    fun toDeviceSettings(baseSettings: DeviceSettings = DeviceSettings.default()): DeviceSettings {
        return baseSettings.copy(
            mouseSensitivity = mouseSensitivity,
            scrollSpeed = scrollSpeed,
            leftHanded = leftHandedMode,
            smoothAcceleration = smoothAcceleration,
            keyRepeatDelay = keyRepeatDelay,
            keyRepeatRate = keyRepeatRate,
            autoReconnect = autoReconnect
        )
    }
}
