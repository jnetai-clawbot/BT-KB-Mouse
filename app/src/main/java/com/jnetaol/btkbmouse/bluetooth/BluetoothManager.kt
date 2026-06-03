package com.jnetaol.btkbmouse.bluetooth

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.jnetaol.btkbmouse.logger.DebugLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DiscoveredDevice(
    val device: BluetoothDevice,
    val rssi: Short = 0,
    val name: String = "Unknown"
)

data class ConnectionState(
    val isConnected: Boolean = false,
    val deviceName: String = "",
    val deviceAddress: String = "",
    val isHidRegistered: Boolean = false,
    val isHidConnected: Boolean = false,
    val error: String? = null
)

class BluetoothManager(private val app: Application) {
    private val TAG = "BTManager"

    private var hidService: HidForegroundService? = null
    private var serviceBound = false

    private val btAdapter: BluetoothAdapter? by lazy {
        (app.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
    }

    private val _pairedDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val pairedDevices: StateFlow<List<DiscoveredDevice>> = _pairedDevices.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var scanRunnable: Runnable? = null

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val dev = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                    if (dev != null && dev.name != null) {
                        val cur = _discoveredDevices.value.toMutableList()
                        cur.removeAll { it.device.address == dev.address }
                        cur.add(DiscoveredDevice(dev, rssi, dev.name))
                        _discoveredDevices.value = cur.sortedByDescending { it.rssi }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    if (state == BluetoothDevice.BOND_BONDED || state == BluetoothDevice.BOND_NONE) refreshPairedDevices()
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _isBluetoothEnabled.value = state == BluetoothAdapter.STATE_ON
                }
            }
        }
    }

    private val svcConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            hidService = (binder as HidForegroundService.LocalBinder).getService()
            serviceBound = true
            hidService?.onStateChanged = { reg, err ->
                _connectionState.value = _connectionState.value.copy(isHidRegistered = reg, error = err)
            }
            hidService?.onDeviceConnected = { dev ->
                _connectionState.value = _connectionState.value.copy(
                    isConnected = dev != null, isHidConnected = dev != null,
                    deviceName = dev?.name ?: "", deviceAddress = dev?.address ?: ""
                )
            }
            DebugLogger.i(TAG, "BT-100 HID service bound")
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            hidService = null
            serviceBound = false
        }
    }

    init {
        _isBluetoothEnabled.value = btAdapter?.isEnabled == true
        try {
            app.registerReceiver(scanReceiver, IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            })
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-001 Register receiver failed", e)
        }
        val intent = Intent(app, HidForegroundService::class.java)
        app.startForegroundService(intent)
        app.bindService(intent, svcConnection, Context.BIND_AUTO_CREATE)
    }

    fun enableBluetooth(): Boolean = try { btAdapter?.enable(); true } catch (e: Exception) { false }

    fun refreshPairedDevices() {
        try {
            _pairedDevices.value = btAdapter?.bondedDevices?.map {
                DiscoveredDevice(it, 0, it.name ?: "Unknown")
            } ?: emptyList()
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-003 Refresh failed", e)
        }
    }

    fun startDiscovery(): Boolean {
        return try {
            _isScanning.value = true
            _discoveredDevices.value = emptyList()
            val ok = btAdapter?.startDiscovery() ?: false
            if (!ok) _isScanning.value = false
            else {
                scanRunnable?.let { handler.removeCallbacks(it) }
                scanRunnable = Runnable { stopDiscovery() }
                scanRunnable?.let { handler.postDelayed(it, 30000L) }
            }
            ok
        } catch (e: Exception) { _isScanning.value = false; false }
    }

    fun stopDiscovery() { try { btAdapter?.cancelDiscovery(); _isScanning.value = false } catch (_: Exception) {} }

    @SuppressLint("MissingPermission")
    fun pairDevice(device: BluetoothDevice) {
        try { if (device.bondState == BluetoothDevice.BOND_NONE) device.createBond() } catch (_: Exception) {}
    }

    fun connectDevice(device: BluetoothDevice) { hidService?.retryRegistration() }

    fun disconnect() {
        _connectionState.value = ConnectionState(isHidRegistered = hidService?.let { true } ?: false)
    }

    fun retryHidRegistration() { hidService?.retryRegistration() }

    fun sendMouseReport(buttons: Int, dx: Float, dy: Float, scroll: Float = 0f) {
        hidService?.sendMouseReport(buttons, dx.toInt().coerceIn(-127, 127), dy.toInt().coerceIn(-127, 127), scroll.toInt().coerceIn(-127, 127))
    }

    fun sendKeyboardReport(modifiers: Byte = 0, keys: ByteArray = byteArrayOf()) {
        hidService?.sendKeyboardReport(modifiers, keys)
    }

    fun sendKeyPress(keyCode: Byte, modifiers: Byte = 0) {
        hidService?.sendKeyboardReport(modifiers, byteArrayOf(keyCode))
        handler.postDelayed({ hidService?.sendKeyboardReport(0, byteArrayOf()) }, 60)
    }

    val keyUsageMap = mapOf('a' to 4, 'b' to 5, 'c' to 6, 'd' to 7, 'e' to 8, 'f' to 9, 'g' to 10,
        'h' to 11, 'i' to 12, 'j' to 13, 'k' to 14, 'l' to 15, 'm' to 16, 'n' to 17, 'o' to 18,
        'p' to 19, 'q' to 20, 'r' to 21, 's' to 22, 't' to 23, 'u' to 24, 'v' to 25, 'w' to 26,
        'x' to 27, 'y' to 28, 'z' to 29, '1' to 30, '2' to 31, '3' to 32, '4' to 33, '5' to 34,
        '6' to 35, '7' to 36, '8' to 37, '9' to 38, '0' to 39, '\n' to 40, ' ' to 44,
        '-' to 45, '=' to 46, '[' to 47, ']' to 48, '\\' to 49, ';' to 51, '\'' to 52,
        ',' to 54, '.' to 55, '/' to 56, '\b' to 42, '\t' to 43)

    val shiftMap = mapOf('A' to 4, 'B' to 5, 'C' to 6, 'D' to 7, 'E' to 8, 'F' to 9, 'G' to 10,
        'H' to 11, 'I' to 12, 'J' to 13, 'K' to 14, 'L' to 15, 'M' to 16, 'N' to 17, 'O' to 18,
        'P' to 19, 'Q' to 20, 'R' to 21, 'S' to 22, 'T' to 23, 'U' to 24, 'V' to 25, 'W' to 26,
        'X' to 27, 'Y' to 28, 'Z' to 29, '!' to 30, '@' to 31, '#' to 32, '$' to 33, '%' to 34,
        '^' to 35, '&' to 36, '*' to 37, '(' to 38, ')' to 39, '_' to 45, '+' to 46,
        '{' to 47, '}' to 48, '|' to 49, ':' to 51, '"' to 52, '<' to 54, '>' to 55, '?' to 56)

    fun sendTextString(text: String) {
        var delay = 0L
        for (c in text) {
            val isShift = c in shiftMap
            val usage = if (isShift) shiftMap[c] else keyUsageMap[c]
            if (usage != null) {
                val mod: Byte = if (isShift) 0x02 else 0x00
                handler.postDelayed({ sendKeyPress(usage.toByte(), mod) }, delay)
                delay += if (text.length > 10) 20 else 80
            }
        }
    }

    fun sendMouseLeftClick(press: Boolean) { if (press) hidService?.sendMouseReport(1, 0, 0) else hidService?.sendMouseReport(0, 0, 0) }
    fun sendMouseRightClick(press: Boolean) { if (press) hidService?.sendMouseReport(2, 0, 0) else hidService?.sendMouseReport(0, 0, 0) }
    fun sendMouseMiddleClick(press: Boolean) { if (press) hidService?.sendMouseReport(4, 0, 0) else hidService?.sendMouseReport(0, 0, 0) }
    fun sendMouseWheel(scroll: Float) { hidService?.sendMouseReport(0, 0, 0, scroll.toInt().coerceIn(-127, 127)) }

    fun sendMediaKey(keyName: String) {
        val map = mapOf("play_pause" to 0xcd, "volume_up" to 0xe9, "volume_down" to 0xea,
            "mute" to 0xe2, "next_track" to 0xb6, "prev_track" to 0xb5, "stop" to 0xb7)
        map[keyName]?.let { hidService?.sendMediaReport(it.toByte()) }
    }

    fun cleanup() {
        try {
            handler.removeCallbacksAndMessages(null)
            app.unregisterReceiver(scanReceiver)
            app.unbindService(svcConnection)
        } catch (_: Exception) {}
    }
}
