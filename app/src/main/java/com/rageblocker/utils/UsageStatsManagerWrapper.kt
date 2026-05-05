package com.rageblocker.utils

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateUtils

object UsageStatsManagerWrapper {

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getUsageStatsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    fun getTodayUsageStats(context: Context, packageName: String): Long {
        if (!hasUsageStatsPermission(context)) return 0L

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - DateUtils.DAY_IN_MILLIS

        return try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                currentTime
            )
            
            stats?.filter { it.packageName == packageName }
                ?.sumOf { it.totalTimeInForeground }
                ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    fun getTodayUsageStatsForApps(context: Context, packageNames: List<String>): Map<String, Long> {
        if (!hasUsageStatsPermission(context)) return emptyMap()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - DateUtils.DAY_IN_MILLIS

        return try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                currentTime
            )
            
            packageNames.associateWith { packageName ->
                stats?.filter { it.packageName == packageName }
                    ?.sumOf { it.totalTimeInForeground }
                    ?: 0L
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun getCurrentlyRunningApp(context: Context): String? {
        if (!hasUsageStatsPermission(context)) return null

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - 1000 * 60 // Last minute

        return try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                currentTime
            )
            
            stats?.filter { it.lastTimeUsed > currentTime - 1000 * 10 } // Last 10 seconds
                ?.maxByOrNull { it.lastTimeUsed }
                ?.packageName
        } catch (e: Exception) {
            null
        }
    }

    fun formatUsageTime(milliseconds: Long): String {
        val hours = milliseconds / (1000 * 60 * 60)
        val minutes = (milliseconds % (1000 * 60 * 60)) / (1000 * 60)
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "< 1m"
        }
    }

    fun getUsageTimeInMinutes(milliseconds: Long): Int {
        return (milliseconds / (1000 * 60)).toInt()
    }
}
