package com.rageblocker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rageblocker.service.UsageMonitorService

class UsageStatsReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_SCREEN_ON -> {
                // User unlocked device or screen turned on
                // Check if monitoring service should be running
                val prefs = context.getSharedPreferences("rageblocker_prefs", Context.MODE_PRIVATE)
                val isMonitoringActive = prefs.getBoolean("is_monitoring_active", false)
                
                if (isMonitoringActive) {
                    // Ensure service is running
                    val serviceIntent = Intent(context, UsageMonitorService::class.java)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }
}
