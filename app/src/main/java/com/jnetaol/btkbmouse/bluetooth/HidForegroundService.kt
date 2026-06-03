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
        const val CHANNEL_ID = "bt_hid"
        const val NOTIFY_ID = 42001
        const val SDP_NAME = "BT Keyboard & Mouse"
        const val SDP_DESC = "Keyboard and Mouse via BT"
        const val SDP_PROV = "jnetai.com"
        const val RID_KB: Byte = 1
        const val RID_MOUSE: Byte = 2
        const val RID_CONS: Byte = 3
        val DESCRIPTOR = byteArrayOf(
            0x05, 0x01, 0x09, 0x06, 0xa1.toByte(), 0x01, 0x85.toByte(), RID_KB,
            0x05, 0x07, 0x19, 0xe0.toByte(), 0x29, 0xe7.toByte(),
            0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95.toByte(), 0x08, 0x81.toByte(), 0x02,
            0x95.toByte(), 0x01, 0x75, 0x08, 0x81.toByte(), 0x01,
            0x95.toByte(), 0x06, 0x75, 0x08, 0x15, 0x00, 0x25, 0x65,
            0x05, 0x07, 0x19, 0x00, 0x29, 0x65, 0x81.toByte(), 0x00, 0xc0.toByte(),
            0x05, 0x01, 0x09, 0x02, 0xa1.toByte(), 0x01, 0x85.toByte(), RID_MOUSE,
            0x09, 0x01, 0xa1.toByte(), 0x00,
            0x05, 0x09, 0x19, 0x01, 0x29, 0x03, 0x15, 0x00, 0x25, 0x01,
            0x95.toByte(), 0x03, 0x75, 0x01, 0x81.toByte(), 0x02,
            0x95.toByte(), 0x01, 0x75, 0x05, 0x81.toByte(), 0x01,
            0x05, 0x01, 0x09, 0x30, 0x09, 0x31, 0x09, 0x38,
            0x15, 0x81.toByte(), 0x25, 0x7f, 0x75, 0x08, 0x95.toByte(), 0x03, 0x81.toByte(), 0x06,
            0xc0.toByte(), 0xc0.toByte(),
            0x05, 0x0c.toByte(), 0x09, 0x01, 0xa1.toByte(), 0x01, 0x85.toByte(), RID_CONS,
            0x15, 0x00, 0x25, 0x01, 0x75, 0x01, 0x95.toByte(), 0x07,
            0x0a, 0xcd.toByte(), 0x00, 0x0a, 0xe9.toByte(), 0x00,
            0x0a, 0xea.toByte(), 0x00, 0x0a, 0xe2.toByte(), 0x00,
            0x0a, 0xb6.toByte(), 0x00, 0x0a, 0xb5.toByte(), 0x00,
            0x0a, 0xb7.toByte(), 0x00, 0x81.toByte(), 0x02, 0xc0.toByte()
        )
    }

    private val binder = LocalBinder()
    private val handler = Handler(Looper.getMainLooper())

    private var adapter: BluetoothAdapter? = null
    private var hid: BluetoothHidDevice? = null
    var device: BluetoothDevice? = null; private set
    var registered = false; private set
    private var bound = false
    private var attempts = 0
    private var alive = false

    var onStatusChanged: ((Boolean, Boolean, String?) -> Unit)? = null
    var onDeviceChanged: ((BluetoothDevice?) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): HidForegroundService = this@HidForegroundService
    }

    override fun onBind(i: Intent?): IBinder = binder

    private val br = object : BroadcastReceiver() {
        override fun onReceive(c: android.content.Context?, i: Intent?) {
            when (i?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    if (i.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) boot()
                    else { alive = false; push() }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    if (i.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1) == BluetoothDevice.BOND_BONDED) boot()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        adapter = (getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
        NotificationChannel(CHANNEL_ID, "BT HID", NotificationManager.IMPORTANCE_LOW).also {
            getSystemService(NotificationManager::class.java).createNotificationChannel(it)
        }
        registerReceiver(br, IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        })
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int): Int {
        startForeground(NOTIFY_ID, note("Starting..."))
        handler.postDelayed({ boot() }, 500L)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        alive = false
        try { hid?.unregisterApp() } catch (_: Exception) {}
        try { unregisterReceiver(br) } catch (_: Exception) {}
        super.onDestroy()
    }

    fun reinit() {
        attempts = 0; bound = false; registered = false; device = null; hid = null; push(); boot()
    }

    private fun boot() {
        val a = adapter ?: return
        if (!a.isEnabled) { retry(5000L); return }
        alive = true
        if (bound && hid != null) { reg(); return }
        try {
            a.getProfileProxy(this, object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(p: Int, x: BluetoothProfile?) {
                    if (x is BluetoothHidDevice) { hid = x; bound = true; handler.post { reg() } }
                    else { bound = false; retry(3000L) }
                }
                override fun onServiceDisconnected(p: Int) {
                    bound = false; hid = null; registered = false; push(); retry(2000L)
                }
            }, 19)
        } catch (e: Exception) { retry(5000L) }
    }

    private fun reg() {
        val p = hid ?: run { boot(); return }
        val a = adapter
        if (a == null || !a.isEnabled) { retry(5000L); return }
        if (registered) { try { p.unregisterApp() } catch (_: Exception) {}; Thread.sleep(200); registered = false }

        val name = a.name?.takeIf { it.isNotBlank() } ?: SDP_NAME
        val sdp = BluetoothHidDeviceAppSdpSettings(name, SDP_DESC, SDP_PROV, BluetoothHidDevice.SUBCLASS1_COMBO, DESCRIPTOR)
        val qos = BluetoothHidDeviceAppQosSettings(BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT, 800, 9, 0, 1000, 1000)

        attempts++
        try {
            val ok = p.registerApp(sdp, qos, qos, mainExecutor, cb)
            if (ok) { registered = true; attempts = 0; push() }
            else retry(3000L)
        } catch (e: Exception) { retry(5000L) }
    }

    private fun retry(d: Long) {
        if (!alive) return
        handler.postDelayed({ boot() }, d.coerceIn(2000L, 30000L))
    }

    private val cb = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(dev: BluetoothDevice?, reg: Boolean) {
            registered = reg
            if (reg && dev != null) { device = dev; onDeviceChanged?.invoke(dev) }
            else if (!reg) { device = null; onDeviceChanged?.invoke(null); if (alive) retry(3000L) }
            push()
        }
        override fun onConnectionStateChanged(dev: BluetoothDevice?, s: Int) {
            if (s == BluetoothProfile.STATE_CONNECTED) { device = dev; onDeviceChanged?.invoke(dev); push() }
            else if (s == BluetoothProfile.STATE_DISCONNECTED && dev == device) {
                device = null; onDeviceChanged?.invoke(null); push()
            }
        }
        override fun onGetReport(d: BluetoothDevice?, t: Byte, id: Byte, sz: Int) {
            try { if (t.toInt() == 1) hid?.replyReport(d, t, id, ByteArray(8)) } catch (_: Exception) {}
        }
        override fun onSetReport(d: BluetoothDevice?, t: Byte, id: Byte, data: ByteArray?) {}
        override fun onInterruptData(d: BluetoothDevice?, id: Byte, data: ByteArray?) {}
    }

    private fun push() {
        onStatusChanged?.invoke(registered, device != null, null)
        val txt = if (device != null) "Connected: ${device?.name}" else if (registered) "Ready - scan BT on host for '${adapter?.name ?: SDP_NAME}'" else "Starting..."
        note(txt)
    }

    private fun note(txt: String): Notification {
        val n = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("BT KB & Mouse Pro")
            .setContentText(txt)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(PendingIntent.getActivity(this, 0,
                Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true)
            .build()
        try { getSystemService(NotificationManager::class.java).notify(NOTIFY_ID, n) } catch (_: Exception) {}
        return n
    }

    fun sendMouse(buttons: Int, dx: Int, dy: Int, scroll: Int = 0) {
        val h = hid ?: return; val d = device ?: return
        try { h.sendReport(d, RID_MOUSE.toInt(), byteArrayOf(buttons.toByte(), dx.toByte(), dy.toByte(), scroll.toByte())) } catch (_: Exception) {}
    }

    fun sendKeyboard(mods: Byte, keys: ByteArray = byteArrayOf()) {
        val h = hid ?: return; val d = device ?: return
        try { val r = ByteArray(8); r[0] = mods; keys.forEachIndexed { i, k -> if (i < 6) r[2 + i] = k }; h.sendReport(d, RID_KB.toInt(), r) } catch (_: Exception) {}
    }

    fun sendConsumer(code: Byte) {
        val h = hid ?: return; val d = device ?: return
        try { h.sendReport(d, RID_CONS.toInt(), byteArrayOf(code)) } catch (_: Exception) {}
    }
}
