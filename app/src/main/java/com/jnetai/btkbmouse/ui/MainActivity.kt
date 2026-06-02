package com.jnetai.btkbmouse.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
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

    private val _isDiscovering = MutableLiveData(false)

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            startDiscovery()
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

        setSupportActionBar(binding.toolbar)
        setupBluetooth()
        setupViewModel()
        setupRecyclerViews()
        observeViewModel()
    }

    private fun setupBluetooth() {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    private fun setupRecyclerViews() {
        deviceAdapter = DeviceAdapter(
            onDeviceClick = { device -> showDeviceOptionsDialog(device) },
            onDeviceLongClick = { device -> showDeviceOptionsDialog(device) }
        )

        discoveredAdapter = DiscoveredDeviceAdapter(
            onDeviceClick = { device -> viewModel.pairDevice(device) }
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

    private fun observeViewModel() {
        viewModel.savedDevices.observe(this) { devices ->
            deviceAdapter.submitList(devices)
            binding.tvNoSavedDevices.visibility = if (devices.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.discoveredDevices.observe(this) { devices ->
            discoveredAdapter.submitList(devices)
            binding.tvNoDevicesFound.visibility = if (devices.isEmpty()) View.VISIBLE else View.GONE
        }

        _isDiscovering.observe(this) { isDiscovering ->
            binding.progressScan.visibility = if (isDiscovering) View.VISIBLE else View.GONE
            binding.tvScanning.visibility = if (isDiscovering) View.VISIBLE else View.GONE
            invalidateOptionsMenu()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_scan -> {
                if (_isDiscovering.value == true) {
                    stopDiscovery()
                } else {
                    requestPermissionsAndScan()
                }
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun requestPermissionsAndScan() {
        if (bluetoothAdapter?.isEnabled != true) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBtIntent)
            return
        }

        if (hasPermissions()) {
            startDiscovery()
        } else {
            requestPermissions()
        }
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

    private fun startDiscovery() {
        _isDiscovering.value = true

        val filter = android.content.IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(discoveryReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(discoveryReceiver, filter)
        }

        try {
            bluetoothAdapter?.startDiscovery()
        } catch (e: SecurityException) {
            Toast.makeText(this, R.string.bluetooth_permission_required, Toast.LENGTH_SHORT).show()
            _isDiscovering.value = false
        }
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
                    device?.let { btDevice ->
                        val name = btDevice.name ?: "Unknown"
                        val address = btDevice.address
                        val type = getDeviceType(btDevice)

                        val device = Device(
                            name = name,
                            address = address,
                            type = type,
                            isPaired = btDevice.bondState == BluetoothDevice.BOND_BONDED,
                            isConnected = false
                        )

                        val currentList = discoveredAdapter.currentList.toMutableList()
                        if (currentList.none { d -> d.address == device.address }) {
                            currentList.add(device)
                            discoveredAdapter.submitList(currentList)
                            viewModel.addDiscoveredDevice(device)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isDiscovering.value = false
                    try {
                        unregisterReceiver(this)
                    } catch (e: Exception) {
                        // Receiver not registered
                    }
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

    private fun showDeviceOptionsDialog(device: Device) {
        val options = arrayOf(
            getString(R.string.option_connect),
            getString(R.string.option_edit),
            getString(R.string.option_forget)
        )

        AlertDialog.Builder(this)
            .setTitle(device.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.connectDevice(device)
                    1 -> {
                        val intent = Intent(this, ConnectionActivity::class.java).apply {
                            putExtra("device_address", device.address)
                            putExtra("device_name", device.name)
                        }
                        startActivity(intent)
                    }
                    2 -> confirmForgetDevice(device)
                }
            }
            .show()
    }

    private fun confirmForgetDevice(device: Device) {
        AlertDialog.Builder(this)
            .setTitle(R.string.forget_device)
            .setMessage(R.string.confirm_forget_device)
            .setPositiveButton(R.string.forget) { _, _ ->
                viewModel.forgetDevice(device)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
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
