package com.jnetaol.btkbmouse.bluetooth

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

class HidForegroundService : Service() {
    companion object {
        const val TAG = "HidService"
        const val CHANNEL_ID = "bt_hid_service"
        const val NOTIFICATION_ID = 1001
        const val SDP_RECORD_NAME = "BT KB & Mouse Pro"
        const val SDP_DESCRIPTION = "Professional Keyboard and Mouse"
        const val SDP_PROVIDER = "jnetai.com"
        const val REPORT_ID_KEYBOARD: Byte = 1
        const val REPORT_ID_MOUSE: Byte = 2
        const val REPORT_ID_CONSUMER: Byte = 3
    }

    private val binder = LocalBinder()
    private val handler = Handler(Looper.getMainLooper())

    private var btAdapter: BluetoothAdapter? = null
    private var hidProxy: BluetoothHidDevice? = null
    var connectedDevice: BluetoothDevice? = null
        private set
    var isRegistered = false
        private set
    private var isBinding = false
    private var registerAttempt = 0
    private var retryRunnable: Runnable? = null

    var onStatusChanged: ((Boolean, Boolean, String?) -> Unit)? = null
    var onDeviceChanged: ((BluetoothDevice?) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): HidForegroundService = this@HidForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: android.content.Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON) {
                        DebugLogger.i(TAG, "BT ON -> init")
                        handler.postDelayed({ initHid() }, 3000L)
                    } else if (state == BluetoothAdapter.STATE_OFF) {
                        isRegistered = false
                        notifyState()
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bond = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    if (bond == BluetoothDevice.BOND_BONDED) {
                        DebugLogger.i(TAG, "New bond -> re-register HID")
                        handler.postDelayed({ reinit() }, 2000L)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        btAdapter = (getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
        createNotificationChannel()
        registerReceiver(receiver, IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        })
        DebugLogger.i(TAG, "Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        handler.postDelayed({ initHid() }, 2000L)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try { hidProxy?.unregisterApp() } catch (_: Exception) {}
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    fun reinit() {
        DebugLogger.i(TAG, "Reinit HID")
        handler.removeCallbacksAndMessages(null)
        retryRunnable = null
        isRegistered = false
        registerAttempt = 0
        isBinding = false
        hidProxy = null
        connectedDevice = null
        notifyState()
        initHid()
    }

    private fun initHid() {
        val adapter = btAdapter
        if (adapter == null || !adapter.isEnabled) {
            scheduleInitRetry()
            return
        }
        if (isBinding) {
            DebugLogger.d(TAG, "Already binding, skip")
            return
        }
        isBinding = true
        DebugLogger.i(TAG, "getProfileProxy(19)...")
        try {
            adapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    isBinding = false
                    if (proxy is BluetoothHidDevice) {
                        hidProxy = proxy
                        DebugLogger.i(TAG, "HID proxy obtained")
                        registerApp()
                    } else {
                        DebugLogger.w(TAG, "Non-HID proxy: ${proxy?.javaClass?.name}")
                        scheduleInitRetry()
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    isBinding = false
                    hidProxy = null
                    isRegistered = false
                    notifyState()
                    scheduleInitRetry()
                }
            }, 19)
        } catch (e: Exception) {
            isBinding = false
            DebugLogger.e(TAG, "getProfileProxy failed", e)
            scheduleInitRetry()
        }
    }

    private fun registerApp() {
        try {
            val proxy = hidProxy ?: run {
                DebugLogger.w(TAG, "No proxy, init first")
                initHid()
                return
            }
            val adapter = btAdapter
            if (adapter == null || !adapter.isEnabled) {
                scheduleInitRetry()
                return
            }

            if (isRegistered) {
                try { proxy.unregisterApp() } catch (_: Exception) {}
                isRegistered = false
                Thread.sleep(300)
            }

            val sdpName = adapter.name?.takeIf { it.isNotBlank() } ?: SDP_RECORD_NAME
            val sdp = BluetoothHidDeviceAppSdpSettings(
                sdpName, SDP_DESCRIPTION, SDP_PROVIDER,
                BluetoothHidDevice.SUBCLASS1_COMBO, hidReportDescriptor
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
            DebugLogger.i(TAG, "registerApp attempt=$registerAttempt sdpName=$sdpName")
            val ok = proxy.registerApp(sdp, qosIn, qosOut, mainExecutor, hidCallback)
            DebugLogger.i(TAG, "registerApp=$ok")

            if (ok) {
                isRegistered = true
                registerAttempt = 0
                retryRunnable = null
                notifyState()
                DebugLogger.i(TAG, "HID registered! Have host scan Bluetooth to see '${sdpName}'")
            } else {
                scheduleRetry()
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "registerApp exception", e)
            scheduleRetry()
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, reg: Boolean) {
            DebugLogger.i(TAG, "StatusChanged: registered=$reg plugged=${pluggedDevice?.name}")
            isRegistered = reg
            if (reg) {
                registerAttempt = 0
                retryRunnable = null
                if (pluggedDevice != null) {
                    connectedDevice = pluggedDevice
                    onDeviceChanged?.invoke(pluggedDevice)
                    DebugLogger.i(TAG, "HOST CONNECTED: ${pluggedDevice.name} (${pluggedDevice.address})")
                }
                notifyState()
            } else if (!reg) {
                connectedDevice = null
                onDeviceChanged?.invoke(null)
                notifyState()
                scheduleRetry()
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            DebugLogger.i(TAG, "ConnState: state=$state dev=${device?.name}")
            if (state == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device
                onDeviceChanged?.invoke(device)
                notifyState()
            } else if (state == BluetoothProfile.STATE_DISCONNECTED && device == connectedDevice) {
                connectedDevice = null
                onDeviceChanged?.invoke(null)
                notifyState()
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, size: Int) {
            if (type.toInt() == 1) {
                try {
                    hidProxy?.replyReport(device, type, id, ByteArray(8))
                } catch (_: Exception) {}
            }
        }
        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {}
        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {}
    }

    private fun scheduleInitRetry() {
        retryRunnable?.let { handler.removeCallbacks(it) }
        retryRunnable = Runnable { initHid() }
        handler.postDelayed(retryRunnable, 5000L)
    }

    private fun scheduleRetry() {
        retryRunnable?.let { handler.removeCallbacks(it) }
        val delay = when {
            registerAttempt <= 3 -> 3000L
            registerAttempt <= 8 -> 10000L
            else -> 30000L
        }
        DebugLogger.i(TAG, "Retry #${registerAttempt + 1} in ${delay}ms")
        retryRunnable = Runnable { registerApp() }
        handler.postDelayed(retryRunnable, delay)
    }

    private fun notifyState() {
        onStatusChanged?.invoke(isRegistered, connectedDevice != null, null)
    }

    fun sendMouseReport(buttons: Int, dx: Int, dy: Int, scroll: Int = 0) {
        try {
            val hid = hidProxy ?: return
            val dev = connectedDevice ?: return
            hid.sendReport(dev, REPORT_ID_MOUSE.toInt(),
                byteArrayOf(buttons.toByte(), dx.toByte(), dy.toByte(), scroll.toByte()))
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Mouse err", e)
        }
    }

    fun sendKeyboardReport(modifiers: Byte, keys: ByteArray) {
        try {
            val hid = hidProxy ?: return
            val dev = connectedDevice ?: return
            val rpt = ByteArray(8)
            rpt[0] = modifiers
            for (i in keys.indices) { if (i < 6) rpt[2 + i] = keys[i] }
            hid.sendReport(dev, REPORT_ID_KEYBOARD.toInt(), rpt)
        } catch (e: Exception) {
            DebugLogger.e(TAG, "KB err", e)
        }
    }

    fun sendMediaReport(code: Byte) {
        try {
            val hid = hidProxy ?: return
            val dev = connectedDevice ?: return
            hid.sendReport(dev, REPORT_ID_CONSUMER.toInt(), byteArrayOf(code))
        } catch (e: Exception) {
            DebugLogger.e(TAG, "Media err", e)
        }
    }

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "BT HID Service", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps HID keyboard/mouse active"
            }
        )
    }

    private fun buildNotification(): Notification {
        val text = if (connectedDevice != null) "Connected to ${connectedDevice?.name}"
        else if (isRegistered) "HID active - waiting for connection"
        else "Starting HID service..."
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("BT KB & Mouse Pro")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(PendingIntent.getActivity(this, 0,
                Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true)
            .build()
    }

    private val hidReportDescriptor = byteArrayOf(
        0x05, 0x01, 0x09, 0x06, 0xa1.toByte(), 0x01, 0x85.toByte(), REPORT_ID_KEYBOARD,
        0x05, 0x07, 0x19, 0xe0.toByte(), 0x29, 0xe7.toByte(),
        0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95.toByte(), 0x08, 0x81.toByte(), 0x02,
        0x95.toByte(), 0x01, 0x75, 0x08, 0x81.toByte(), 0x01,
        0x95.toByte(), 0x06, 0x75, 0x08, 0x15, 0x00, 0x25, 0x65,
        0x05, 0x07, 0x19, 0x00, 0x29, 0x65, 0x81.toByte(), 0x00, 0xc0.toByte(),
        0x05, 0x01, 0x09, 0x02, 0xa1.toByte(), 0x01, 0x85.toByte(), REPORT_ID_MOUSE,
        0x09, 0x01, 0xa1.toByte(), 0x00,
        0x05, 0x09, 0x19, 0x01, 0x29, 0x03, 0x15, 0x00, 0x25, 0x01,
        0x95.toByte(), 0x03, 0x75, 0x01, 0x81.toByte(), 0x02,
        0x95.toByte(), 0x01, 0x75, 0x05, 0x81.toByte(), 0x01,
        0x05, 0x01, 0x09, 0x30, 0x09, 0x31, 0x09, 0x38,
        0x15, 0x81.toByte(), 0x25, 0x7f, 0x75, 0x08, 0x95.toByte(), 0x03, 0x81.toByte(), 0x06,
        0xc0.toByte(), 0xc0.toByte(),
        0x05, 0x0c.toByte(), 0x09, 0x01, 0xa1.toByte(), 0x01, 0x85.toByte(), REPORT_ID_CONSUMER,
        0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95.toByte(), 0x07,
        0x0a, 0xcd.toByte(), 0x00, 0x0a, 0xe9.toByte(), 0x00,
        0x0a, 0xea.toByte(), 0x00, 0x0a, 0xe2.toByte(), 0x00,
        0x0a, 0xb6.toByte(), 0x00, 0x0a, 0xb5.toByte(), 0x00,
        0x0a, 0xb7.toByte(), 0x00, 0x81.toByte(), 0x02, 0xc0.toByte()
    )
}
