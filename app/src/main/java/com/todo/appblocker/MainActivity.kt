package com.todo.appblocker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.todo.appblocker.navigation.TimeOUTApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            if (!hasUsageAccess(this)) {
                requestUsageAccess(this)
            } else
                TimeOUTApp()

        }
    }
}
fun requestUsageAccess(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}
fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}


fun saveUserSession(context: Context, isFirstTime: Boolean, userType: String) {
    val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putBoolean("is_first_time", isFirstTime)
        putString("user_type", userType)
        apply() // Save changes
    }
}
fun shouldShowLoginPage(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("is_first_time", true) // Default is true (show login page)
}

fun getUserType(context: Context): String {
    val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    return sharedPreferences.getString("user_type", "child") ?: "child" // Default: child
}

fun shouldShowPermissionScreen(context: Context): Boolean {
    // List of required permissions
    val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean("permission", true) // Default is true (show login page)

}
fun savePermission(context: Context, permissions: Boolean,) {
    val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putBoolean("permission", permissions)
        apply() // Save changes
    }
}







