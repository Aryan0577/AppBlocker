package com.todo.appblocker.screens.utils

import android.content.SharedPreferences
import org.json.JSONArray

fun loadBlockedAppsFromPrefs(sharedPreferences: SharedPreferences): List<String> {
    val jsonString = sharedPreferences.getString("blocked_apps", "[]") // Default to empty JSON array
    val blockedAppsList = mutableListOf<String>()

    try {
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            blockedAppsList.add(jsonArray.getString(i))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return blockedAppsList
}