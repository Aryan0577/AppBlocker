package com.todo.appblocker.blocking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.todo.appblocker.MainActivity
import com.todo.appblocker.screens.utils.loadBlockedAppsFromPrefs

class AppBlockerService : Service() {
    private lateinit var windowManager: WindowManager
    private var blockedApps: List<String> = listOf()
    private lateinit var usageStatsManager: UsageStatsManager
    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkCurrentApp()
            handler.postDelayed(this, 500)
        }
    }

    companion object {
        var isRunning = false


    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // Load the initial list of blocked apps
        val sharedPreferences = getSharedPreferences("TimeOUT_Preferences", Context.MODE_PRIVATE)

        blockedApps = AppBlockerUtils.loadBlockedAppsFromPrefs(sharedPreferences)

        // Create notification channel
        createNotificationChannel()

        // Start as foreground service
        val notification = createNotification()
        startForeground(1, notification)

        // Start checking for blocked apps
        handler.post(checkRunnable)
    }

    private fun updateBlockedApps(newBlockedApps: List<String>) {
        blockedApps = newBlockedApps
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "App Blocker Service"
            val descriptionText = "Monitors and blocks restricted apps"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("app_blocker_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "app_blocker_channel")
            .setContentTitle("TimeOUT App Blocker")
            .setContentText("Monitoring for restricted apps")
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun checkCurrentApp() {
        val currentApp = getCurrentForegroundApp()

        if (currentApp != null && blockedApps.contains(currentApp) && currentApp != packageName) {
            showBlockingOverlay(currentApp)
        }
    }

    private fun getCurrentForegroundApp(): String? {
        val time = System.currentTimeMillis()
        // Check the last 5 seconds of app usage
        val usageEvents = usageStatsManager.queryEvents(time - 5000, time)
        val event = UsageEvents.Event()
        var lastForegroundApp: String? = null

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastForegroundApp = event.packageName
            }
        }

        return lastForegroundApp
    }

    private fun showBlockingOverlay(packageName: String) {
        // Launch blocking activity with overlay permission
        val intent = Intent(this, BlockingOverlayActivity::class.java).apply {
            putExtra("packageName", packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle updates to the blocked apps list
        if (intent?.action == "UPDATE_BLOCKED_APPS") {
            val sharedPreferences = getSharedPreferences("TimeOUT_Preferences", Context.MODE_PRIVATE)
            val newBlockedApps = AppBlockerUtils.loadBlockedAppsFromPrefs(sharedPreferences)

            updateBlockedApps(newBlockedApps)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacks(checkRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
