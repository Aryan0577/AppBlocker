package com.todo.appblocker.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean,
    val icon: Drawable,
    var isBlocked: Boolean = false,
    var usageTimeToday: Long = 0L // Usage time in milliseconds
)