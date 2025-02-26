package com.todo.appblocker.screens.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

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
