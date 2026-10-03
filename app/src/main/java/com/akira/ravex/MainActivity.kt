package com.akira.ravex

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.ravex.data.RavexPreferences
import com.akira.ravex.service.RavexOverlayService
import com.akira.ravex.service.ThermalGuardService
import com.akira.ravex.ui.screens.*
import com.akira.ravex.ui.theme.*
import com.akira.ravex.util.RavexGuard

enum class SharinganNavigationTab(val label: String, val icon: ImageVector) {
    GAMEFORGE("GAMEFORGE", Icons.Default.Home),
    COPILOT("AI COPILOT", Icons.Default.Person),
    LAG_DIAG("LAG DIAG", Icons.Default.Warning),
    CAMERA("AI CAMERA", Icons.Default.PlayArrow),
    VOICE("VOICE", Icons.Default.Call),
    CROSSHAIR("CROSSHAIR", Icons.Default.Build),
    PROFILES("PROFILES", Icons.Default.List),
    PERFORMANCE("HEALTH", Icons.Default.Info),
    NETWORK("NETWORK", Icons.Default.Star),
    HUD_SETTINGS("HUD", Icons.Default.Lock),
    CORE_AI("CORE AI", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private lateinit var ravexPrefs: RavexPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RavexGuard.init(this)
        ravexPrefs = RavexPreferences(this)

        if (ravexPrefs.isThermalGuardEnabled) {
            startService(Intent(this, ThermalGuardService::class.java))
        }

        if (ravexPrefs.isHudEnabled && Settings.canDrawOverlays(this)) {
            startService(Intent(this, RavexOverlayService::class.java))
        }

        setContent {
            AkiraRavexTheme {
                MainAppScaffold(ravexPrefs = ravexPrefs)
            }
        }
    }
}

@Composable
fun MainAppScaffold(ravexPrefs: RavexPreferences) {
    var selectedTab by remember { mutableStateOf(SharinganNavigationTab.GAMEFORGE) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = RavexSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.height(46.dp)
            ) {
                SharinganNavigationTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(15.dp)) },
                        label = { Text(tab.label, fontSize = 7.5.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RavexCyan,
                            selectedTextColor = RavexCyan,
                            indicatorColor = RavexSurfaceVariant,
                            unselectedIconColor = RavexTextMuted,
                            unselectedTextColor = RavexTextMuted
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(RavexBlack)
        ) {
            when (selectedTab) {
                SharinganNavigationTab.GAMEFORGE -> GameForgeLandscapeScreen(
                    ravexPrefs = ravexPrefs,
                    onNavigateTab = { tabName ->
                        when (tabName) {
                            "COPILOT" -> selectedTab = SharinganNavigationTab.COPILOT
                            "GAMES" -> selectedTab = SharinganNavigationTab.CROSSHAIR
                            "PERFORMANCE" -> selectedTab = SharinganNavigationTab.PERFORMANCE
                            "HUD" -> selectedTab = SharinganNavigationTab.HUD_SETTINGS
                        }
                    }
                )
                SharinganNavigationTab.COPILOT -> RavexCopilotScreen(ravexPrefs = ravexPrefs)
                SharinganNavigationTab.LAG_DIAG -> SmartLagDetectionScreen(ravexPrefs = ravexPrefs)
                SharinganNavigationTab.CAMERA -> AiCameraScreen(ravexPrefs = ravexPrefs)
                SharinganNavigationTab.VOICE -> AiVoiceCommandsScreen(
                    ravexPrefs = ravexPrefs,
                    onNavigateTab = { target ->
                        when (target) {
                            "COPILOT" -> selectedTab = SharinganNavigationTab.COPILOT
                            "PERFORMANCE" -> selectedTab = SharinganNavigationTab.PERFORMANCE
                            "CROSSHAIR" -> selectedTab = SharinganNavigationTab.CROSSHAIR
                            "NETWORK" -> selectedTab = SharinganNavigationTab.NETWORK
                            "LAG" -> selectedTab = SharinganNavigationTab.LAG_DIAG
                            "SETTINGS" -> selectedTab = SharinganNavigationTab.CORE_AI
                        }
                    }
                )
                SharinganNavigationTab.CROSSHAIR -> CrosshairEngineScreen(ravexPrefs = ravexPrefs)
                SharinganNavigationTab.PROFILES -> GameProfilesScreen(ravexPrefs = ravexPrefs)
                SharinganNavigationTab.PERFORMANCE -> PhoneHealthLandscapeScreen()
                SharinganNavigationTab.NETWORK -> SmartNetworkScreen()
                SharinganNavigationTab.HUD_SETTINGS -> PersonalizedHudSettingsScreen(ravexPrefs = ravexPrefs)
                SharinganNavigationTab.CORE_AI -> RavexCoreAiScreen(ravexPrefs = ravexPrefs)
            }
        }
    }
}
