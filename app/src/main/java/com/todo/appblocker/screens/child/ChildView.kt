package com.todo.appblocker.screens.child

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import com.todo.appblocker.saveUserSession
import com.todo.appblocker.screens.utils.ScreenTimeInfoItem
import com.todo.appblocker.screens.utils.TopAppUsage
import com.todo.appblocker.screens.utils.formatScreenTime
import com.todo.appblocker.screens.utils.loadAllInstalledApps

import com.todo.appblocker.screens.parent.AppInfo
import com.todo.appblocker.screens.utils.CompactTopAppUsageItem
import com.todo.appblocker.screens.utils.getDetailedAppUsageStats
import com.todo.appblocker.screens.utils.loadBlockedAppsFromPrefs
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun ChildDashboard(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    saveUserSession(context, false, "child")

    // State for apps
    var blockedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var topApps by remember { mutableStateOf<List<TopAppUsage>>(emptyList()) }
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var totalScreenTimeToday by remember { mutableStateOf(0L) }
    var screenTimeLimit by remember { mutableStateOf(120L) } // 2 hours in minutes
    var screenTimeRemaining by remember { mutableStateOf(0L) }

    // SharedPreferences for reading blocked apps
    val sharedPreferences = remember {
        context.getSharedPreferences("TimeOUT_Preferences", Context.MODE_PRIVATE)
    }

    // Function to refresh usage stats
    val refreshUsageStats = {
        coroutineScope.launch {
            // Get current usage stats
            val (appUsageStats, totalUsage, topUsedApps) = getDetailedAppUsageStats(context)

            // Update usage time for all apps
            allApps.forEach { app ->
                app.usageTimeToday = appUsageStats[app.packageName] ?: 0L
            }

            totalScreenTimeToday = totalUsage

            // Calculate remaining time
            screenTimeRemaining = maxOf(0L, TimeUnit.MINUTES.toMillis(screenTimeLimit) - totalScreenTimeToday)

            // Filter out system apps from top apps list
            val nonSystemTopApps = topUsedApps.filter { topApp ->
                allApps.any { app -> app.packageName == topApp.packageName && !app.isSystemApp }
            }.take(3)

            topApps = nonSystemTopApps
        }
    }

    // Load data on initial composition
    LaunchedEffect(key1 = Unit) {
        coroutineScope.launch {
            val savedBlockedApps = loadBlockedAppsFromPrefs(sharedPreferences)

            // Load all non-system apps instead of just blocked apps
            val installedApps = loadAllInstalledApps(context)

            // Get usage stats
            val (appUsageStats, totalUsage, topUsedApps) = getDetailedAppUsageStats(context)

            // Add usage time to apps and mark blocked apps
            installedApps.forEach { app ->
                app.usageTimeToday = appUsageStats[app.packageName] ?: 0L
                app.isBlocked = savedBlockedApps.contains(app.packageName)
            }

            // Filter for blocked apps
            val blocked = installedApps.filter { it.isBlocked }

            totalScreenTimeToday = totalUsage
            // Calculate remaining time
            screenTimeRemaining = maxOf(0L, TimeUnit.MINUTES.toMillis(screenTimeLimit) - totalScreenTimeToday)

            blockedApps = blocked
            allApps = installedApps

            // Filter out system apps from top apps list
            val nonSystemTopApps = topUsedApps.filter { topApp ->
                installedApps.any { app -> app.packageName == topApp.packageName && !app.isSystemApp }
            }.take(3)

            topApps = nonSystemTopApps
            isLoading = false
        }
    }

    // Set up periodic refresh (every 5 minutes)
    LaunchedEffect(key1 = Unit) {
        while (true) {
            kotlinx.coroutines.delay(5 * 60 * 1000) // 5 minutes in milliseconds
            refreshUsageStats()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Dashboard",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Add refresh button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                refreshUsageStats()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Usage Data",
                            tint = Color.White
                        )
                    }
                    // Add sign-out button
                    IconButton(onClick = {
                        navController.navigate("login")
                        saveUserSession(context, true, "none")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3A1C71)
                )
            )
        }
    ) { padding ->
        // Use LazyColumn for the entire content to enable proper scrolling
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Screen Time Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .animateItemPlacement(
                            animationSpec = tween(durationMillis = 300)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Colorful time remaining indicator
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFF3A1C71),
                                            Color(0xFFD76D77),
                                            Color(0xFFFFAF7B),
                                            Color(0xFF3A1C71)
                                        )
                                    )
                                )
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Time Left",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )

                                    // Add animation to time left display
                                    AnimatedContent(
                                        targetState = screenTimeRemaining,
                                        transitionSpec = {
                                            fadeIn(animationSpec = tween(300)) with
                                                    fadeOut(animationSpec = tween(300))
                                        }
                                    ) { targetTime ->
                                        Text(
                                            text = formatScreenTime(targetTime),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF3A1C71)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Screen time details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ScreenTimeInfoItem(
                                title = "Used Today",
                                value = formatScreenTime(totalScreenTimeToday),
                                color = Color(0xFFD76D77)
                            )

                            ScreenTimeInfoItem(
                                title = "Daily Limit",
                                value = "${screenTimeLimit}m",
                                color = Color(0xFF3A1C71)
                            )

                            ScreenTimeInfoItem(
                                title = "Apps Blocked",
                                value = blockedApps.size.toString(),
                                color = Color(0xFFFFAF7B)
                            )
                        }
                    }
                }
            }

            // Top apps section
            if (!isLoading && topApps.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItemPlacement(
                                animationSpec = tween(durationMillis = 300)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Most Used Apps Today",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3A1C71),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // Display top apps
                            topApps.forEach { app ->
                                CompactTopAppUsageItem(app = app)
                            }
                        }
                    }
                }
            }

            // Title for blocked apps
            item {
                Text(
                    text = "Apps You Can't Use",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )

                Text(
                    text = "These apps have been blocked by your parent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Blocked app list or appropriate message
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF3A1C71))
                    }
                }
            } else if (blockedApps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF3A1C71),
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(bottom = 8.dp)
                            )
                            Text(
                                text = "No apps have been blocked yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(blockedApps) { app ->
                    ChildAppItem(app = app)
                }

                // Add bottom space for better UX
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun ChildAppItem(app: AppInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),

        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            Image(
                bitmap = app.icon.toBitmap().asImageBitmap(),
                contentDescription = app.appName,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            // App details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (app.usageTimeToday > 0) {
                    Text(
                        text = "Used today: ${formatScreenTime(app.usageTimeToday)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD76D77)
                    )
                }
            }

            // Blocked icon
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Blocked",
                tint = Color(0xFFD76D77),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}