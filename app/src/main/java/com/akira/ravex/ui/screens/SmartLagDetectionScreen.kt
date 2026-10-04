package com.akira.ravex.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.ravex.ai.AiChatMessage
import com.akira.ravex.ai.AiFeatureModule
import com.akira.ravex.ai.AiRequestRouter
import com.akira.ravex.data.RavexPreferences
import com.akira.ravex.ui.theme.*
import com.akira.ravex.util.SystemMonitorUtil
import kotlinx.coroutines.launch

@Composable
fun SmartLagDetectionScreen(ravexPrefs: RavexPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val router = remember { AiRequestRouter(context) }

    var metrics by remember { mutableStateOf(SystemMonitorUtil.getSystemMetrics(context)) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var aiAnalysisResult by remember { mutableStateOf<String?>(null) }

    fun runDiagnostics() {
        metrics = SystemMonitorUtil.getSystemMetrics(context)
        isAnalyzing = true

        scope.launch {
            val prompt = """
                Analyze current gaming phone diagnostics:
                - Memory: ${metrics.usedRamMb}MB used / ${metrics.totalRamMb}MB total (${metrics.ramPercentage.toInt()}%)
                - Battery Temp: ${metrics.batteryTempC}°C
                - Thermal State Level: ${metrics.thermalState}
                - Battery Level: ${metrics.batteryLevel}% (Charging: ${metrics.isCharging})
                - Network Ping Estimate: ${metrics.pingMs}ms

                Provide 3 concise, highly accurate evidence-backed conclusions explaining potential lag causes and actionable steps.
            """.trimIndent()

            val response = router.executeRequest(
                feature = AiFeatureModule.LAG_DIAGNOSIS,
                messages = listOf(AiChatMessage("user", prompt))
            )
            isAnalyzing = false
            aiAnalysisResult = response.text
        }
    }

    LaunchedEffect(Unit) {
        runDiagnostics()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("RAVEX SMART LAG DETECTION", color = RavexCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Honest hardware telemetry & lag cause diagnosis", color = RavexTextMuted, fontSize = 10.sp)
            }

            IconButton(onClick = { runDiagnostics() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Diagnostics", tint = RavexCyan, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Hardware Evidence Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Memory Card
            Card(
                colors = CardDefaults.cardColors(containerColor = RavexSurface),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("RAM PRESSURE", color = RavexTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("${metrics.ramPercentage.toInt()}%", color = if (metrics.ramPercentage > 85) RavexRed else RavexCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${metrics.usedRamMb}MB / ${metrics.totalRamMb}MB", color = Color.White, fontSize = 8.sp)
                }
            }

            // Thermal Card
            Card(
                colors = CardDefaults.cardColors(containerColor = RavexSurface),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("TEMPERATURE", color = RavexTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("${metrics.batteryTempC}°C", color = if (metrics.batteryTempC >= 40) RavexRed else RavexGreen, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(if (metrics.batteryTempC >= 40) "WARM / THROTTLE" else "OPTIMAL", color = Color.White, fontSize = 8.sp)
                }
            }

            // Latency Card
            Card(
                colors = CardDefaults.cardColors(containerColor = RavexSurface),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("NETWORK PING", color = RavexTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("${metrics.pingMs}ms", color = if (metrics.pingMs > 80) RavexRed else RavexCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(if (metrics.isNetworkOnline) "ONLINE" else "DISCONNECTED", color = Color.White, fontSize = 8.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // AI Diagnostic Evidence Summary
        Card(
            colors = CardDefaults.cardColors(containerColor = RavexSurface),
            border = BorderStroke(1.dp, RavexCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = RavexGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DIAGNOSTIC EVIDENCE & ANALYSIS", color = RavexGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isAnalyzing) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RavexCyan, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing system telemetry evidence...", color = RavexTextMuted, fontSize = 11.sp)
                    }
                } else {
                    Text(
                        text = aiAnalysisResult ?: "Diagnostic ready. Click refresh to perform lag analysis.",
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
