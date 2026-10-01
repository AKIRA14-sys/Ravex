package com.akira.ravex.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.ravex.ui.theme.*
import com.akira.ravex.util.RavexGuard

@Composable
fun RavexGuardScreen() {
    val context = LocalContext.current
    var crashLogs by remember { mutableStateOf(RavexGuard.getCrashLogs(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = RavexGold, modifier = Modifier.size(32.dp))
                Column {
                    Text("RAVEX GUARD", color = RavexGold, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Local Crash Diagnostic & Stability Monitor", color = RavexTextMuted, fontSize = 11.sp)
                }
            }

            IconButton(
                onClick = {
                    RavexGuard.clearCrashLogs(context)
                    crashLogs = RavexGuard.getCrashLogs(context)
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = RavexRed)
            }
        }

        // Diagnostics Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(RavexSurface)
                .border(1.dp, RavexSurfaceVariant, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = crashLogs,
                    color = RavexCyan,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}
