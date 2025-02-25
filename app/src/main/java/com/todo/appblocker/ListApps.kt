package com.todo.appblocker

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Calendar
import java.util.concurrent.TimeUnit


// Model for app information
data class AppInfo(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean,
    val icon: Drawable,
    var isBlocked: Boolean = false,
    var usageTimeToday: Long = 0L // Usage time in milliseconds
)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class
)
@Composable
fun ParentDashboard(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State for apps
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var blockedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var showBlockedOnly by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var totalScreenTimeToday by remember { mutableStateOf(0L) }
    var topUsedApps by remember { mutableStateOf<List<TopAppUsage>>(emptyList()) }

    // Animation states
    val fabRotation = remember { Animatable(0f) }

    // SharedPreferences for saving blocked apps
    val sharedPreferences = remember {
        context.getSharedPreferences("TimeOUT_Preferences", Context.MODE_PRIVATE)
    }

    // Function to refresh usage stats
    val refreshUsageStats = {
        coroutineScope.launch {
            // Get current usage stats
            val (appUsageStats, totalUsage, topApps) = getDetailedAppUsageStats(context)

            // Update usage time for all apps
            allApps.forEach { app ->
                app.usageTimeToday = appUsageStats[app.packageName] ?: 0L
            }

            totalScreenTimeToday = totalUsage
            topUsedApps = topApps
        }
    }

    // Load apps on initial composition
    LaunchedEffect(key1 = Unit) {
        coroutineScope.launch {
            val apps = loadInstalledApps(context)
            val savedBlockedApps = loadBlockedAppsFromPrefs(sharedPreferences)

            // Get detailed usage stats
            val (appUsageStats, totalUsage, topApps) = getDetailedAppUsageStats(context)

            // Mark apps as blocked if they're in the saved list and add usage time
            apps.forEach { app ->
                app.isBlocked = savedBlockedApps.contains(app.packageName)
                app.usageTimeToday = appUsageStats[app.packageName] ?: 0L
            }

            totalScreenTimeToday = totalUsage
            topUsedApps = topApps
            allApps = apps
            blockedApps = apps.filter { it.isBlocked }
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

    // Animate FAB rotation when view mode changes
    LaunchedEffect(showBlockedOnly) {
        fabRotation.animateTo(
            targetValue = if (showBlockedOnly) 0f else 180f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        )
    }

    // Filter apps based on search query and view mode
    val filteredApps = remember(allApps, blockedApps, showBlockedOnly, searchQuery) {
        val appsToFilter = if (showBlockedOnly) blockedApps else allApps
        appsToFilter.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    // Function to toggle block status
    val toggleBlockStatus = { app: AppInfo ->
        app.isBlocked = !app.isBlocked

        // Update blocked apps list
        if (app.isBlocked) {
            blockedApps = blockedApps + app
        } else {
            blockedApps = blockedApps - app
        }

        // Save to SharedPreferences
        saveBlockedAppsToPrefs(sharedPreferences, blockedApps.map { it.packageName })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Parent Dashboard",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Add refresh animation
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
                    IconButton(onClick = { /* Settings Action */ }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3A1C71)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showBlockedOnly = !showBlockedOnly
                },
                containerColor = Color(0xFF3A1C71)
            ) {
                Icon(
                    imageVector = if (showBlockedOnly) Icons.Default.List else Icons.Default.Lock,
                    contentDescription = if (showBlockedOnly) "Show All Apps" else "Show Blocked Apps",
                    tint = Color.White,
                    modifier = Modifier.rotate(fabRotation.value)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Use LazyColumn for the entire content to enable proper scrolling
            LazyColumn(
                modifier = Modifier.fillMaxSize()
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
                            Text(
                                text = "Today's Screen Time",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3A1C71)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Add animation to screen time display
                            AnimatedContent(
                                targetState = totalScreenTimeToday,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(300)) with
                                            fadeOut(animationSpec = tween(300))
                                }
                            ) { targetCount ->
                                Text(
                                    text = formatScreenTime(targetCount),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD76D77)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ScreenTimeInfoItem(
                                    title = "Blocked Time",
                                    value = formatScreenTime(
                                        blockedApps.sumOf { it.usageTimeToday }
                                    ),
                                    color = Color(0xFF3A1C71)
                                )

                                Divider(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .width(1.dp),
                                    color = Color.LightGray
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

                // Top Apps Card - Made more compact
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

                            if (topUsedApps.isEmpty()) {
                                Text(
                                    text = "No app usage data available yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                // Display top 3 apps in a more compact format
                                topUsedApps.take(3).forEach { app ->
                                    CompactTopAppUsageItem(app = app)
                                }
                            }
                        }
                    }
                }

                // Search bar
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .animateItemPlacement(),
                        placeholder = { Text("Search apps") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear"
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF3A1C71),
                            focusedLabelColor = Color(0xFF3A1C71)
                        ),
                        singleLine = true
                    )
                }

                // Section Title with animated transition
                item {
                    // AnimatedContent for the title based on view mode
                    AnimatedContent(
                        targetState = showBlockedOnly,
                        transitionSpec = {
                            slideInHorizontally { width -> if (targetState) -width else width } with
                                    slideOutHorizontally { width -> if (targetState) width else -width }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { isShowingBlockedOnly ->
                        Text(
                            text = if (isShowingBlockedOnly) "Blocked Apps" else "All Applications",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Loading state
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
                }
                // Empty state
                else if (filteredApps.isEmpty()) {
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
                                    imageVector = if (showBlockedOnly) Icons.Default.Lock else Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .padding(bottom = 8.dp)
                                )
                                Text(
                                    text = if (showBlockedOnly) "No blocked apps yet" else "No apps found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
                // App list items with animations
                else {
                    items(
                        items = filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AnimatedAppItem(
                            app = app,
                            onToggleBlock = { toggleBlockStatus(app) }
                        )
                    }

                    // Add some bottom padding for better UX
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedAppItem(
    app: AppInfo,
    onToggleBlock: () -> Unit
) {
    var animatedProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(key1 = Unit) {
        animatedProgress = 0f
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animatedProgress = value
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .alpha(animatedProgress)
            .scale(0.9f + (0.1f * animatedProgress)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon with animated background when blocked
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (app.isBlocked)
                            Color(0xFFFFECED)
                        else
                            Color.Transparent
                    )
                    .padding(if (app.isBlocked) 4.dp else 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = app.icon.toBitmap().asImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
            }

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

                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Usage time for today with animated appearance
                if (app.usageTimeToday > 0) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + expandVertically()
                    ) {
                        Text(
                            text = "Used: ${formatScreenTime(app.usageTimeToday)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD76D77),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Block/Unblock switch with animation
            val switchProgress = animateFloatAsState(
                targetValue = if (app.isBlocked) 1f else 0f,
                animationSpec = tween(durationMillis = 300)
            )

            Switch(
                checked = app.isBlocked,
                onCheckedChange = { onToggleBlock() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF3A1C71),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Gray.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun CompactTopAppUsageItem(app: TopAppUsage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        Image(
            bitmap = app.icon.toBitmap().asImageBitmap(),
            contentDescription = app.appName,
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
        )

        // App name and usage time
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatScreenTime(app.usageTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFD76D77)
                )

                // Usage bar visualization - more compact
                LinearProgressIndicator(
                    progress = { app.usageTime.toFloat() / 7200000f }, // Scale against 2 hours
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF3A1C71),
                    trackColor = Color.LightGray
                )
            }
        }

        // Show if app is blocked
        if (app.isBlocked) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Blocked",
                tint = Color(0xFF3A1C71),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Helper function to animate entering items
@Composable
fun <T> animateItem(
    item: T,
    content: @Composable (T) -> Unit
) {
    var animatedProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(key1 = item) {
        animatedProgress = 0f
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 300)
        ) { value, _ ->
            animatedProgress = value
        }
    }

    Box(
        modifier = Modifier
            .alpha(animatedProgress)
            .scale(0.8f + (0.2f * animatedProgress))
    ) {
        content(item)
    }
}