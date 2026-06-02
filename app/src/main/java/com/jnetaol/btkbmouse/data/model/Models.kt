package com.jnetaol.btkbmouse.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profiles", indices = [Index(value = ["name"], unique = true)])
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val deviceAddress: String = "",
    val deviceName: String = "",
    val mouseSensitivity: Float = 1.0f,
    val mouseScrollSpeed: Float = 1.0f,
    val keyboardDelay: Long = 100L,
    val keyboardRepeatDelay: Long = 500L,
    val speakerVolume: Float = 0.5f,
    val micGain: Float = 0.5f,
    val isActive: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String = ""
)

@Entity(tableName = "preset_phrases", indices = [Index(value = ["label"])])
data class PresetPhrase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "clipboard_history")
data class ClipboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "emulated_devices")
data class EmulatedDevice(
    @PrimaryKey val deviceType: String,
    val isEnabled: Boolean = false,
    val label: String = "",
    val iconName: String = ""
)
