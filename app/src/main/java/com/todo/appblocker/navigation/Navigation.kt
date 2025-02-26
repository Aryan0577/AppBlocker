package com.todo.appblocker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.todo.appblocker.getUserType
import com.todo.appblocker.screens.login.LoginScreen
import com.todo.appblocker.screens.UserTypeScreen
import com.todo.appblocker.screens.child.ChildDashboard
import com.todo.appblocker.screens.login.ParentPasswordScreen
import com.todo.appblocker.screens.login.PermissionCard
import com.todo.appblocker.screens.login.PermissionsScreen
import com.todo.appblocker.screens.login.SignupScreen
import com.todo.appblocker.screens.parent.ParentDashboard
import com.todo.appblocker.shouldShowLoginPage
import com.todo.appblocker.shouldShowPermissionScreen

@Composable
fun TimeOUTApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val shouldShowLogin = shouldShowLoginPage(context) // First-time check
    val userType = getUserType(context) // Parent or Child
    val needsPermissions = shouldShowPermissionScreen(context) // If permissions are required

    val startDestination = when {
        shouldShowLogin -> "login" // First-time user → Login
        needsPermissions -> "permissions" // Permissions not granted → Permissions Screen
        userType == "parent" -> "dashboard" // Parent → Parent Dashboard
        userType == "child" -> "cDashboard" // Child → Child Dashboard
        else -> "user" // No user type → Ask them
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(navController)
        }
        composable("pin"){
            ParentPasswordScreen(navController)
        }
        composable("signin") {
            SignupScreen(navController)
        }
        composable("permissions") {
            PermissionsScreen(navController)
        }
        composable("user") {
            UserTypeScreen(navController)
        }
        composable("dashboard") {
            ParentDashboard(navController)
        }
        composable("cDashboard") {
            ChildDashboard(navController)
        }
    }
}
