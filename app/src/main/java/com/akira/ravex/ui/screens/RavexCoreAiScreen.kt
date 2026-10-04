package com.akira.ravex.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.ravex.ai.*
import com.akira.ravex.data.RavexPreferences
import com.akira.ravex.ui.theme.*
import com.akira.ravex.util.RavexSecurity
import kotlinx.coroutines.launch

@Composable
fun RavexCoreAiScreen(ravexPrefs: RavexPreferences) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val router = remember { AiRequestRouter(context) }

    var selectedProvider by remember { mutableStateOf(AiProvider.GROQ) }
    var apiKeyInput by remember { mutableStateOf(RavexSecurity.getApiKey(context, selectedProvider.id)) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf("Not Tested") }
    var isTesting by remember { mutableStateOf(false) }
    var isDiscovering by remember { mutableStateOf(false) }

    var discoveredModels by remember { mutableStateOf(router.getCachedModels(selectedProvider)) }
    var searchQuery by remember { mutableStateOf("") }

    // Refresh state when provider tab changes
    LaunchedEffect(selectedProvider) {
        apiKeyInput = RavexSecurity.getApiKey(context, selectedProvider.id)
        connectionStatus = if (apiKeyInput.isNotBlank()) "Key Configured" else "Key Required"
        discoveredModels = router.getCachedModels(selectedProvider)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "RAVEX CORE — AI PROVIDER CENTER",
            color = RavexCyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Configure Groq, OpenRouter, Google Gemini, or Custom OpenAI API keys with hardware-encrypted Android KeyStore storage.",
            color = RavexTextMuted,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Provider Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(RavexSurface, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AiProvider.values().forEach { provider ->
                val isSelected = selectedProvider == provider
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSelected) RavexCyan.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(6.dp))
                        .border(1.dp, if (isSelected) RavexCyan else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { selectedProvider = provider }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = provider.displayName.replace(" API", ""),
                        color = if (isSelected) RavexCyan else RavexTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Key Configuration Card
        Card(
            colors = CardDefaults.cardColors(containerColor = RavexSurface),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedProvider.displayName} Credentials",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = connectionStatus,
                        color = when {
                            connectionStatus.contains("Connected") || connectionStatus.contains("Valid") -> Color.Green
                            connectionStatus.contains("Error") || connectionStatus.contains("Invalid") -> RavexRed
                            else -> RavexGold
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("API Key", fontSize = 11.sp, color = RavexTextMuted) },
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RavexCyan,
                        unfocusedBorderColor = RavexSurfaceVariant,
                        focusedLabelColor = RavexCyan,
                        cursorColor = RavexCyan,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    trailingIcon = {
                        Text(
                            text = if (isKeyVisible) "HIDE" else "SHOW",
                            color = RavexCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { isKeyVisible = !isKeyVisible }
                                .padding(8.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            RavexSecurity.saveApiKey(context, selectedProvider.id, apiKeyInput.trim())
                            Toast.makeText(context, "${selectedProvider.displayName} key saved securely!", Toast.LENGTH_SHORT).show()
                            connectionStatus = "Key Saved"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RavexCyan),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SAVE KEY", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = {
                            isTesting = true
                            connectionStatus = "Testing..."
                            scope.launch {
                                val adapter = router.getAdapter(selectedProvider)
                                val success = adapter.testConnection(context)
                                isTesting = false
                                connectionStatus = if (success) "Connected & Valid" else "Connection Failed"
                            }
                        },
                        border = BorderStroke(1.dp, RavexCyan),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isTesting) "TESTING..." else "TEST CONNECTION", color = RavexCyan, fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = {
                            RavexSecurity.clearApiKey(context, selectedProvider.id)
                            apiKeyInput = ""
                            connectionStatus = "Key Cleared"
                            Toast.makeText(context, "Key removed.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RavexRed),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("DELETE", color = RavexRed, fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Model Discovery Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DISCOVERED MODELS (${discoveredModels.size})",
                color = RavexCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    isDiscovering = true
                    scope.launch {
                        discoveredModels = router.discoverAndCacheModels(selectedProvider)
                        isDiscovering = false
                        Toast.makeText(context, "Retrieved ${discoveredModels.size} models", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Models",
                    tint = RavexCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Model Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter model list...", fontSize = 10.sp, color = RavexTextMuted) },
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RavexTextMuted, modifier = Modifier.size(14.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RavexCyan,
                unfocusedBorderColor = RavexSurfaceVariant,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Model List Items
        val filteredModels = discoveredModels.filter { it.name.contains(searchQuery, ignoreCase = true) || it.id.contains(searchQuery, ignoreCase = true) }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (filteredModels.isEmpty()) {
                Text(
                    text = if (isDiscovering) "Discovering real provider models..." else "No models cached or retrieved. Enter key and tap refresh icon.",
                    color = RavexTextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(12.dp)
                )
            } else {
                filteredModels.forEach { model ->
                    val isDefault = ravexPrefs.defaultModel == model.id
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isDefault) RavexSurfaceVariant else RavexSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable {
                                ravexPrefs.defaultProvider = selectedProvider.id
                                ravexPrefs.defaultModel = model.id
                                Toast.makeText(context, "Set ${model.id} as default model!", Toast.LENGTH_SHORT).show()
                            },
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ID: ${model.id} | Vision: ${if (model.supportsVision) "YES" else "NO"}",
                                    color = RavexTextMuted,
                                    fontSize = 9.sp
                                )
                            }
                            if (isDefault) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = RavexCyan, modifier = Modifier.size(14.dp))
                                    Text("DEFAULT", color = RavexCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
