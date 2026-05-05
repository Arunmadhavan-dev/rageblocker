package com.rageblocker.data.model

data class MonitoredApp(
    val packageName: String,
    val appName: String,
    val isMonitored: Boolean = false
)
