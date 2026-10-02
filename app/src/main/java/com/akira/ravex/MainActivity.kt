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
import androidx.compose.material.icons.filled.Settings
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

enum class SharinganNavigationTab(val label: String, val icon: ImageVector) {
    GAMEFORGE("GAMEFORGE", Icons.Default.Home),
    PERFORMANCE("PERFORMANCE", Icons.Default.Info),
    GAMES("GAMES", Icons.Default.Build),
    HUD("HUD", Icons.Default.Lock),
    NETWORK("NETWORK", Icons.Default.Star),
    SETTINGS("SETTINGS", Icons.Default.Settings)
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
                modifier = Modifier.height(44.dp)
            ) {
                SharinganNavigationTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(16.dp)) },
                        label = { Text(tab.label, fontSize = 8.5.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
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
                            "GAMES" -> selectedTab = SharinganNavigationTab.GAMES
                            "PERFORMANCE" -> selectedTab = SharinganNavigationTab.PERFORMANCE
                            "HUD" -> selectedTab = SharinganNavigationTab.HUD
                        }
                    }
                )
                SharinganNavigationTab.PERFORMANCE -> PhoneHealthLandscapeScreen()
                SharinganNavigationTab.GAMES -> CrosshairEngineScreen(ravexPrefs = ravexPrefs)
                SharinganNavigationTab.HUD -> ThermalGuardScreen(ravexPrefs = ravexPrefs)
                SharinganNavigationTab.NETWORK -> RavexNetworkScreen()
                SharinganNavigationTab.SETTINGS -> RavexGuardScreen()
            }
        }
    }
}
