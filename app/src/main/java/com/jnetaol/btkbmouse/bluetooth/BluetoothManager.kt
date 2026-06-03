package com.jnetaol.btkbmouse.bluetooth

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import com.jnetaol.btkbmouse.logger.DebugLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

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
    companion object {
        private const val TAG = "BTManager"
        private const val SDP_RECORD_NAME = "BT KB & Mouse Pro"
        private const val SDP_DESCRIPTION = "Professional Bluetooth Keyboard and Mouse"
        private const val SDP_PROVIDER = "jnetai.com"
        private const val REPORT_ID_KEYBOARD: Byte = 1
        private const val REPORT_ID_MOUSE: Byte = 2
        private const val REPORT_ID_CONSUMER: Byte = 3
    }

    private val btAdapter: BluetoothAdapter? by lazy {
        (app.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
    }

    private var hidDeviceProxy: BluetoothHidDevice? = null
    private var connectedHidDevice: BluetoothDevice? = null
    private var isHidAppRegistered = false
    private var isHidServiceBound = false
    private var registerAttempt = 0
    private val hidExecutor = Executors.newSingleThreadExecutor()

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
    private var registerRunnable: Runnable? = null

    private val keyboardReportDescriptor = byteArrayOf(
        0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x06.toByte(),
        0xa1.toByte(), 0x01.toByte(), 0x85.toByte(), REPORT_ID_KEYBOARD,
        0x05.toByte(), 0x07.toByte(), 0x19.toByte(), 0xe0.toByte(), 0x29.toByte(), 0xe7.toByte(),
        0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x01.toByte(), 0x95.toByte(), 0x08.toByte(),
        0x81.toByte(), 0x02.toByte(),
        0x95.toByte(), 0x01.toByte(), 0x75.toByte(), 0x08.toByte(),
        0x81.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x06.toByte(), 0x75.toByte(), 0x08.toByte(),
        0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x65.toByte(),
        0x05.toByte(), 0x07.toByte(), 0x19.toByte(), 0x00.toByte(), 0x29.toByte(), 0x65.toByte(),
        0x81.toByte(), 0x00.toByte(),
        0xc0.toByte()
    )

    private val mouseReportDescriptor = byteArrayOf(
        0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x02.toByte(),
        0xa1.toByte(), 0x01.toByte(), 0x85.toByte(), REPORT_ID_MOUSE,
        0x09.toByte(), 0x01.toByte(), 0xa1.toByte(), 0x00.toByte(),
        0x05.toByte(), 0x09.toByte(), 0x19.toByte(), 0x01.toByte(), 0x29.toByte(), 0x03.toByte(),
        0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x01.toByte(),
        0x95.toByte(), 0x03.toByte(), 0x75.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x02.toByte(),
        0x95.toByte(), 0x01.toByte(), 0x75.toByte(), 0x05.toByte(),
        0x81.toByte(), 0x01.toByte(),
        0x05.toByte(), 0x01.toByte(), 0x09.toByte(), 0x30.toByte(),
        0x09.toByte(), 0x31.toByte(), 0x09.toByte(), 0x38.toByte(),
        0x15.toByte(), 0x81.toByte(), 0x25.toByte(), 0x7f.toByte(),
        0x75.toByte(), 0x08.toByte(), 0x95.toByte(), 0x03.toByte(),
        0x81.toByte(), 0x06.toByte(),
        0xc0.toByte(), 0xc0.toByte()
    )

    private val consumerReportDescriptor = byteArrayOf(
        0x05.toByte(), 0x0c.toByte(), 0x09.toByte(), 0x01.toByte(),
        0xa1.toByte(), 0x01.toByte(), 0x85.toByte(), REPORT_ID_CONSUMER,
        0x15.toByte(), 0x00.toByte(), 0x25.toByte(), 0x01.toByte(),
        0x75.toByte(), 0x01.toByte(), 0x95.toByte(), 0x07.toByte(),
        0x0a.toByte(), 0xcd.toByte(), 0x00.toByte(),
        0x0a.toByte(), 0xe9.toByte(), 0x00.toByte(),
        0x0a.toByte(), 0xea.toByte(), 0x00.toByte(),
        0x0a.toByte(), 0xe2.toByte(), 0x00.toByte(),
        0x0a.toByte(), 0xb6.toByte(), 0x00.toByte(),
        0x0a.toByte(), 0xb5.toByte(), 0x00.toByte(),
        0x0a.toByte(), 0xb7.toByte(), 0x00.toByte(),
        0x81.toByte(), 0x02.toByte(),
        0xc0.toByte()
    )

    private val keyUsageMap = linkedMapOf(
        'a' to 4, 'b' to 5, 'c' to 6, 'd' to 7, 'e' to 8, 'f' to 9,
        'g' to 10, 'h' to 11, 'i' to 12, 'j' to 13, 'k' to 14, 'l' to 15,
        'm' to 16, 'n' to 17, 'o' to 18, 'p' to 19, 'q' to 20, 'r' to 21,
        's' to 22, 't' to 23, 'u' to 24, 'v' to 25, 'w' to 26, 'x' to 27,
        'y' to 28, 'z' to 29,
        '1' to 30, '2' to 31, '3' to 32, '4' to 33, '5' to 34,
        '6' to 35, '7' to 36, '8' to 37, '9' to 38, '0' to 39,
        '\n' to 40, ' ' to 44, '-' to 45, '=' to 46, '[' to 47,
        ']' to 48, '\\' to 49, ';' to 51, '\'' to 52, ',' to 54,
        '.' to 55, '/' to 56, '\b' to 42, '\t' to 43
    )

    private val shiftMap = linkedMapOf(
        'A' to 4, 'B' to 5, 'C' to 6, 'D' to 7, 'E' to 8, 'F' to 9,
        'G' to 10, 'H' to 11, 'I' to 12, 'J' to 13, 'K' to 14, 'L' to 15,
        'M' to 16, 'N' to 17, 'O' to 18, 'P' to 19, 'Q' to 20, 'R' to 21,
        'S' to 22, 'T' to 23, 'U' to 24, 'V' to 25, 'W' to 26, 'X' to 27,
        'Y' to 28, 'Z' to 29,
        '!' to 30, '@' to 31, '#' to 32, '$' to 33, '%' to 34,
        '^' to 35, '&' to 36, '*' to 37, '(' to 38, ')' to 39,
        '_' to 45, '+' to 46, '{' to 47, '}' to 48, '|' to 49,
        ':' to 51, '"' to 52, '<' to 54, '>' to 55, '?' to 56
    )

    private val mediaKeyMap = mapOf(
        "play_pause" to 0xcd.toByte(),
        "volume_up" to 0xe9.toByte(),
        "volume_down" to 0xea.toByte(),
        "mute" to 0xe2.toByte(),
        "next_track" to 0xb6.toByte(),
        "prev_track" to 0xb5.toByte(),
        "stop" to 0xb7.toByte()
    )

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            DebugLogger.i(TAG, "BT-060 HID app status: plugged=${pluggedDevice?.name}, registered=$registered")
            if (registered) {
                isHidAppRegistered = true
                _connectionState.value = _connectionState.value.copy(isHidRegistered = true, error = null)
                if (pluggedDevice != null) {
                    connectedHidDevice = pluggedDevice
                    _connectionState.value = _connectionState.value.copy(
                        isConnected = true, isHidConnected = true,
                        deviceName = pluggedDevice.name ?: "Unknown",
                        deviceAddress = pluggedDevice.address
                    )
                    DebugLogger.i(TAG, "BT-061 HID host connected: ${pluggedDevice.name}")
                } else {
                    DebugLogger.i(TAG, "BT-082 SDP registered, waiting for host connection")
                }
            } else {
                isHidAppRegistered = false
                connectedHidDevice = null
                _connectionState.value = _connectionState.value.copy(
                    isHidRegistered = false, isHidConnected = false, isConnected = false
                )
                DebugLogger.w(TAG, "BT-069 HID unregistered, scheduling re-register")
                scheduleRegister()
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            DebugLogger.i(TAG, "BT-062 HID conn state: $state for ${device?.name}")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHidDevice = device
                    _connectionState.value = _connectionState.value.copy(
                        isConnected = true, isHidConnected = true,
                        deviceName = device?.name ?: "Unknown",
                        deviceAddress = device?.address ?: "", error = null
                    )
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (device == connectedHidDevice) {
                        connectedHidDevice = null
                        _connectionState.value = _connectionState.value.copy(
                            isConnected = false, isHidConnected = false
                        )
                    }
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            DebugLogger.d(TAG, "BT-063 GetReport: type=$type id=$id buf=$bufferSize")
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            DebugLogger.d(TAG, "BT-064 SetReport: type=$type id=$id")
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            DebugLogger.d(TAG, "BT-065 Interrupt data from host: rpt=$reportId len=${data?.size}")
            if (reportId == REPORT_ID_KEYBOARD && data != null && data.size > 0) {
                val ledState = data[0]
                DebugLogger.d(TAG, "BT-070 KB LED report: 0x${String.format("%02X", ledState)}")
            }
        }
    }

    private val scanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                    if (device != null && device.name != null) {
                        val entry = DiscoveredDevice(device, rssi, device.name ?: "Unknown")
                        val current = _discoveredDevices.value.toMutableList()
                        current.removeAll { it.device.address == device.address }
                        current.add(entry)
                        _discoveredDevices.value = current.sortedByDescending { it.rssi }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    if (state == BluetoothDevice.BOND_BONDED || state == BluetoothDevice.BOND_NONE) {
                        refreshPairedDevices()
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _isBluetoothEnabled.value = state == BluetoothAdapter.STATE_ON
                    if (state == BluetoothAdapter.STATE_ON) {
                        handler.postDelayed({ forceReinit() }, 2000L)
                    }
                }
            }
        }
    }

    init {
        _isBluetoothEnabled.value = btAdapter?.isEnabled == true
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            app.registerReceiver(scanReceiver, filter)
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-001 Register receiver failed", e)
        }
        if (_isBluetoothEnabled.value) {
            handler.postDelayed({ forceReinit() }, 2000L)
        }
    }

    fun forceReinit() {
        DebugLogger.i(TAG, "BT-099 Force reinitialize HID")
        registerAttempt = 0
        isHidServiceBound = false
        isHidAppRegistered = false
        hidDeviceProxy = null
        connectedHidDevice = null
        bindToHidProfile()
    }

    private fun bindToHidProfile() {
        try {
            val adapter = btAdapter ?: run {
                DebugLogger.e(TAG, "BT-052b No adapter")
                return
            }
            if (!adapter.isEnabled) {
                DebugLogger.w(TAG, "BT-052c BT not enabled")
                return
            }
            DebugLogger.i(TAG, "BT-050 Binding to HID_DEVICE profile")
            adapter.getProfileProxy(app, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    DebugLogger.i(TAG, "BT-050b onServiceConnected: profile=$profile proxyType=${proxy?.javaClass?.name}")
                    if (proxy is BluetoothHidDevice) {
                        hidDeviceProxy = proxy
                        isHidServiceBound = true
                        DebugLogger.i(TAG, "BT-065 HID proxy bound")
                        handler.post { doRegister() }
                    } else {
                        DebugLogger.w(TAG, "BT-051 Not HID_DEVICE proxy, got: ${proxy?.javaClass?.name}")
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    DebugLogger.w(TAG, "BT-052 HID proxy disconnected, will rebind")
                    isHidServiceBound = false
                    hidDeviceProxy = null
                    isHidAppRegistered = false
                    handler.postDelayed({ forceReinit() }, 2000L)
                }
            }, BluetoothProfile.HID_DEVICE)
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-053 Bind HID failed", e)
            scheduleRegister()
        }
    }

    private fun doRegister() {
        try {
            val proxy = hidDeviceProxy
            if (proxy == null) {
                DebugLogger.w(TAG, "BT-054 No proxy for register, rebinding")
                bindToHidProfile()
                return
            }

            val adapter = btAdapter
            if (adapter == null || !adapter.isEnabled) {
                DebugLogger.w(TAG, "BT-054b BT not ready for register")
                scheduleRegister()
                return
            }

            if (isHidAppRegistered) {
                DebugLogger.i(TAG, "BT-055 Unregistering previous HID app")
                try { proxy.unregisterApp() } catch (_: Exception) {}
                isHidAppRegistered = false
            }

            val combinedDescriptor = keyboardReportDescriptor + mouseReportDescriptor + consumerReportDescriptor
            val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                SDP_RECORD_NAME, SDP_DESCRIPTION, SDP_PROVIDER,
                BluetoothHidDevice.SUBCLASS1_COMBO, combinedDescriptor
            )
            val qosIn = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800, 9, 0, 1000, 1000
            )
            val qosOut = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800, 9, 0, 1000, 1000
            )

            registerAttempt++
            val registered = proxy.registerApp(sdpSettings, qosIn, qosOut, hidExecutor, hidCallback)
            DebugLogger.i(TAG, "BT-066 registerApp result: $registered (attempt $registerAttempt)")

            if (registered) {
                isHidAppRegistered = true
                registerAttempt = 0
                _connectionState.value = _connectionState.value.copy(isHidRegistered = true, error = null)
                DebugLogger.i(TAG, "BT-082 HID SDP published successfully!")
            } else {
                isHidAppRegistered = false
                DebugLogger.w(TAG, "BT-056 registerApp returned false, will retry")
                scheduleRegister()
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-008 HID register exception", e)
            _connectionState.value = _connectionState.value.copy(error = "HID error: ${e.message}")
            scheduleRegister()
        }
    }

    private fun scheduleRegister() {
        registerRunnable?.let { handler.removeCallbacks(it) }
        val delay = when {
            registerAttempt <= 3 -> 2000L
            registerAttempt <= 6 -> 10000L
            else -> 30000L
        }
        DebugLogger.i(TAG, "BT-080 Scheduling register attempt ${registerAttempt + 1} in ${delay}ms")
        registerRunnable = Runnable { doRegister() }
        handler.postDelayed(registerRunnable, delay)
    }

    fun retryHidRegistration() {
        DebugLogger.i(TAG, "BT-068 Manual HID retry")
        registerAttempt = 0
        if (isHidAppRegistered && hidDeviceProxy != null) {
            try { hidDeviceProxy?.unregisterApp() } catch (_: Exception) {}
            isHidAppRegistered = false
        }
        forceReinit()
    }

    fun enableBluetooth(): Boolean {
        return try {
            btAdapter?.enable()
            true
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-002 Enable BT failed", e)
            false
        }
    }

    fun connectDevice(device: BluetoothDevice) {
        DebugLogger.i(TAG, "BT-050 Device selected: ${device.name}")
        if (!isHidServiceBound) {
            forceReinit()
        } else if (!isHidAppRegistered) {
            doRegister()
        }
    }

    fun disconnect() {
        try {
            connectedHidDevice = null
            _connectionState.value = ConnectionState(isHidRegistered = isHidAppRegistered)
            DebugLogger.i(TAG, "BT-070 Disconnected")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-009 Disconnect error", e)
        }
    }

    fun refreshPairedDevices() {
        try {
            val devices = btAdapter?.bondedDevices?.map { device ->
                DiscoveredDevice(device, 0, device.name ?: "Unknown")
            } ?: emptyList()
            _pairedDevices.value = devices
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-003 Refresh paired failed", e)
        }
    }

    fun startDiscovery(): Boolean {
        return try {
            _isScanning.value = true
            _discoveredDevices.value = emptyList()
            val started = btAdapter?.startDiscovery() ?: false
            if (!started) _isScanning.value = false
            else {
                scanRunnable?.let { handler.removeCallbacks(it) }
                scanRunnable = Runnable { stopDiscovery() }
                scanRunnable?.let { handler.postDelayed(it, 30000L) }
            }
            started
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-004 Discovery error", e)
            _isScanning.value = false
            false
        }
    }

    fun stopDiscovery() {
        try {
            btAdapter?.cancelDiscovery()
            _isScanning.value = false
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-005 Stop discovery error", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun pairDevice(device: BluetoothDevice) {
        try {
            if (device.bondState == BluetoothDevice.BOND_NONE) {
                device.createBond()
                DebugLogger.i(TAG, "BT-040 Pairing with ${device.name}")
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-006 Pair error", e)
        }
    }

    fun sendMouseReport(buttons: Int, dx: Float, dy: Float, scroll: Float = 0f) {
        try {
            val hid = hidDeviceProxy ?: return
            val device = connectedHidDevice ?: return
            val x = dx.toInt().coerceIn(-127, 127)
            val y = dy.toInt().coerceIn(-127, 127)
            val w = scroll.toInt().coerceIn(-127, 127)
            hid.sendReport(device, REPORT_ID_MOUSE.toInt(), byteArrayOf(buttons.toByte(), x.toByte(), y.toByte(), w.toByte()))
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-019 Mouse report error", e)
        }
    }

    fun sendKeyboardReport(modifiers: Byte = 0, keys: ByteArray = byteArrayOf()) {
        try {
            val hid = hidDeviceProxy ?: return
            val device = connectedHidDevice ?: return
            val report = ByteArray(8)
            report[0] = modifiers
            for (i in keys.indices) {
                if (i >= 6) break
                report[2 + i] = keys[i]
            }
            hid.sendReport(device, REPORT_ID_KEYBOARD.toInt(), report)
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-020 Keyboard report error", e)
        }
    }

    fun sendKeyPress(keyCode: Byte, modifiers: Byte = 0) {
        sendKeyboardReport(modifiers, byteArrayOf(keyCode))
        handler.postDelayed({ sendKeyboardReport() }, 60)
    }

    fun sendTextString(text: String, delayMultiplier: Float = 1.0f) {
        DebugLogger.i(TAG, "BT-083 Sending: ${text.take(40)}")
        var delay = 0L
        val baseDelay = (80 * delayMultiplier).toLong().coerceIn(10, 500)
        for (char in text) {
            val isShift = char in shiftMap
            val usage = if (isShift) shiftMap[char] else keyUsageMap[char]
            if (usage != null) {
                val mod: Byte = if (isShift) 0x02 else 0x00
                handler.postDelayed({ sendKeyPress(usage.toByte(), mod) }, delay)
                delay += if (text.length > 10) 20 else baseDelay
            }
        }
    }

    fun sendMouseLeftClick(press: Boolean) {
        val buttons = if (press) 1 else 0
        try {
            val hid = hidDeviceProxy ?: return
            val device = connectedHidDevice ?: return
            hid.sendReport(device, REPORT_ID_MOUSE.toInt(), byteArrayOf(buttons.toByte(), 0, 0, 0))
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-021 Click error", e)
        }
    }

    fun sendMouseRightClick(press: Boolean) {
        val buttons = if (press) 2 else 0
        try {
            val hid = hidDeviceProxy ?: return
            val device = connectedHidDevice ?: return
            hid.sendReport(device, REPORT_ID_MOUSE.toInt(), byteArrayOf(buttons.toByte(), 0, 0, 0))
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-022 Right click error", e)
        }
    }

    fun sendMouseMiddleClick(press: Boolean) {
        val buttons = if (press) 4 else 0
        try {
            val hid = hidDeviceProxy ?: return
            val device = connectedHidDevice ?: return
            hid.sendReport(device, REPORT_ID_MOUSE.toInt(), byteArrayOf(buttons.toByte(), 0, 0, 0))
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-025 Middle click error", e)
        }
    }

    fun sendMouseWheel(scroll: Float) {
        try {
            val hid = hidDeviceProxy ?: return
            val device = connectedHidDevice ?: return
            val w = scroll.toInt().coerceIn(-127, 127)
            hid.sendReport(device, REPORT_ID_MOUSE.toInt(), byteArrayOf(0, 0, 0, w.toByte()))
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-023 Wheel error", e)
        }
    }

    fun sendMediaKey(keyName: String) {
        try {
            val hid = hidDeviceProxy ?: return
            val device = connectedHidDevice ?: return
            val code = mediaKeyMap[keyName] ?: return
            hid.sendReport(device, REPORT_ID_CONSUMER.toInt(), byteArrayOf(code))
            handler.postDelayed({
                try { hid.sendReport(device, REPORT_ID_CONSUMER.toInt(), byteArrayOf(0)) } catch (_: Exception) {}
            }, 100)
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-027 Media key error", e)
        }
    }

    fun cleanup() {
        try {
            handler.removeCallbacksAndMessages(null)
            app.unregisterReceiver(scanReceiver)
            if (isHidAppRegistered && hidDeviceProxy != null) {
                try { hidDeviceProxy?.unregisterApp() } catch (_: Exception) {}
                isHidAppRegistered = false
            }
            hidDeviceProxy = null
            connectedHidDevice = null
            isHidServiceBound = false
            registerAttempt = 0
            DebugLogger.i(TAG, "BT-090 Cleanup complete")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BT-010 Cleanup error", e)
        }
    }
}
