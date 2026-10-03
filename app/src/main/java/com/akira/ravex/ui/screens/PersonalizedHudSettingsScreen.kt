package com.akira.ravex.ui.screens

import android.widget.Toast
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
import com.akira.ravex.data.RavexPreferences
import com.akira.ravex.ui.theme.*

@Composable
fun PersonalizedHudSettingsScreen(ravexPrefs: RavexPreferences) {
    val context = LocalContext.current

    var isHudEnabled by remember { mutableStateOf(ravexPrefs.isHudEnabled) }
    var isCrosshairEnabled by remember { mutableStateOf(ravexPrefs.isCrosshairEnabled) }
    var opacity by remember { mutableStateOf(ravexPrefs.hudOpacityFloat) }
    var accentColorHex by remember { mutableStateOf(ravexPrefs.hudAccentColorHex) }

    val accentColors = listOf("#00E5FF", "#FF2A55", "#39FF14", "#FFD700", "#9D00FF")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(10.dp)
    ) {
        Text("RAVEX PERSONALIZED HUD SETTINGS", color = RavexCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text("Customize floating bubble overlay size, opacity, accent colors, and module visibility.", color = RavexTextMuted, fontSize = 10.sp)

        Spacer(modifier = Modifier.height(10.dp))

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
                    Text("Floating RAVEX HUD Overlay", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = isHudEnabled,
                        onCheckedChange = {
                            isHudEnabled = it
                            ravexPrefs.isHudEnabled = it
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Visual Crosshair Overlay", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Switch(
                        checked = isCrosshairEnabled,
                        onCheckedChange = {
                            isCrosshairEnabled = it
                            ravexPrefs.isCrosshairEnabled = it
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("HUD PANEL OPACITY (${(opacity * 100).toInt()}%)", color = RavexTextMuted, fontSize = 10.sp)
                Slider(
                    value = opacity,
                    onValueChange = {
                        opacity = it
                        ravexPrefs.hudOpacityFloat = it
                    },
                    valueRange = 0.5f..1.0f,
                    colors = SliderDefaults.colors(thumbColor = RavexCyan, activeTrackColor = RavexCyan)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("HUD ACCENT COLOR", color = RavexTextMuted, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accentColors.forEach { hex ->
                        val parsedColor = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = accentColorHex == hex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(parsedColor, RoundedCornerShape(4.dp))
                                .border(2.dp, if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(4.dp))
                                .clickable {
                                    accentColorHex = hex
                                    ravexPrefs.hudAccentColorHex = hex
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        ravexPrefs.isHudEnabled = true
                        ravexPrefs.isCrosshairEnabled = true
                        ravexPrefs.hudOpacityFloat = 0.95f
                        ravexPrefs.hudAccentColorHex = "#00E5FF"
                        isHudEnabled = true
                        isCrosshairEnabled = true
                        opacity = 0.95f
                        accentColorHex = "#00E5FF"
                        Toast.makeText(context, "Reset HUD settings to default", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RavexSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("RESET TO DEFAULT", color = Color.White, fontSize = 10.sp)
                }
            }
        }
    }
}
