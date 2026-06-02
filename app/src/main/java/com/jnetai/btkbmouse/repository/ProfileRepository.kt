package com.jnetai.btkbmouse.repository

import com.jnetai.btkbmouse.data.Profile
import com.jnetai.btkbmouse.data.ProfileDao
import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val profileDao: ProfileDao) {

    val allProfiles: Flow<List<Profile>> = profileDao.getAllProfiles()

    fun getProfilesByDevice(deviceAddress: String): Flow<List<Profile>> {
        return profileDao.getProfilesByDevice(deviceAddress)
    }

    suspend fun getProfileById(id: Long): Profile? {
        return profileDao.getProfileById(id)
    }

    suspend fun insertProfile(profile: Profile): Long {
        return profileDao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: Profile) {
        profileDao.updateProfile(profile.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProfile(profile: Profile) {
        profileDao.deleteProfile(profile)
    }

    suspend fun duplicateProfile(profile: Profile, newName: String): Long {
        val newProfile = profile.copy(
            id = 0,
            name = newName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return profileDao.insertProfile(newProfile)
    }
}
