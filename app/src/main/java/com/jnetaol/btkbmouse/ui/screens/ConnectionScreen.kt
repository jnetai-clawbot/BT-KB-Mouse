package com.jnetaol.btkbmouse.ui.screens

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jnetaol.btkbmouse.logger.DebugLogger
import com.jnetaol.btkbmouse.ui.AppViewModel
import com.jnetaol.btkbmouse.ui.components.*
import com.jnetaol.btkbmouse.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val btManager = viewModel.btManager
    val connectionState by btManager.connectionState.collectAsState()
    val pairedDevices by btManager.pairedDevices.collectAsState()
    val discoveredDevices by btManager.discoveredDevices.collectAsState()
    val isBluetoothEnabled by btManager.isBluetoothEnabled.collectAsState()
    val isScanning by btManager.isScanning.collectAsState()

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        DebugLogger.i("ConnectionScreen", "BK-100 Permissions granted: $allGranted")
        if (allGranted && isBluetoothEnabled) {
            btManager.refreshPairedDevices()
            btManager.startDiscovery()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissions)
        if (isBluetoothEnabled) {
            btManager.refreshPairedDevices()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connection", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground, titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ConnectionStatusBadge(
                    isConnected = connectionState.isConnected,
                    deviceName = connectionState.deviceName,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { ErrorDisplay(connectionState.error) }

            item {
                if (!isBluetoothEnabled) {
                    GlowButton(
                        text = "Enable Bluetooth",
                        icon = Icons.Default.Bluetooth,
                        onClick = { btManager.enableBluetooth() },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (connectionState.isConnected) {
                    GlowButton(
                        text = "Disconnect",
                        icon = Icons.Default.Bluetooth,
                        onClick = { btManager.disconnect() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (connectionState.isConnected) {
                item {
                    NeonCard {
                        Column(Modifier.padding(16.dp)) {
                            Text("Connected Device", style = MaterialTheme.typography.titleMedium, color = SuccessGreen)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = connectionState.deviceName.ifBlank { "Unknown" },
                                style = MaterialTheme.typography.bodyLarge, color = TextPrimary
                            )
                            Text(
                                text = connectionState.deviceAddress,
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary
                            )
                            Spacer(Modifier.height(8.dp))
                            val hidLabel = when {
                                connectionState.isHidConnected -> "HID Connected & Ready"
                                connectionState.isHidRegistered -> "HID Registered (Waiting for host...)"
                                else -> "HID Not Registered"
                            }
                            val hidColor = when {
                                connectionState.isHidConnected -> SuccessGreen
                                connectionState.isHidRegistered -> NeonBlue
                                else -> WarningOrange
                            }
                            Text(text = hidLabel, style = MaterialTheme.typography.bodySmall, color = hidColor)
                            if (!connectionState.isHidConnected) {
                                Spacer(Modifier.height(8.dp))
                                GlowButton(
                                    text = "Retry HID Registration",
                                    icon = Icons.Default.Refresh,
                                    onClick = { btManager.retryHidRegistration() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            if (isBluetoothEnabled) {
                item { SectionHeader("Paired Devices") }

                if (pairedDevices.isEmpty()) {
                    item {
                        Text(
                            "No paired devices",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                items(pairedDevices) { device ->
                    DeviceCard(
                        device = device,
                        isConnected = connectionState.isConnected && connectionState.deviceAddress == device.device.address,
                        onConnect = { btManager.connectDevice(device.device) },
                        onPair = { btManager.pairDevice(device.device) }
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader("Nearby Devices")
                        if (isScanning) {
                            Text("Scanning...", style = MaterialTheme.typography.bodySmall, color = NeonBlue)
                        }
                    }
                }

                item {
                    if (isScanning) {
                        GlowButton(
                            text = "Stop Scan",
                            icon = Icons.Default.Stop,
                            onClick = { btManager.stopDiscovery() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        GlowButton(
                            text = "Scan for Devices",
                            icon = Icons.Default.Search,
                            onClick = {
                                permissionLauncher.launch(permissions)
                                btManager.startDiscovery()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (discoveredDevices.isEmpty() && !isScanning) {
                    item {
                        Text(
                            "Tap Scan to discover nearby devices",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        )
                    }
                }

                items(discoveredDevices) { device ->
                    DeviceCard(
                        device = device,
                        isConnected = connectionState.isConnected && connectionState.deviceAddress == device.device.address,
                        showRssi = true,
                        onConnect = { btManager.connectDevice(device.device) },
                        onPair = {
                            btManager.stopDiscovery()
                            btManager.pairDevice(device.device)
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun DeviceCard(
    device: com.jnetaol.btkbmouse.bluetooth.DiscoveredDevice,
    isConnected: Boolean = false,
    showRssi: Boolean = false,
    onConnect: () -> Unit,
    onPair: () -> Unit
) {
    NeonCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) SuccessGreen.copy(alpha = 0.2f) else NeonBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (isConnected) SuccessGreen else NeonBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isConnected) SuccessGreen else TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    device.device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (showRssi) {
                    Text("RSSI: ${device.rssi} dBm", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                }
            }
            if (isConnected) {
                ConnectionStatusBadge(isConnected = true, deviceName = "Connected")
            } else {
                Row {
                    if (device.device.bondState == BluetoothDevice.BOND_NONE) {
                        TextButton(onClick = onPair) {
                            Text("Pair", color = NeonBlue)
                        }
                    }
                    TextButton(onClick = onConnect) {
                        Text("Connect", color = SuccessGreen)
                    }
                }
            }
        }
    }
}
