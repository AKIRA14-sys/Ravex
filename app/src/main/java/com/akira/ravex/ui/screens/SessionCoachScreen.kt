package com.akira.ravex.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SessionRecord(
    val id: String,
    val gameName: String,
    val durationSeconds: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun SessionCoachScreen(
    ravexPrefs: RavexPreferences,
    activeGameName: String = "General Gaming"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val router = remember { AiRequestRouter(context) }

    var isSessionActive by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var sessionHistory by remember { mutableStateOf(mutableListOf<SessionRecord>()) }

    var hydrationReminderEnabled by remember { mutableStateOf(true) }
    var breakReminderEnabled by remember { mutableStateOf(true) }

    var isGeneratingSummary by remember { mutableStateOf(false) }
    var aiSessionSummary by remember { mutableStateOf<String?>(null) }

    // Session Timer Ticker
    LaunchedEffect(isSessionActive) {
        while (isSessionActive) {
            delay(1000L)
            elapsedSeconds += 1
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("RAVEX GAMING SESSION COACH", color = RavexCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Track gaming sessions, receive hydration/break reminders, and get AI post-match coaching summaries.", color = RavexTextMuted, fontSize = 10.sp)

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Live Session Control Panel
            Card(
                colors = CardDefaults.cardColors(containerColor = RavexSurface),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ACTIVE GAME SESSION", color = RavexGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(activeGameName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(formattedTime, color = RavexCyan, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(if (isSessionActive) "SESSION RUNNING" else "SESSION STOPPED", color = if (isSessionActive) RavexGreen else RavexTextMuted, fontSize = 9.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (!isSessionActive) {
                                    isSessionActive = true
                                } else {
                                    isSessionActive = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSessionActive) RavexRed else RavexGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isSessionActive) "PAUSE" else "START", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (elapsedSeconds > 0) {
                                    val rec = SessionRecord("s_${System.currentTimeMillis()}", activeGameName, elapsedSeconds)
                                    sessionHistory.add(0, rec)

                                    // Generate AI Summary
                                    isGeneratingSummary = true
                                    scope.launch {
                                        val prompt = "Summarize post-gaming session performance: Game=$activeGameName, Duration=${elapsedSeconds / 60} minutes. Provide 2 coaching bullet points for wrist fatigue recovery and aim consistency."
                                        val resp = router.executeRequest(
                                            feature = AiFeatureModule.SESSION_COACH,
                                            messages = listOf(AiChatMessage("user", prompt))
                                        )
                                        isGeneratingSummary = false
                                        aiSessionSummary = resp.text
                                    }

                                    isSessionActive = false
                                    elapsedSeconds = 0
                                    Toast.makeText(context, "Session saved!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RavexCyan),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("FINISH & COACH", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hydration Alerts (20 min)", color = Color.White, fontSize = 10.sp)
                        Switch(checked = hydrationReminderEnabled, onCheckedChange = { hydrationReminderEnabled = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Break / Rest Alerts (45 min)", color = Color.White, fontSize = 10.sp)
                        Switch(checked = breakReminderEnabled, onCheckedChange = { breakReminderEnabled = it })
                    }
                }
            }

            // AI Session Coaching & History
            Card(
                colors = CardDefaults.cardColors(containerColor = RavexSurface),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("AI POST-SESSION COACHING", color = RavexCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isGeneratingSummary) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = RavexCyan, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analyzing session telemetry & generating summary...", color = RavexTextMuted, fontSize = 10.sp)
                        }
                    } else {
                        Text(
                            text = aiSessionSummary ?: "Complete a session and tap 'FINISH & COACH' to generate AI coaching feedback.",
                            color = Color.White,
                            fontSize = 10.sp,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("SESSION HISTORY", color = RavexGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (sessionHistory.isEmpty()) {
                            Text("No recorded sessions yet.", color = RavexTextMuted, fontSize = 10.sp, modifier = Modifier.padding(vertical = 4.dp))
                        } else {
                            sessionHistory.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .background(RavexSurfaceVariant, RoundedCornerShape(4.dp))
                                        .padding(6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.gameName, color = Color.White, fontSize = 10.sp)
                                    Text("${item.durationSeconds / 60} min ${item.durationSeconds % 60} sec", color = RavexCyan, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
