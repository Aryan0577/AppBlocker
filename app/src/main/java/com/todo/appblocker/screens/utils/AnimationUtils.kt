package com.todo.appblocker.screens.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.todo.appblocker.screens.parent.AppInfo


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
fun <T> AnimateItem(
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