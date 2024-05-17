package com.bsu.battwiz

import android.Manifest
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
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.os.Handler
import android.widget.SeekBar
import android.bluetooth.BluetoothAdapter
import android.widget.Switch
import android.widget.Toast
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    lateinit var context: Context

//    Bluetooth
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_ENABLE_BT_PERMISSION = 2
    private val REQUEST_DISABLE_BT_PERMISSION = 3
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private lateinit var bluetoothStatusLabel: TextView
    private lateinit var bluetoothSwitch: Switch

    private lateinit var batteryLevelTextView: TextView
    private lateinit var chargingStatusTextView: TextView
    private lateinit var appsListView: ListView
    private lateinit var estimatedBatteryLifeTextView: TextView
    private lateinit var dischargeRateTextView: TextView
    private lateinit var batteryCapacityTextView: TextView
    private lateinit var brightnessSeekBar: SeekBar

    private val handler = Handler()
    private val updateIntervalMillis = 1000L // 1 second interval
    private lateinit var runnable: Runnable


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        batteryLevelTextView = findViewById(R.id.batteryLevelTextView)
        chargingStatusTextView = findViewById(R.id.chargingStatusTextView)
        appsListView = findViewById(R.id.appsListView)
        estimatedBatteryLifeTextView = findViewById(R.id.estimatedBatteryLifeTextView)
        dischargeRateTextView = findViewById(R.id.dischargeRateTextView)
        batteryCapacityTextView = findViewById(R.id.batteryCapacityTextView)
        bluetoothStatusLabel = findViewById(R.id.bluetoothStatusLabel)
        bluetoothSwitch = findViewById(R.id.bluetoothSwitch)

        context = applicationContext
        // Brightness control
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar)
        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    setBrightness(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Bluetooth
        // Initialize BluetoothAdapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Check if device supports Bluetooth
        if (bluetoothAdapter == null) {
            bluetoothStatusLabel.text = "Bluetooth is not supported on this device"
            bluetoothSwitch.isEnabled = false
            return
        }
        updateBluetoothStatus()

        // Set listener for Bluetooth switch
        bluetoothSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // User wants to enable Bluetooth
                enableBluetooth()
            } else {
                // User wants to disable Bluetooth
                disableBluetooth()
            }
        }

        if (hasUsageStatsPermission()) {
            updateAppUsageStats()
        } else {
            requestUsageStatsPermission()
        }
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                updateBatteryInfo(intent)
            }
        }
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Initialize the runnable
        runnable = object : Runnable {
            override fun run() {
                // Call your function or perform any task here
                val dischargeRate = getDischargeRatePerHour()
                dischargeRateTextView.text = dischargeRate.toString()
                estimateBatteryLife(intent)

                // Schedule the next execution
                handler.postDelayed(this, updateIntervalMillis)
            }
        }

        // Start the periodic task
        handler.post(runnable)

        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the periodic task when the activity is destroyed to avoid memory leaks
        handler.removeCallbacks(runnable)
    }

    //Bluetooth
    private fun updateBluetoothStatus() {
        if (bluetoothAdapter.isEnabled) {
            bluetoothStatusLabel.text = "Bluetooth is enabled"
            bluetoothSwitch.isChecked = true
        } else {
            bluetoothStatusLabel.text = "Bluetooth is disabled"
            bluetoothSwitch.isChecked = false
        }
    }

    private fun enableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            // Check if the app has BLUETOOTH_ADMIN permission
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted, enable Bluetooth
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                // Permission is not granted, request the permission
                requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_ADMIN), REQUEST_ENABLE_BT_PERMISSION)
            }
        }
    }
    private fun disableBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            // Check if the app has BLUETOOTH_ADMIN permission
            if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted, disable Bluetooth
                bluetoothAdapter.disable()
            } else {
                // Permission is not granted, request the permission
                requestPermissions(arrayOf(android.Manifest.permission.BLUETOOTH_ADMIN), REQUEST_DISABLE_BT_PERMISSION)
            }
        }
        updateBluetoothStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                updateBluetoothStatus()
            } else {
                Toast.makeText(this, "Permission to enable Bluetooth was denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_DISABLE_BT_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, disable Bluetooth
                    disableBluetooth()
                } else {
                    // Permission denied, show a message or handle accordingly
                    Toast.makeText(this, "Permission to disable Bluetooth was denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateBatteryInfo(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = level * 100 / scale.toFloat()
        batteryLevelTextView.text = "Battery Level: $batteryPct%"

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging =
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        chargingStatusTextView.text =
            "Charging Status: ${if (isCharging) "Charging" else "Discharging"}"

        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val capacity = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val chargeRemaining = batteryManager.computeChargeTimeRemaining()

        batteryCapacityTextView.text = "Energy Counter: $capacity microampere-hours (uAh)/ Charge: $chargeRemaining"
    }

//    private fun updateAppUsageStats() {
//        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
//        val endTime = System.currentTimeMillis()
//        val startTime = endTime - 1000 * 60 * 60
//        val usageStats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
//
//        val topAppsList = usageStats
//            .filter { it.totalTimeInForeground > 0 }
//            .sortedByDescending { it.totalTimeInForeground }
//            .map {
//                try {
//                    "${getAppNameFromPackageName(it.packageName)}: ${it.totalTimeInForeground / (1000 * 60)} minutes"
//                } catch (e: PackageManager.NameNotFoundException) {
//                    "${it.packageName}: ${it.totalTimeInForeground / (1000 * 60)} minutes"
//                }
//            }
//
//        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, topAppsList)
//        appsListView.adapter = adapter
//    }

    private fun updateAppUsageStats() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60 // Set the time range for the past hour

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST, startTime, endTime
        )

        // Map the package names of apps that have been in the foreground to their total usage time
        val appUsageMap = mutableMapOf<String, Long>()
        for (usageStats in usageStatsList) {
            val packageName = usageStats.packageName
            val totalTimeInForeground = usageStats.totalTimeInForeground
            if (totalTimeInForeground > 0) {
                appUsageMap[packageName] = totalTimeInForeground
            }
        }

        // Sort the apps by their total usage time
        val sortedAppUsageList = appUsageMap.toList().sortedByDescending { (_, value) -> value }

        // Display the top apps and their usage time
        val topAppsList = mutableListOf<String>()
        val packageManager = packageManager
        for ((packageName, usageTime) in sortedAppUsageList) {
            try {
                val appName = packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA))
                topAppsList.add("$appName: ${usageTime / (1000 * 60)} minutes")
            } catch (e: PackageManager.NameNotFoundException) {
                // Handle the case where the app's name cannot be found
                topAppsList.add("$packageName: ${usageTime / (1000 * 60)} minutes")
            }
        }

        // Set up the ListView to display the top apps
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
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
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

        estimatedBatteryLifeTextView.text =
            "Estimated Battery Life: $estimatedHours hours $estimatedMinutes minutes"
    }

    private val LAST_MEASUREMENT_TIME_KEY = "last_measurement_time"
    private val BATTERY_LEVEL_KEY = "battery_level"

    fun getDischargeRatePerHour(): Double {
//        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryStatus: Intent? = registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val currentBatteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val currentTimeMillis = System.currentTimeMillis()

        // Calculate discharge rate
        val previousBatteryLevel = getPreviousBatteryLevel()
        val timeDiffHours =
            (currentTimeMillis - getLastMeasurementTimeMillis()) / (1000 * 60 * 60).toDouble()
        val dischargeRate =
            if (timeDiffHours > 0) (previousBatteryLevel - currentBatteryLevel) / timeDiffHours else 0.0

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
        return sharedPreferences.getInt(BATTERY_LEVEL_KEY, 100)
    }

    private fun saveLastMeasurementTime(timeMillis: Long) {
        val sharedPreferences = getSharedPreferences("battery_data", Context.MODE_PRIVATE)
        sharedPreferences.edit().putLong(LAST_MEASUREMENT_TIME_KEY, timeMillis).apply()
    }

    private fun getLastMeasurementTimeMillis(): Long {
        val sharedPreferences = getSharedPreferences("battery_data", Context.MODE_PRIVATE)
        return sharedPreferences.getLong(LAST_MEASUREMENT_TIME_KEY, System.currentTimeMillis())
    }

    override fun onResume() {
        super.onResume()
    }

    private fun setBrightness(brightness: Int) {
        if (Settings.System.canWrite(context)) {
            // Set system brightness
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            ) // Disable auto brightness
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightness
            ) // Set brightness level
        } else {
            // Request write settings permission
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            startActivity(intent)
        }
    }
}