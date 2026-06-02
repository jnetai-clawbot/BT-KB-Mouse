package com.jnetai.btkbmouse.ui

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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jnetai.btkbmouse.BTKBMouseApp
import com.jnetai.btkbmouse.data.Device
import com.jnetai.btkbmouse.data.Profile
import com.jnetai.btkbmouse.repository.DeviceRepository
import com.jnetai.btkbmouse.repository.ProfileRepository
import com.jnetai.btkbmouse.service.HidService
import kotlinx.coroutines.launch

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    private val deviceRepository: DeviceRepository
    private val profileRepository: ProfileRepository
    private val bluetoothManager: BluetoothManager?

    private val _device = MutableLiveData<Device?>()
    val device: LiveData<Device?> = _device

    private val _connectionState = MutableLiveData<Int>()
    val connectionState: LiveData<Int> = _connectionState

    private val _profiles = MutableLiveData<List<Profile>>()
    val profiles: LiveData<List<Profile>> = _profiles

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val _batteryLevel = MutableLiveData<Int?>()
    val batteryLevel: LiveData<Int?> = _batteryLevel

    private val _signalStrength = MutableLiveData<Int?>()
    val signalStrength: LiveData<Int?> = _signalStrength

    private var hidService: HidService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as HidService.LocalBinder
            hidService = binder.getService()
            isBound = true
            observeConnectionState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hidService = null
            isBound = false
        }
    }

    init {
        val app = application as BTKBMouseApp
        deviceRepository = DeviceRepository(app.database.deviceDao())
        profileRepository = ProfileRepository(app.database.profileDao())
        bluetoothManager = app.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    fun loadDevice(deviceAddress: String) {
        viewModelScope.launch {
            val loadedDevice = deviceRepository.getDeviceByAddress(deviceAddress)
            _device.value = loadedDevice
            loadedDevice?.let {
                _batteryLevel.value = it.batteryLevel
            }
        }
        loadProfiles(deviceAddress)
    }

    private fun loadProfiles(deviceAddress: String) {
        viewModelScope.launch {
            profileRepository.getProfilesByDevice(deviceAddress).collect { profileList ->
                _profiles.value = profileList
            }
        }
    }

    fun connect(device: Device) {
        val context = getApplication<BTKBMouseApp>()
        val intent = Intent(context, HidService::class.java).apply {
            action = HidService.ACTION_CONNECT
            putExtra(HidService.EXTRA_DEVICE_ADDRESS, device.address)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        bindService()
    }

    fun disconnect(device: Device) {
        val context = getApplication<BTKBMouseApp>()
        val intent = Intent(context, HidService::class.java).apply {
            action = HidService.ACTION_DISCONNECT
            putExtra(HidService.EXTRA_DEVICE_ADDRESS, device.address)
        }
        context.startService(intent)
    }

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
                _toastMessage.value = "Pairing failed: ${e.message}"
            }
        }
    }

    fun unpairDevice(device: Device) {
        viewModelScope.launch {
            try {
                val bluetoothDevice = bluetoothManager?.adapter?.getRemoteDevice(device.address)
                bluetoothDevice?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        it.javaClass.getMethod("removeBond").invoke(it)
                    } else {
                        @Suppress("DEPRECATION")
                        it.javaClass.getMethod("removeBond").invoke(it)
                    }
                }
                deviceRepository.setDeviceTrusted(device.address, false)
                _toastMessage.value = "Device unpaired"
            } catch (e: Exception) {
                _toastMessage.value = "Unpair failed: ${e.message}"
            }
        }
    }

    fun setTrusted(device: Device, trusted: Boolean) {
        viewModelScope.launch {
            deviceRepository.setDeviceTrusted(device.address, trusted)
            _device.value = device.copy(isTrusted = trusted)
            _toastMessage.value = if (trusted) "Device marked as trusted" else "Device removed from trusted"
        }
    }

    fun saveDevice(device: Device) {
        viewModelScope.launch {
            deviceRepository.updateDevice(device.copy(lastConnected = System.currentTimeMillis()))
        }
    }

    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            deviceRepository.deleteDevice(device)
            _toastMessage.value = "Device removed"
        }
    }

    fun createProfile(name: String, deviceAddress: String) {
        viewModelScope.launch {
            val newProfile = Profile(
                name = name,
                deviceAddress = deviceAddress,
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

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profile)
            _toastMessage.value = "Profile deleted"
        }
    }

    fun selectProfile(profile: Profile) {
        viewModelScope.launch {
            profileRepository.updateProfile(profile.copy(isActive = true))
            _toastMessage.value = "Profile '${profile.name}' activated"
        }
    }

    private fun bindService() {
        val context = getApplication<BTKBMouseApp>()
        val intent = Intent(context, HidService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun observeConnectionState() {
        hidService?.connectionState?.observeForever { state ->
            _connectionState.value = state
        }
    }

    fun unbindService() {
        if (isBound) {
            getApplication<BTKBMouseApp>().unbindService(serviceConnection)
            isBound = false
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        unbindService()
    }
}
