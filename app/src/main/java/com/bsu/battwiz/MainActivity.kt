package com.bsu.battwiz

import android.Manifest
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    lateinit var context: Context
    // Inside your activity or fragment
    private val bluetoothAdapter: BluetoothAdapter? by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

//    Bluetooth
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_ENABLE_BT_PERMISSION = 2
    private val REQUEST_DISABLE_BT_PERMISSION = 3
    private lateinit var bluetoothSwitch: Switch
    private lateinit var wlanSwitch: Switch

    private lateinit var batteryLevelTextView: TextView
    private lateinit var chargingStatusTextView: TextView
    private lateinit var appsListView: ListView
    private lateinit var estimatedBatteryLifeTextView: TextView
    private lateinit var dischargeRateTextView: TextView
    private lateinit var batteryCapacityTextView: TextView
    private lateinit var brightnessSeekBar: SeekBar
    private lateinit var wifiManager: WifiManager
    private lateinit var closeAppsButton: Button
    private lateinit var startMonitoringButton: Button
    private lateinit var stopMonitoringButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        batteryLevelTextView = findViewById(R.id.batteryLevelTextView)
        chargingStatusTextView = findViewById(R.id.chargingStatusTextView)
        appsListView = findViewById(R.id.appsListView)
        estimatedBatteryLifeTextView = findViewById(R.id.estimatedBatteryLifeTextView)
        batteryCapacityTextView = findViewById(R.id.batteryCapacityTextView)
        bluetoothSwitch = findViewById(R.id.bluetoothSwitch)
        wlanSwitch = findViewById<Switch>(R.id.wlanSwitch)
        closeAppsButton = findViewById<Switch>(R.id.closeAppsButton)
        startMonitoringButton = findViewById(R.id.startMonitoringButton)
        stopMonitoringButton = findViewById(R.id.stopMonitoringButton)


        // Service
        startMonitoringButton.setOnClickListener{
            val startServiceIntent = Intent(this, BrightnessMonitorService::class.java)
            startService(startServiceIntent)
            val startMonitoringIntent = Intent(this, StatusMonitorService::class.java)
            startService(startMonitoringIntent)
            val monitoringIntent = Intent(this, MonitoringService::class.java)
            startService(monitoringIntent)
        }

        stopMonitoringButton.setOnClickListener{
            val stopBrightnessServiceIntent = Intent(this, BrightnessMonitorService::class.java)
            stopService(stopBrightnessServiceIntent)

            val stopStatusServiceIntent = Intent(this, StatusMonitorService::class.java)
            stopService(stopStatusServiceIntent)

            val stopMonitoringServiceIntent = Intent(this, MonitoringService::class.java)
            stopService(stopMonitoringServiceIntent)
        }

        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

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
        // Check if device supports Bluetooth
        if (bluetoothAdapter == null) {
            bluetoothSwitch.isEnabled = false
            return
        }
        updateBluetoothStatus()

        // Set listener for Bluetooth switch
        bluetoothSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableBluetooth()
            } else {
                disableBluetooth()
            }
        }

        // WLAN
        // Reflect the initial state of WLAN
        wlanSwitch.isChecked = isWlanEnabled()

        wlanSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                enableWlan()
            } else {
                disableWlan()
            }
        }
        closeAppsButton.setOnClickListener {
            closeBackgroundApps()
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

        if (!hasUsageStatsPermission()) {
            requestUsageStatsPermission()
        }
    }
    //Bluetooth
    private fun updateBluetoothStatus() {
        bluetoothSwitch.isChecked = bluetoothAdapter!!.isEnabled
    }
private fun enableBluetooth() {
    if (!bluetoothAdapter?.isEnabled!!) {
        // Check if the app has BLUETOOTH_ADMIN permission
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, enable Bluetooth
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            // Permission is not granted, request the permission
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_ADMIN), REQUEST_ENABLE_BT_PERMISSION)
        }
    }
    updateBluetoothStatus()
}
private fun disableBluetooth() {
    if (bluetoothAdapter?.isEnabled == true) {
        // Check if the app has BLUETOOTH_ADMIN permission
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, disable Bluetooth
            bluetoothAdapter?.disable()
            updateBluetoothStatus()
        } else {
            // Permission is not granted, request the permission
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_ADMIN), REQUEST_DISABLE_BT_PERMISSION)
        }
    } else {
        // Bluetooth is already disabled
        updateBluetoothStatus()
    }
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
            REQUEST_ENABLE_BT_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, try enabling Bluetooth again
                    enableBluetooth()
                } else {
                    // Permission denied
                    Toast.makeText(this, "Permission denied to enable Bluetooth", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_DISABLE_BT_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, try disabling Bluetooth again
                    disableBluetooth()
                } else {
                    // Permission denied
                    Toast.makeText(this, "Permission denied to disable Bluetooth", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun updateBatteryInfo(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = level * 100 / scale.toFloat()
        val isPresent = intent.getBooleanExtra("present", false)
        val bundle = intent.extras
        val str = bundle.toString()
        var volts: Int = 0
        var batteryLife = 0.0
        Log.i("Battery Info", str)


        if (isPresent) {
            volts = bundle!!.getInt("voltage")
        } else {
            Toast.makeText(this, "Battery not present!!!", Toast.LENGTH_SHORT).show()
        }

        batteryLevelTextView.text = "Battery Level: $batteryPct%"

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging =
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        chargingStatusTextView.text =
            "Charging Status: ${if (isCharging) "Charging" else "Discharging"}"

        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val capacity = (batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER))/1000
        val currentNow = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
//        val totalCapacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (currentNow < 0){
            batteryLife = capacity / ((currentNow * -1)*(1-0.05))
        }
        val hours = batteryLife.toInt()
        val minutes = (batteryLife * 60).toInt() % 60
        val seconds = (batteryLife * (60 * 60)).toInt() % 60
        if (!isCharging && currentNow < 0) {
            estimatedBatteryLifeTextView.text =
                "Estimated Battery Life: $hours hours $minutes minutes $seconds seconds"
        }
        if(isCharging) {
        estimatedBatteryLifeTextView.text =
            "Currently charging"
        }
        if(currentNow < 0) {
            estimatedBatteryLifeTextView.text =
                "Estimating"
        }

        batteryCapacityTextView.text = "Energy Counter: $capacity (mAh)\n Discharge current: $currentNow \n volts: $volts mV"

//        val dischargeRate = getDischargeRatePerHour()
//        dischargeRateTextView.text = dischargeRate.toString()
//        estimateBatteryLife(intent)
    }

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

//    private fun getAppNameFromPackageName(packageName: String): String {
//        return try {
//            val packageManager = applicationContext.packageManager
//            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
//            packageManager.getApplicationLabel(applicationInfo).toString()
//        } catch (e: PackageManager.NameNotFoundException) {
//            packageName
//        }
//    }

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

//    private fun estimateBatteryLife(intent: Intent) {
//        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
//        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
//        val batteryPct = level * 100 / scale.toFloat()
//
//        val dischargeRate = 10
//        val estimatedHours = (batteryPct / dischargeRate).toInt()
//        val estimatedMinutes = ((batteryPct % dischargeRate) * 60 / dischargeRate).toInt()
//
//        estimatedBatteryLifeTextView.text =
//            "Estimated Battery Life: $estimatedHours hours $estimatedMinutes minutes"
//    }

//    private val LAST_MEASUREMENT_TIME_KEY = "last_measurement_time"
//    private val BATTERY_LEVEL_KEY = "battery_level"

//    fun getDischargeRatePerHour(): Double {
////        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
//        val batteryStatus: Intent? = registerReceiver(
//            null,
//            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
//        )
//
//        val currentBatteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
//        val currentTimeMillis = System.currentTimeMillis()
//
//        // Calculate discharge rate
//        val previousBatteryLevel = getPreviousBatteryLevel()
//        val timeDiffHours =
//            (currentTimeMillis - getLastMeasurementTimeMillis()) / (1000 * 60 * 60).toDouble()
//        val dischargeRate =
//            if (timeDiffHours > 0) (previousBatteryLevel - currentBatteryLevel) / timeDiffHours else 0.0
//
//        // Update last measurement time and battery level
//        saveBatteryLevel(currentBatteryLevel)
//        saveLastMeasurementTime(currentTimeMillis)
//
//        return dischargeRate
//    }

//    private fun saveBatteryLevel(level: Int) {
//        val sharedPreferences = getSharedPreferences("battery_data", Context.MODE_PRIVATE)
//        sharedPreferences.edit().putInt(BATTERY_LEVEL_KEY, level).apply()
//    }
//
//    private fun getPreviousBatteryLevel(): Int {
//        val sharedPreferences = getSharedPreferences("battery_data", Context.MODE_PRIVATE)
//        return sharedPreferences.getInt(BATTERY_LEVEL_KEY, 100)
//    }
//
//    private fun saveLastMeasurementTime(timeMillis: Long) {
//        val sharedPreferences = getSharedPreferences("battery_data", Context.MODE_PRIVATE)
//        sharedPreferences.edit().putLong(LAST_MEASUREMENT_TIME_KEY, timeMillis).apply()
//    }
//
//    private fun getLastMeasurementTimeMillis(): Long {
//        val sharedPreferences = getSharedPreferences("battery_data", Context.MODE_PRIVATE)
//        return sharedPreferences.getLong(LAST_MEASUREMENT_TIME_KEY, System.currentTimeMillis())
//    }

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

    // WLAN
    private fun isWlanEnabled(): Boolean {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    private fun enableWlan() {
        if (!wifiManager.isWifiEnabled) {
            val success = wifiManager.setWifiEnabled(true)
            if (!success) {
                // Failed to enable WLAN
                Toast.makeText(this, "Failed to enable WLAN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disableWlan() {
        if (wifiManager.isWifiEnabled) {
            val success = wifiManager.setWifiEnabled(false)
            if (!success) {
                // Failed to disable WLAN
                Toast.makeText(this, "Failed to disable WLAN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Close background apps
    private fun closeBackgroundApps() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        // Get the list of running app processes
        val runningAppProcesses = activityManager.runningAppProcesses

        for (processInfo in runningAppProcesses) {
            // Exclude system apps and the current app itself
            if (processInfo.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND &&
                processInfo.processName != packageName) {
                activityManager.killBackgroundProcesses(processInfo.processName)
            }
        }
        val runningApps = getRunningApps()

        if (!isUsageStatsPermissionGranted()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            val foregroundApps = getForegroundApps()
            Log.i("Foreground Apps: ", foregroundApps.toString())
        }
        Log.i("Running Apps: ", runningApps.toString())
        showDelayedToast()
    }

    private fun getRunningApps(): List<String> {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses = activityManager.runningAppProcesses
        val runningApps = mutableListOf<String>()

        runningAppProcesses?.let {
            for (processInfo in it) {
                runningApps.add(processInfo.processName)
            }
        }

        // For devices with API level 21 and above, you can also use getRunningServices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)
            for (serviceInfo in runningServices) {
                runningApps.add(serviceInfo.service.className)
            }
        }

        return runningApps
    }

    private fun showDelayedToast() {
        val delayMillis: Long = 2500
        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(this, "Background apps closed", Toast.LENGTH_SHORT).show()
        }, delayMillis)
    }

    private fun isUsageStatsPermissionGranted(): Boolean {
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOpsManager.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), packageName)
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
    private fun getForegroundApps(): List<String> {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60 // One hour ago

        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        val foregroundApps = mutableListOf<String>()
        if (usageStatsList.isNotEmpty()) {
            val sortedStats = usageStatsList.sortedByDescending { it.lastTimeUsed }
            for (usageStats in sortedStats) {
                if (isAppInForeground(usageStats)) {
                    foregroundApps.add(usageStats.packageName)
                }
            }
        }

        return foregroundApps
    }

    private fun isAppInForeground(usageStats: UsageStats): Boolean {
        val currentTime = System.currentTimeMillis()
        return usageStats.lastTimeUsed > currentTime - 1000 * 10 // Within the last 10 seconds
    }

}