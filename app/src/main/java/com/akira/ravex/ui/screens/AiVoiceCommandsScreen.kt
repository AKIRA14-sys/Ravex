package com.akira.ravex.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun AiVoiceCommandsScreen(
    ravexPrefs: RavexPreferences,
    onNavigateTab: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val router = remember { AiRequestRouter(context) }

    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var textFallbackInput by remember { mutableStateOf("") }
    var executionStatus by remember { mutableStateOf("Tap microphone and speak command...") }

    var hasMicPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission required for voice commands.", Toast.LENGTH_SHORT).show()
        }
    }

    fun processCommand(command: String) {
        val lower = command.lowercase(Locale.ROOT)
        recognizedText = command
        executionStatus = "Processing: \"$command\""

        when {
            lower.contains("copilot") || lower.contains("assistant") -> {
                executionStatus = "✅ Action: Navigating to RAVEX AI Copilot"
                onNavigateTab("COPILOT")
            }
            lower.contains("performance") || lower.contains("health") || lower.contains("ram") -> {
                executionStatus = "✅ Action: Opening Phone Health & Performance"
                onNavigateTab("PERFORMANCE")
            }
            lower.contains("crosshair") -> {
                executionStatus = "✅ Action: Opening Crosshair Engine"
                onNavigateTab("CROSSHAIR")
            }
            lower.contains("network") || lower.contains("ping") -> {
                executionStatus = "✅ Action: Opening Network Diagnosis"
                onNavigateTab("NETWORK")
            }
            lower.contains("lag") -> {
                executionStatus = "✅ Action: Running Smart Lag Detection"
                onNavigateTab("LAG")
            }
            lower.contains("ai") || lower.contains("settings") || lower.contains("provider") -> {
                executionStatus = "✅ Action: Opening RAVEX Core AI Settings"
                onNavigateTab("SETTINGS")
            }
            else -> {
                // Natural Language interpretation via AI Router
                scope.launch {
                    executionStatus = "Interpreting natural command via AI..."
                    val resp = router.executeRequest(
                        feature = AiFeatureModule.VOICE_COMMAND,
                        messages = listOf(AiChatMessage("user", "Interpret this voice gaming command and specify action: \"$command\""))
                    )
                    executionStatus = "AI Interpretation: ${resp.text}"
                }
            }
        }
    }

    fun startListening() {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Speech recognition unavailable on this device. Use text fallback.", Toast.LENGTH_SHORT).show()
            return
        }

        val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true; executionStatus = "Listening..." }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false; executionStatus = "Error code: $error. Try speaking again or text fallback." }
            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    processCommand(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer.startListening(intent)
    }

    val sampleCommands = listOf(
        "\"Open my gaming copilot\"",
        "\"Show performance\"",
        "\"Open crosshair settings\"",
        "\"Show network diagnostics\"",
        "\"Explain my lag report\"",
        "\"Open AI settings\""
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("RAVEX AI VOICE COMMANDS", color = RavexCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Hands-free voice control for gaming navigation and copilot", color = RavexTextMuted, fontSize = 10.sp)

        Spacer(modifier = Modifier.height(14.dp))

        // Large PTT Mic Sphere Button
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(if (isListening) RavexRed else RavexSurface, CircleShape)
                .border(2.dp, if (isListening) RavexRed else RavexCyan, CircleShape)
                .clickable { startListening() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (isListening) "LISTENING" else "PUSH TO TALK", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(if (isListening) "🎤" else "🎙️", fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = RavexSurface),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("RECOGNIZED TRANSCRIPTION", color = RavexGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (recognizedText.isBlank()) "No command recorded yet." else "\"$recognizedText\"",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text("EXECUTION STATUS", color = RavexTextMuted, fontSize = 9.sp)
                Text(executionStatus, color = RavexCyan, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Text Fallback Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textFallbackInput,
                onValueChange = { textFallbackInput = it },
                placeholder = { Text("Text fallback command...", fontSize = 10.sp, color = RavexTextMuted) },
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
                onClick = {
                    if (textFallbackInput.isNotBlank()) {
                        processCommand(textFallbackInput)
                        textFallbackInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = RavexCyan)
            ) {
                Text("RUN", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sample Commands Guidance
        Text("SAMPLE VOICE COMMANDS", color = RavexCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(sampleCommands) { cmd ->
                Text(
                    text = "• $cmd",
                    color = RavexTextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { processCommand(cmd.replace("\"", "")) }
                        .padding(vertical = 3.dp)
                )
            }
        }
    }
}
