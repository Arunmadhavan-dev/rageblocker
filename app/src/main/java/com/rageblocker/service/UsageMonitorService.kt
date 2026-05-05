package com.rageblocker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.rageblocker.MainActivity
import com.rageblocker.R
import com.rageblocker.data.model.MonitoredApp
import com.rageblocker.data.repository.PreferencesRepository
import com.rageblocker.ui.RageOverlayActivity
import com.rageblocker.utils.UsageStatsManagerWrapper
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class UsageMonitorService : Service() {

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var scheduledExecutor: ScheduledExecutorService
    private val handler = Handler(Looper.getMainLooper())
    
    private var isOverlayActive = false

    companion object {
        private const val TAG = "UsageMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "rageblocker_channel"
        private const val CHECK_INTERVAL_SECONDS = 3L

        @Volatile
        var instance: UsageMonitorService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        preferencesRepository = PreferencesRepository(this)
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        startMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopMonitoring()
        Log.d(TAG, "Service destroyed")
    }

    private fun startMonitoring() {
        scheduledExecutor.scheduleWithFixedDelay({
            try {
                checkUsageAndBlock()
            } catch (e: Exception) {
                Log.e(TAG, "Error checking usage", e)
            }
        }, 0, CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS)
    }

    private fun stopMonitoring() {
        scheduledExecutor.shutdown()
        try {
            if (!scheduledExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow()
            }
        } catch (_: InterruptedException) {
            scheduledExecutor.shutdownNow()
        }
    }

    private fun checkUsageAndBlock() {
        if (!preferencesRepository.isMonitoringActive()) {
            stopSelf()
            return
        }

        val monitoredApps = preferencesRepository.getMonitoredApps()
            .filter { it.isMonitored }
        
        if (monitoredApps.isEmpty()) {
            return
        }

        // Get current foreground app
        val currentPackage = UsageStatsManagerWrapper.getCurrentlyRunningApp(this)
        
        // Trigger overlay immediately if a blocked app is in foreground
        if (currentPackage != null && !isOverlayActive) {
            val monitoredApp = monitoredApps.find { it.packageName == currentPackage }
            if (monitoredApp != null) {
                val usageTimeMs = UsageStatsManagerWrapper.getTodayUsageStats(this, currentPackage)
                val usageMinutes = UsageStatsManagerWrapper.getUsageTimeInMinutes(usageTimeMs)
                Log.d(TAG, "Blocked app detected: ${monitoredApp.appName}, usage: ${usageMinutes}min")
                
                // Force close the blocked app by sending user to home screen
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(homeIntent)
                
                triggerRageOverlay(monitoredApp, usageMinutes)
            }
        }
    }

    private fun triggerRageOverlay(monitoredApp: MonitoredApp, usageMinutes: Int) {
        if (isOverlayActive || RageOverlayService.isShowing) return
        
        isOverlayActive = true
        
        handler.post {
            try {
                // Prefer TYPE_APPLICATION_OVERLAY service (draws over everything)
                if (Settings.canDrawOverlays(this)) {
                    RageOverlayService.show(
                        this,
                        monitoredApp.appName,
                        monitoredApp.packageName,
                        usageMinutes
                    )
                    Log.d(TAG, "Rage overlay SERVICE triggered for ${monitoredApp.appName}")
                } else {
                    // Fallback to Activity overlay
                    val intent = Intent(this, RageOverlayActivity::class.java).apply {
                        putExtra("app_name", monitoredApp.appName)
                        putExtra("package_name", monitoredApp.packageName)
                        putExtra("usage_minutes", usageMinutes)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    startActivity(intent)
                    Log.d(TAG, "Rage overlay ACTIVITY triggered for ${monitoredApp.appName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start rage overlay", e)
                isOverlayActive = false
            }
        }
    }

    fun onOverlayDismissed() {
        isOverlayActive = false
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val monitoredAppsCount = preferencesRepository.getMonitoredApps()
            .count { it.isMonitored }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.monitoring_active, monitoredAppsCount))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Restart service if it's removed
        val restartServiceIntent = Intent(applicationContext, UsageMonitorService::class.java)
        restartServiceIntent.setPackage(packageName)
        startForegroundService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }
}
