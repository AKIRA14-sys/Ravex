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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
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

enum class NavigationTab(val label: String, val icon: ImageVector) {
    DASHBOARD("GameForge", Icons.Default.Home),
    CROSSHAIR("Crosshairs", Icons.Default.Build),
    THERMAL("Thermal", Icons.Default.Lock),
    NETWORK("Network", Icons.Default.Star),
    GUARD("Guard", Icons.Default.Info)
}

class MainActivity : ComponentActivity() {

    private lateinit var ravexPrefs: RavexPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RavexGuard.init(this)
        ravexPrefs = RavexPreferences(this)

        // Start background thermal guard if enabled
        if (ravexPrefs.isThermalGuardEnabled) {
            startService(Intent(this, ThermalGuardService::class.java))
        }

        // Start overlay service if HUD enabled & overlay permission granted
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
    var selectedTab by remember { mutableStateOf(NavigationTab.DASHBOARD) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = RavexSurface,
                tonalElevation = 8.dp
            ) {
                NavigationTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RavexRed,
                            selectedTextColor = RavexRed,
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
                NavigationTab.DASHBOARD -> DashboardScreen(
                    ravexPrefs = ravexPrefs,
                    onNavigateToCrosshairs = { selectedTab = NavigationTab.CROSSHAIR },
                    onNavigateToThermalGuard = { selectedTab = NavigationTab.THERMAL }
                )
                NavigationTab.CROSSHAIR -> CrosshairEngineScreen(ravexPrefs = ravexPrefs)
                NavigationTab.THERMAL -> ThermalGuardScreen(ravexPrefs = ravexPrefs)
                NavigationTab.NETWORK -> RavexNetworkScreen()
                NavigationTab.GUARD -> RavexGuardScreen()
            }
        }
    }
}
