package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.actions.ActionResult
import com.example.ui.MainViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

data class AppActionItem(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun AppsActionsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settingsFlow.collectAsState()
    val accentColor = remember(settings.accentColorHex) {
        parseAccentColor(settings.accentColorHex)
    }

    var customUrlInput by remember { mutableStateOf("") }
    var searchWebInput by remember { mutableStateOf("") }
    var mathInput by remember { mutableStateOf("25 * 18 + 140") }
    var mathResult by remember { mutableStateOf("") }
    var notificationTitle by remember { mutableStateOf("YESMIN AI Notice") }
    var notificationBody by remember { mutableStateOf("Jaan, remember to drink water and smile today!") }

    var confirmationAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    val appItems = listOf(
        AppActionItem("youtube", "YouTube", Icons.Default.PlayCircle, Color(0xFFFF0000)),
        AppActionItem("whatsapp", "WhatsApp", Icons.Default.Chat, Color(0xFF25D366)),
        AppActionItem("telegram", "Telegram", Icons.Default.Send, Color(0xFF0088CC)),
        AppActionItem("gmail", "Gmail", Icons.Default.Mail, Color(0xFFEA4335)),
        AppActionItem("maps", "Google Maps", Icons.Default.Map, Color(0xFF34A853)),
        AppActionItem("chrome", "Chrome", Icons.Default.Language, Color(0xFF4285F4)),
        AppActionItem("camera", "Camera", Icons.Default.CameraAlt, ElectricCyan),
        AppActionItem("calculator", "Calculator", Icons.Default.Calculate, AmberGold),
        AppActionItem("calendar", "Calendar", Icons.Default.CalendarToday, NeonPurple),
        AppActionItem("instagram", "Instagram", Icons.Default.PhotoCamera, NeonMagenta)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBgDark,
        topBar = {
            Surface(
                color = CyberSurfaceDark,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Apps & Native Actions",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Native Android Intent Bridge",
                            color = accentColor,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. Native App Launchers Grid
            Text(
                text = "REAL DEVICE APPS",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                userScrollEnabled = false
            ) {
                items(appItems) { app ->
                    GlassCard(
                        borderColor = app.color.copy(alpha = 0.4f),
                        onClick = {
                            val res = viewModel.actionManager.openApp(app.id)
                            when (res) {
                                is ActionResult.Success -> Toast.makeText(context, res.message, Toast.LENGTH_SHORT).show()
                                is ActionResult.Error -> Toast.makeText(context, res.errorMessage, Toast.LENGTH_SHORT).show()
                                else -> {}
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(app.color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(app.icon, contentDescription = null, tint = app.color, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = app.name,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 2. Web Search & URL Opener
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = ElectricCyan.copy(alpha = 0.4f)
            ) {
                Text(
                    text = "SEARCH & BROWSER BRIDGE",
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Search field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchWebInput,
                        onValueChange = { searchWebInput = it },
                        placeholder = { Text("Search query...", color = TextTertiary, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CyberSurfaceVariant,
                            unfocusedContainerColor = CyberSurfaceVariant,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (searchWebInput.isNotBlank()) {
                                viewModel.actionManager.searchWeb(searchWebInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Search", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Open URL field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        placeholder = { Text("https://...", color = TextTertiary, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CyberSurfaceVariant,
                            unfocusedContainerColor = CyberSurfaceVariant,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (customUrlInput.isNotBlank()) {
                                viewModel.actionManager.openUrl(customUrlInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open Link", color = TextPrimary)
                    }
                }
            }

            // 3. Calculator Tool
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = AmberGold.copy(alpha = 0.4f)
            ) {
                Text(
                    text = "SAFE MATH CALCULATOR",
                    color = AmberGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = mathInput,
                        onValueChange = { mathInput = it },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CyberSurfaceVariant,
                            unfocusedContainerColor = CyberSurfaceVariant,
                            focusedBorderColor = AmberGold,
                            unfocusedBorderColor = CyberCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            mathResult = viewModel.actionManager.calculateMath(mathInput)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Eval", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                if (mathResult.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = mathResult,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 4. Send Android System Notification
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = NeonPurple.copy(alpha = 0.4f)
            ) {
                Text(
                    text = "SEND NATIVE NOTIFICATION",
                    color = NeonPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notificationTitle,
                    onValueChange = { notificationTitle = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = notificationBody,
                    onValueChange = { notificationBody = it },
                    label = { Text("Message Body") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val res = viewModel.actionManager.sendNotification(notificationTitle, notificationBody)
                        if (res is ActionResult.Success) {
                            Toast.makeText(context, "Notification sent to system drawer!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Notification", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Confirmation dialog for sensitive actions
    confirmationAction?.let { (actionDesc, onConfirm) ->
        AlertDialog(
            onDismissRequest = { confirmationAction = null },
            title = { Text("Confirm Device Action", color = TextPrimary) },
            text = { Text("Are you sure you want to execute: $actionDesc?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        confirmationAction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmationAction = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CyberSurfaceDark
        )
    }
}
