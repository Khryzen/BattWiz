package com.bsu.battwiz

import android.app.*
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat

class MonitoringService : Service() {
    private val handler = Handler()
    companion object {
        const val NOTIFICATION_ID = 3
        const val CHANNEL_ID = "MonitoringServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startMonitoring()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startMonitoring() {
        // You can trigger notifications directly here without using intervals
//        sendNotifications()

        handler.postDelayed({
            sendNotifications()
            startMonitoring()
        }, 1 * 60 * 60 * 1000)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitoring Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotifications() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var notification = "Running services: "

        if (isLocationEnabled(applicationContext)) {
//            showNotification(applicationContext, notificationManager, "Please turn off Location to save battery")
            notification += "GPS "
        }

        if (isBluetoothEnabled()) {
//            showNotification(applicationContext, notificationManager, "Please turn off Bluetooth to save battery")
            notification += "Bluetooth "
        }

        if (isWifiEnabled(applicationContext)) {
//            showNotification(applicationContext, notificationManager, "Please turn off WLAN to save battery")
            notification += "WLAN "
        }

        showNotification(applicationContext, notificationManager, notification)
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    private fun isWifiEnabled(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    private fun showNotification(context: Context, notificationManager: NotificationManager, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Monitoring Service")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

class MonitoringReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (isLocationEnabled(context)) {
                showNotification(context, notificationManager, "Please turn off Location to save battery")
            }

            if (isBluetoothEnabled()) {
                showNotification(context, notificationManager, "Please turn off Bluetooth to save battery")
            }

            if (isWifiEnabled(context)) {
                showNotification(context, notificationManager, "Please turn off WLAN to save battery")
            }
        }
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    private fun isWifiEnabled(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    private fun showNotification(context: Context, notificationManager: NotificationManager, message: String) {
        val notification = NotificationCompat.Builder(context, MonitoringService.CHANNEL_ID)
            .setContentTitle("Monitoring Service")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(MonitoringService.NOTIFICATION_ID, notification)
    }
}