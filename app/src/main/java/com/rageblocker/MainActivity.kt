package com.rageblocker

import android.Manifest
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.rageblocker.data.model.AppInfo
import com.rageblocker.data.model.MonitoredApp
import com.rageblocker.data.repository.PreferencesRepository
import com.rageblocker.databinding.ActivityMainBinding
import com.rageblocker.service.UsageMonitorService
import com.rageblocker.adapter.AppSelectionAdapter
import com.rageblocker.utils.PermissionUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var appSelectionAdapter: AppSelectionAdapter

    private val usageStatsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (PermissionUtils.hasUsageStatsPermission(this)) {
            checkAndRequestOverlayPermission()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (PermissionUtils.canDrawOverlays(this)) {
            checkBatteryOptimization()
        }
    }

    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        requestNotificationPermissionIfNeeded()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Continue to setup regardless of notification permission result
        setupUI()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        preferencesRepository = PreferencesRepository(this)
        
        if (savedInstanceState == null) {
            checkPermissionsAndSetup()
        } else {
            setupUI()
        }
    }

    private fun checkPermissionsAndSetup() {
        when {
            !PermissionUtils.hasUsageStatsPermission(this) -> {
                showPermissionDialog("Usage Stats", "This app needs usage stats permission to monitor your app usage time.") {
                    usageStatsPermissionLauncher.launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            }
            !PermissionUtils.canDrawOverlays(this) -> {
                showPermissionDialog("Overlay", "This app needs overlay permission to display blocking screens.") {
                    overlayPermissionLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = android.net.Uri.parse("package:$packageName")
                        }
                    )
                }
            }
            !PermissionUtils.isIgnoringBatteryOptimizations(this) -> {
                showBatteryOptimizationDialog()
            }
            else -> {
                setupUI()
            }
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (!PermissionUtils.canDrawOverlays(this)) {
            showPermissionDialog("Overlay", "This app needs overlay permission to display blocking screens.") {
                overlayPermissionLauncher.launch(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                )
            }
        } else {
            checkBatteryOptimization()
        }
    }

    private fun checkBatteryOptimization() {
        if (!PermissionUtils.isIgnoringBatteryOptimizations(this)) {
            showBatteryOptimizationDialog()
        } else {
            requestNotificationPermissionIfNeeded()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        setupUI()
    }

    private fun showBatteryOptimizationDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.battery_optimization_title))
            .setMessage(getString(R.string.battery_optimization_message))
            .setPositiveButton("Disable") { _, _ ->
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = android.net.Uri.parse("package:$packageName")
                }
                batteryOptimizationLauncher.launch(intent)
            }
            .setNegativeButton("Skip") { _, _ ->
                setupUI()
            }
            .show()
    }

    private fun showPermissionDialog(title: String, message: String, onGrant: () -> Unit) {
        val spannableMessage = SpannableString(message + "\n\nClick here to go to settings.")
        Linkify.addLinks(spannableMessage, Linkify.ALL)
        
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(spannableMessage)
            .setPositiveButton(getString(R.string.grant_permission)) { _, _ ->
                onGrant()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                finish()
            }
            .show()
            .also { dialog ->
                dialog.findViewById<android.widget.TextView>(android.R.id.message)?.movementMethod =
                    LinkMovementMethod.getInstance()
            }
    }

    private fun setupUI() {
        setupRecyclerView()
        setupToggleButton()
    }

    private fun setupRecyclerView() {
        appSelectionAdapter = AppSelectionAdapter { appInfo, isSelected ->
            // Handle app selection
        }
        
        binding.rvApps.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = appSelectionAdapter
        }
        
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        val apps = getInstalledApps()
        appSelectionAdapter.submitList(apps)
        
        // Pre-select previously selected apps
        val monitoredApps = preferencesRepository.getMonitoredApps()
        val selectedPackages = monitoredApps.filter { it.isMonitored }.map { it.packageName }.toSet()
        appSelectionAdapter.selectPackages(selectedPackages)
    }

    private fun getInstalledApps(): List<AppInfo> {
        val packageManager = packageManager
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
        return installedApps
            .filter { appInfo ->
                // Filter out system apps and our own app
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && 
                appInfo.packageName != packageName
            }
            .sortedBy { it.loadLabel(packageManager).toString() }
            .map { appInfo ->
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = appInfo.loadLabel(packageManager).toString(),
                    icon = appInfo.loadIcon(packageManager)
                )
            }
    }

    private fun setupToggleButton() {
        updateToggleButton()
        
        binding.btnToggleMonitoring.setOnClickListener {
            if (preferencesRepository.isMonitoringActive()) {
                stopMonitoring()
            } else {
                startMonitoring()
            }
        }
    }

    private fun updateToggleButton() {
        val isActive = preferencesRepository.isMonitoringActive()
        binding.btnToggleMonitoring.text = if (isActive) {
            getString(R.string.stop_monitoring)
        } else {
            getString(R.string.start_monitoring)
        }
        binding.btnToggleMonitoring.setBackgroundColor(
            ContextCompat.getColor(
                this,
                if (isActive) android.R.color.holo_red_dark else android.R.color.holo_green_dark
            )
        )
    }

    private fun startMonitoring() {
        val selectedPackages = appSelectionAdapter.getSelectedPackages()
        if (selectedPackages.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_apps_selected), Toast.LENGTH_SHORT).show()
            return
        }

        // Save monitored apps
        val monitoredApps = selectedPackages.map { packageName ->
            val appInfo = appSelectionAdapter.currentList.find { it.packageName == packageName }
            MonitoredApp(
                packageName = packageName,
                appName = appInfo?.appName ?: packageName,
                isMonitored = true
            )
        }
        
        preferencesRepository.saveMonitoredApps(monitoredApps)
        preferencesRepository.setMonitoringActive(true)

        // Start monitoring service
        Intent(this, UsageMonitorService::class.java).also { intent ->
            startForegroundService(intent)
        }

        updateToggleButton()
        Toast.makeText(this, "Monitoring started for ${selectedPackages.size} apps", Toast.LENGTH_SHORT).show()
    }

    private fun stopMonitoring() {
        preferencesRepository.setMonitoringActive(false)
        
        // Stop monitoring service
        Intent(this, UsageMonitorService::class.java).also { intent ->
            stopService(intent)
        }

        updateToggleButton()
        Toast.makeText(this, "Monitoring stopped", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        updateToggleButton()
    }
}
