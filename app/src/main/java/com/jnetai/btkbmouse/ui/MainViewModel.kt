package com.jnetai.btkbmouse.ui

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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.jnetai.btkbmouse.BTKBMouseApp
import com.jnetai.btkbmouse.data.Device
import com.jnetai.btkbmouse.repository.DeviceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val deviceRepository: DeviceRepository
    private val bluetoothManager: BluetoothManager?
    private val bluetoothAdapter: BluetoothAdapter?

    private val _devices = MutableLiveData<List<Device>>()
    val devices: LiveData<List<Device>> = _devices

    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private val _bluetoothState = MutableLiveData<Int>()
    val bluetoothState: LiveData<Int> = _bluetoothState

    private val _discoveredDevices = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: LiveData<List<BluetoothDevice>> = _discoveredDevices

    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage

    private val _connectionState = MutableLiveData<Map<String, Int>>(emptyMap())
    val connectionState: LiveData<Map<String, Int>> = _connectionState

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
        val app = application as BTKBMouseApp
        deviceRepository = DeviceRepository(app.database.deviceDao())
        bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        loadDevices()
        registerBluetoothReceiver()
        refreshConnectionState()
    }

    private fun loadDevices() {
        viewModelScope.launch {
            deviceRepository.allDevices.collect { deviceList ->
                _devices.value = deviceList
            }
        }
    }

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
            getApplication<BTKBMouseApp>().registerReceiver(bluetoothReceiver, filter)
        }
    }

    fun startScan() {
        if (_isScanning.value == true) return

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

    fun stopScan() {
        bluetoothAdapter?.cancelDiscovery()
        _isScanning.value = false
    }

    fun pairDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                device.createBond()
            } else {
                @Suppress("DEPRECATION")
                device.createBond()
            }
        }
    }

    fun unpairDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    device.javaClass.getMethod("removeBond").invoke(device)
                } else {
                    @Suppress("DEPRECATION")
                    device.javaClass.getMethod("removeBond").invoke(device)
                }
            } catch (e: Exception) {
                _toastMessage.value = "Failed to unpair device: ${e.message}"
            }
        }
    }

    fun saveDevice(device: BluetoothDevice, type: String, trusted: Boolean = false) {
        viewModelScope.launch {
            val existingDevice = deviceRepository.getDeviceByAddress(device.address)
            if (existingDevice == null) {
                val newDevice = Device(
                    name = device.name ?: "Unknown Device",
                    address = device.address,
                    type = type,
                    isTrusted = trusted,
                    lastConnected = System.currentTimeMillis(),
                    batteryLevel = null
                )
                deviceRepository.insertDevice(newDevice)
            }
        }
    }

    fun deleteDevice(device: Device) {
        viewModelScope.launch {
            deviceRepository.deleteDevice(device)
            _toastMessage.value = "Device removed"
        }
    }

    fun refreshConnectionState() {
        viewModelScope.launch {
            val stateMap = mutableMapOf<String, Int>()
            bluetoothManager?.getConnectedDevices(BluetoothProfile.GATT)?.forEach { device ->
                stateMap[device.address] = BluetoothProfile.STATE_CONNECTED
            }
            _connectionState.value = stateMap
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun clearToast() {
        _toastMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        try {
            getApplication<BTKBMouseApp>().unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
        stopScan()
    }
}
