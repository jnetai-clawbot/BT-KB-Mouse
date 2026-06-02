package com.jnetai.btkbmouse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jnetai.btkbmouse.BTKBMouseApp
import com.jnetai.btkbmouse.repository.DeviceRepository
import com.jnetai.btkbmouse.repository.ProfileRepository
import com.jnetai.btkbmouse.repository.SettingsRepository

/**
 * Factory for creating ConnectionViewModel with dependencies
 */
class ConnectionViewModelFactory(
    private val application: BTKBMouseApp
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConnectionViewModel::class.java)) {
            return ConnectionViewModel(
                application,
                DeviceRepository(application.database.deviceDao()),
                ProfileRepository(application.database.profileDao()),
                SettingsRepository(application)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
