package com.jnetai.btkbmouse.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing Profile entities.
 * Provides Flow-based observation and CRUD operations for device profiles.
 */
class ProfileRepository(private val database: AppDatabase) {

    private val profileDao: ProfileDao = database.profileDao()

    /**
     * Observe all profiles
     */
    fun observeAllProfiles(): Flow<List<Profile>> {
        return profileDao.getAllProfiles()
    }

    /**
     * Observe profiles for a specific device
     */
    fun observeProfilesForDevice(deviceAddress: String): Flow<List<Profile>> {
        return profileDao.getProfilesByDevice(deviceAddress)
    }

    /**
     * Get a profile by ID (one-shot)
     */
    suspend fun getProfileById(id: Long): Profile? {
        return profileDao.getProfileById(id)
    }

    /**
     * Get all profiles as a list (one-shot)
     */
    suspend fun getAllProfilesList(): List<Profile> {
        return profileDao.getAllProfilesList()
    }

    /**
     * Get profiles for a device as a list (one-shot)
     */
    suspend fun getProfilesByDeviceList(deviceAddress: String): List<Profile> {
        return profileDao.getProfilesByDeviceList(deviceAddress)
    }

    /**
     * Save a profile (insert or update)
     */
    suspend fun saveProfile(profile: Profile): Long {
        return profileDao.insertProfile(profile)
    }

    /**
     * Create and save a new profile from basic info
     */
    suspend fun createProfile(
        name: String,
        deviceAddress: String,
        mouseSensitivity: Int = DeviceSettings.MOUSE_SENSITIVITY_DEFAULT,
        scrollSpeed: Int = DeviceSettings.SCROLL_SPEED_DEFAULT,
        keyRepeatDelay: Int = DeviceSettings.KEY_REPEAT_DELAY_DEFAULT,
        keyRepeatRate: Int = DeviceSettings.KEY_REPEAT_RATE_DEFAULT,
        leftHandedMode: Boolean = false,
        smoothAcceleration: Boolean = true,
        autoReconnect: Boolean = true
    ): Long {
        val profile = Profile.create(
            name = name,
            deviceAddress = deviceAddress,
            mouseSensitivity = mouseSensitivity,
            scrollSpeed = scrollSpeed,
            keyRepeatDelay = keyRepeatDelay,
            keyRepeatRate = keyRepeatRate,
            leftHandedMode = leftHandedMode,
            smoothAcceleration = smoothAcceleration,
            autoReconnect = autoReconnect
        )
        return profileDao.insertProfile(profile)
    }

    /**
     * Create a profile from current device settings
     */
    suspend fun createProfileFromSettings(
        name: String,
        deviceAddress: String,
        settings: DeviceSettings
    ): Long {
        val profile = Profile.fromDeviceSettings(name, deviceAddress, settings)
        return profileDao.insertProfile(profile)
    }

    /**
     * Update an existing profile
     */
    suspend fun updateProfile(profile: Profile) {
        profileDao.updateProfile(profile.withUpdatedTimestamp())
    }

    /**
     * Delete a profile
     */
    suspend fun deleteProfile(profile: Profile) {
        profileDao.deleteProfile(profile)
    }

    /**
     * Delete a profile by ID
     */
    suspend fun deleteProfileById(id: Long) {
        profileDao.deleteProfileById(id)
    }

    /**
     * Delete all profiles for a device
     */
    suspend fun deleteProfilesByDevice(deviceAddress: String) {
        profileDao.deleteProfilesByDevice(deviceAddress)
    }

    /**
     * Check if a profile name exists for a device
     */
    suspend fun profileNameExists(name: String, deviceAddress: String): Boolean {
        return profileDao.profileNameExists(name, deviceAddress)
    }

    /**
     * Get profile count
     */
    suspend fun getProfileCount(): Int {
        return profileDao.getProfileCount()
    }

    /**
     * Get profile count for a device
     */
    suspend fun getProfileCountForDevice(deviceAddress: String): Int {
        return profileDao.getProfileCountForDevice(deviceAddress)
    }

    /**
     * Duplicate an existing profile with a new name
     */
    suspend fun duplicateProfile(profile: Profile, newName: String): Long {
        val duplicated = profile.copy(
            id = 0,
            name = newName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return profileDao.insertProfile(duplicated)
    }

    /**
     * Rename a profile
     */
    suspend fun renameProfile(profile: Profile, newName: String) {
        profileDao.updateProfile(profile.copy(name = newName).withUpdatedTimestamp())
    }
}
