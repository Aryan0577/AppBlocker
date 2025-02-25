package com.todo.appblocker

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Calendar
import java.util.concurrent.TimeUnit

fun saveBlockedAppsToPrefs(prefs: SharedPreferences, blockedPackageNames: List<String>) {
    val editor = prefs.edit()
    val jsonArray = JSONArray()

    blockedPackageNames.forEach { packageName ->
        jsonArray.put(packageName)
    }
    editor.putString("blocked_apps", jsonArray.toString())
    editor.apply()
}

suspend fun loadInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
    val packageManager = context.packageManager
    val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    // List of popular apps that should be included even if they're system apps
    val popularSystemApps = listOf(
        "com.android.chrome",                 // Chrome
        "com.google.android.youtube",         // YouTube
        "com.google.android.apps.maps",       // Google Maps
        "com.google.android.gm",              // Gmail
        "com.google.android.apps.photos",     // Google Photos
        "com.google.android.apps.youtube.music", // YouTube Music
        "com.google.android.googlequicksearchbox", // Google Search
        "com.google.android.apps.docs",       // Google Drive
        "com.google.android.calendar",        // Google Calendar
        "com.google.android.keep",            // Google Keep
        "com.google.android.apps.messaging",  // Google Messages
        "com.facebook.katana",                // Facebook
        "com.instagram.android",              // Instagram
        "com.snapchat.android",               // Snapchat
        "com.whatsapp",                       // WhatsApp
        "com.spotify.music",                  // Spotify
        "com.tiktok.tiktok"                   // TikTok
    )

    // List of system apps/utilities to explicitly exclude
    val excludedSystemApps = listOf(
        "com.android.settings",               // Settings
        "com.android.systemui",               // System UI
        "com.android.launcher",               // Home screen
        "com.android.launcher3",              // Home screen (alternative)
        "com.google.android.launcher",        // Pixel Launcher
        "com.android.hotspot",                // Hotspot
        "com.android.bluetooth",              // Bluetooth
        "com.android.wifi",                   // WiFi
        "com.android.mms",                    // MMS service
        "com.android.phone",                  // Phone service
        "com.android.provision",              // Provisioning
        "com.android.statementservice",       // Statement service
        "com.android.calculator",             // Calculator (can be included if desired)
        "com.android.calendar",               // Calendar (can be included if desired)
        "com.android.camera",                 // Camera (can be included if desired)
        "com.android.deskclock",              // Clock (can be included if desired)
        "com.android.server.telecom"          // Telecom server
    )

    // Filter and convert to AppInfo
    installedApps
        .filter { appInfo ->
            val packageName = appInfo.packageName
            val isPopularSystemApp = popularSystemApps.contains(packageName)
            val isExcludedSystemApp = excludedSystemApps.contains(packageName)

            // Include the app if:
            // 1. It has a launch intent (user can open it) AND
            // 2. Either it's a popular system app OR it's not an excluded system app
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            launchIntent != null && (isPopularSystemApp || (!isSystemApp(appInfo) && !isExcludedSystemApp))
        }
        .map { applicationInfo ->
            AppInfo(
                appName = packageManager.getApplicationLabel(applicationInfo).toString(),
                packageName = applicationInfo.packageName,
                isSystemApp = isSystemApp(applicationInfo),
                icon = packageManager.getApplicationIcon(applicationInfo.packageName),
                isBlocked = false // Will be updated later
            )
        }
        .sortedBy { it.appName }
}

// Improved version of isSystemApp function to better detect system apps
fun isSystemApp(appInfo: ApplicationInfo): Boolean {
    return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
            (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
}

@Composable
fun ScreenTimeInfoItem(title: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

// Function to load all non-system installed apps
suspend fun loadAllInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
    loadInstalledApps(context)
}

// Helper function to format screen time
fun formatScreenTime(timeInMillis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "< 1m"
    }
}

fun loadBlockedAppsFromPrefs(sharedPreferences: SharedPreferences): List<String> {
    val blockedAppsJson = sharedPreferences.getString("blocked_apps", "[]") ?: "[]"
    val blockedAppsList = mutableListOf<String>()

    try {
        val jsonArray = JSONArray(blockedAppsJson)
        for (i in 0 until jsonArray.length()) {
            blockedAppsList.add(jsonArray.getString(i))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return blockedAppsList
}


// Data class for app info

// Data class for top app usage
data class TopAppUsage(
    val appName: String,
    val packageName: String,
    val usageTime: Long,
    val icon: Drawable,
    val isBlocked:Boolean
)
// Fixed getDetailedAppUsageStats function
suspend fun getDetailedAppUsageStats(context: Context): Triple<Map<String, Long>, Long, List<TopAppUsage>> =
    withContext(Dispatchers.IO) {
        val usageStatsMap = mutableMapOf<String, Long>()
        var totalUsageTime = 0L
        val topApps = mutableListOf<TopAppUsage>()
        val packageManager = context.packageManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

                // Set time range to today
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startTime = calendar.timeInMillis
                val endTime = System.currentTimeMillis()

                // Get daily usage stats
                val queryUsageStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    startTime,
                    endTime
                ).filter { it.lastTimeUsed >= startTime }

                // Get installed apps list to filter system apps
                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                val nonSystemApps = installedApps.filter { !isSystemApp(it) }.map { it.packageName }.toSet()

                // Process stats and calculate total usage time for non-system apps only
                queryUsageStats.forEach { stat ->
                    val timeInForeground =
                        stat.totalTimeVisible

                    if (timeInForeground > 0 && stat.packageName in nonSystemApps) {
                        usageStatsMap[stat.packageName] = timeInForeground
                        totalUsageTime += timeInForeground  // Add only non-system apps' usage time
                    }
                }

                // Get app details for top used apps
                val sortedApps = queryUsageStats
                    .filter { it.packageName in nonSystemApps && it.totalTimeInForeground > 0 }
                    .sortedByDescending {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            it.totalTimeVisible
                        } else {
                            it.totalTimeInForeground
                        }
                    }

                for (stat in sortedApps) {
                    try {
                        val appInfo = packageManager.getApplicationInfo(stat.packageName, 0)
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        val icon = packageManager.getApplicationIcon(stat.packageName)
                        val usageTime = usageStatsMap[stat.packageName] ?: 0L

                        topApps.add(
                            TopAppUsage(
                                appName = appName,
                                packageName = stat.packageName,
                                usageTime = usageTime,
                                icon = icon,
                                isBlocked = false
                            )
                        )
                    } catch (_: PackageManager.NameNotFoundException) {
                        // App uninstalled, ignore
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Triple(usageStatsMap, totalUsageTime, topApps)
    }
