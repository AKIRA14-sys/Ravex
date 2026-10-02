package com.akira.ravex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.akira.ravex.model.SystemMetrics
import com.akira.ravex.ui.theme.*
import com.akira.ravex.util.SystemMonitorUtil
import kotlinx.coroutines.delay

@Composable
fun PhoneHealthLandscapeScreen() {
    val context = LocalContext.current
    var metrics by remember { mutableStateOf(SystemMetrics()) }

    LaunchedEffect(Unit) {
        while (true) {
            metrics = SystemMonitorUtil.getSystemMetrics(context)
            delay(1000)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Left Column: CPU & Memory Hardware Telemetry
        Card(
            colors = CardDefaults.cardColors(containerColor = RavexSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(1.dp, RavexCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("RAVEX DEVICE HEALTH & HARDWARE READOUT", color = RavexCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                HealthMetricRow("SYSTEM RAM", "${metrics.usedRamMb} MB / ${metrics.totalRamMb} MB (${metrics.ramPercentage.toInt()}%)")
                HealthMetricRow("DISPLAY REFRESH RATE", "${metrics.refreshRate} Hz")
                HealthMetricRow("BATTERY LEVEL", "${metrics.batteryLevel}% (${if (metrics.isCharging) "CHARGING" else "DISCHARGING"})")
                HealthMetricRow("BATTERY TEMP", "${metrics.batteryTempC}°C")
                HealthMetricRow("BATTERY VOLTAGE", "${metrics.batteryVoltage} mV")
                HealthMetricRow("THERMAL STATE", "STAGE ${metrics.thermalState}")
            }
        }

        // Right Column: Hardware Safety Rules Notice
        Card(
            colors = CardDefaults.cardColors(containerColor = RavexSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(1.dp, RavexSurfaceVariant, RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("HARDWARE INTEGRITY GUARANTEE", color = RavexRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                Text(
                    text = "RAVEX Phone Health is a pure telemetry readout monitoring system. It reads genuine Android kernel metrics and NEVER overclocks CPU/GPU, undervolts hardware, or tampers with system kernel settings.",
                    color = RavexTextWhite,
                    fontSize = 11.sp
                )

                Text(
                    text = "• Genuine RAM Measurement via ActivityManager\n• Genuine Thermal Readings via Android PowerManager\n• Zero Fake Boost Claims or Artificial Multipliers",
                    color = RavexTextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun HealthMetricRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = RavexTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(value, color = RavexCyan, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}
