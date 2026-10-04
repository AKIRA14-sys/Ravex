package com.akira.ravex.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.ravex.data.RavexPreferences
import com.akira.ravex.model.GameProfile
import com.akira.ravex.ui.theme.*
import com.akira.ravex.util.SystemMonitorUtil
import com.google.gson.Gson

@Composable
fun GameProfilesScreen(ravexPrefs: RavexPreferences) {
    val context = LocalContext.current
    val gson = Gson()

    var profilesMap by remember { mutableStateOf(ravexPrefs.getGameProfiles()) }
    var selectedPackageName by remember { mutableStateOf<String?>(profilesMap.keys.firstOrNull()) }

    var installedGamesList by remember { mutableStateOf(emptyList<com.akira.ravex.model.GameInfo>()) }

    LaunchedEffect(Unit) {
        installedGamesList = SystemMonitorUtil.getInstalledGames(context)
    }

    val activeProfile = selectedPackageName?.let { profilesMap[it] }

    var targetFps by remember { mutableStateOf(activeProfile?.targetFps ?: 60) }
    var assignedCrosshairId by remember { mutableStateOf(activeProfile?.assignedCrosshairId ?: "preset_default_1") }
    var customNotes by remember { mutableStateOf(activeProfile?.customNotes ?: "") }

    LaunchedEffect(selectedPackageName) {
        activeProfile?.let { p ->
            targetFps = p.targetFps
            assignedCrosshairId = p.assignedCrosshairId
            customNotes = p.customNotes
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("RAVEX SMART GAME PROFILES", color = RavexCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Per-game graphics preferences, sensitivity notes, crosshair mapping, and custom layout profiles.", color = RavexTextMuted, fontSize = 10.sp)

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Profile Selector Sidebar
            Card(
                colors = CardDefaults.cardColors(containerColor = RavexSurface),
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("SAVED PROFILES", color = RavexGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        profilesMap.values.forEach { profile ->
                            val isSelected = profile.packageName == selectedPackageName
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .background(if (isSelected) RavexCyan.copy(alpha = 0.2f) else RavexSurfaceVariant, RoundedCornerShape(6.dp))
                                    .border(1.dp, if (isSelected) RavexCyan else Color.Transparent, RoundedCornerShape(6.dp))
                                    .clickable { selectedPackageName = profile.packageName }
                                    .padding(8.dp)
                            ) {
                                Text(profile.gameName, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Add Profile from installed games
                    Button(
                        onClick = {
                            val firstUnsaved = installedGamesList.firstOrNull { !profilesMap.containsKey(it.packageName) }
                            if (firstUnsaved != null) {
                                val newP = GameProfile(firstUnsaved.packageName, firstUnsaved.appName, "preset_default_1", 60, "")
                                ravexPrefs.saveGameProfile(newP)
                                profilesMap = ravexPrefs.getGameProfiles()
                                selectedPackageName = newP.packageName
                                Toast.makeText(context, "Added profile for ${newP.gameName}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "All detected games already have profiles!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RavexCyan),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ADD GAME", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Profile Configuration Details
            Card(
                colors = CardDefaults.cardColors(containerColor = RavexSurface),
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (activeProfile == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select or add a game profile on the left.", color = RavexTextMuted, fontSize = 11.sp)
                    }
                } else {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(activeProfile.gameName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                val json = gson.toJson(activeProfile)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Game Profile JSON", json)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Exported JSON to clipboard!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Export Profile", tint = RavexCyan, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("TARGET FPS CAP", color = RavexTextMuted, fontSize = 10.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(60, 90, 120).forEach { fps ->
                                val isSelected = targetFps == fps
                                Box(
                                    modifier = Modifier
                                        .background(if (isSelected) RavexCyan.copy(alpha = 0.3f) else RavexSurfaceVariant, RoundedCornerShape(4.dp))
                                        .border(1.dp, if (isSelected) RavexCyan else Color.Transparent, RoundedCornerShape(4.dp))
                                        .clickable { targetFps = fps }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("${fps} FPS", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("SENSITIVITY & STRATEGY NOTES", color = RavexTextMuted, fontSize = 10.sp)
                        OutlinedTextField(
                            value = customNotes,
                            onValueChange = { customNotes = it },
                            placeholder = { Text("Record custom claw layout, gyro scale, or graphics notes...", fontSize = 10.sp, color = RavexTextMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RavexCyan,
                                unfocusedBorderColor = RavexSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val updated = activeProfile.copy(
                                    targetFps = targetFps,
                                    assignedCrosshairId = assignedCrosshairId,
                                    customNotes = customNotes
                                )
                                ravexPrefs.saveGameProfile(updated)
                                profilesMap = ravexPrefs.getGameProfiles()
                                Toast.makeText(context, "Saved profile for ${updated.gameName}", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RavexCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SAVE GAME PROFILE", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
