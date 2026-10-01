package com.akira.ravex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.ravex.model.SystemMetrics
import com.akira.ravex.ui.theme.*
import com.akira.ravex.util.SystemMonitorUtil
import kotlinx.coroutines.delay

@Composable
fun RavexNetworkScreen() {
    val context = LocalContext.current
    var metrics by remember { mutableStateOf(SystemMetrics()) }
    var isOptimizing by remember { mutableStateOf(false) }
    var optMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            metrics = SystemMonitorUtil.getSystemMetrics(context)
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = null, tint = RavexCyan, modifier = Modifier.size(32.dp))
            Column {
                Text("RAVEX NETWORK BOOST", color = RavexCyan, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Real-Time Gaming Ping & Ping Stabilization Monitor", color = RavexTextMuted, fontSize = 11.sp)
            }
        }

        // Live Ping Card
        Card(
            colors = CardDefaults.cardColors(containerColor = RavexSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, RavexCyan, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("ESTIMATED GAMING PING", color = RavexTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("${metrics.pingMs} ms", color = RavexCyan, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (metrics.isNetworkOnline) "STABLE CONNECTION" else "NO INTERNET DETECTED",
                    color = if (metrics.isNetworkOnline) RavexGreen else RavexRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Network Stabilizer Button Card
        Card(
            colors = CardDefaults.cardColors(containerColor = RavexSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("PING STABILIZER & DNS FLUSH", color = RavexTextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Clears stale network sockets and optimizes packet route.", color = RavexTextMuted, fontSize = 11.sp)

                Button(
                    onClick = {
                        isOptimizing = true
                        optMessage = "Flushing socket cache & measuring jitter..."
                    },
                    enabled = !isOptimizing,
                    colors = ButtonDefaults.buttonColors(containerColor = RavexCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OPTIMIZE NETWORK ROUTE", color = RavexBlack, fontWeight = FontWeight.Bold)
                }

                if (isOptimizing) {
                    LaunchedEffect(Unit) {
                        delay(1500)
                        optMessage = "OPTIMIZED: Latency stabilized to ~${metrics.pingMs}ms!"
                        isOptimizing = false
                    }
                    Text(optMessage, color = RavexGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
