package com.akira.ravex.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.ravex.data.RavexPreferences
import com.akira.ravex.model.SystemMetrics
import com.akira.ravex.service.ThermalGuardService
import com.akira.ravex.ui.theme.*
import com.akira.ravex.util.SystemMonitorUtil
import kotlinx.coroutines.delay

@Composable
fun ThermalGuardScreen(ravexPrefs: RavexPreferences) {
    val context = LocalContext.current
    var isGuardEnabled by remember { mutableStateOf(ravexPrefs.isThermalGuardEnabled) }
    var metrics by remember { mutableStateOf(SystemMetrics()) }

    LaunchedEffect(Unit) {
        while (true) {
            metrics = SystemMonitorUtil.getSystemMetrics(context)
            delay(1000)
        }
    }

    val thermalStatus = SystemMonitorUtil.getThermalStatus(metrics.batteryTempC)

    val statusColor = when (thermalStatus.warningLevel) {
        com.akira.ravex.model.ThermalStatus.WarningLevel.NORMAL -> RavexGreen
        com.akira.ravex.model.ThermalStatus.WarningLevel.WARM -> RavexGold
        com.akira.ravex.model.ThermalStatus.WarningLevel.HOT -> RavexRed
        com.akira.ravex.model.ThermalStatus.WarningLevel.CRITICAL -> RavexRed
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = RavexRed, modifier = Modifier.size(32.dp))
            Column {
                Text("RAVEX THERMAL GUARD", color = RavexRed, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Overheating Guard & System Cooldown System", color = RavexCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Active Temperature Gauge Card
        Card(
            colors = CardDefaults.cardColors(containerColor = RavexSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, statusColor, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(3.dp, statusColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${metrics.batteryTempC}°C",
                        color = statusColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = thermalStatus.warningLevel.name,
                    color = statusColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = thermalStatus.statusMessage,
                    color = RavexTextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Background Guard Toggle
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
                    Text("BACKGROUND OVERHEAT MONITOR", color = RavexTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Auto-alert when battery temp exceeds 42°C", color = RavexTextMuted, fontSize = 11.sp)
                }
                Switch(
                    checked = isGuardEnabled,
                    onCheckedChange = { active ->
                        isGuardEnabled = active
                        ravexPrefs.isThermalGuardEnabled = active
                        val intent = Intent(context, ThermalGuardService::class.java)
                        if (active) {
                            context.startService(intent)
                        } else {
                            context.stopService(intent)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RavexRed,
                        checkedTrackColor = RavexRed.copy(alpha = 0.3f)
                    )
                )
            }
        }

        // Thermal Guidelines & Safeguards
        Card(
            colors = CardDefaults.cardColors(containerColor = RavexSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("THERMAL SAFEGUARD METRICS", color = RavexCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("• < 36°C: Optimal gaming performance.", color = RavexTextWhite, fontSize = 11.sp)
                Text("• 36°C - 42°C: Warm. Normal during heavy 60+ FPS gaming.", color = RavexTextWhite, fontSize = 11.sp)
                Text("• > 42°C: High temp. Auto notification triggered to prevent throttling.", color = RavexRed, fontSize = 11.sp)
            }
        }
    }
}
