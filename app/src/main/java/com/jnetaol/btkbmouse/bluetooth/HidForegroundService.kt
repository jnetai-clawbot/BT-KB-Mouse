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
import android.content.Intent
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
    }

    private val binder = LocalBinder()
    private val handler = Handler(Looper.getMainLooper())
    private val hidExecutor = Executors.newSingleThreadExecutor()

    private var btAdapter: BluetoothAdapter? = null
    private var hidDeviceProxy: BluetoothHidDevice? = null
    var connectedHidDevice: BluetoothDevice? = null
        private set
    private var isRegistered = false
    private var isBound = false
    private var attempt = 0

    var onStateChanged: ((Boolean, String?) -> Unit)? = null
    var onDeviceConnected: ((BluetoothDevice?) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): HidForegroundService = this@HidForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        btAdapter = (getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
        createNotificationChannel()
        DebugLogger.i(TAG, "BK-SVC Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        DebugLogger.i(TAG, "BK-SVC Foreground started")
        handler.postDelayed({ forceReinit() }, 1000L)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try { hidDeviceProxy?.unregisterApp() } catch (_: Exception) {}
        super.onDestroy()
        DebugLogger.i(TAG, "BK-SVC Destroyed")
    }

    fun forceReinit() {
        DebugLogger.i(TAG, "BK-SVC Force reinit")
        attempt = 0
        isBound = false
        isRegistered = false
        hidDeviceProxy = null
        connectedHidDevice = null
        bindToHidProfile()
    }

    private fun bindToHidProfile() {
        val adapter = btAdapter ?: return
        if (!adapter.isEnabled) {
            handler.postDelayed({ forceReinit() }, 5000L)
            return
        }
        DebugLogger.i(TAG, "BK-SVC Binding HID_DEVICE profile")
        try {
            adapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    DebugLogger.i(TAG, "BK-SVC Profile connected: $profile proxy=${proxy?.javaClass?.name}")
                    if (proxy is BluetoothHidDevice) {
                        hidDeviceProxy = proxy
                        isBound = true
                        doRegister()
                    } else {
                        handler.postDelayed({ forceReinit() }, 3000L)
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    DebugLogger.w(TAG, "BK-SVC Profile disconnected")
                    isBound = false
                    hidDeviceProxy = null
                    isRegistered = false
                    handler.postDelayed({ forceReinit() }, 2000L)
                }
            }, 19)
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BK-SVC Bind failed", e)
            handler.postDelayed({ forceReinit() }, 5000L)
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
                try { proxy.unregisterApp() } catch (_: Exception) {}
                isRegistered = false
            }

            val descriptor = byteArrayOf(
                0x05, 0x01, 0x09, 0x06, 0xa1, 0x01, 0x85, REPORT_ID_KEYBOARD,
                0x05, 0x07, 0x19, 0xe0.toByte(), 0x29, 0xe7.toByte(),
                0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95, 0x08, 0x81, 0x02,
                0x95, 0x01, 0x75, 0x08, 0x81, 0x01,
                0x95, 0x06, 0x75, 0x08, 0x15, 0x00, 0x25, 0x65,
                0x05, 0x07, 0x19, 0x00, 0x29, 0x65, 0x81, 0x00, 0xc0,
                0x05, 0x01, 0x09, 0x02, 0xa1, 0x01, 0x85, REPORT_ID_MOUSE,
                0x09, 0x01, 0xa1, 0x00,
                0x05, 0x09, 0x19, 0x01, 0x29, 0x03, 0x15, 0x00, 0x25, 0x01,
                0x95, 0x03, 0x75, 0x01, 0x81, 0x02,
                0x95, 0x01, 0x75, 0x05, 0x81, 0x01,
                0x05, 0x01, 0x09, 0x30, 0x09, 0x31, 0x09, 0x38,
                0x15, 0x81.toByte(), 0x25, 0x7f, 0x75, 0x08, 0x95, 0x03, 0x81, 0x06,
                0xc0, 0xc0,
                0x05, 0x0c.toByte(), 0x09, 0x01, 0xa1, 0x01, 0x85, REPORT_ID_CONSUMER,
                0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95, 0x07,
                0x0a, 0xcd.toByte(), 0x00, 0x0a, 0xe9.toByte(), 0x00,
                0x0a, 0xea.toByte(), 0x00, 0x0a, 0xe2.toByte(), 0x00,
                0x0a, 0xb6.toByte(), 0x00, 0x0a, 0xb5.toByte(), 0x00,
                0x0a, 0xb7.toByte(), 0x00, 0x81, 0x02, 0xc0
            )

            val sdp = BluetoothHidDeviceAppSdpSettings(
                SDP_RECORD_NAME, SDP_DESCRIPTION, SDP_PROVIDER,
                BluetoothHidDevice.SUBCLASS1_COMBO, descriptor
            )
            val qos = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800, 9, 0, 1000, 1000
            )

            val cb = object : BluetoothHidDevice.Callback() {
                override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, reg: Boolean) {
                    DebugLogger.i(TAG, "BK-SVC AppStatus: plugged=${pluggedDevice?.name} reg=$reg")
                    if (reg) {
                        isRegistered = true
                        attempt = 0
                        onStateChanged?.invoke(true, null)
                        if (pluggedDevice != null) {
                            connectedHidDevice = pluggedDevice
                            onDeviceConnected?.invoke(pluggedDevice)
                            DebugLogger.i(TAG, "BK-SVC Host connected: ${pluggedDevice.name}")
                        }
                    } else {
                        isRegistered = false
                        connectedHidDevice = null
                        onStateChanged?.invoke(false, null)
                        onDeviceConnected?.invoke(null)
                        scheduleRetry()
                    }
                }

                override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                    DebugLogger.i(TAG, "BK-SVC ConnState: state=$state dev=${device?.name}")
                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        connectedHidDevice = device
                        onDeviceConnected?.invoke(device)
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        if (device == connectedHidDevice) {
                            connectedHidDevice = null
                            onDeviceConnected?.invoke(null)
                        }
                    }
                }

                override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, size: Int) {}
                override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {}
                override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {}
            }

            attempt++
            val ok = proxy.registerApp(sdp, qos, qos, hidExecutor, cb)
            DebugLogger.i(TAG, "BK-SVC registerApp=$ok (attempt $attempt)")

            if (ok) {
                isRegistered = true
                attempt = 0
                onStateChanged?.invoke(true, null)
            } else {
                scheduleRetry()
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BK-SVC Register error", e)
            onStateChanged?.invoke(false, e.message)
            scheduleRetry()
        }
    }

    private fun scheduleRetry() {
        val delay = when {
            attempt <= 2 -> 2000L
            attempt <= 5 -> 10000L
            else -> 30000L
        }
        DebugLogger.i(TAG, "BK-SVC Retry #${attempt + 1} in ${delay}ms")
        handler.postDelayed({ doRegister() }, delay)
    }

    fun retryRegistration() {
        DebugLogger.i(TAG, "BK-SVC Manual retry")
        attempt = 0
        if (isRegistered) {
            try { hidDeviceProxy?.unregisterApp() } catch (_: Exception) {}
            isRegistered = false
        }
        forceReinit()
    }

    fun sendMouseReport(buttons: Int, dx: Int, dy: Int, scroll: Int = 0) {
        try {
            val hid = hidDeviceProxy ?: return
            val dev = connectedHidDevice ?: return
            hid.sendReport(dev, REPORT_ID_MOUSE.toInt(),
                byteArrayOf(buttons.toByte(), dx.toByte(), dy.toByte(), scroll.toByte()))
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BK-SVC Mouse error", e)
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
            DebugLogger.e(TAG, "BK-SVC Keyboard error", e)
        }
    }

    fun sendMediaReport(code: Byte) {
        try {
            val hid = hidDeviceProxy ?: return
            val dev = connectedHidDevice ?: return
            hid.sendReport(dev, REPORT_ID_CONSUMER.toInt(), byteArrayOf(code))
        } catch (e: Exception) {
            DebugLogger.e(TAG, "BK-SVC Media error", e)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "BT HID Service",
            NotificationManager.IMPORTANCE_LOW).apply {
            description = "Keeps Bluetooth HID keyboard/mouse active"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("BT KB & Mouse Pro")
            .setContentText("HID keyboard & mouse active")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_LOW)
            .build()
    }
}
