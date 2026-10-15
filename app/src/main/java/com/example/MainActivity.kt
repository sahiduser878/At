package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val settings by mainViewModel.settingsFlow.collectAsState()
            val accentColor = remember(settings.accentColorHex) {
                parseAccentColor(settings.accentColorHex)
            }

            // Notification permission request on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) {}
                LaunchedEffect(Unit) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            MyApplicationTheme(accentColor = accentColor) {
                MainAppScaffold(viewModel = mainViewModel, accentColor = accentColor)
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    viewModel: MainViewModel,
    accentColor: Color
) {
    var currentScreen by remember { mutableStateOf("home") }
    val statusNotice by viewModel.statusNotice.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()

    // Status snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusNotice) {
        statusNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.clearStatusNotice()
        }
    }

    // Handle back button on secondary screens
    if (currentScreen != "home") {
        BackHandler {
            currentScreen = "home"
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CyberBgDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Hide bottom bar in full-screen Live Voice mode
            if (currentScreen != "live") {
                NavigationBar(
                    containerColor = CyberSurfaceDark,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == "home",
                        onClick = { currentScreen = "home" },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 11.sp, fontWeight = if (currentScreen == "home") FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accentColor,
                            selectedTextColor = accentColor,
                            indicatorColor = accentColor.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_item_home")
                    )

                    NavigationBarItem(
                        selected = currentScreen == "chat",
                        onClick = { currentScreen = "chat" },
                        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat") },
                        label = { Text("Chat", fontSize = 11.sp, fontWeight = if (currentScreen == "chat") FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accentColor,
                            selectedTextColor = accentColor,
                            indicatorColor = accentColor.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_item_chat")
                    )

                    // Central Glowing Live Voice Tab
                    NavigationBarItem(
                        selected = currentScreen == "live",
                        onClick = { currentScreen = "live" },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSpeaking) accentColor else accentColor.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Live Voice", tint = if (isSpeaking) Color.White else accentColor, modifier = Modifier.size(18.dp))
                            }
                        },
                        label = { Text("Live", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accentColor,
                            selectedTextColor = accentColor,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = accentColor,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_item_live")
                    )

                    NavigationBarItem(
                        selected = currentScreen == "coding",
                        onClick = { currentScreen = "coding" },
                        icon = { Icon(Icons.Default.Code, contentDescription = "Code Agent") },
                        label = { Text("Code", fontSize = 11.sp, fontWeight = if (currentScreen == "coding") FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accentColor,
                            selectedTextColor = accentColor,
                            indicatorColor = accentColor.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_item_coding")
                    )

                    NavigationBarItem(
                        selected = currentScreen == "settings",
                        onClick = { currentScreen = "settings" },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", fontSize = 11.sp, fontWeight = if (currentScreen == "settings") FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accentColor,
                            selectedTextColor = accentColor,
                            indicatorColor = accentColor.copy(alpha = 0.15f),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_item_settings")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    "home" -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToChat = { currentScreen = "chat" },
                        onNavigateToLive = { currentScreen = "live" },
                        onNavigateToCoding = { currentScreen = "coding" },
                        onNavigateToActions = { currentScreen = "actions" },
                        onNavigateToMemory = { currentScreen = "memory" },
                        onNavigateToTasks = { currentScreen = "tasks" },
                        onNavigateToSettings = { currentScreen = "settings" }
                    )
                    "chat" -> ChatScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = "home" }
                    )
                    "live" -> LiveVoiceScreen(
                        viewModel = viewModel,
                        onEndCall = { currentScreen = "home" }
                    )
                    "coding" -> CodingScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = "home" }
                    )
                    "actions" -> AppsActionsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = "home" }
                    )
                    "memory" -> MemoryScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = "home" }
                    )
                    "tasks" -> TasksScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = "home" }
                    )
                    "projects" -> ProjectsScreen(
                        viewModel = viewModel,
                        onOpenCodingAgent = { currentScreen = "coding" },
                        onNavigateBack = { currentScreen = "home" }
                    )
                    "settings" -> SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { currentScreen = "home" }
                    )
                }
            }
        }
    }
}
