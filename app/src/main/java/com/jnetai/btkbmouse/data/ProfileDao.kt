package com.jnetai.btkbmouse.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for Profile entity
 */
@Dao
interface ProfileDao {

    /**
     * Observe all profiles ordered by name
     */
    @Query("SELECT * FROM profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<Profile>>

    /**
     * Observe profiles for a specific device ordered by name
     */
    @Query("SELECT * FROM profiles WHERE deviceAddress = :deviceAddress ORDER BY name ASC")
    fun getProfilesByDevice(deviceAddress: String): Flow<List<Profile>>

    /**
     * Get a single profile by ID
     */
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: Long): Profile?

    /**
     * Get all profiles as a one-shot list (non-Flow)
     */
    @Query("SELECT * FROM profiles ORDER BY name ASC")
    suspend fun getAllProfilesList(): List<Profile>

    /**
     * Get profiles for a device as a one-shot list (non-Flow)
     */
    @Query("SELECT * FROM profiles WHERE deviceAddress = :deviceAddress ORDER BY name ASC")
    suspend fun getProfilesByDeviceList(deviceAddress: String): List<Profile>

    /**
     * Insert or replace a profile
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    /**
     * Update an existing profile
     */
    @Update
    suspend fun updateProfile(profile: Profile)

    /**
     * Delete a profile
     */
    @Delete
    suspend fun deleteProfile(profile: Profile)

    /**
     * Delete a profile by ID
     */
    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    /**
     * Delete all profiles for a device
     */
    @Query("DELETE FROM profiles WHERE deviceAddress = :deviceAddress")
    suspend fun deleteProfilesByDevice(deviceAddress: String)

    /**
     * Check if a profile name exists for a device
     */
    @Query("SELECT COUNT(*) > 0 FROM profiles WHERE name = :name AND deviceAddress = :deviceAddress")
    suspend fun profileNameExists(name: String, deviceAddress: String): Boolean

    /**
     * Get profile count
     */
    @Query("SELECT COUNT(*) FROM profiles")
    suspend fun getProfileCount(): Int

    /**
     * Get profile count for a device
     */
    @Query("SELECT COUNT(*) FROM profiles WHERE deviceAddress = :deviceAddress")
    suspend fun getProfileCountForDevice(deviceAddress: String): Int
}
