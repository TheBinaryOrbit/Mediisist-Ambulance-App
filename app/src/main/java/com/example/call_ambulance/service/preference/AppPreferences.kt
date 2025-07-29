package com.example.call_ambulance.service.preference

import android.content.Context

object AppPreferences {
    private const val PREF_NAME = "call_support_prefs"
    private const val KEY_ONLINE = "is_online"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_SESSION_KEY = "session_key"
    var isAppInForeground: Boolean = false

    fun setOnline(context: Context, isOnline: Boolean) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ONLINE, isOnline).apply()
    }

    fun isOnline(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ONLINE, true)
    }

    fun setUserName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_NAME, null)
    }

    fun getUserId(context: Context): String? {
        val sharedPref = context.getSharedPreferences("CallSupportPrefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_id", null)
    }

    fun saveSessionKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SESSION_KEY, key).apply()
    }

    fun getSessionKey(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SESSION_KEY, null)
    }
}
