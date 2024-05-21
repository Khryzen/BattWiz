package com.bsu.battwiz

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat

class StatusMonitorService : Service() {
//    companion object {
//        const val CHANNEL_ID = "StatusMonitorServiceChannel"
//        const val NOTIFICATION_ID = 2
//    }
//
//    private lateinit var wifiManager: WifiManager
//    private lateinit var bluetoothAdapter: BluetoothAdapter
//    private lateinit var locationManager: LocationManager
//
//    private val wifiReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            val wifiState = intent?.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
//            val status = when (wifiState) {
//                WifiManager.WIFI_STATE_ENABLED -> "enabled"
//                WifiManager.WIFI_STATE_DISABLED -> "disabled"
//                else -> "unknown"
//            }
//            updateNotification("Wi-Fi is $status")
//        }
//    }
//
//    private val bluetoothReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
//            val status = when (state) {
//                BluetoothAdapter.STATE_ON -> "enabled"
//                BluetoothAdapter.STATE_OFF -> "disabled"
//                else -> "unknown"
//            }
//            updateNotification("Bluetooth is $status")
//        }
//    }
//
//    private val locationReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent?) {
//            val isEnabled = isLocationEnabled()
//            val status = if (isEnabled) "enabled" else "disabled"
//            updateNotification("Location is $status")
//        }
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        createNotificationChannel()
//        val notification = createNotification("Monitoring system statuses...")
//        startForeground(NOTIFICATION_ID, notification)
//
//        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
//
//        registerReceivers()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        unregisterReceivers()
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//    }
//
//    private fun registerReceivers() {
//        registerReceiver(wifiReceiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
//        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
//        registerReceiver(locationReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
//    }
//
//    private fun unregisterReceivers() {
//        unregisterReceiver(wifiReceiver)
//        unregisterReceiver(bluetoothReceiver)
//        unregisterReceiver(locationReceiver)
//    }
//
//    private fun isLocationEnabled(): Boolean {
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            locationManager.isLocationEnabled
//        } else {
//            try {
//                Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF
//            } catch (e: Settings.SettingNotFoundException) {
//                false
//            }
//        }
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val serviceChannel = NotificationChannel(
//                CHANNEL_ID,
//                "Status Monitor Service Channel",
//                NotificationManager.IMPORTANCE_DEFAULT
//            )
//            val manager = getSystemService(NotificationManager::class.java)
//            manager.createNotificationChannel(serviceChannel)
//        }
//    }
//
//    private fun createNotification(contentText: String): Notification {
//        return NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle("Status Monitor Service")
//            .setContentText(contentText)
//            .setSmallIcon(androidx.viewpager.R.drawable.notification_bg)
//            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            .build()
//    }
//
//    private fun updateNotification(contentText: String) {
//        val notification = createNotification(contentText)
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.notify(NOTIFICATION_ID, notification)
//    }

    companion object {
        const val CHANNEL_ID = "StatusMonitorServiceChannel"
        const val NOTIFICATION_ID = 2
    }

    private lateinit var wifiManager: WifiManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var locationManager: LocationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var notificationManager: NotificationManager? = null // Initialize as nullable
    private lateinit var cameraManager: CameraManager

    private var torchStatus: String = "unknown"

    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val wifiState = intent?.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
            val status = when (wifiState) {
                WifiManager.WIFI_STATE_ENABLED -> "enabled"
                WifiManager.WIFI_STATE_DISABLED -> "disabled"
                else -> "unknown"
            }
            if (status == "enabled"){
                updateNotification("Turn off WLAN to save battery")
            }else{
                updateNotification("")
            }
//            updateNotification("Wi-Fi is $status")
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
            val status = when (state) {
                BluetoothAdapter.STATE_ON -> "enabled"
                BluetoothAdapter.STATE_OFF -> "disabled"
                else -> "unknown"
            }
            if (status == "enabled"){
                updateNotification("Turn off bluetooth to save battery")
            }else{
                updateNotification("")
            }
        }
    }

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isEnabled = isLocationEnabled()
//            val status = if (isEnabled) "enabled" else "disabled"
            if (isEnabled){
                updateNotification("Turn off GPS to save battery")
            }else{
                updateNotification("")
            }

        }
    }

    private val torchStateCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            torchStatus = if (enabled) "on" else "off"
            updateNotification("Torch is $torchStatus")
        }
    }


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        notificationBuilder = createNotificationBuilder("Monitoring system statuses...")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.registerTorchCallback(torchStateCallback, null)
        registerReceivers()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.unregisterTorchCallback(torchStateCallback)
        unregisterReceivers()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun registerReceivers() {
        registerReceiver(wifiReceiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        registerReceiver(locationReceiver, IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION))
    }

    private fun unregisterReceivers() {
        unregisterReceiver(wifiReceiver)
        unregisterReceiver(bluetoothReceiver)
        unregisterReceiver(locationReceiver)
    }

    private fun isLocationEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            try {
                Settings.Secure.getInt(contentResolver, Settings.Secure.LOCATION_MODE) != Settings.Secure.LOCATION_MODE_OFF
            } catch (e: Settings.SettingNotFoundException) {
                false
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Status Monitor Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotificationBuilder(contentText: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Status Monitor Service")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.anbo)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    private fun updateNotification(contentText: String) {
        if (torchStatus == "on"){
            val notificationText = "$contentText\nTorch is $torchStatus"
            notificationBuilder.setContentText(notificationText)
        }else{
            notificationBuilder.setContentText(contentText)
        }
        notificationManager?.notify(NOTIFICATION_ID, notificationBuilder.build())
    }
}