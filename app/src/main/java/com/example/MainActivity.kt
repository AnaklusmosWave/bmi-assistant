package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WeightViewModel
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

enum class MainTab {
    DASHBOARD, HISTORY, SETTINGS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request Post Notifications permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                BmiAppMainContainer()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BmiAppMainContainer() {
    val viewModel: WeightViewModel = viewModel()
    val profileOpt by viewModel.userProfile.collectAsStateWithLifecycle()
    val logs by viewModel.allLogs.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    var isDevModeUnlockedThisSession by remember { mutableStateOf(false) }
    var settingsClickCount by remember { mutableStateOf(0) }
    var lastSettingsClickTime by remember { mutableStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val profile = profileOpt
    if (profile == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        if (!profile.isOnboarded) {
            com.example.ui.screens.OnboardingScreen(
                viewModel = viewModel,
                profile = profile
            )
        } else {
            Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = when (activeTab) {
                                MainTab.DASHBOARD -> com.example.ui.components.Localization.get("app_title_dashboard", profile.language)
                                MainTab.HISTORY -> com.example.ui.components.Localization.get("app_title_history", profile.language)
                                MainTab.SETTINGS -> com.example.ui.components.Localization.get("app_title_settings", profile.language)
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = activeTab == MainTab.DASHBOARD,
                        onClick = { activeTab = MainTab.DASHBOARD },
                        label = { Text(com.example.ui.components.Localization.get("home", profile.language), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = com.example.ui.components.Localization.get("home", profile.language)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    NavigationBarItem(
                        selected = activeTab == MainTab.HISTORY,
                        onClick = { activeTab = MainTab.HISTORY },
                        label = { Text(com.example.ui.components.Localization.get("history", profile.language), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(imageVector = Icons.Default.List, contentDescription = com.example.ui.components.Localization.get("history", profile.language)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    NavigationBarItem(
                        selected = activeTab == MainTab.SETTINGS,
                        onClick = { 
                            activeTab = MainTab.SETTINGS
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastSettingsClickTime > 2000L) {
                                settingsClickCount = 1
                            } else {
                                settingsClickCount++
                            }
                            lastSettingsClickTime = currentTime

                            if (settingsClickCount >= 5) {
                                isDevModeUnlockedThisSession = true
                                val activatedMsg = if (profile.language == "en-US") "🧪 Developer mode unlocked!" else "🧪 開發者模式已顯示！"
                                android.widget.Toast.makeText(context, activatedMsg, android.widget.Toast.LENGTH_SHORT).show()
                                settingsClickCount = 0
                            }
                        },
                        label = { Text(com.example.ui.components.Localization.get("settings", profile.language), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = com.example.ui.components.Localization.get("settings", profile.language)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (activeTab) {
                    MainTab.DASHBOARD -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            profile = profile,
                            logs = logs,
                            onNavigateToHistory = { activeTab = MainTab.HISTORY }
                        )
                    }
                    MainTab.HISTORY -> {
                        HistoryScreen(
                            viewModel = viewModel,
                            profile = profile,
                            logs = logs
                        )
                    }
                    MainTab.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            profile = profile,
                            isDevModeUnlockedThisSession = isDevModeUnlockedThisSession
                        )
                    }
                }
            }
        }
    }
}
}
