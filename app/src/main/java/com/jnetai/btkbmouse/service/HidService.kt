package com.jnetai.btkbmouse.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jnetai.btkbmouse.BTKBMouseApp
import com.jnetai.btkbmouse.R
import com.jnetai.btkbmouse.ui.MainActivity
import java.util.UUID

class HidService : Service() {

    private val binder = LocalBinder()
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedDevice: BluetoothDevice? = null

    private val _connectionState = MutableLiveData<Int>()
    val connectionState: LiveData<Int> = _connectionState

    private val _batteryLevel = MutableLiveData<Int?>()
    val batteryLevel: LiveData<Int?> = _batteryLevel

    private val bluetoothManager: BluetoothManager? by lazy {
        getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }

    inner class LocalBinder : Binder() {
        fun getService(): HidService = this@HidService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (deviceAddress != null) {
                    connect(deviceAddress)
                }
            }
            ACTION_DISCONNECT -> {
                val deviceAddress = intent.getStringExtra(EXTRA_DEVICE_ADDRESS)
                if (deviceAddress != null) {
                    disconnect(deviceAddress)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun connect(deviceAddress: String) {
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        if (device == null) {
            Log.e(TAG, "Device not found: $deviceAddress")
            _connectionState.value = STATE_DISCONNECTED
            return
        }

        connectedDevice = device
        _connectionState.value = STATE_CONNECTING

        startForeground(NOTIFICATION_ID, createNotification("Connecting..."))

        device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private fun disconnect(deviceAddress: String) {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        connectedDevice = null
        _connectionState.value = STATE_DISCONNECTED
        _batteryLevel.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    _connectionState.postValue(STATE_CONNECTED)
                    updateNotification("Connected: ${connectedDevice?.name ?: "Device"}")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    _connectionState.postValue(STATE_DISCONNECTED)
                    _batteryLevel.postValue(null)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
                // Enable battery service notifications if available
                enableBatteryNotifications(gatt)
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (characteristic.uuid) {
                    BATTERY_LEVEL_UUID -> {
                        val batteryLevel = characteristic.getIntValue(
                            BluetoothGattCharacteristic.FORMAT_UINT8, 1
                        )
                        _batteryLevel.postValue(batteryLevel)
                        Log.d(TAG, "Battery level: $batteryLevel%")
                    }
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            when (characteristic.uuid) {
                BATTERY_LEVEL_UUID -> {
                    val batteryLevel = characteristic.getIntValue(
                        BluetoothGattCharacteristic.FORMAT_UINT8, 1
                    )
                    _batteryLevel.postValue(batteryLevel)
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Log.d(TAG, "Descriptor write status: $status")
        }
    }

    private fun enableBatteryNotifications(gatt: BluetoothGatt) {
        val batteryService = gatt.getService(BATTERY_SERVICE_UUID)
        if (batteryService != null) {
            val batteryLevel = batteryService.getCharacteristic(BATTERY_LEVEL_UUID)
            if (batteryLevel != null) {
                gatt.setCharacteristicNotification(batteryLevel, true)
                val descriptor = batteryLevel.getDescriptor(CLIENT_CONFIG_UUID)
                if (descriptor != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        @Suppress("DEPRECATION")
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        gatt.writeDescriptor(descriptor)
                    }
                }
                // Read initial battery level
                gatt.readCharacteristic(batteryLevel)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "HID Connection",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Bluetooth HID device connection status"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BT-KB-Mouse")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    companion object {
        private const val TAG = "HidService"

        const val ACTION_CONNECT = "com.jnetai.btkbmouse.CONNECT"
        const val ACTION_DISCONNECT = "com.jnetai.btkbmouse.DISCONNECT"
        const val EXTRA_DEVICE_ADDRESS = "device_address"

        const val STATE_DISCONNECTED = 0
        const val STATE_CONNECTING = 1
        const val STATE_CONNECTED = 2

        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "bt_hid_service_channel"

        // Standard Bluetooth UUIDs
        private val BATTERY_SERVICE_UUID = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB")
        private val BATTERY_LEVEL_UUID = UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB")
        private val CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")
    }
}
