package com.jnetaol.btkbmouse.bluetooth

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.jnetaol.btkbmouse.MainActivity
import com.jnetaol.btkbmouse.logger.DebugLogger
import java.util.concurrent.Executors

class HidForegroundService : Service() {
    companion object {
        const val TAG = "HidService"
        const val CHANNEL_ID = "bt_hid_service"
        const val NOTIFICATION_ID = 1001
        const val SDP_RECORD_NAME = "BT KB & Mouse Pro"
        const val SDP_DESCRIPTION = "Professional Bluetooth Keyboard and Mouse"
        const val SDP_PROVIDER = "jnetai.com"
        const val REPORT_ID_KEYBOARD: Byte = 1
        const val REPORT_ID_MOUSE: Byte = 2
        const val REPORT_ID_CONSUMER: Byte = 3
        const val DISCOVERABLE_DURATION = 0
    }

    private val binder = LocalBinder()
    private val handler = Handler(Looper.getMainLooper())
    private val hidExecutor = Executors.newSingleThreadExecutor()
    private val discoverableExecutor = Executors.newSingleThreadExecutor()

    private var btAdapter: BluetoothAdapter? = null
    private var hidDeviceProxy: BluetoothHidDevice? = null
    var connectedHidDevice: BluetoothDevice? = null
        private set
    private var isRegistered = false
    private var isBound = false
    private var attempt = 0
    private var discoveryKeepAlive = false

    var onStateChanged: ((Boolean, String?) -> Unit)? = null
    var onDeviceConnected: ((BluetoothDevice?) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): HidForegroundService = this@HidForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private val btStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON) {
                        DebugLogger.i(TAG, "BK-SVC BT turned ON, force reinit")
                        handler.postDelayed({ forceReinit() }, 2000L)
                    } else if (state == BluetoothAdapter.STATE_OFF) {
                        discoveryKeepAlive = false
                    }
                }
                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                    val scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR)
                    DebugLogger.i(TAG, "BK-SVC Scan mode changed: $scanMode")
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    DebugLogger.i(TAG, "BK-SVC Bond state changed: $bondState")
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        handler.postDelayed({ forceReinit() }, 1500L)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        btAdapter = (getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
        createNotificationChannel()
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        registerReceiver(btStateReceiver, filter)
        DebugLogger.i(TAG, "BK-SVC Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        DebugLogger.i(TAG, "BK-SVC Foreground started")
        handler.postDelayed({ forceReinit() }, 1000L)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        discoveryKeepAlive = false
        try { hidDeviceProxy?.unregisterApp() } catch (_: Exception) {}
        try { unregisterReceiver(btStateReceiver) } catch (_: Exception) {}
        super.onDestroy()
        DebugLogger.i(TAG, "BK-SVC Destroyed")
    }

    fun forceReinit() {
        DebugLogger.i(TAG, "BK-SVC Force reinit")
        handler.removeCallbacksAndMessages(null)
        attempt = 0
        isBound = false
        isRegistered = false
        hidDeviceProxy = null
        connectedHidDevice = null
        bindToHidProfile()
    }

    private fun bindToHidProfile() {
        val adapter = btAdapter
        if (adapter == null || !adapter.isEnabled) {
            DebugLogger.w(TAG, "BK-SVC BT not ready, retry in 5s")
            handler.postDelayed({ forceReinit() }, 5000L)
            return
        }
        startDiscoveryMode()
        DebugLogger.i(TAG, "BK-SVC Binding to profile 19 (HID_DEVICE)")
        try {
            adapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    DebugLogger.i(TAG, "BK-SVC onServiceConnected: profile=$profile type=${proxy?.javaClass?.simpleName}")
                    if (proxy is BluetoothHidDevice) {
                        hidDeviceProxy = proxy
                        isBound = true
                        DebugLogger.i(TAG, "BK-SVC HID proxy obtained, registering app")
                        handler.post { doRegister() }
                    } else {
                        DebugLogger.w(TAG, "BK-SVC Wrong profile type, got=${proxy?.javaClass?.name}")
                        handler.postDelayed({ forceReinit() }, 3000L)
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    DebugLogger.w(TAG, "BK-SVC Profile disconnected, will rebind")
                    isBound = false
                    hidDeviceProxy = null
                    isRegistered = false
                    handler.postDelayed({ forceReinit() }, 2000L)
                }
            }, 19)
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BK-SVC getProfileProxy failed", e)
            handler.postDelayed({ forceReinit() }, 5000L)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDiscoveryMode() {
        try {
            discoveryKeepAlive = true
            discoverableExecutor.execute {
                while (discoveryKeepAlive) {
                    try {
                        val adapter = btAdapter
                        if (adapter?.isEnabled == true) {
                            val currentMode = adapter.scanMode
                            if (currentMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                                DebugLogger.i(TAG, "BK-SVC ScanMode=$currentMode (non-discoverable)")
                            }
                        }
                        Thread.sleep(300000)
                    } catch (e: Exception) {
                        DebugLogger.e(TAG, "BK-SVC Discoverable check error", e)
                        Thread.sleep(30000)
                    }
                }
            }
            DebugLogger.i(TAG, "BK-SVC Discovery keep-alive started")
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BK-SVC Start discovery failed", e)
        }
    }

    private fun doRegister() {
        try {
            val proxy = hidDeviceProxy
            if (proxy == null) {
                DebugLogger.w(TAG, "BK-SVC No proxy, rebinding")
                bindToHidProfile()
                return
            }
            val adapter = btAdapter
            if (adapter == null || !adapter.isEnabled) {
                handler.postDelayed({ forceReinit() }, 5000L)
                return
            }

            if (isRegistered) {
                DebugLogger.i(TAG, "BK-SVC Already registered, unregistering first")
                try { proxy.unregisterApp() } catch (_: Exception) {}
                Thread.sleep(500)
                isRegistered = false
            }

            val deviceName = adapter.name ?: SDP_RECORD_NAME
            val sdp = BluetoothHidDeviceAppSdpSettings(
                deviceName, SDP_DESCRIPTION, SDP_PROVIDER,
                BluetoothHidDevice.SUBCLASS1_COMBO, hidReportDescriptor
            )
            val qosIn = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800, 9, 0, 1000, 1000
            )
            val qosOut = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_NO_TRAFFIC,
                0, 0, 0, 0, 0
            )

            attempt++
            DebugLogger.i(TAG, "BK-SVC registerApp attempt $attempt with name=$deviceName")
            val ok = proxy.registerApp(sdp, qosIn, qosOut, hidExecutor, hidCallback)
            DebugLogger.i(TAG, "BK-SVC registerApp result: $ok")

            if (ok) {
                isRegistered = true
                attempt = 0
                onStateChanged?.invoke(true, null)
                DebugLogger.i(TAG, "BK-SVC HID registered! Waiting for host to connect...")
            } else {
                DebugLogger.w(TAG, "BK-SVC registerApp returned false")
                scheduleRetry()
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BK-SVC Register exception", e)
            onStateChanged?.invoke(false, e.message)
            scheduleRetry()
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, reg: Boolean) {
            DebugLogger.i(TAG, "BK-SVC onAppStatusChanged: plugged=${pluggedDevice?.name}, registered=$reg")
            if (reg) {
                isRegistered = true
                attempt = 0
                onStateChanged?.invoke(true, null)
                if (pluggedDevice != null) {
                    connectedHidDevice = pluggedDevice
                    onDeviceConnected?.invoke(pluggedDevice)
                    DebugLogger.i(TAG, "BK-SVC HID host CONNECTED: ${pluggedDevice.name}")
                } else {
                    DebugLogger.i(TAG, "BK-SVC HID registered, no host plugged yet")
                }
            } else {
                isRegistered = false
                connectedHidDevice = null
                onStateChanged?.invoke(false, null)
                onDeviceConnected?.invoke(null)
                DebugLogger.w(TAG, "BK-SVC HID unregistered, auto-retrying")
                scheduleRetry()
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            DebugLogger.i(TAG, "BK-SVC onConnectionStateChanged: state=$state, device=${device?.name}")
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedHidDevice = device
                    onDeviceConnected?.invoke(device)
                    DebugLogger.i(TAG, "BK-SVC HID link established: ${device?.name}")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (device != null && device == connectedHidDevice) {
                        connectedHidDevice = null
                        onDeviceConnected?.invoke(null)
                        DebugLogger.w(TAG, "BK-SVC HID link lost")
                    }
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, size: Int) {
            DebugLogger.d(TAG, "BK-SVC onGetReport: type=$type, id=$id")
            if (type.toInt() == 1) {
                handler.post {
                    try {
                        val empty = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
                        hidDeviceProxy?.replyReport(device, type, id, empty)
                    } catch (_: Exception) {}
                }
            }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            DebugLogger.d(TAG, "BK-SVC onSetReport: type=$type, id=$id")
        }

        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
            DebugLogger.d(TAG, "BK-SVC onInterruptData: rpt=$reportId, len=${data?.size}")
        }
    }

    private fun scheduleRetry() {
        val delay = when {
            attempt <= 3 -> 3000L
            attempt <= 8 -> 15000L
            else -> 30000L
        }
        DebugLogger.i(TAG, "BK-SVC Scheduling retry #${attempt + 1} in ${delay}ms")
        handler.postDelayed({ doRegister() }, delay)
    }

    fun retryRegistration() {
        DebugLogger.i(TAG, "BK-SVC Manual retry requested")
        attempt = 0
        if (isRegistered && hidDeviceProxy != null) {
            try { hidDeviceProxy?.unregisterApp() } catch (_: Exception) {}
            Thread.sleep(500)
            isRegistered = false
        }
        bindToHidProfile()
    }

    fun sendMouseReport(buttons: Int, dx: Int, dy: Int, scroll: Int = 0) {
        try {
            val hid = hidDeviceProxy ?: return
            val dev = connectedHidDevice ?: return
            val report = byteArrayOf(buttons.toByte(), dx.toByte(), dy.toByte(), scroll.toByte())
            hid.sendReport(dev, REPORT_ID_MOUSE.toInt(), report)
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BK-SVC Mouse report error", e)
        }
    }

    fun sendKeyboardReport(modifiers: Byte, keys: ByteArray) {
        try {
            val hid = hidDeviceProxy ?: return
            val dev = connectedHidDevice ?: return
            val report = ByteArray(8)
            report[0] = modifiers
            for (i in keys.indices) {
                if (i >= 6) break
                report[2 + i] = keys[i]
            }
            hid.sendReport(dev, REPORT_ID_KEYBOARD.toInt(), report)
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BK-SVC Keyboard report error", e)
        }
    }

    fun sendMediaReport(code: Byte) {
        try {
            val hid = hidDeviceProxy ?: return
            val dev = connectedHidDevice ?: return
            hid.sendReport(dev, REPORT_ID_CONSUMER.toInt(), byteArrayOf(code))
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BK-SVC Media report error", e)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "BT HID Service", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps Bluetooth HID keyboard/mouse active"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("BT KB & Mouse Pro")
            .setContentText("HID keyboard & mouse active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .build()
    }

    private val hidReportDescriptor = byteArrayOf(
        0x05, 0x01, 0x09, 0x06, 0xa1.toByte(), 0x01, 0x85.toByte(), REPORT_ID_KEYBOARD.toByte(),
        0x05, 0x07, 0x19, 0xe0.toByte(), 0x29, 0xe7.toByte(),
        0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95.toByte(), 0x08, 0x81.toByte(), 0x02,
        0x95.toByte(), 0x01, 0x75, 0x08, 0x81.toByte(), 0x01,
        0x95.toByte(), 0x06, 0x75, 0x08, 0x15, 0x00, 0x25, 0x65,
        0x05, 0x07, 0x19, 0x00, 0x29, 0x65, 0x81.toByte(), 0x00, 0xc0.toByte(),
        0x05, 0x01, 0x09, 0x02, 0xa1.toByte(), 0x01, 0x85.toByte(), REPORT_ID_MOUSE.toByte(),
        0x09, 0x01, 0xa1.toByte(), 0x00,
        0x05, 0x09, 0x19, 0x01, 0x29, 0x03, 0x15, 0x00, 0x25, 0x01,
        0x95.toByte(), 0x03, 0x75, 0x01, 0x81.toByte(), 0x02,
        0x95.toByte(), 0x01, 0x75, 0x05, 0x81.toByte(), 0x01,
        0x05, 0x01, 0x09, 0x30, 0x09, 0x31, 0x09, 0x38,
        0x15, 0x81.toByte(), 0x25, 0x7f, 0x75, 0x08, 0x95.toByte(), 0x03, 0x81.toByte(), 0x06,
        0xc0.toByte(), 0xc0.toByte(),
        0x05, 0x0c.toByte(), 0x09, 0x01, 0xa1.toByte(), 0x01, 0x85.toByte(), REPORT_ID_CONSUMER.toByte(),
        0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95.toByte(), 0x07,
        0x0a, 0xcd.toByte(), 0x00, 0x0a, 0xe9.toByte(), 0x00,
        0x0a, 0xea.toByte(), 0x00, 0x0a, 0xe2.toByte(), 0x00,
        0x0a, 0xb6.toByte(), 0x00, 0x0a, 0xb5.toByte(), 0x00,
        0x0a, 0xb7.toByte(), 0x00, 0x81.toByte(), 0x02, 0xc0.toByte()
    )
}
