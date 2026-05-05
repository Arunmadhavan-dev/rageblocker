package com.rageblocker.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.rageblocker.data.model.MonitoredApp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencesRepository(context: Context) {
    
    private val sharedPrefs: SharedPreferences = 
        context.getSharedPreferences("rageblocker_prefs", Context.MODE_PRIVATE)
    
    private val gson = Gson()
    
    companion object {
        private const val KEY_MONITORED_APPS = "monitored_apps"
        private const val KEY_IS_MONITORING_ACTIVE = "is_monitoring_active"
    }
    
    fun saveMonitoredApps(apps: List<MonitoredApp>) {
        val json = gson.toJson(apps)
        sharedPrefs.edit()
            .putString(KEY_MONITORED_APPS, json)
            .apply()
    }
    
    fun getMonitoredApps(): List<MonitoredApp> {
        val json = sharedPrefs.getString(KEY_MONITORED_APPS, null)
        return if (json != null) {
            val type = object : TypeToken<List<MonitoredApp>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    fun setMonitoringActive(isActive: Boolean) {
        sharedPrefs.edit()
            .putBoolean(KEY_IS_MONITORING_ACTIVE, isActive)
            .apply()
    }
    
    fun isMonitoringActive(): Boolean {
        return sharedPrefs.getBoolean(KEY_IS_MONITORING_ACTIVE, false)
    }
}
