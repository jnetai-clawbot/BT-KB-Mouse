package com.jnetai.btkbmouse.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jnetai.btkbmouse.BTKBMouseApp
import com.jnetai.btkbmouse.repository.SettingsRepository

/**
 * Factory for creating SettingsViewModel with dependencies
 */
class SettingsViewModelFactory(
    private val application: BTKBMouseApp
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(
                application,
                SettingsRepository(application)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
