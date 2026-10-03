package com.akira.ravex.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun AiTroubleshooterScreen(ravexPrefs: RavexPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val router = remember { AiRequestRouter(context) }

    val commonIssues = listOf(
        "Floating Overlay Not Appearing",
        "FPS Drops & Thermal Stutter",
        "High Ping & Network Spikes",
        "Game Launch Crash / Black Screen",
        "Mic / Voice Chat Permission Denied",
        "API Key Validation / Model Discovery Error"
    )

    var selectedIssue by remember { mutableStateOf(commonIssues[0]) }
    var customProblemDesc by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var diagnosisSteps by remember { mutableStateOf<String?>(null) }

    fun runTroubleshooting(issueText: String) {
        isAnalyzing = true
        scope.launch {
            val metrics = SystemMonitorUtil.getSystemMetrics(context)
            val prompt = """
                Troubleshoot gaming issue: "$issueText".
                Current telemetry context: RAM Free ${metrics.totalRamMb - metrics.usedRamMb}MB / ${metrics.totalRamMb}MB, Thermal Temp ${metrics.batteryTempC}°C, Charging=${metrics.isCharging}.
                Provide a structured 5-step safe troubleshooting guide. Mark each step as [REVERSIBLE] or [SAFE]. Explain probable root causes based on Android system mechanics.
            """.trimIndent()

            val response = router.executeRequest(
                feature = AiFeatureModule.TROUBLESHOOTER,
                messages = listOf(AiChatMessage("user", prompt))
            )
            isAnalyzing = false
            diagnosisSteps = response.text
        }
    }

    LaunchedEffect(selectedIssue) {
        runTroubleshooting(selectedIssue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(10.dp)
    ) {
        Text("RAVEX AI TROUBLESHOOTER", color = RavexCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Interactive diagnosis for floating overlay, performance, crash, network, and permission issues.", color = RavexTextMuted, fontSize = 10.sp)

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Problem Selector Sidebar
            Card(
                colors = CardDefaults.cardColors(containerColor = RavexSurface),
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("SELECT GAMING ISSUE", color = RavexGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(commonIssues) { issue ->
                            val isSelected = issue == selectedIssue
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .background(if (isSelected) RavexCyan.copy(alpha = 0.2f) else RavexSurfaceVariant, RoundedCornerShape(6.dp))
                                    .border(1.dp, if (isSelected) RavexCyan else Color.Transparent, RoundedCornerShape(6.dp))
                                    .clickable { selectedIssue = issue }
                                    .padding(8.dp)
                            ) {
                                Text(issue, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = customProblemDesc,
                        onValueChange = { customProblemDesc = it },
                        placeholder = { Text("Describe custom problem...", fontSize = 9.sp, color = RavexTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RavexCyan,
                            unfocusedBorderColor = RavexSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (customProblemDesc.isNotBlank()) {
                                selectedIssue = customProblemDesc
                                runTroubleshooting(customProblemDesc)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RavexCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("DIAGNOSE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Diagnostic Results & Action Plan
            Card(
                colors = CardDefaults.cardColors(containerColor = RavexSurface),
                border = BorderStroke(1.dp, RavexCyan.copy(alpha = 0.3f)),
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("TROUBLESHOOTING RESOLUTION GUIDE", color = RavexCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Issue: $selectedIssue", color = RavexTextMuted, fontSize = 9.sp)

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isAnalyzing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RavexCyan, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Running system diagnostics & compiling safe resolution steps...", color = RavexTextMuted, fontSize = 10.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item {
                                Text(
                                    text = diagnosisSteps ?: "Select an issue to view step-by-step resolution steps.",
                                    color = Color.White,
                                    fontSize = 10.5.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
