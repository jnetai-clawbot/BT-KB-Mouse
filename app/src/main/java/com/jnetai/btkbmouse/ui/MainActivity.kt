package com.jnetai.btkbmouse.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.jnetai.btkbmouse.R
import com.jnetai.btkbmouse.data.Device
import com.jnetai.btkbmouse.databinding.ActivityMainBinding
import com.jnetai.btkbmouse.ui.adapter.DeviceAdapter
import com.jnetai.btkbmouse.ui.adapter.DiscoveredDeviceAdapter
import com.jnetai.btkbmouse.ui.service.HidService
import com.jnetai.btkbmouse.ui.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var discoveredAdapter: DiscoveredDeviceAdapter

    private var isScanning = false

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            enableBluetooth()
        } else {
            Toast.makeText(this, R.string.bluetooth_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startDiscovery()
        } else {
            Toast.makeText(this, R.string.location_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBluetooth()
        setupViewModel()
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupBluetooth() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        updateBluetoothStatus()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    private fun setupRecyclerViews() {
        deviceAdapter = DeviceAdapter(
            onDeviceClick = { device ->
                showDeviceOptionsDialog(device)
            },
            onDeviceLongClick = { device ->
                showDeviceOptionsDialog(device)
            }
        )

        discoveredAdapter = DiscoveredDeviceAdapter(
            onDeviceClick = { device ->
                viewModel.pairDevice(device)
            }
        )

        binding.rvSavedDevices.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = deviceAdapter
        }

        binding.rvDiscoveredDevices.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = discoveredAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabScan.setOnClickListener {
            if (viewModel.isBluetoothEnabled()) {
                if (hasPermissions()) {
                    toggleScan()
                } else {
                    requestPermissions()
                }
            } else {
                enableBluetooth()
            }
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun observeViewModel() {
        viewModel.savedDevices.observe(this) { devices ->
            deviceAdapter.submitList(devices)
            binding.tvNoDevices.visibility = if (devices.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.discoveredDevices.observe(this) { devices ->
            discoveredAdapter.submitList(devices)
        }

        viewModel.isDiscovering.observe(this) { isDiscovering ->
            isScanning = isDiscovering
            updateScanButton()
            binding.progressScanning.visibility = if (isDiscovering) View.VISIBLE else View.GONE
        }

        viewModel.connectionState.observe(this) { state ->
            updateConnectionStatus(state)
        }
    }

    private fun toggleScan() {
        if (isScanning) {
            stopDiscovery()
        } else {
            startDiscovery()
        }
    }

    private fun startDiscovery() {
        _isDiscovering.value = true
        val devices = mutableListOf<Device>()
        discoveredAdapter.submitList(devices)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                doDiscovery()
            } else {
                bluetoothPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                doDiscovery()
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun doDiscovery() {
        val filter = android.content.IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(discoveryReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(discoveryReceiver, filter)
        }
        bluetoothAdapter?.startDiscovery()
    }

    private fun stopDiscovery() {
        _isDiscovering.value = false
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: SecurityException) {
            // Handle permission error
        }
        try {
            unregisterReceiver(discoveryReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }

    private val discoveryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        val name = it.name ?: "Unknown"
                        val address = it.address
                        val deviceType = getDeviceType(it)

                        val device = Device(
                            name = name,
                            address = address,
                            type = deviceType,
                            isPaired = it.bondState == BluetoothDevice.BOND_BONDED,
                            isConnected = false
                        )

                        val currentList = discoveredAdapter.currentList.toMutableList()
                        if (currentList.none { d -> d.address == device.address }) {
                            currentList.add(device)
                            discoveredAdapter.submitList(currentList)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isDiscovering.value = false
                }
            }
        }
    }

    private fun getDeviceType(device: BluetoothDevice): com.jnetai.btkbmouse.data.DeviceType {
        return when (device.type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> com.jnetai.btkbmouse.data.DeviceType.KEYBOARD
            BluetoothDevice.DEVICE_TYPE_LE -> com.jnetai.btkbmouse.data.DeviceType.MOUSE
            BluetoothDevice.DEVICE_TYPE_DUAL -> com.jnetai.btkbmouse.data.DeviceType.COMBO
            else -> com.jnetai.btkbmouse.data.DeviceType.UNKNOWN
        }
    }

    private val _isDiscovering = androidx.lifecycle.MutableLiveData(false)

    private fun updateScanButton() {
        binding.fabScan.setImageResource(
            if (isScanning) R.drawable.ic_stop else R.drawable.ic_bluetooth_searching
        )
    }

    private fun updateBluetoothStatus() {
        val isEnabled = bluetoothAdapter?.isEnabled == true
        binding.ivBluetoothStatus?.setImageResource(
            if (isEnabled) R.drawable.ic_bluetooth_enabled else R.drawable.ic_bluetooth_disabled
        )
        binding.tvBluetoothStatus.text = if (isEnabled) "On" else "Off"
    }

    private fun updateConnectionStatus(state: HidService.HidConnectionState) {
        binding.ivConnectionStatus?.setImageResource(
            when (state) {
                HidService.HidConnectionState.CONNECTED -> R.drawable.ic_connected
                HidService.HidConnectionState.CONNECTING -> R.drawable.ic_connecting
n                else -> R.drawable.ic_disconnected
            }
        )
    }

    private fun showDeviceOptionsDialog(device: Device) {
        val options = arrayOf(
            getString(R.string.option_connect),
            getString(R.string.option_edit),
            getString(R.string.option_forget)
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(device.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.connectDevice(device)
                    1 -> showEditDeviceDialog(device)
                    2 -> confirmForgetDevice(device)
                }
            }
            .show()
    }

    private fun showEditDeviceDialog(device: Device) {
        val intent = Intent(this, ConnectionActivity::class.java).apply {
            putExtra("device_address", device.address)
            putExtra("device_name", device.name)
        }
        startActivity(intent)
    }

    private fun confirmForgetDevice(device: Device) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.forget_device)
            .setMessage(R.string.confirm_forget_device)
            .setPositiveButton(R.string.forget) { _, _ ->
                viewModel.forgetDevice(device)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bluetoothPermissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun enableBluetooth() {
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBtIntent)
        }
        updateBluetoothStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(discoveryReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }
}
