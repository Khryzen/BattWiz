package com.bsu.battwiz

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.provider.Settings
import androidx.core.app.NotificationCompat

class BrightnessMonitorService : Service() {
    companion object {
        const val CHANNEL_ID = "BrightnessMonitorServiceChannel"
        const val NOTIFICATION_ID = 1
    }

    private lateinit var brightnessObserver: ContentObserver

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification = createNotification("Monitoring screen brightness changes...")
        startForeground(NOTIFICATION_ID, notification)

        brightnessObserver = object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                val brightness = getScreenBrightness()
                val brightnessPercentage = (brightness / 255.0 * 100).toInt()
                Log.d("BrightnessMonitor", "Screen brightness changed to: $brightness ($brightnessPercentage%)")

                // Update the notification with the current brightness level as percentage
                val updatedNotification = createNotification("Screen brightness: $brightnessPercentage%")
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, updatedNotification)
            }
        }

        // Register the observer to listen for brightness changes
        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            brightnessObserver
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        contentResolver.unregisterContentObserver(brightnessObserver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Return null because this is an unbounded service
        return null
    }

    private fun getScreenBrightness(): Int {
        return try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            0
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Brightness Monitor Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Brightness Monitor Service")
            .setContentText(contentText)
//            .setSmallIcon(androidx.viewpager.R.drawable.notification_bg)
            .setSmallIcon(R.drawable.anbo)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }
}