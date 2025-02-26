package com.todo.appblocker.screens.login

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.todo.appblocker.saveUserSession
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentPasswordScreen(navController: NavController) {
    val context = LocalContext.current

    // State for PIN entry
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var isAuthenticated by remember { mutableStateOf(false) }

    // Vibration effect for feedback



    // Animation states
    val shake = remember { Animatable(0f) }

    // Shared preferences for storing/retrieving PIN
    val sharedPreferences = remember {
        context.getSharedPreferences("TimeOUT_Preferences", Context.MODE_PRIVATE)
    }

    // Default PIN is "1234" if not set
    val correctPin = remember {
        sharedPreferences.getString("parent_pin", "5555") ?: "5555"
    }

    // Function to verify PIN
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            if (pin == correctPin) {
                isAuthenticated = true
                // Navigate to parent dashboard
                delay(200) // Small delay for better UX
                navController.navigate("dashboard")
                saveUserSession(context, false, "parent")
            } else {
                // Incorrect PIN
                showError = true
                errorMessage = "Incorrect PIN"

                // Shake animation for wrong PIN
                shake.animateTo(
                    targetValue = 10f,
                    animationSpec = repeatable(
                        iterations = 5,
                        animation = tween(durationMillis = 50, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                shake.animateTo(0f)

                // Clear PIN after wrong attempt
                pin = ""
            }
        } else {
            showError = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Parent Access",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF3A1C71)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .offset(x = with(LocalDensity.current) { shake.value.dp }),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Lock icon
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Pin Lock",
                    tint = Color(0xFF3A1C71),
                    modifier = Modifier
                        .size(80.dp)
                        .padding(bottom = 24.dp)
                )

                // Title
                Text(
                    text = "Enter Parent PIN",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3A1C71)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = "Please enter your 4-digit PIN to access parent controls",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // PIN dots display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 0 until 4) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    if (i < pin.length) {
                                        if (showError) Color(0xFFD76D77) else Color(0xFF3A1C71)
                                    } else Color.LightGray
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (i < pin.length) {
                                        if (showError) Color(0xFFD76D77) else Color(0xFF3A1C71)
                                    } else Color.LightGray,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Error message
                AnimatedVisibility(
                    visible = showError,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFD76D77),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Keypad
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Row 1: 1, 2, 3
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        for (number in 1..3) {
                            KeypadButton(
                                number = number.toString(),
                                onClick = {
                                    if (pin.length < 4) {
                                        pin += number.toString()
                                    }
                                }
                            )
                        }
                    }

                    // Row 2: 4, 5, 6
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        for (number in 4..6) {
                            KeypadButton(
                                number = number.toString(),
                                onClick = {
                                    if (pin.length < 4) {
                                        pin += number.toString()
                                    }
                                }
                            )
                        }
                    }

                    // Row 3: 7, 8, 9
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        for (number in 7..9) {
                            KeypadButton(
                                number = number.toString(),
                                onClick = {
                                    if (pin.length < 4) {
                                        pin += number.toString()
                                    }
                                }
                            )
                        }
                    }

                    // Row 4: Blank, 0, Delete
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Empty button (placeholder)
                        Box(
                            modifier = Modifier.size(64.dp)
                        )

                        // 0 button
                        KeypadButton(
                            number = "0",
                            onClick = {
                                if (pin.length < 4) {
                                    pin += "0"
                                }
                            }
                        )

                        // Delete button
                        IconButton(
                            onClick = {
                                if (pin.isNotEmpty()) {
                                    pin = pin.dropLast(1)
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    color = Color(0xFFEEEEEE),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backspace,
                                contentDescription = "Delete",
                                tint = Color(0xFF3A1C71),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Forgot PIN option
                TextButton(onClick = {
                    // Reset PIN functionality would go here
                    // For demo, we'll just show a toast
                    Toast.makeText(context, "PIN reset functionality will go here", Toast.LENGTH_SHORT).show()
                }) {
                    Text(
                        text = "Forgot PIN?",
                        color = Color(0xFFD76D77),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    number: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Color(0xFFEEEEEE))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = true, color = Color(0xFFD76D77)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF3A1C71)
        )
    }
}