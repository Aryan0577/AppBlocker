package com.todo.appblocker.blocking

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }

    return mode == AppOpsManager.MODE_ALLOWED
}

fun requestUsageStatsPermission(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle("Usage Access Required")
        .setMessage("To detect app launches, TimeOUT needs usage access permission. Please enable this permission for TimeOUT in the next screen.")
        .setPositiveButton("Open Settings") { _, _ ->
            context.startActivity(intent)
        }
        .setNegativeButton("Cancel", null)
        .create()

    dialog.show()
}

fun requestOverlayPermission(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )

    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle("Overlay Permission Required")
        .setMessage("To block apps, TimeOUT needs to display over other apps. Please enable this permission in the next screen.")
        .setPositiveButton("Open Settings") { _, _ ->
            context.startActivity(intent)
        }
        .setNegativeButton("Cancel", null)
        .create()

    dialog.show()
}

