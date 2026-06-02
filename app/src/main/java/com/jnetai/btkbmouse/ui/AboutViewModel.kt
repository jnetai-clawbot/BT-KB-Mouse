package com.jnetai.btkbmouse.ui

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AboutViewModel(application: Application) : AndroidViewModel(application) {

    private val _appName = MutableLiveData<String>()
    val appName: LiveData<String> = _appName

    private val _versionName = MutableLiveData<String>()
    val versionName: LiveData<String> = _versionName

    private val _versionCode = MutableLiveData<String>()
    val versionCode: LiveData<String> = _versionCode

    private val _buildDate = MutableLiveData<String>()
    val buildDate: LiveData<String> = _buildDate

    private val _developerName = MutableLiveData<String>()
    val developerName: LiveData<String> = _developerName

    init {
        loadAppInfo()
    }

    private fun loadAppInfo() {
        val context = getApplication<Application>()
        
        try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
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
            
            // Get build date from package info
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

    fun getShareText(): String {
        return """
            Check out BT-KB-Mouse - Android app for Bluetooth keyboard and mouse control
            
            App: ${_appName.value}
            Version: ${_versionName.value}
            Developer: ${_developerName.value}
            
            Download: https://github.com/jnetai/BT-KB-Mouse/releases
        """.trimIndent()
    }

    fun getSourceCodeUrl(): String {
        return "https://github.com/jnetai/BT-KB-Mouse"
    }

    fun getPlayStoreUrl(): String {
        return "https://play.google.com/store/apps/details?id=com.jnetai.btkbmouse"
    }

    fun getDocumentationUrl(): String {
        return "https://github.com/jnetai/BT-KB-Mouse#readme"
    }

    fun getBugReportUrl(): String {
        return "https://github.com/jnetai/BT-KB-Mouse/issues/new"
    }
}
