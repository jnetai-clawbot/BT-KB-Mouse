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
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.jnetaol.btkbmouse.MainActivity
import com.jnetaol.btkbmouse.logger.DebugLogger
import java.util.UUID

class HidForegroundService : Service() {
    companion object {
        const val TAG = "HidService"
        const val CHANNEL_ID = "bt_hid_service"
        const val NOTIFICATION_ID = 1001
        const val SDP_NAME = "BT KB & Mouse Pro"
        const val SDP_DESC = "Professional Keyboard and Mouse"
        const val SDP_PROVIDER = "jnetai.com"
        const val REPORT_ID_KEYBOARD: Byte = 1
        const val REPORT_ID_MOUSE: Byte = 2
        const val REPORT_ID_CONSUMER: Byte = 3
        val HID_UUID = UUID.fromString("00001124-0000-1000-8000-00805F9B34FB")
        val HID_SDP_UUID = UUID.fromString("00001112-0000-1000-8000-00805F9B34FB")
    }

    private val binder = LocalBinder()
    private val handler = Handler(Looper.getMainLooper())

    private var btAdapter: BluetoothAdapter? = null
    private var hidProxy: BluetoothHidDevice? = null
    private var serverSocket: BluetoothServerSocket? = null
    var connectedDevice: BluetoothDevice? = null
        private set
    var isRegistered = false
        private set
    private var isHidBound = false
    private var reinitScheduled = false
    private var initCount = 0

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
                    if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                        DebugLogger.i(TAG, "BT on -> start HID")
                        scheduleInit(1000L)
                    } else {
                        isRegistered = false
                        notifyChanged()
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val dev = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    DebugLogger.i(TAG, "ACL connected: ${dev?.name}")
                    scheduleInit(1000L)
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val dev = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    DebugLogger.i(TAG, "ACL disconnected: ${dev?.name}")
                    if (dev == connectedDevice) {
                        connectedDevice = null
                        onDeviceChanged?.invoke(null)
                        notifyChanged()
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bond = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                    if (bond == BluetoothDevice.BOND_BONDED) {
                        DebugLogger.i(TAG, "Device bonded -> re-register HID")
                        scheduleInit(2000L)
                    }
                }
            }
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(plugged: BluetoothDevice?, registered: Boolean) {
            DebugLogger.i(TAG, "HID status: reg=$registered plugged=${plugged?.name}")
            isRegistered = registered
            if (registered && plugged != null) {
                connectedDevice = plugged
                onDeviceChanged?.invoke(plugged)
                DebugLogger.i(TAG, "HID HOST CONNECTED: ${plugged.name} (${plugged.address})")
            } else if (!registered) {
                connectedDevice = null
                onDeviceChanged?.invoke(null)
                scheduleInit(2000L)
            }
            notifyChanged()
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            DebugLogger.i(TAG, "HID conn state=$state dev=${device?.name}")
            if (state == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device
                onDeviceChanged?.invoke(device)
                notifyChanged()
            } else if (state == BluetoothProfile.STATE_DISCONNECTED && device == connectedDevice) {
                connectedDevice = null
                onDeviceChanged?.invoke(null)
                notifyChanged()
            }
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, size: Int) {
            try {
                if (type.toInt() == 1 && id.toInt() == 0) {
                    hidProxy?.replyReport(device, type, id, ByteArray(8))
                }
            } catch (_: Exception) {}
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {}
        override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {}
    }

    override fun onCreate() {
        super.onCreate()
        btAdapter = (getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
        createNotificationChannel()
        registerReceiver(receiver, IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        })
        DebugLogger.i(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        scheduleInit(1000L)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        try { hidProxy?.unregisterApp() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun scheduleInit(delayMs: Long) {
        if (reinitScheduled) return
        reinitScheduled = true
        handler.postDelayed({
            reinitScheduled = false
            initCount++
            initHid()
        }, delayMs)
    }

    fun manualReinit() {
        DebugLogger.i(TAG, "Manual reinit requested")
        isHidBound = false
        isRegistered = false
        connectedDevice = null
        hidProxy = null
        initCount = 0
        notifyChanged()
        scheduleInit(500L)
    }

    @SuppressLint("MissingPermission")
    private fun initHid() {
        val adapter = btAdapter
        if (adapter == null || !adapter.isEnabled) {
            DebugLogger.w(TAG, "BT not ready, retry")
            scheduleInit(5000L)
            return
        }

        startDiscoveryServer()

        if (!isHidBound) {
            DebugLogger.i(TAG, "Binding HID profile (init #$initCount)")
            try {
                adapter.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                        DebugLogger.i(TAG, "HID profile connected: profile=$profile type=${proxy?.javaClass?.simpleName}")
                        if (proxy is BluetoothHidDevice) {
                            hidProxy = proxy
                            isHidBound = true
                            handler.post { registerHidApp() }
                        } else {
                            DebugLogger.w(TAG, "Wrong proxy type: ${proxy?.javaClass?.name}")
                            isHidBound = false
                            scheduleInit(5000L)
                        }
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        DebugLogger.w(TAG, "HID profile disconnected")
                        isHidBound = false
                        hidProxy = null
                        isRegistered = false
                        notifyChanged()
                        scheduleInit(3000L)
                    }
                }, 19)
            } catch (e: Exception) {
                DebugLogger.e(TAG, "getProfileProxy error", e)
                isHidBound = false
                scheduleInit(5000L)
            }
        } else if (!isRegistered && hidProxy != null) {
            registerHidApp()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDiscoveryServer() {
        try {
            serverSocket?.close()
            val name = btAdapter?.name ?: SDP_NAME
            serverSocket = btAdapter?.listenUsingInsecureRfcommWithServiceRecord(name, HID_UUID)
            DebugLogger.i(TAG, "RFCOMM server socket listening as '$name'")
        } catch (e: Exception) {
            DebugLogger.d(TAG, "RFCOMM server not critical: ${e.message}")
        }
    }

    private fun registerHidApp() {
        try {
            val proxy = hidProxy ?: run { scheduleInit(2000L); return }
            val adapter = btAdapter
            if (adapter == null || !adapter.isEnabled) { scheduleInit(5000L); return }

            if (isRegistered) {
                DebugLogger.i(TAG, "Already registered, unregistering first")
                try { proxy.unregisterApp() } catch (_: Exception) {}
                Thread.sleep(300)
                isRegistered = false
            }

            val btName = adapter.name?.takeIf { it.isNotBlank() } ?: SDP_NAME
            val sdp = BluetoothHidDeviceAppSdpSettings(
                btName, SDP_DESC, SDP_PROVIDER,
                BluetoothHidDevice.SUBCLASS1_COMBO, HID_DESCRIPTOR
            )
            val qosIn = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800, 9, 0, 1000, 1000
            )
            val qosOut = BluetoothHidDeviceAppQosSettings(
                BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                800, 9, 0, 1000, 1000
            )

            DebugLogger.i(TAG, "registerApp as '$btName' (init #$initCount)")
            val ok = proxy.registerApp(sdp, qosIn, qosOut, mainExecutor, hidCallback)
            DebugLogger.i(TAG, "registerApp result: $ok")

            if (ok) {
                isRegistered = true
                notifyChanged()
                DebugLogger.i(TAG, "HID registered! Tell host to scan BT to find '$btName' as keyboard/mouse")
            } else {
                DebugLogger.w(TAG, "registerApp returned false")
                scheduleInit(3000L)
            }
        } catch (e: Exception) {
            DebugLogger.e(TAG, "registerApp exception", e)
            scheduleInit(5000L)
        }
    }

    private fun notifyChanged() {
        onStatusChanged?.invoke(isRegistered, connectedDevice != null, null)
        updateNotification()
    }

    private fun updateNotification() {
        val text = if (connectedDevice != null)
            "Connected to ${connectedDevice?.name}"
        else if (isRegistered)
            "HID active - Ready to connect"
        else
            "HID service starting..."
        try {
            val nm = getSystemService(NotificationManager::class.java)
            nm.notify(NOTIFICATION_ID, Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("BT KB & Mouse Pro")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                    Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                .setOngoing(true)
                .build())
        } catch (_: Exception) {}
    }

    fun sendMouseReport(buttons: Int, dx: Int, dy: Int, scroll: Int = 0) {
        val hid = hidProxy ?: return
        val dev = connectedDevice ?: return
        try {
            hid.sendReport(dev, REPORT_ID_MOUSE.toInt(),
                byteArrayOf(buttons.toByte(), dx.toByte(), dy.toByte(), scroll.toByte()))
        } catch (e: Exception) { DebugLogger.e(TAG, "Mouse err", e) }
    }

    fun sendKeyboardReport(modifiers: Byte, keys: ByteArray = byteArrayOf()) {
        val hid = hidProxy ?: return
        val dev = connectedDevice ?: return
        try {
            val rpt = ByteArray(8)
            rpt[0] = modifiers
            keys.forEachIndexed { i, k -> if (i < 6) rpt[2 + i] = k }
            hid.sendReport(dev, REPORT_ID_KEYBOARD.toInt(), rpt)
        } catch (e: Exception) { DebugLogger.e(TAG, "KB err", e) }
    }

    fun sendConsumerReport(code: Byte) {
        val hid = hidProxy ?: return
        val dev = connectedDevice ?: return
        try { hid.sendReport(dev, REPORT_ID_CONSUMER.toInt(), byteArrayOf(code)) } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "BT HID Service", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Bluetooth HID keyboard/mouse service"
            }
        )
    }

    private fun buildNotification() = Notification.Builder(this, CHANNEL_ID)
        .setContentTitle("BT KB & Mouse Pro")
        .setContentText("HID keyboard & mouse active")
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentIntent(PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        .setOngoing(true)
        .build()

    companion object {
        val HID_DESCRIPTOR = byteArrayOf(
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
}
