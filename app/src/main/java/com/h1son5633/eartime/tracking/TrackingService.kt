package com.h1son5633.eartime.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.h1son5633.eartime.EarTimeApp
import com.h1son5633.eartime.MainActivity
import com.h1son5633.eartime.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that turns Bluetooth audio-profile connection
 * events into wearing sessions. Sources of truth, in order:
 *  1. A2DP / HEADSET connection-state broadcasts (primary)
 *  2. Profile proxies on start (devices already connected)
 *  3. ACL_DISCONNECTED as a safety net for a missed profile event
 */
class TrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var manager: SessionManager
    private var a2dpProxy: BluetoothProfile? = null
    private var headsetProxy: BluetoothProfile? = null
    private var receiverRegistered = false

    private val heartbeat = object : Runnable {
        override fun run() {
            scope.launch { manager.heartbeat() }
            handler.postDelayed(this, 30_000)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device: BluetoothDevice? =
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            when (intent.action) {
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    if (device == null) return
                    val state = intent.getIntExtra(
                        BluetoothProfile.EXTRA_STATE, BluetoothProfile.STATE_DISCONNECTED
                    )
                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        scope.launch { manager.onConnected(device.safeName(), device.address) }
                    } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                        scope.launch { manager.onDisconnected(device.address) }
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    if (device != null) scope.launch { manager.onDisconnected(device.address) }
                }
            }
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.A2DP) a2dpProxy = proxy
            if (profile == BluetoothProfile.HEADSET) headsetProxy = proxy
            syncConnectedDevices()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.A2DP) a2dpProxy = null
            if (profile == BluetoothProfile.HEADSET) headsetProxy = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        manager = (application as EarTimeApp).sessionManager
        startForegroundNotification()
        registerBtReceiver()
        connectProfileProxies()
        handler.postDelayed(heartbeat, 30_000)
        // Give the profile proxies a moment, then close stale sessions
        // whose device did not come back.
        handler.postDelayed({ syncConnectedDevices(reconcile = true) }, 2_500)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        syncConnectedDevices()
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(heartbeat)
        if (receiverRegistered) {
            runCatching { unregisterReceiver(receiver) }
            receiverRegistered = false
        }
        val adapter = bluetoothAdapter()
        if (adapter != null) {
            a2dpProxy?.let { runCatching { adapter.closeProfileProxy(BluetoothProfile.A2DP, it) } }
            headsetProxy?.let { runCatching { adapter.closeProfileProxy(BluetoothProfile.HEADSET, it) } }
        }
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun bluetoothAdapter(): BluetoothAdapter? =
        (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private fun hasConnectPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    private fun BluetoothDevice.safeName(): String =
        if (hasConnectPermission()) (runCatching { name }.getOrNull() ?: address) else address

    private fun registerBtReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
    }

    private fun connectProfileProxies() {
        val adapter = bluetoothAdapter() ?: return
        if (!hasConnectPermission()) return
        runCatching { adapter.getProfileProxy(this, profileListener, BluetoothProfile.A2DP) }
        runCatching { adapter.getProfileProxy(this, profileListener, BluetoothProfile.HEADSET) }
    }

    private fun connectedAudioDevices(): Set<BluetoothDevice> {
        if (!hasConnectPermission()) return emptySet()
        val out = HashSet<BluetoothDevice>()
        for (proxy in listOfNotNull(a2dpProxy, headsetProxy)) {
            runCatching { out.addAll(proxy.connectedDevices) }
        }
        return out
    }

    private fun syncConnectedDevices(reconcile: Boolean = false) {
        val devices = connectedAudioDevices()
        scope.launch {
            for (d in devices) manager.onConnected(d.safeName(), d.address)
            if (reconcile) manager.reconcile(devices.map { it.address }.toSet())
        }
    }

    private fun startForegroundNotification() {
        val channelId = "tracking"
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(channelId, "佩戴记录", NotificationManager.IMPORTANCE_LOW)
        )
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_headphones)
            .setContentTitle("耳机佩戴统计运行中")
            .setContentText("连接蓝牙耳机时自动记录佩戴时长")
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(
            this, 1001, notification,
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        )
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, TrackingService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
