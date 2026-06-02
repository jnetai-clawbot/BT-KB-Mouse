package com.jnetai.btkbmouse.ui.service

import android.app.Service
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.MutableLiveData
import com.jnetai.btkbmouse.R
import com.jnetai.btkbmouse.data.Device
import com.jnetai.btkbmouse.data.DeviceType

class HidService : Service() {

    private val binder = LocalBinder()
    private var bluetoothGatt: BluetoothGatt? = null

    private val _connectionState = MutableLiveData<HidConnectionState>()
    val connectionState: MutableLiveData<HidConnectionState> = _connectionState

    private val _connectedDevice = MutableLiveData<Device?>()
    val connectedDevice: MutableLiveData<Device?> = _connectedDevice

    private val _inputEvent = MutableLiveData<InputEvent?>()
    val inputEvent: MutableLiveData<InputEvent?> = _inputEvent

    inner class LocalBinder : Binder() {
        fun getService(): HidService = this@HidService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        _connectionState.postValue(HidConnectionState.DISCONNECTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        return START_STICKY
    }

    private fun createNotification(): android.app.Notification {
        val channel = android.app.NotificationChannel(
            "hid_service", "BT KB Mouse Service",
            android.app.NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return android.app.Notification.Builder(this, "hid_service")
            .setContentTitle("BT KB Mouse")
            .setContentText("Running in background")
            .setSmallIcon(R.drawable.ic_mouse)
            .build()
    }

    fun connectDevice(device: Device) {
        try {
            val bluetoothDevice = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
                ?.getRemoteDevice(device.address)

            bluetoothDevice?.let { btDevice ->
                _connectionState.postValue(HidConnectionState.CONNECTING)
                bluetoothGatt = btDevice.connectGatt(this, false, gattCallback)
            } ?: run {
                _connectionState.postValue(HidConnectionState.ERROR)
            }
        } catch (e: SecurityException) {
            _connectionState.postValue(HidConnectionState.ERROR)
        }
    }

    fun disconnectDevice() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.postValue(HidConnectionState.DISCONNECTED)
        _connectedDevice.postValue(null)
    }

    fun sendMouseEvent(buttons: Int, dx: Float, dy: Float, wheel: Int) {
        val report = ByteArray(4)
        report[0] = buttons.toByte()
        report[1] = dx.toInt().toByte()
        report[2] = dy.toInt().toByte()
        report[3] = wheel.toByte()
        writeHidReport(report)
    }

    fun sendKeyboardEvent(keyCode: Int, modifiers: Int) {
        val report = ByteArray(8)
        report[0] = modifiers.toByte()
        report[2] = keyCode.toByte()
        writeHidReport(report)
    }

    private fun writeHidReport(report: ByteArray) {
        val service = bluetoothGatt?.getService(android.bluetooth.BluetoothUuid.HidFromIntents.uuid)
        val characteristic = service?.getCharacteristic(android.bluetooth.BluetoothUuid.HidReport.uuid)

        characteristic?.let {
            it.value = report
            bluetoothGatt?.writeCharacteristic(it)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.postValue(HidConnectionState.CONNECTED)
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.postValue(HidConnectionState.DISCONNECTED)
                    _connectedDevice.postValue(null)
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val device = gatt.device
                _connectedDevice.postValue(Device(
                    name = device.name ?: "Unknown",
                    address = device.address,
                    type = DeviceType.UNKNOWN,
                    isPaired = true,
                    isConnected = true
                ))
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            characteristic.value?.let { parseHidReport(it) }
        }
    }

    private fun parseHidReport(data: ByteArray) {
        if (data.isEmpty()) return

        when (data[0].toInt() and 0x01) {
            0 -> {
                if (data.size >= 3) {
                    _inputEvent.postValue(InputEvent.MouseData(
                        buttons = data[0].toInt(),
                        dx = data[1].toInt().toFloat(),
                        dy = data[2].toInt().toFloat()
                    ))
                }
            }
            1 -> {
                if (data.size >= 2) {
                    _inputEvent.postValue(InputEvent.KeyboardData(
                        modifiers = data[0].toInt(),
                        keyCode = if (data.size > 2) data[2].toInt() else 0
                    ))
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    sealed class HidConnectionState {
        object DISCONNECTED : HidConnectionState()
        object CONNECTING : HidConnectionState()
        object CONNECTED : HidConnectionState()
        object ERROR : HidConnectionState()
    }

    sealed class InputEvent {
        data class MouseData(val buttons: Int, val dx: Float, val dy: Float) : InputEvent()
        data class KeyboardData(val modifiers: Int, val keyCode: Int) : InputEvent()
    }
}
