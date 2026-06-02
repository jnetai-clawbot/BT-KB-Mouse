package com.jnetai.btkbmouse.ui.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for About screen.
 * Uses StateFlow for reactive UI updates as per specification.
 */
class AboutViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // StateFlow for app info
    private val _appName = MutableStateFlow("BT-KB-Mouse")
    val appName: StateFlow<String> = _appName.asStateFlow()

    private val _versionName = MutableStateFlow("")
    val versionName: StateFlow<String> = _versionName.asStateFlow()

    private val _versionCode = MutableStateFlow("")
    val versionCode: StateFlow<String> = _versionCode.asStateFlow()

    private val _buildDate = MutableStateFlow("")
    val buildDate: StateFlow<String> = _buildDate.asStateFlow()

    private val _developerName = MutableStateFlow("jnetai.com")
    val developerName: StateFlow<String> = _developerName.asStateFlow()

    // StateFlow for update checking
    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    // URLs
    val sourceCodeUrl = "https://github.com/jnetai/BT-KB-Mouse"
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.jnetai.btkbmouse"
    val documentationUrl = "https://github.com/jnetai/BT-KB-Mouse#readme"
    val bugReportUrl = "https://github.com/jnetai/BT-KB-Mouse/issues/new"

    init {
        loadAppInfo()
    }

    /**
     * Load app information from PackageManager
     */
    private fun loadAppInfo() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.getPackageInfo(
                            context.packageName,
                            PackageManager.PackageInfoFlags.of(0)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    }

                    _appName.value = "BT-KB-Mouse"
                    _versionName.value = "v${packageInfo.versionName}"
                    _versionCode.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toString()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toString()
                    }

                    // Get build/install date from package info
                    val installTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageInfo.firstInstallTime
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.firstInstallTime
                    }

                    val dateFormat = SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.getDefault())
                    _buildDate.value = dateFormat.format(Date(installTime))

                    _developerName.value = "jnetai.com"
                } catch (e: Exception) {
                    _appName.value = "BT-KB-Mouse"
                    _versionName.value = "v1.0.0"
                    _versionCode.value = "1"
                    _buildDate.value = "Unknown"
                    _developerName.value = "jnetai.com"
                }
            }
        }
    }

    /**
     * Get current version code for comparison
     */
    fun getVersionCode(): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).longVersionCode
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
            }
        } catch (e: Exception) {
            1L
        }
    }

    /**
     * Get version name string
     */
    fun getVersion(): String = _versionName.value

    /**
     * Get build date string
     */
    fun getBuildDate(): String = _buildDate.value

    /**
     * Check for app updates
     */
    fun checkForUpdate() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _updateStatus.value = UpdateStatus.Checking

            withContext(Dispatchers.IO) {
                try {
                    // Simulate update check - in production this would call GitHub API or similar
                    kotlinx.coroutines.delay(1500)

                    // For demo purposes, always report up to date
                    // In production, compare with latest release from GitHub API
                    _updateStatus.value = UpdateStatus.UpToDate
                } catch (e: Exception) {
                    _updateStatus.value = UpdateStatus.Error(e.message ?: "Unknown error")
                }
            }

            _isCheckingUpdate.value = false
        }
    }

    /**
     * Get share text for the app
     */
    fun getShareText(): String {
        return """
            Check out BT-KB-Mouse - Android app for Bluetooth keyboard and mouse control

            App: ${_appName.value}
            Version: ${_versionName.value}
            Developer: ${_developerName.value}

            Download: https://github.com/jnetai/BT-KB-Mouse/releases
        """.trimIndent()
    }

    /**
     * Reset update status to idle
     */
    fun resetUpdateStatus() {
        _updateStatus.value = UpdateStatus.Idle
    }

    /**
     * Sealed class for update status
     */
    sealed class UpdateStatus {
        data object Idle : UpdateStatus()
        data object Checking : UpdateStatus()
        data object UpToDate : UpdateStatus()
        data class UpdateAvailable(val version: String) : UpdateStatus()
        data class Error(val message: String) : UpdateStatus()
    }
}
