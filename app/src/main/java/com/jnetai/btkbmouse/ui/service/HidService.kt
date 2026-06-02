package com.jnetai.btkbmouse.ui.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.jnetai.btkbmouse.R
import com.jnetai.btkbmouse.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground Service for Bluetooth HID keyboard/mouse implementation.
 * Handles input from connected devices and emulates locally.
 */
class HidService : Service() {

    companion object {
        private const val TAG = "HidService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "hid_service_channel"

        const val ACTION_START = "com.jnetai.btkbmouse.action.START_HID"
        const val ACTION_STOP = "com.jnetai.btkbmouse.action.STOP_HID"
        const val EXTRA_DEVICE_ADDRESS = "device_address"

        // HID Report IDs
        private const val HID_REPORT_ID_KEYBOARD = 1
        private const val HID_REPORT_ID_MOUSE = 2
    }

    private val binder = HidBinder()

    // Bluetooth components
    private var bluetoothManager: BluetoothManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothGattServer: BluetoothGattServer? = null

    // StateFlow for connection state
    private val _connectionState = MutableStateFlow(HidConnectionState.DISCONNECTED)
    val connectionState: StateFlow<HidConnectionState> = _connectionState.asStateFlow()

    // StateFlow for connected device
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    // StateFlow for battery level
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()

    // StateFlow for input events
    private val _inputEvent = MutableStateFlow<InputEvent?>(null)
    val inputEvent: StateFlow<InputEvent?> = _inputEvent.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeBluetooth()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (deviceAddress != null) {
                    startForeground(NOTIFICATION_ID, createNotification("Connecting..."))
                    connectDevice(deviceAddress)
                }
            }
            ACTION_STOP -> {
                disconnectDevice()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectDevice()
    }

    private fun initializeBluetooth() {
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "HID Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Bluetooth HID device connection service"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(status: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BT-KB-Mouse")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_devices)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(status: String) {
        val notification = createNotification(status)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Connect to a Bluetooth HID device
     */
    fun connectDevice(deviceAddress: String) {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e(TAG, "Bluetooth is not available")
            _connectionState.value = HidConnectionState.ERROR
            return
        }

        _connectionState.value = HidConnectionState.CONNECTING
        updateNotification("Connecting to device...")

        try {
            val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            if (device != null) {
                _connectedDevice.value = device
                // Note: In production, you would use BluetoothGattCallback
                // For HID devices, you typically use createRfcommSocketToServiceRecord
                // with SPP or connectGatt for GATT-based HID
                connectGatt(device)
            } else {
                _connectionState.value = HidConnectionState.ERROR
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device", e)
            _connectionState.value = HidConnectionState.ERROR
        }
    }

    private fun connectGatt(device: BluetoothDevice) {
        // Note: Real HID implementation requires specific UUIDs and protocols
        // This is a simplified version for demonstration
        bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server")
                    _connectionState.value = HidConnectionState.CONNECTED
                    updateNotification("Connected")

                    // Discover services
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server")
                    _connectionState.value = HidConnectionState.DISCONNECTED
                    _connectedDevice.value = null
                    _batteryLevel.value = null
                    updateNotification("Disconnected")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered")
                // Parse HID service and characteristics
                // Typically: 00001812-0000-1000-8000-00805f9b34fb (HID Service)
                // With report characteristics for keyboard/mouse input
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            // Parse HID report data
            parseHidReport(value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            characteristic.value?.let { parseHidReport(it) }
        }
    }

    /**
     * Disconnect from the current device
     */
    fun disconnectDevice() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = HidConnectionState.DISCONNECTED
        _connectedDevice.value = null
        _batteryLevel.value = null
    }

    /**
     * Parse HID report data from the device
     */
    private fun parseHidReport(data: ByteArray) {
        if (data.isEmpty()) return

        val reportId = data[0]
        when (reportId) {
            HID_REPORT_ID_KEYBOARD -> parseKeyboardReport(data)
            HID_REPORT_ID_MOUSE -> parseMouseReport(data)
        }
    }

    /**
     * Parse keyboard HID report
     */
    private fun parseKeyboardReport(data: ByteArray) {
        // HID Keyboard report format:
        // [reportId, modifiers, reserved, key1, key2, key3, key4, key5, key6]
        if (data.size < 8) return

        val modifiers = data[1]
        val keys = data.sliceArray(3..7)

        val event = KeyboardEvent(
            modifiers = modifiers,
            keys = keys.filter { it != 0.toByte() },
            timestamp = System.currentTimeMillis()
        )

        _inputEvent.value = InputEvent.KeyboardEvent(event)
        sendKeyboardEvent(event)
    }

    /**
     * Parse mouse HID report
     */
    private fun parseMouseReport(data: ByteArray) {
        // HID Mouse report format:
        // [reportId, buttons, X, Y, wheel, pan]
        if (data.size < 5) return

        val buttons = data[1]
        val x = data[2].toInt()
        val y = data[3].toInt()
        val wheel = data[4].toByte()

        val event = MouseEvent(
            buttons = buttons,
            x = x,
            y = y,
            wheel = wheel,
            timestamp = System.currentTimeMillis()
        )

        _inputEvent.value = InputEvent.MouseEvent(event)
        sendMouseEvent(event)
    }

    /**
     * Send keyboard event to emulate input
     */
    fun sendKeyboardEvent(event: KeyboardEvent) {
        // In production, this would use InputManager or AccessibilityService
        // to inject keyboard events into the system
        Log.d(TAG, "Keyboard event: modifiers=${event.modifiers}, keys=${event.keys}")
    }

    /**
     * Send mouse event to emulate input
     */
    fun sendMouseEvent(event: MouseEvent) {
        // In production, this would use InputManager or AccessibilityService
        // to inject mouse events into the system
        Log.d(TAG, "Mouse event: x=${event.x}, y=${event.y}, wheel=${event.wheel}")
    }

    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    /**
     * Get connected device name
     */
    fun getConnectedDeviceName(): String? {
        return _connectedDevice.value?.name
    }

    /**
     * Binder for local service binding
     */
    inner class HidBinder : Binder() {
        fun getService(): HidService = this@HidService
    }

    /**
     * Connection state enum
     */
    enum class HidConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    /**
     * Input event sealed class for keyboard and mouse events
     */
    sealed class InputEvent {
        data class KeyboardEvent(val event: com.jnetai.btkbmouse.ui.service.KeyboardEvent) : InputEvent()
        data class MouseEvent(val event: com.jnetai.btkbmouse.ui.service.MouseEvent) : InputEvent()
    }

    /**
     * Keyboard event data class
     */
    data class KeyboardEvent(
        val modifiers: Byte,
        val keys: List<Byte>,
        val timestamp: Long
    )

    /**
     * Mouse event data class
     */
    data class MouseEvent(
        val buttons: Byte,
        val x: Int,
        val y: Int,
        val wheel: Byte,
        val timestamp: Long
    )
}
