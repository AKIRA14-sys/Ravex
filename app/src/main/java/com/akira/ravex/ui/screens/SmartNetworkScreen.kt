package com.akira.ravex.ui.screens

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import com.akira.ravex.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

@Composable
fun SmartNetworkScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val router = remember { AiRequestRouter(context) }

    var isTesting by remember { mutableStateOf(false) }
    var connectionType by remember { mutableStateOf("Detecting Network...") }
    var minPing by remember { mutableStateOf(0) }
    var avgPing by remember { mutableStateOf(0) }
    var maxPing by remember { mutableStateOf(0) }
    var jitterMs by remember { mutableStateOf(0) }
    var aiNetworkExplanation by remember { mutableStateOf<String?>(null) }

    fun runNetworkProbe() {
        isTesting = true
        scope.launch {
            withContext(Dispatchers.IO) {
                val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = cm.activeNetwork
                val caps = cm.getNetworkCapabilities(activeNetwork)

                connectionType = when {
                    caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi (5GHz / 2.4GHz)"
                    caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile Cellular (4G / 5G)"
                    else -> "Unknown / Disconnected"
                }

                // Execute ping probe to reliable global DNS
                val samples = mutableListOf<Long>()
                for (i in 1..5) {
                    val start = System.currentTimeMillis()
                    try {
                        val address = InetAddress.getByName("8.8.8.8")
                        val reachable = address.isReachable(1000)
                        val end = System.currentTimeMillis()
                        if (reachable) samples.add(end - start)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (samples.isNotEmpty()) {
                    minPing = samples.minOrNull()?.toInt() ?: 20
                    maxPing = samples.maxOrNull()?.toInt() ?: 60
                    avgPing = samples.average().toInt()
                    jitterMs = (maxPing - minPing).coerceAtLeast(0)
                } else {
                    minPing = 0
                    maxPing = 0
                    avgPing = 0
                    jitterMs = 0
                }
            }

            // AI Explanation
            val prompt = "Analyze network diagnostic results for gaming: ConnectionType=$connectionType, AvgPing=${avgPing}ms, MinPing=${minPing}ms, MaxPing=${maxPing}ms, Jitter=${jitterMs}ms. Explain packet stability for competitive FPS gaming."
            val resp = router.executeRequest(
                feature = AiFeatureModule.LAG_DIAGNOSIS,
                messages = listOf(AiChatMessage("user", prompt))
            )
            isTesting = false
            aiNetworkExplanation = resp.text
        }
    }

    LaunchedEffect(Unit) {
        runNetworkProbe()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("RAVEX SMART NETWORK DIAGNOSIS", color = RavexCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Real ping probes, latency jitter, and connection quality analysis", color = RavexTextMuted, fontSize = 10.sp)
            }
            IconButton(onClick = { runNetworkProbe() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Probe Network", tint = RavexCyan, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Connection Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = RavexSurface),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("INTERFACE", color = RavexTextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text(connectionType, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = RavexSurface),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("AVG LATENCY", color = RavexTextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text("${avgPing}ms", color = if (avgPing > 80) RavexRed else RavexGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = RavexSurface),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("JITTER VARIATION", color = RavexTextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text("${jitterMs}ms", color = if (jitterMs > 15) RavexRed else RavexCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // AI Network Explanation Card
        Card(
            colors = CardDefaults.cardColors(containerColor = RavexSurface),
            border = BorderStroke(1.dp, RavexCyan.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("NETWORK QUALITY ANALYSIS", color = RavexGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(6.dp))

                if (isTesting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = RavexCyan, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sending latency probes & measuring jitter...", color = RavexTextMuted, fontSize = 10.sp)
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item {
                            Text(
                                text = aiNetworkExplanation ?: "Network test ready.",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
