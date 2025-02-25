package com.todo.appblocker

import android.app.AppOpsManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.todo.appblocker.ui.theme.AppBlockerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            if (!hasUsageAccess(this)) {
                requestUsageAccess(this)
            } else {
                Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show()
            }

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



class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val _blockedApps = mutableStateListOf<String>()
    val blockedApps: List<String> = _blockedApps

    private val prefs = application.getSharedPreferences("timeout_prefs", Context.MODE_PRIVATE)

    init {
        loadBlockedApps()
    }

    fun addBlockedApp(packageName: String) {
        _blockedApps.add(packageName)
        saveBlockedApps()
    }

    fun removeBlockedApp(packageName: String) {
        _blockedApps.remove(packageName)
        saveBlockedApps()
    }

    private fun loadBlockedApps() {
        prefs.getStringSet("blocked_apps", setOf())?.let {
            _blockedApps.addAll(it)
        }
    }

    private fun saveBlockedApps() {
        prefs.edit().putStringSet("blocked_apps", _blockedApps.toSet()).apply()
    }

    fun isAppBlocked(packageName: String): Boolean {
        return blockedApps.contains(packageName)
    }
}


@Composable
fun TimeOUTApp(
    viewModel: AppViewModel= viewModel()
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
           LoginScreen(navController)
        }
        composable("user"){
            UserTypeScreen(navController)
        }
        composable("dashboard") {
            ParentDashboard(
                navController )
        }
        composable("cDashboard") {
            ChildDashboard(navController)
        }

    }
}

