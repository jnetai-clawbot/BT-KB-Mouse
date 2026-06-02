package com.jnetai.btkbmouse.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Device type enum representing the type of Bluetooth HID device
 */
enum class DeviceType {
    MOUSE,
    KEYBOARD,
    COMBO,  // Combined keyboard and mouse
    UNKNOWN;

    companion object {
        fun fromString(value: String): DeviceType {
            return entries.find { it.name == value } ?: UNKNOWN
        }
    }
}

/**
 * Room Entity representing a Bluetooth HID device
 */
@Entity(tableName = "devices")
data class Device(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val address: String,
    val type: DeviceType = DeviceType.UNKNOWN,
    val isTrusted: Boolean = false,
    val isConnected: Boolean = false,
    val lastConnected: Long = 0L,
    val batteryLevel: Int? = null,
    val signalStrength: Int? = null
) {
    /**
     * Check if device type is a known type
     */
    fun isKnownType(): Boolean = type != DeviceType.UNKNOWN

    /**
     * Check if device supports mouse functionality
     */
    fun supportsMouse(): Boolean = type == DeviceType.MOUSE || type == DeviceType.COMBO

    /**
     * Check if device supports keyboard functionality
     */
    fun supportsKeyboard(): Boolean = type == DeviceType.KEYBOARD || type == DeviceType.COMBO

    companion object {
        const val TABLE_NAME = "devices"

        /**
         * Create a new device with current timestamp
         */
        fun create(
            name: String,
            address: String,
            type: DeviceType = DeviceType.UNKNOWN,
            isTrusted: Boolean = false
        ): Device {
            return Device(
                name = name,
                address = address,
                type = type,
                isTrusted = isTrusted,
                lastConnected = System.currentTimeMillis()
            )
        }
    }
}
