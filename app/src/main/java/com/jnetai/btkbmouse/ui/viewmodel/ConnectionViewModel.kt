package com.jnetai.btkbmouse.ui.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jnetai.btkbmouse.BTKBMouseApp
import com.jnetai.btkbmouse.data.Device
import com.jnetai.btkbmouse.data.DeviceSettings
import com.jnetai.btkbmouse.data.Profile
import com.jnetai.btkbmouse.repository.DeviceRepository
import com.jnetai.btkbmouse.repository.ProfileRepository
import com.jnetai.btkbmouse.repository.SettingsRepository
import com.jnetai.btkbmouse.service.HidService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Connection states for Bluetooth HID device
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

/**
 * ViewModel for device connection management.
 * Uses StateFlow for reactive UI updates as per specification.
 */
class ConnectionViewModel(
    application: Application,
    private val deviceRepository: DeviceRepository,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

    private val bluetoothManager: BluetoothManager? = application.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    // StateFlow for device
    private val _device = MutableStateFlow<Device?>(null)
    val device: StateFlow<Device?> = _device.asStateFlow()

    // StateFlow for connection state
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // StateFlow for profiles
    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    // StateFlow for device settings
    private val _settings = MutableStateFlow(DeviceSettings.default())
    val settings: StateFlow<DeviceSettings> = _settings.asStateFlow()

    // StateFlow for battery level
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    // StateFlow for signal strength
    private val _signalStrength = MutableStateFlow<Int?>(null)
    val signalStrength: StateFlow<Int?> = _signalStrength.asStateFlow()

    // StateFlow for error messages
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // StateFlow for toast messages
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // StateFlow for connection time
    private val _connectionTime = MutableStateFlow<Long?>(null)
    val connectionTime: StateFlow<Long?> = _connectionTime.asStateFlow()

    // StateFlow for active profile
    private val _activeProfile = MutableStateFlow<Profile?>(null)
    val activeProfile: StateFlow<Profile?> = _activeProfile.asStateFlow()

    private var hidService: HidService? = null
    private var isBound = false
    private var connectionStartTime: Long = 0L

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as HidService.LocalBinder
            hidService = binder.getService()
            isBound = true
            observeServiceState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hidService = null
            isBound = false
        }
    }

    init {
        loadSettings()
    }

    /**
     * Load device by address from database
     */
    fun loadDevice(deviceAddress: String) {
        viewModelScope.launch {
            val loadedDevice = deviceRepository.getDeviceByAddress(deviceAddress)
            _device.value = loadedDevice
            loadedDevice?.let {
                _batteryLevel.value = it.batteryLevel
                _signalStrength.value = it.signalStrength
            }
        }
        loadProfiles(deviceAddress)
    }

    /**
     * Load profiles for specific device
     */
    private fun loadProfiles(deviceAddress: String) {
        viewModelScope.launch {
            profileRepository.getProfilesByDevice(deviceAddress).collect { profileList ->
                _profiles.value = profileList
                // Find active profile
                _activeProfile.value = profileList.find { it.isActive }
            }
        }
    }

    /**
     * Load app settings
     */
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _settings.value = settings
            }
        }
    }

    /**
     * Connect to device
     */
    fun connectDevice() {
        val currentDevice = _device.value ?: return
        
        val context = getApplication<BTKBMouseApp>()
        val intent = Intent(context, HidService::class.java).apply {
            action = HidService.ACTION_CONNECT
            putExtra(HidService.EXTRA_DEVICE_ADDRESS, currentDevice.address)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        _connectionState.value = ConnectionState.CONNECTING
        connectionStartTime = System.currentTimeMillis()
        bindService()
    }

    /**
     * Disconnect from device
     */
    fun disconnectDevice() {
        val currentDevice = _device.value ?: return
        
        val context = getApplication<BTKBMouseApp>()
        val intent = Intent(context, HidService::class.java).apply {
            action = HidService.ACTION_DISCONNECT
            putExtra(HidService.EXTRA_DEVICE_ADDRESS, currentDevice.address)
        }
        context.startService(intent)
        
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectionTime.value = null
        updateLastConnected()
    }

    /**
     * Bind to HID service
     */
    private fun bindService() {
        val context = getApplication<BTKBMouseApp>()
        val intent = Intent(context, HidService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * Observe service connection state
     */
    private fun observeServiceState() {
        hidService?.let { service ->
            viewModelScope.launch {
                service.connectionState.observeForever { state ->
                    when (state) {
                        HidService.STATE_CONNECTED -> {
                            _connectionState.value = ConnectionState.CONNECTED
                            _connectionTime.value = System.currentTimeMillis() - connectionStartTime
                        }
                        HidService.STATE_CONNECTING -> {
                            _connectionState.value = ConnectionState.CONNECTING
                        }
                        else -> {
                            _connectionState.value = ConnectionState.DISCONNECTED
                            _connectionTime.value = null
                        }
                    }
                }
            }
            viewModelScope.launch {
                service.batteryLevel.observeForever { level ->
                    _batteryLevel.value = level
                    updateDeviceBattery(level)
                }
            }
        }
    }

    /**
     * Update device battery level in database
     */
    private fun updateDeviceBattery(level: Int?) {
        viewModelScope.launch {
            _device.value?.let { device ->
                deviceRepository.updateDevice(device.copy(batteryLevel = level))
            }
        }
    }

    /**
     * Update last connected timestamp
     */
    private fun updateLastConnected() {
        viewModelScope.launch {
            _device.value?.let { device ->
                deviceRepository.updateDevice(device.copy(lastConnected = System.currentTimeMillis()))
            }
        }
    }

    /**
     * Pair device via Bluetooth
     */
    fun pairDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    device.createBond()
                } else {
                    @Suppress("DEPRECATION")
                    device.createBond()
                }
                _toastMessage.value = "Pairing initiated"
            } catch (e: Exception) {
                _error.value = "Pairing failed: ${e.message}"
            }
        }
    }

    /**
     * Unpair device from system
     */
    fun unpairDevice() {
        viewModelScope.launch {
            val currentDevice = _device.value ?: return@launch
            try {
                val bluetoothDevice = bluetoothManager?.adapter?.getRemoteDevice(currentDevice.address)
                bluetoothDevice?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        it.javaClass.getMethod("removeBond").invoke(it)
                    } else {
                        @Suppress("DEPRECATION")
                        it.javaClass.getMethod("removeBond").invoke(it)
                    }
                }
                deviceRepository.setDeviceTrusted(currentDevice.address, false)
                _toastMessage.value = "Device unpaired"
            } catch (e: Exception) {
                _error.value = "Unpair failed: ${e.message}"
            }
        }
    }

    /**
     * Set device as trusted
     */
    fun setTrusted(trusted: Boolean) {
        viewModelScope.launch {
            val currentDevice = _device.value ?: return@launch
            deviceRepository.setDeviceTrusted(currentDevice.address, trusted)
            _device.value = currentDevice.copy(isTrusted = trusted)
            _toastMessage.value = if (trusted) "Device marked as trusted" else "Device removed from trusted"
        }
    }

    /**
     * Save current device state
     */
    fun saveDevice() {
        viewModelScope.launch {
            _device.value?.let { device ->
                deviceRepository.updateDevice(device.copy(lastConnected = System.currentTimeMillis()))
            }
        }
    }

    /**
     * Forget device - remove from database and unpair
     */
    fun forgetDevice() {
        viewModelScope.launch {
            val currentDevice = _device.value ?: return@launch
            // Disconnect first if connected
            if (_connectionState.value == ConnectionState.CONNECTED) {
                disconnectDevice()
            }
            // Unpair from system
            unpairDevice()
            // Delete from database
            deviceRepository.deleteDevice(currentDevice)
            _toastMessage.value = "Device forgotten"
        }
    }

    /**
     * Delete device from database only
     */
    fun deleteDevice() {
        viewModelScope.launch {
            _device.value?.let { device ->
                deviceRepository.deleteDevice(device)
                _toastMessage.value = "Device removed"
            }
        }
    }

    /**
     * Create new profile for device
     */
    fun createProfile(name: String) {
        viewModelScope.launch {
            val currentDevice = _device.value ?: return@launch
            val newProfile = Profile(
                name = name,
                deviceAddress = currentDevice.address,
                mouseSensitivity = 50,
                scrollSpeed = 50,
                keyRepeatDelay = 500,
                keyRepeatRate = 100,
                leftHandedMode = false,
                smoothAcceleration = true,
                autoReconnect = true
            )
            profileRepository.insertProfile(newProfile)
            _toastMessage.value = "Profile created"
        }
    }

    /**
     * Delete profile
     */
    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profile)
            _toastMessage.value = "Profile deleted"
        }
    }

    /**
     * Select and activate profile
     */
    fun selectProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.updateProfile(profile.copy(isActive = true))
            _activeProfile.value = profile
            _toastMessage.value = "Profile '${profile.name}' activated"
        }
    }

    /**
     * Save profile with updated settings
     */
    fun saveProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.updateProfile(profile.copy(updatedAt = System.currentTimeMillis()))
            _toastMessage.value = "Profile saved"
        }
    }

    /**
     * Update mouse sensitivity for active profile
     */
    fun updateMouseSensitivity(sensitivity: Int) {
        viewModelScope.launch {
            _activeProfile.value?.let { profile ->
                val updated = profile.copy(mouseSensitivity = sensitivity, updatedAt = System.currentTimeMillis())
                profileRepository.updateProfile(updated)
                _activeProfile.value = updated
            }
        }
    }

    /**
     * Clear toast message
     */
    fun clearToast() {
        _toastMessage.value = null
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<BTKBMouseApp>().unbindService(serviceConnection)
            isBound = false
        }
    }
}
