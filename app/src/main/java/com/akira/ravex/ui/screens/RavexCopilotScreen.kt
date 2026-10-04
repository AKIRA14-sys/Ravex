package com.akira.ravex.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.ravex.ai.*
import com.akira.ravex.data.RavexPreferences
import com.akira.ravex.ui.theme.*
import com.akira.ravex.util.SystemMonitorUtil
import kotlinx.coroutines.launch

@Composable
fun RavexCopilotScreen(
    ravexPrefs: RavexPreferences,
    gameContextName: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val router = remember { AiRequestRouter(context) }
    val listState = rememberLazyListState()

    var chatMessages by remember { mutableStateOf(mutableListOf<AiChatMessage>()) }
    var userPromptText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val activeProviderId = ravexPrefs.getFeatureProvider(AiFeatureModule.COPILOT.key)
    val activeProvider = AiProvider.fromId(activeProviderId)
    val activeModel = ravexPrefs.getFeatureModel(AiFeatureModule.COPILOT.key).ifBlank { ravexPrefs.defaultModel }

    val suggestedPrompts = listOf(
        "Give me optimal sensitivity & gyro settings for ${gameContextName ?: "PUBG / COD Mobile"}.",
        "Help me understand my device lag and memory pressure.",
        "Recommend a crosshair style for fast-paced FPS games.",
        "Prepare my phone before I launch a competitive match."
    )

    fun sendUserMessage(prompt: String) {
        if (prompt.isBlank() || isGenerating) return

        val userMsg = AiChatMessage(role = "user", content = prompt)
        chatMessages.add(userMsg)
        userPromptText = ""
        isGenerating = true

        scope.launch {
            listState.animateScrollToItem((chatMessages.size - 1).coerceAtLeast(0))

            val systemMetrics = SystemMonitorUtil.getSystemMetrics(context)
            val freeRam = systemMetrics.totalRamMb - systemMetrics.usedRamMb
            val systemContext = "Device Info: RAM Free ${freeRam}MB / ${systemMetrics.totalRamMb}MB (${systemMetrics.ramPercentage.toInt()}%), Thermal: ${systemMetrics.batteryTempC}°C, Game Mode: ${ravexPrefs.gameMode}, Target Game: ${gameContextName ?: "General Gaming"}"
            val systemPrompt = "You are RAVEX AI Gaming Copilot, an expert esports coach and hardware optimizer. Provide concise, high-value advice. $systemContext"

            val response = router.executeRequest(
                feature = AiFeatureModule.COPILOT,
                messages = chatMessages.toList(),
                systemPrompt = systemPrompt
            )

            isGenerating = false
            if (response.isSuccess) {
                chatMessages.add(AiChatMessage(role = "assistant", content = response.text))
            } else {
                chatMessages.add(AiChatMessage(role = "assistant", content = "❌ Error: ${response.errorMessage}"))
            }
            listState.animateScrollToItem((chatMessages.size - 1).coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(10.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RavexSurface, RoundedCornerShape(8.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("RAVEX AI GAMING COPILOT", color = RavexCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Active: ${activeProvider.displayName} ($activeModel)", color = RavexTextMuted, fontSize = 9.sp)
            }
            IconButton(
                onClick = {
                    chatMessages.clear()
                    Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Chat", tint = RavexRed, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Suggested Prompts Carousel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            suggestedPrompts.take(2).forEach { prompt ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(RavexSurfaceVariant, RoundedCornerShape(6.dp))
                        .border(1.dp, RavexCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .clickable { sendUserMessage(prompt) }
                        .padding(6.dp)
                ) {
                    Text(prompt, color = RavexTextWhite, fontSize = 9.sp, maxLines = 2)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message List with Vertical Touch Scrolling
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (chatMessages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Ask the RAVEX AI Copilot anything about sensitivity, graphics optimization, lag diagnostics, or competitive gaming strategies!",
                            color = RavexTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            } else {
                items(chatMessages) { msg ->
                    val isUser = msg.role == "user"
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            color = if (isUser) RavexCyan.copy(alpha = 0.2f) else RavexSurface,
                            border = BorderStroke(1.dp, if (isUser) RavexCyan else RavexSurfaceVariant),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isUser) "YOU" else "RAVEX AI COPILOT",
                                    color = if (isUser) RavexCyan else RavexGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.content,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )

                                if (!isUser) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "COPY RESPONSE",
                                        color = RavexCyan,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Copilot Response", msg.content)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Input Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userPromptText,
                onValueChange = { userPromptText = it },
                placeholder = { Text("Ask Gaming Copilot...", fontSize = 11.sp, color = RavexTextMuted) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = RavexCyan,
                    unfocusedBorderColor = RavexSurfaceVariant,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(6.dp))

            Button(
                onClick = { sendUserMessage(userPromptText) },
                enabled = !isGenerating && userPromptText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = RavexCyan),
                shape = RoundedCornerShape(6.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
