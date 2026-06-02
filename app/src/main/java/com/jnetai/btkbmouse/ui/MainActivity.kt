package com.jnetai.btkbmouse.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.jnetai.btkbmouse.BTKBMouseApp
import com.jnetai.btkbmouse.R
import com.jnetai.btkbmouse.databinding.ActivityMainBinding
import com.jnetai.btkbmouse.ui.adapter.DeviceAdapter
import com.jnetai.btkbmouse.ui.adapter.DiscoveredDeviceAdapter
import com.jnetai.btkbmouse.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

/**
 * Main Activity for device scanning and management.
 * Uses StateFlow for reactive UI updates.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Create ViewModel with factory to pass repository
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory((application as BTKBMouseApp))
    }

    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var discoveredAdapter: DiscoveredDeviceAdapter

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            viewModel.startScan()
        } else {
            Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
        checkPermissions()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun setupRecyclerViews() {
        deviceAdapter = DeviceAdapter(
            onDeviceClick = { device ->
                val intent = Intent(this, ConnectionActivity::class.java).apply {
                    putExtra(ConnectionActivity.EXTRA_DEVICE_ADDRESS, device.address)
                }
                startActivity(intent)
            },
            onDeviceLongClick = { device ->
                showDeviceOptionsDialog(device)
            }
        )

        discoveredAdapter = DiscoveredDeviceAdapter(
            onDeviceClick = { device ->
                viewModel.pairDeviceByAddress(device.address)
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
                Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe devices
                launch {
                    viewModel.devices.collect { devices ->
                        deviceAdapter.submitList(devices)
                        updateEmptyState(devices.isEmpty())
                    }
                }

                // Observe scanning state
                launch {
                    viewModel.isScanning.collect { isScanning ->
                        updateScanButtonState(isScanning)
                    }
                }

                // Observe discovered devices
                launch {
                    viewModel.discoveredDevices.collect { devices ->
                        discoveredAdapter.submitList(devices)
                        binding.tvNoDevicesFound.visibility = if (devices.isEmpty()) View.VISIBLE else View.GONE
                    }
                }

                // Observe connection states
                launch {
                    viewModel.connectionState.collect { stateMap ->
                        deviceAdapter.updateConnectionStates(stateMap)
                    }
                }

                // Observe toast messages
                launch {
                    viewModel.toastMessage.collect { message ->
                        message?.let {
                            Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
                            viewModel.clearToast()
                        }
                    }
                }

                // Observe errors
                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.cardSavedDevices.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun toggleScan() {
        if (viewModel.isScanning.value) {
            viewModel.stopScan()
        } else {
            viewModel.startScan()
        }
    }

    private fun updateScanButtonState(isScanning: Boolean) {
        if (isScanning) {
            binding.fabScan.setImageResource(R.drawable.ic_stop)
            binding.fabScan.backgroundTintList = ContextCompat.getColorStateList(this, R.color.error)
            binding.progressScan.visibility = View.VISIBLE
            binding.tvScanning.visibility = View.VISIBLE

            ObjectAnimator.ofFloat(binding.fabScan, "rotation", 0f, 360f).apply {
                duration = 1000
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        } else {
            binding.fabScan.setImageResource(R.drawable.ic_bluetooth)
            binding.fabScan.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
            binding.progressScan.visibility = View.GONE
            binding.tvScanning.visibility = View.GONE
            binding.fabScan.rotation = 0f
        }
    }

    private fun hasPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        permissionLauncher.launch(permissions)
    }

    private fun showDeviceOptionsDialog(device: com.jnetai.btkbmouse.data.Device) {
        val options = arrayOf(
            getString(R.string.option_connect),
            getString(R.string.option_edit),
            getString(R.string.option_forget)
        )

        android.app.AlertDialog.Builder(this)
            .setTitle(device.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(this, ConnectionActivity::class.java).apply {
                            putExtra(ConnectionActivity.EXTRA_DEVICE_ADDRESS, device.address)
                        }
                        startActivity(intent)
                    }
                    1 -> {
                        val intent = Intent(this, ConnectionActivity::class.java).apply {
                            putExtra(ConnectionActivity.EXTRA_DEVICE_ADDRESS, device.address)
                        }
                        startActivity(intent)
                    }
                    2 -> showConfirmForgetDialog(device)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showConfirmForgetDialog(device: com.jnetai.btkbmouse.data.Device) {
        android.app.AlertDialog.Builder(this)
            .setTitle(R.string.forget_device)
            .setMessage(getString(R.string.confirm_forget_device, device.name))
            .setPositiveButton(R.string.forget) { _, _ ->
                viewModel.deleteDevice(device)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
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

    override fun onResume() {
        super.onResume()
        viewModel.refreshConnectionState()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (viewModel.isScanning.value) {
            viewModel.stopScan()
        }
    }
}

/**
 * Factory for creating MainViewModel with dependencies
 */
class MainViewModelFactory(private val application: BTKBMouseApp) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(application, com.jnetai.btkbmouse.repository.DeviceRepository(application.database.deviceDao())) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
