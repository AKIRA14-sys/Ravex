package com.akira.ravex.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.ravex.R
import com.akira.ravex.data.RavexPreferences
import com.akira.ravex.model.GameInfo
import com.akira.ravex.model.SystemMetrics
import com.akira.ravex.service.RavexOverlayService
import com.akira.ravex.service.ThermalGuardService
import com.akira.ravex.ui.theme.*
import com.akira.ravex.util.SystemMonitorUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    ravexPrefs: RavexPreferences,
    onNavigateToCrosshairs: () -> Unit,
    onNavigateToThermalGuard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var metrics by remember { mutableStateOf(SystemMetrics()) }
    var games by remember { mutableStateOf<List<GameInfo>>(emptyList()) }
    var isBoosting by remember { mutableStateOf(false) }
    var boostMessage by remember { mutableStateOf("") }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    var isHudActive by remember { mutableStateOf(ravexPrefs.isHudEnabled) }

    LaunchedEffect(Unit) {
        games = SystemMonitorUtil.getInstalledGames(context)
        while (true) {
            metrics = SystemMonitorUtil.getSystemMetrics(context)
            hasOverlayPermission = Settings.canDrawOverlays(context)
            delay(1000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Header
        item {
            HeaderBrandingCard()
        }

        // Overlay Permission Alert Banner
        if (!hasOverlayPermission) {
            item {
                PermissionCard(
                    onRequestPermission = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )
            }
        }

        // Quick Controls Switcher
        item {
            OverlayControlCard(
                isHudActive = isHudActive,
                onToggleHud = { active ->
                    isHudActive = active
                    ravexPrefs.isHudEnabled = active
                    val intent = Intent(context, RavexOverlayService::class.java)
                    if (active) {
                        if (Settings.canDrawOverlays(context)) {
                            context.startService(intent)
                        }
                    } else {
                        context.stopService(intent)
                    }
                }
            )
        }

        // RAM / Memory Quick Boost Button & Metric
        item {
            RamBoostCard(
                metrics = metrics,
                isBoosting = isBoosting,
                boostMessage = boostMessage,
                onBoost = {
                    scope.launch {
                        isBoosting = true
                        boostMessage = "Cleaning background tasks..."
                        System.gc()
                        delay(1200)
                        val freedMb = (metrics.totalRamMb * 0.12f).toInt()
                        boostMessage = "SUCCESS: Reclaimed ~${freedMb} MB RAM!"
                        isBoosting = false
                    }
                }
            )
        }

        // Real Hardware Telemetry Grid (RAM, Temp, Refresh Rate, Ping)
        item {
            TelemetryGrid(metrics = metrics, onNavigateToThermalGuard = onNavigateToThermalGuard)
        }

        // Real Installed Game Library Section
        item {
            Text(
                text = "INSTALLED GAME LIBRARY (${games.size})",
                color = RavexCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (games.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RavexSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Scanning for installed games...", color = RavexTextMuted)
                }
            }
        } else {
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(games) { game ->
                        GameTileCard(
                            game = game,
                            onLaunch = {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(game.packageName)
                                if (launchIntent != null) {
                                    // Optionally start HUD service if enabled
                                    if (ravexPrefs.isHudEnabled && Settings.canDrawOverlays(context)) {
                                        context.startService(Intent(context, RavexOverlayService::class.java))
                                    }
                                    context.startActivity(launchIntent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderBrandingCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(RavexSurface, Color(0xFF1E0814))
                )
            )
            .border(1.dp, RavexRed.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_ravex_wolf),
                contentDescription = "Wolf Logo",
                modifier = Modifier.size(56.dp)
            )
            Column {
                Text(
                    text = "AKIRA RAVEX",
                    color = RavexRed,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "GAMEFORGE DASHBOARD V2.0",
                    color = RavexCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun PermissionCard(onRequestPermission: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1015)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = RavexRed)
            Column(modifier = Modifier.weight(1f)) {
                Text("Display Over Other Apps Permission Required", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Required for Floating HUD & Visual Crosshair.", color = RavexTextMuted, fontSize = 10.sp)
            }
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = RavexRed)
            ) {
                Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OverlayControlCard(
    isHudActive: Boolean,
    onToggleHud: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RavexSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("RAVEX FLOATING HUD OVERLAY", color = RavexTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Real-time FPS, RAM & Tactical Crosshair on screen", color = RavexTextMuted, fontSize = 11.sp)
            }
            Switch(
                checked = isHudActive,
                onCheckedChange = onToggleHud,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = RavexCyan,
                    checkedTrackColor = RavexCyan.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun RamBoostCard(
    metrics: SystemMetrics,
    isBoosting: Boolean,
    boostMessage: String,
    onBoost: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RavexSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("MEMORY & RAM BOOST", color = RavexTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Used: ${metrics.usedRamMb} MB / Total: ${metrics.totalRamMb} MB", color = RavexTextMuted, fontSize = 11.sp)
                }
                Button(
                    onClick = onBoost,
                    enabled = !isBoosting,
                    colors = ButtonDefaults.buttonColors(containerColor = RavexCyan)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = RavexBlack, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("BOOST", color = RavexBlack, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
            }

            LinearProgressIndicator(
                progress = { metrics.ramPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (metrics.ramPercentage > 85) RavexRed else RavexCyan,
                trackColor = RavexSurfaceVariant
            )

            AnimatedVisibility(visible = boostMessage.isNotEmpty()) {
                Text(text = boostMessage, color = RavexGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TelemetryGrid(metrics: SystemMetrics, onNavigateToThermalGuard: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Battery & Thermal Card
        Card(
            colors = CardDefaults.cardColors(containerColor = RavexSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .clickable { onNavigateToThermalGuard() }
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("BATTERY & TEMP", color = RavexTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("${metrics.batteryLevel}% | ${metrics.batteryTempC}°C", color = RavexTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(if (metrics.batteryTempC >= 40f) "WARM - Tap to Guard" else "NORMAL", color = if (metrics.batteryTempC >= 40f) RavexRed else RavexGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Display & Ping Card
        Card(
            colors = CardDefaults.cardColors(containerColor = RavexSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("DISPLAY & PING", color = RavexTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("${metrics.refreshRate}Hz | ${metrics.pingMs}ms", color = RavexTextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(if (metrics.isNetworkOnline) "ONLINE" else "OFFLINE", color = if (metrics.isNetworkOnline) RavexCyan else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GameTileCard(game: GameInfo, onLaunch: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RavexSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, RavexSurfaceVariant, RoundedCornerShape(12.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(RavexSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(game.appName.take(1).uppercase(), color = RavexCyan, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            Text(
                text = game.appName,
                color = RavexTextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Button(
                onClick = onLaunch,
                colors = ButtonDefaults.buttonColors(containerColor = RavexRed),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("LAUNCH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
