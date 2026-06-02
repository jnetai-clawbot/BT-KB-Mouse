package com.jnetaol.btkbmouse.data.db

import androidx.room.*
import com.jnetaol.btkbmouse.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    @Update
    suspend fun updateProfile(profile: Profile)

    @Delete
    suspend fun deleteProfile(profile: Profile)

    @Query("SELECT * FROM profiles ORDER BY updatedTimestamp DESC")
    fun getAllProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): Profile?

    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProfile(): Profile?

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun deactivateAllProfiles()

    @Query("UPDATE profiles SET isActive = 1 WHERE id = :id")
    suspend fun setActiveProfile(id: Long)

    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getProfileCount(): Int
}

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: AppSetting)

    @Query("SELECT * FROM settings WHERE `key` = :key")
    suspend fun getSetting(key: String): AppSetting?

    @Query("SELECT value FROM settings WHERE `key` = :key")
    suspend fun getSettingValue(key: String): String?

    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<AppSetting>>

    @Delete
    suspend fun deleteSetting(setting: AppSetting)
}

@Dao
interface PresetPhraseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhrase(phrase: PresetPhrase): Long

    @Update
    suspend fun updatePhrase(phrase: PresetPhrase)

    @Delete
    suspend fun deletePhrase(phrase: PresetPhrase)

    @Query("SELECT * FROM preset_phrases ORDER BY timestamp DESC")
    fun getAllPhrases(): Flow<List<PresetPhrase>>

    @Query("SELECT * FROM preset_phrases WHERE id = :id")
    suspend fun getPhraseById(id: Long): PresetPhrase?
}

@Dao
interface ClipboardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: ClipboardEntry): Long

    @Query("SELECT * FROM clipboard_history ORDER BY timestamp DESC LIMIT 50")
    fun getAllEntries(): Flow<List<ClipboardEntry>>

    @Query("DELETE FROM clipboard_history")
    suspend fun clearAll()
}

@Dao
interface EmulatedDeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setDevice(device: EmulatedDevice)

    @Query("SELECT * FROM emulated_devices")
    fun getAllDevices(): Flow<List<EmulatedDevice>>

    @Query("SELECT * FROM emulated_devices WHERE isEnabled = 1")
    fun getEnabledDevices(): Flow<List<EmulatedDevice>>

    @Query("UPDATE emulated_devices SET isEnabled = :enabled WHERE deviceType = :deviceType")
    suspend fun setDeviceEnabled(deviceType: String, enabled: Boolean)
}
