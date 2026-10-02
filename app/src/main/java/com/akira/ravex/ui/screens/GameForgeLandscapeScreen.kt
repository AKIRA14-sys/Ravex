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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.akira.ravex.R
import com.akira.ravex.data.RavexPreferences
import com.akira.ravex.model.GameInfo
import com.akira.ravex.model.SystemMetrics
import com.akira.ravex.service.RavexOverlayService
import com.akira.ravex.service.ThermalGuardService
import com.akira.ravex.ui.components.SharinganEyeView
import com.akira.ravex.ui.theme.*
import com.akira.ravex.util.SystemMonitorUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GameForgeLandscapeScreen(
    ravexPrefs: RavexPreferences,
    onNavigateTab: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var metrics by remember { mutableStateOf(SystemMetrics()) }
    var games by remember { mutableStateOf<List<GameInfo>>(emptyList()) }
    var selectedIndex by remember { mutableStateOf(0) }
    var isBoosting by remember { mutableStateOf(false) }
    var boostMessage by remember { mutableStateOf("") }
    var launchErrorMsg by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        games = SystemMonitorUtil.getInstalledGames(context)
        while (true) {
            metrics = SystemMonitorUtil.getSystemMetrics(context)
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
    ) {
        // Sharingan Ambient Canvas Centerpiece
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            SharinganEyeView(sizeDp = 220.dp)
        }

        // Top Header Bar - Telemetry & Branding
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Brand Logo & Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_ravex_wolf),
                    contentDescription = "Ravex Wolf Logo",
                    modifier = Modifier.size(36.dp)
                )
                Column {
                    Text(
                        text = "AKIRA RAVEX",
                        color = RavexCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "GAMEFORGE 2.0",
                        color = RavexTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Tech Telemetry HUD Header Widget
            SharinganTelemetryHeader(metrics = metrics)
        }

        // Center PLAY Button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 60.dp),
            contentAlignment = Alignment.Center
        ) {
            val selectedGame = games.getOrNull(selectedIndex)

            Button(
                onClick = {
                    if (selectedGame != null) {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(selectedGame.packageName)
                        if (launchIntent != null) {
                            if (ravexPrefs.isHudEnabled && Settings.canDrawOverlays(context)) {
                                context.startService(Intent(context, RavexOverlayService::class.java))
                            }
                            context.startActivity(launchIntent)
                        } else {
                            launchErrorMsg = "GAME COULD NOT BE LAUNCHED"
                        }
                    } else {
                        launchErrorMsg = "NO REAL INSTALLED GAME SELECTED"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RavexCyan),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .width(180.dp)
                    .height(46.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = RavexBlack,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "PLAY",
                        color = RavexBlack,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        // Bottom Real Games Carousel Section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (games.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RavexSurface)
                        .border(1.dp, RavexSurfaceVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO INSTALLED GAMES DETECTED - ADD GAMES TO LIBRARY",
                        color = RavexTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (selectedIndex > 0) selectedIndex--
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Prev", tint = RavexCyan)
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(games) { index, game ->
                            SharinganGameTile(
                                game = game,
                                isSelected = index == selectedIndex,
                                onSelect = { selectedIndex = index }
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            if (selectedIndex < games.size - 1) selectedIndex++
                        }
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next", tint = RavexCyan)
                    }
                }
            }
        }

        // Error message popup toast if game cannot launch
        if (launchErrorMsg.isNotEmpty()) {
            LaunchedEffect(launchErrorMsg) {
                delay(2500)
                launchErrorMsg = ""
            }
            Surface(
                color = RavexRed,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
            ) {
                Text(
                    text = launchErrorMsg,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SharinganTelemetryHeader(metrics: SystemMetrics) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(RavexSurface.copy(alpha = 0.85f))
            .border(1.dp, RavexCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TelemetryHeaderItem(label = "RAM", value = "${metrics.ramPercentage.toInt()}%", isAlert = metrics.ramPercentage > 85f)
        TelemetryHeaderItem(label = "FPS", value = "${metrics.fps}", isAlert = false)
        TelemetryHeaderItem(label = "TEMP", value = "${metrics.batteryTempC.toInt()}°C", isAlert = metrics.batteryTempC >= 40f)
        TelemetryHeaderItem(label = "BATTERY", value = "${metrics.batteryLevel}%", isAlert = metrics.batteryLevel < 15)
        TelemetryHeaderItem(label = "PING", value = "${metrics.pingMs}ms", isAlert = metrics.pingMs > 100)
    }
}

@Composable
fun TelemetryHeaderItem(label: String, value: String, isAlert: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = RavexTextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(
            text = value,
            color = if (isAlert) RavexRed else RavexCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun SharinganGameTile(
    game: GameInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (isSelected) RavexCyan else RavexSurfaceVariant
    val bgColor = if (isSelected) RavexSurfaceVariant else RavexSurface

    Card(
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(130.dp)
            .height(72.dp)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val iconDrawable = game.icon
            if (iconDrawable != null) {
                val bitmap = remember(iconDrawable) { iconDrawable.toBitmap().asImageBitmap() }
                Image(
                    bitmap = bitmap,
                    contentDescription = game.appName,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(RavexRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = game.appName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Column {
                Text(
                    text = game.appName,
                    color = RavexTextWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = if (isSelected) "● OPTIMIZED" else "● READY",
                    color = if (isSelected) RavexCyan else RavexGreen,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
