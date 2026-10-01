package com.akira.ravex.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.akira.ravex.data.CrosshairPresetsRepository
import com.akira.ravex.data.RavexPreferences
import com.akira.ravex.model.CrosshairCategory
import com.akira.ravex.model.CrosshairPreset
import com.akira.ravex.model.CrosshairShape
import com.akira.ravex.service.RavexOverlayService
import com.akira.ravex.ui.components.CrosshairCanvas
import com.akira.ravex.ui.theme.*

@Composable
fun CrosshairEngineScreen(ravexPrefs: RavexPreferences) {
    val context = LocalContext.current
    var activeId by remember { mutableStateOf(ravexPrefs.activeCrosshairId) }
    var selectedCategory by remember { mutableStateOf<CrosshairCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val customPresets = remember { mutableStateListOf(*ravexPrefs.getCustomPresets().toTypedArray()) }
    val allPresets = remember(customPresets.size) {
        customPresets + CrosshairPresetsRepository.presets
    }

    val filteredPresets = remember(selectedCategory, searchQuery, allPresets) {
        allPresets.filter { preset ->
            val matchesCategory = selectedCategory == null || preset.category == selectedCategory
            val matchesSearch = searchQuery.isEmpty() || preset.name.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    var editingPreset by remember { mutableStateOf<CrosshairPreset?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RavexBlack)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CROSSHAIR ENGINE",
                    color = RavexRed,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "200+ Presets & Studio Customizer",
                    color = RavexCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    editingPreset = CrosshairPreset(
                        id = "custom_${System.currentTimeMillis()}",
                        name = "Custom Crosshair ${customPresets.size + 1}",
                        category = CrosshairCategory.CUSTOM,
                        shape = CrosshairShape.CROSS,
                        colorHex = "#FF2A55",
                        sizeDp = 24f,
                        strokeWidthDp = 2.5f,
                        gapDp = 4f
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = RavexRed),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("CREATE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search 200+ presets...", color = RavexTextMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = RavexCyan) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RavexCyan,
                unfocusedBorderColor = RavexSurfaceVariant,
                focusedContainerColor = RavexSurface,
                unfocusedContainerColor = RavexSurface
            ),
            shape = RoundedCornerShape(10.dp),
            singleLine = true
        )

        // Category Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("ALL (${allPresets.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RavexCyan,
                    selectedLabelColor = RavexBlack
                )
            )
            CrosshairCategory.values().take(3).forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                    label = { Text(cat.label.take(12), fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RavexRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // Preset Grid Display
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredPresets) { preset ->
                val isActive = preset.id == activeId
                CrosshairGridTile(
                    preset = preset,
                    isActive = isActive,
                    onSelect = {
                        activeId = preset.id
                        ravexPrefs.activeCrosshairId = preset.id
                        // Broadcast update to service
                        val intent = Intent(context, RavexOverlayService::class.java).apply {
                            action = RavexOverlayService.ACTION_REFRESH_CROSSHAIR
                        }
                        context.startService(intent)
                    },
                    onEdit = {
                        editingPreset = preset
                    }
                )
            }
        }
    }

    // Customizer Modal Dialog
    editingPreset?.let { presetToEdit ->
        CustomizerModalDialog(
            initialPreset = presetToEdit,
            onDismiss = { editingPreset = null },
            onSave = { updatedPreset ->
                ravexPrefs.saveCustomPreset(updatedPreset)
                customPresets.removeAll { it.id == updatedPreset.id }
                customPresets.add(0, updatedPreset)
                activeId = updatedPreset.id
                ravexPrefs.activeCrosshairId = updatedPreset.id
                val intent = Intent(context, RavexOverlayService::class.java).apply {
                    action = RavexOverlayService.ACTION_REFRESH_CROSSHAIR
                }
                context.startService(intent)
                editingPreset = null
            }
        )
    }
}

@Composable
fun CrosshairGridTile(
    preset: CrosshairPreset,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = RavexSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) RavexCyan else RavexSurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(RavexBlack),
                contentAlignment = Alignment.Center
            ) {
                CrosshairCanvas(preset = preset)
            }

            Text(
                text = preset.name,
                color = RavexTextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = RavexCyan, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("ACTIVE", color = RavexCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text("TAP TO USE", color = RavexTextMuted, fontSize = 9.sp)
                }

                Text(
                    text = "EDIT",
                    color = RavexRed,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onEdit() }
                )
            }
        }
    }
}

@Composable
fun CustomizerModalDialog(
    initialPreset: CrosshairPreset,
    onDismiss: () -> Unit,
    onSave: (CrosshairPreset) -> Unit
) {
    var name by remember { mutableStateOf(initialPreset.name) }
    var sizeDp by remember { mutableStateOf(initialPreset.sizeDp) }
    var strokeWidthDp by remember { mutableStateOf(initialPreset.strokeWidthDp) }
    var gapDp by remember { mutableStateOf(initialPreset.gapDp) }
    var opacity by remember { mutableStateOf(initialPreset.opacity) }
    var showDot by remember { mutableStateOf(initialPreset.showDot) }
    var isAnimated by remember { mutableStateOf(initialPreset.isAnimated) }
    var selectedColor by remember { mutableStateOf(initialPreset.colorHex) }
    var selectedShape by remember { mutableStateOf(initialPreset.shape) }

    val currentPreset = initialPreset.copy(
        name = name,
        sizeDp = sizeDp,
        strokeWidthDp = strokeWidthDp,
        gapDp = gapDp,
        opacity = opacity,
        showDot = showDot,
        isAnimated = isAnimated,
        colorHex = selectedColor,
        shape = selectedShape
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = RavexSurface,
        title = { Text("RAVEX STUDIO CUSTOMIZER", color = RavexRed, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Live Preview Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(RavexBlack),
                    contentAlignment = Alignment.Center
                ) {
                    CrosshairCanvas(preset = currentPreset)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset Name", fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RavexCyan)
                )

                Text("Shape:", color = RavexTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CrosshairShape.values().take(4).forEach { shape ->
                        Button(
                            onClick = { selectedShape = shape },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedShape == shape) RavexCyan else RavexSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(shape.name.take(6), fontSize = 9.sp, color = if (selectedShape == shape) RavexBlack else Color.White)
                        }
                    }
                }

                Text("Size: ${sizeDp.toInt()} dp", fontSize = 11.sp, color = RavexTextWhite)
                Slider(value = sizeDp, onValueChange = { sizeDp = it }, valueRange = 10f..50f)

                Text("Stroke: ${String.format("%.1f", strokeWidthDp)} dp", fontSize = 11.sp, color = RavexTextWhite)
                Slider(value = strokeWidthDp, onValueChange = { strokeWidthDp = it }, valueRange = 1f..8f)

                Text("Gap: ${gapDp.toInt()} dp", fontSize = 11.sp, color = RavexTextWhite)
                Slider(value = gapDp, onValueChange = { gapDp = it }, valueRange = 0f..20f)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Center Dot", fontSize = 12.sp, color = RavexTextWhite)
                    Switch(checked = showDot, onCheckedChange = { showDot = it })
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Animated Pulse", fontSize = 12.sp, color = RavexTextWhite)
                    Switch(checked = isAnimated, onCheckedChange = { isAnimated = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(currentPreset) },
                colors = ButtonDefaults.buttonColors(containerColor = RavexCyan)
            ) {
                Text("SAVE PRESET", color = RavexBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = RavexTextMuted)
            }
        }
    )
}
