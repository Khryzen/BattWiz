package com.bsu.battwiz

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var batteryLevelTextView: TextView
    private lateinit var chargingStatusTextView: TextView
    private lateinit var appsListView: ListView
    private lateinit var estimatedBatteryLifeTextView: TextView
    private lateinit var dischargeRateTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        batteryLevelTextView = findViewById(R.id.batteryLevelTextView)
        chargingStatusTextView = findViewById(R.id.chargingStatusTextView)
        appsListView = findViewById(R.id.appsListView)
        estimatedBatteryLifeTextView = findViewById(R.id.estimatedBatteryLifeTextView)
        dischargeRateTextView = findViewById(R.id.dischargeRateTextView)

        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateBatteryInfo(intent)
                if (hasUsageStatsPermission()) {
                    updateAppUsageStats()
                } else {
                    requestUsageStatsPermission()
                }
                val dischargeRate = getDischargeRatePerHour()
                dischargeRateTextView.text = dischargeRate.toString()
                estimateBatteryLife(intent)
            }
        }
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        }
    }

    private fun updateBatteryInfo(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = level * 100 / scale.toFloat()
        batteryLevelTextView.text = "Battery Level: $batteryPct%"

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        chargingStatusTextView.text = "Charging Status: ${if (isCharging) "Charging" else "Discharging"}"
    }

    private fun updateAppUsageStats() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60
        val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        val topAppsList = usageStats
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .map {
                try {
                    "${getAppNameFromPackageName(it.packageName)}: ${it.totalTimeInForeground / (1000 * 60)} minutes"
                } catch (e: PackageManager.NameNotFoundException) {
                    "${it.packageName}: ${it.totalTimeInForeground / (1000 * 60)} minutes"
                }
            }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, topAppsList)
        appsListView.adapter = adapter
    }

    private fun getAppNameFromPackageName(packageName: String): String {
        return try {
            val packageManager = applicationContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    private fun estimateBatteryLife(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = level * 100 / scale.toFloat()

        val dischargeRate = 10 // Placeholder: Assuming an average discharge rate of 10% per hour
        val estimatedHours = (batteryPct / dischargeRate).toInt()
        val estimatedMinutes = ((batteryPct % dischargeRate) * 60 / dischargeRate).toInt()

        estimatedBatteryLifeTextView.text = "Estimated Battery Life: $estimatedHours hours $estimatedMinutes minutes"
    }

    private val LAST_MEASUREMENT_TIME_KEY = "last_measurement_time"
    private val BATTERY_LEVEL_KEY = "battery_level"

    fun getDischargeRatePerHour(): Double {
//        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryStatus: Intent? = registerReceiver(null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val currentBatteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val currentTimeMillis = System.currentTimeMillis()

        // Calculate discharge rate
        val previousBatteryLevel = getPreviousBatteryLevel()
        val timeDiffHours = (currentTimeMillis - getLastMeasurementTimeMillis()) / (1000 * 60 * 60).toDouble()
        val dischargeRate = if (timeDiffHours > 0) (previousBatteryLevel - currentBatteryLevel) / timeDiffHours else 0.0

        // Update last measurement time and battery level
        saveBatteryLevel(currentBatteryLevel)
        saveLastMeasurementTime(currentTimeMillis)

        return dischargeRate
    }

    private fun saveBatteryLevel(level: Int) {
        val sharedPreferences = getSharedPreferences("battery_data", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt(BATTERY_LEVEL_KEY, level).apply()
    }

    private fun getPreviousBatteryLevel(): Int {
        val sharedPreferences = getSharedPreferences("battery_data", Context.MODE_PRIVATE)
        return sharedPreferences.getInt(BATTERY_LEVEL_KEY, 100) // Default value is 100
    }

    private fun saveLastMeasurementTime(timeMillis: Long) {
        val sharedPreferences = getSharedPreferences("battery_data", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong(LAST_MEASUREMENT_TIME_KEY, timeMillis).apply()
    }

    private fun getLastMeasurementTimeMillis(): Long {
        val sharedPreferences = getSharedPreferences("battery_data", Context.MODE_PRIVATE)
        return sharedPreferences.getLong(LAST_MEASUREMENT_TIME_KEY, System.currentTimeMillis())
    }
}