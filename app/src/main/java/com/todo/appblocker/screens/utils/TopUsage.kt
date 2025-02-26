package com.todo.appblocker.screens.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap


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
