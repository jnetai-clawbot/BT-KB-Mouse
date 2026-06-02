package com.jnetai.btkbmouse.ui.viewmodel

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jnetai.btkbmouse.BTKBMouseApp
import com.jnetai.btkbmouse.data.Device
import com.jnetai.btkbmouse.data.DeviceType
import com.jnetai.btkbmouse.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Main ViewModel for device scanning and management.
 * Uses StateFlow for reactive UI updates as per specification.
 */
class MainViewModel(
    application: Application,
    private val deviceRepository: DeviceRepository
) : AndroidViewModel(application) {

    private val bluetoothManager: BluetoothManager? = application.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    // StateFlow for devices list
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    // StateFlow for scanning state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    // StateFlow for error messages
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // StateFlow for discovered Bluetooth devices
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()

    // StateFlow for connection states
    private val _connectionState = MutableStateFlow<Map<String, Int>>(emptyMap())
    val connectionState: StateFlow<Map<String, Int>> = _connectionState.asStateFlow()

    // StateFlow for toast messages
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // StateFlow for Bluetooth state
    private val _bluetoothState = MutableStateFlow(BluetoothAdapter.ERROR)
    val bluetoothState: StateFlow<Int> = _bluetoothState.asStateFlow()

    private val discoveredDevicesSet = mutableSetOf<BluetoothDevice>()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        if (!discoveredDevicesSet.contains(it)) {
                            discoveredDevicesSet.add(it)
                            _discoveredDevices.value = discoveredDevicesSet.toList()
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isScanning.value = true
                    discoveredDevicesSet.clear()
                    _discoveredDevices.value = emptyList()
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    _bluetoothState.value = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let { refreshConnectionState() }
                }
            }
        }
    }

    init {
        loadDevices()
        registerBluetoothReceiver()
        refreshConnectionState()
    }

    /**
     * Load all devices from Room database
     */
    private fun loadDevices() {
        viewModelScope.launch {
            deviceRepository.allDevices.collect { deviceList ->
                _devices.value = deviceList
            }
        }
    }

    /**
     * Register Bluetooth broadcast receiver
     */
    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplication<BTKBMouseApp>().registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            getApplication<BTKBMouseApp>().registerReceiver(bluetoothReceiver, filter)
        }
    }

    /**
     * Start Bluetooth discovery scan
     */
    fun startScan() {
        if (_isScanning.value) return

        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }

        val discoveryIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        getApplication<BTKBMouseApp>().startActivity(discoveryIntent)

        bluetoothAdapter?.startDiscovery()
    }

    /**
     * Stop Bluetooth discovery scan
     */
    fun stopScan() {
        bluetoothAdapter?.cancelDiscovery()
        _isScanning.value = false
    }

    /**
     * Pair with a Bluetooth device
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
                // Auto-save device to database
                saveDevice(device, DeviceType.UNKNOWN.name, false)
                _toastMessage.value = "Pairing initiated"
            } catch (e: Exception) {
                _error.value = "Pairing failed: ${e.message}"
            }
        }
    }

    /**
     * Unpair a Bluetooth device
     */
    fun unpairDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    device.javaClass.getMethod("removeBond").invoke(device)
                } else {
                    @Suppress("DEPRECATION")
                    device.javaClass.getMethod("removeBond").invoke(device)
                }
                _toastMessage.value = "Device unpaired"
            } catch (e: Exception) {
                _error.value = "Failed to unpair device: ${e.message}"
            }
        }
    }

    /**
     * Save discovered device to database
     */
    fun saveDevice(device: BluetoothDevice, type: String, trusted: Boolean = false) {
        viewModelScope.launch {
            val existingDevice = deviceRepository.getDeviceByAddress(device.address)
            if (existingDevice == null) {
                val newDevice = Device(
                    name = device.name ?: "Unknown Device",
                    address = device.address,
                    type = DeviceType.fromString(type),
                    isTrusted = trusted,
                    lastConnected = System.currentTimeMillis(),
                    batteryLevel = null
                )
                deviceRepository.insertDevice(newDevice)
            }
        }
    }

    /**
     * Delete a device from database
     */
    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            deviceRepository.deleteDevice(device)
            _toastMessage.value = "Device removed"
        }
    }

    /**
     * Refresh connection states for all devices
     */
    fun refreshConnectionState() {
        viewModelScope.launch {
            val stateMap = mutableMapOf<String, Int>()
            bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT)?.forEach { device ->
                stateMap[device.address] = BluetoothProfile.STATE_CONNECTED
            }
            _connectionState.value = stateMap
        }
    }

    /**
     * Clear toast message after displaying
     */
    fun clearToast() {
        _toastMessage.value = null
    }

    /**
     * Clear error message after displaying
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Get Bluetooth adapter for permission checks
     */
    fun getBluetoothAdapter(): BluetoothAdapter? = bluetoothAdapter

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<BTKBMouseApp>().unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // Receiver may not be registered
        }
    }
}
