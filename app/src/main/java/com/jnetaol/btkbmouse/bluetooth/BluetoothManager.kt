package com.jnetaol.btkbmouse.bluetooth

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.jnetaol.btkbmouse.logger.DebugLogger
import kotlinx.coroutines.flow.*

data class DiscoveredDevice(val device: BluetoothDevice, val rssi: Short = 0, val name: String = "Unknown")

data class ConnectionState(
    val isConnected: Boolean = false, val deviceName: String = "", val deviceAddress: String = "",
    val isHidRegistered: Boolean = false, val isHidConnected: Boolean = false, val error: String? = null
)

class BluetoothManager(private val app: Application) {
    private val TAG = "BTManager"
    private var hidService: HidForegroundService? = null
    private var svcBound = false

    private val adapter: BluetoothAdapter? by lazy {
        (app.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
    }

    private val _paired = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val pairedDevices: StateFlow<List<DiscoveredDevice>> = _paired.asStateFlow()
    private val _discovered = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discovered.asStateFlow()
    private val _conn = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _conn.asStateFlow()
    private val _scan = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _scan.asStateFlow()
    private val _btOn = MutableStateFlow(false)
    val isBluetoothEnabled: StateFlow<Boolean> = _btOn.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var scanStop: Runnable? = null
    private var hidConn: ServiceConnection? = null

    private val scanRx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val d = i.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE) ?: return
                    if (d.name != null) {
                        val cur = _discovered.value.toMutableList()
                        cur.removeAll { it.device.address == d.address }
                        cur.add(DiscoveredDevice(d, i.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0), d.name))
                        _discovered.value = cur.sortedByDescending { it.rssi }
                    }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    if (i.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1) in listOf(BluetoothDevice.BOND_BONDED, BluetoothDevice.BOND_NONE))
                        refreshPaired()
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    _btOn.value = i.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON
                }
            }
        }
    }

    init {
        _btOn.value = adapter?.isEnabled == true
        try { app.registerReceiver(scanRx, IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }) } catch (_: Exception) {}

        hidConn = object : ServiceConnection {
            override fun onServiceConnected(n: ComponentName?, b: IBinder?) {
                hidService = (b as HidForegroundService.LocalBinder).getService()
                svcBound = true
                hidService?.onStatusChanged = { reg, conn, err ->
                    _conn.value = _conn.value.copy(isHidRegistered = reg, isHidConnected = conn, isConnected = conn, error = err)
                }
                hidService?.onDeviceChanged = { dev ->
                    _conn.value = _conn.value.copy(isConnected = dev != null, isHidConnected = dev != null,
                        deviceName = dev?.name ?: "", deviceAddress = dev?.address ?: "")
                }
            }
            override fun onServiceDisconnected(n: ComponentName?) { hidService = null; svcBound = false }
        }
        val i = Intent(app, HidForegroundService::class.java)
        app.startForegroundService(i)
        app.bindService(i, hidConn!!, Context.BIND_AUTO_CREATE)
    }

    fun enableBluetooth() = try { adapter?.enable(); true } catch (_: Exception) { false }

    fun refreshPaired() {
        try { _paired.value = adapter?.bondedDevices?.map { DiscoveredDevice(it, 0, it.name ?: "Unknown") } ?: emptyList() } catch (_: Exception) {}
    }

    fun refreshPairedDevices() = refreshPaired()

    fun startDiscovery() = try {
        _scan.value = true; _discovered.value = emptyList()
        val ok = adapter?.startDiscovery() ?: false
        if (!ok) _scan.value = false
        val r = Runnable { stopDiscovery() }; scanStop = r; handler.postDelayed(r, 30000L)
        ok
    } catch (_: Exception) { _scan.value = false; false }

    fun stopDiscovery() = try { adapter?.cancelDiscovery(); _scan.value = false } catch (_: Exception) {}

    @SuppressLint("MissingPermission")
    fun pairDevice(d: BluetoothDevice) { try { if (d.bondState == BluetoothDevice.BOND_NONE) d.createBond() } catch (_: Exception) {} }

    fun connectDevice(d: BluetoothDevice) { hidService?.reinit() }
    fun disconnect() { _conn.value = ConnectionState(isHidRegistered = hidService?.registered ?: false) }
    fun retryHidRegistration() { hidService?.reinit() }

    fun sendMouseReport(buttons: Int, dx: Float, dy: Float, scroll: Float = 0f) {
        hidService?.sendMouse(buttons, dx.toInt().coerceIn(-127, 127), dy.toInt().coerceIn(-127, 127), scroll.toInt().coerceIn(-127, 127))
    }

    fun sendKeyPress(code: Byte, modifiers: Byte = 0) {
        hidService?.sendKeyboard(modifiers, byteArrayOf(code))
        handler.postDelayed({ hidService?.sendKeyboard(0) }, 60)
    }

    fun sendTextString(text: String) {
        var d = 0L
        for (c in text) {
            val shift = c in shiftMap; val u = (if (shift) shiftMap[c] else keyMap[c]) ?: continue
            handler.postDelayed({ sendKeyPress(u.toByte(), if (shift) 0x02 else 0x00) }, d)
            d += if (text.length > 10) 20 else 80
        }
    }

    fun sendMouseLeftClick(press: Boolean) { hidService?.sendMouse(if (press) 1 else 0, 0, 0) }
    fun sendMouseRightClick(press: Boolean) { hidService?.sendMouse(if (press) 2 else 0, 0, 0) }
    fun sendMouseMiddleClick(press: Boolean) { hidService?.sendMouse(if (press) 4 else 0, 0, 0) }
    fun sendMouseWheel(scroll: Float) { hidService?.sendMouse(0, 0, 0, scroll.toInt().coerceIn(-127, 127)) }

    fun sendMediaKey(key: String) {
        val m = mapOf("play_pause" to 0xcd, "volume_up" to 0xe9, "volume_down" to 0xea, "mute" to 0xe2,
            "next_track" to 0xb6, "prev_track" to 0xb5, "stop" to 0xb7)
        m[key]?.let { hidService?.sendConsumer(it.toByte()) }
    }

    fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        try { app.unregisterReceiver(scanRx) } catch (_: Exception) {}
        hidConn?.let { try { app.unbindService(it) } catch (_: Exception) {} }
    }

    val keyMap = mapOf('a' to 4,'b' to 5,'c' to 6,'d' to 7,'e' to 8,'f' to 9,'g' to 10,'h' to 11,'i' to 12,'j' to 13,'k' to 14,'l' to 15,'m' to 16,'n' to 17,'o' to 18,'p' to 19,'q' to 20,'r' to 21,'s' to 22,'t' to 23,'u' to 24,'v' to 25,'w' to 26,'x' to 27,'y' to 28,'z' to 29,'1' to 30,'2' to 31,'3' to 32,'4' to 33,'5' to 34,'6' to 35,'7' to 36,'8' to 37,'9' to 38,'0' to 39,'\n' to 40,' ' to 44,'-' to 45,'=' to 46,'[' to 47,']' to 48,'\\' to 49,';' to 51,'\'' to 52,',' to 54,'.' to 55,'/' to 56,'\b' to 42,'\t' to 43)
    val shiftMap = mapOf('A' to 4,'B' to 5,'C' to 6,'D' to 7,'E' to 8,'F' to 9,'G' to 10,'H' to 11,'I' to 12,'J' to 13,'K' to 14,'L' to 15,'M' to 16,'N' to 17,'O' to 18,'P' to 19,'Q' to 20,'R' to 21,'S' to 22,'T' to 23,'U' to 24,'V' to 25,'W' to 26,'X' to 27,'Y' to 28,'Z' to 29,'!' to 30,'@' to 31,'#' to 32,'$' to 33,'%' to 34,'^' to 35,'&' to 36,'*' to 37,'(' to 38,')' to 39,'_' to 45,'+' to 46,'{' to 47,'}' to 48,'|' to 49,':' to 51,'"' to 52,'<' to 54,'>' to 55,'?' to 56)
}
