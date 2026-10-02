package com.akira.ravex.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.akira.ravex.R
import com.akira.ravex.data.CrosshairPresetsRepository
import com.akira.ravex.data.RavexPreferences
import com.akira.ravex.model.CrosshairPreset
import com.akira.ravex.model.SystemMetrics
import com.akira.ravex.ui.components.CrosshairCanvas
import com.akira.ravex.util.SystemMonitorUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

class RavexOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var ravexPrefs: RavexPreferences
    private val lifecycleOwner = OverlayLifecycleOwner()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var hudView: ComposeView? = null
    private var crosshairView: ComposeView? = null

    private var metricsState by mutableStateOf(SystemMetrics())
    private var activePresetState by mutableStateOf<CrosshairPreset?>(null)
    private var isExpanded by mutableStateOf(false)

    private var monitorJob: Job? = null

    companion object {
        private const val NOTIF_CHANNEL_ID = "ravex_overlay_channel"
        private const val NOTIF_ID = 1001
        const val ACTION_STOP_HUD = "com.akira.ravex.ACTION_STOP_HUD"
        const val ACTION_REFRESH_CROSSHAIR = "com.akira.ravex.ACTION_REFRESH_CROSSHAIR"
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        ravexPrefs = RavexPreferences(this)
        lifecycleOwner.onCreate()
        lifecycleOwner.onStart()
        lifecycleOwner.onResume()

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())

        startMetricsMonitor()
        refreshActivePreset()

        if (Settings.canDrawOverlays(this)) {
            setupCrosshairOverlay()
            setupHudOverlay()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_HUD -> stopSelf()
            ACTION_REFRESH_CROSSHAIR -> refreshActivePreset()
        }
        return START_STICKY
    }

    private fun refreshActivePreset() {
        val presetId = ravexPrefs.activeCrosshairId
        val customPresets = ravexPrefs.getCustomPresets()
        val customMatch = customPresets.firstOrNull { it.id == presetId }
        activePresetState = customMatch ?: CrosshairPresetsRepository.getPresetById(presetId)
    }

    private fun updateActivePreset(newPreset: CrosshairPreset) {
        activePresetState = newPreset
        ravexPrefs.activeCrosshairId = newPreset.id
        if (newPreset.isCustom) {
            ravexPrefs.saveCustomPreset(newPreset)
        }
    }

    private fun startMetricsMonitor() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (true) {
                val metrics = SystemMonitorUtil.getSystemMetrics(this@RavexOverlayService)
                metricsState = metrics
                delay(1000)
            }
        }
    }

    private fun setupCrosshairOverlay() {
        if (crosshairView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        crosshairView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                val preset = activePresetState
                if (preset != null) {
                    CrosshairCanvas(preset = preset)
                }
            }
        }

        try {
            windowManager.addView(crosshairView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupHudOverlay() {
        if (hudView != null) return

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 120
        }

        hudView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                SharinganHudBubbleComposable(
                    metrics = metricsState,
                    activePreset = activePresetState,
                    isExpanded = isExpanded,
                    onToggleExpand = { isExpanded = !isExpanded },
                    onPresetChanged = { updatedPreset ->
                        updateActivePreset(updatedPreset)
                    },
                    onDragDelta = { dx, dy ->
                        params.x += dx.toInt()
                        params.y += dy.toInt()
                        try {
                            windowManager.updateViewLayout(hudView, params)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                )
            }
        }

        try {
            windowManager.addView(hudView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "RAVEX Floating HUD Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time FPS, RAM, and crosshair overlay during gameplay."
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("RAVEX GAMEFORGE HUD Active")
            .setContentText("Overlay system & crosshair active in background")
            .setSmallIcon(R.drawable.ic_ravex_wolf)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
        try {
            if (hudView != null) windowManager.removeView(hudView)
            if (crosshairView != null) windowManager.removeView(crosshairView)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        lifecycleOwner.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun SharinganHudBubbleComposable(
    metrics: SystemMetrics,
    activePreset: CrosshairPreset?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPresetChanged: (CrosshairPreset) -> Unit,
    onDragDelta: (Float, Float) -> Unit
) {
    val darkBg = Color(0xFF08080C)
    val accentCyan = Color(0xFF00F0FF)
    val accentRed = Color(0xFFE50914)

    val presets = remember { CrosshairPresetsRepository.presets }

    if (!isExpanded) {
        // Floating Draggable Sharingan / Wolf Bubble Icon
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(darkBg)
                .border(2.dp, accentRed, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.x, dragAmount.y)
                    }
                }
                .clickable { onToggleExpand() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_ravex_wolf),
                contentDescription = "Ravex Floating Bubble",
                modifier = Modifier.size(38.dp)
            )
        }
    } else {
        // Expanded In-Game Gaming Panel with Telemetry & Interactive In-Game Crosshair Customizer
        Column(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(darkBg.copy(alpha = 0.96f))
                .border(1.5.dp, accentCyan, RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row (Functions as drag handle for expanded panel)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDragDelta(dragAmount.x, dragAmount.y)
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_ravex_wolf),
                        contentDescription = "Logo",
                        modifier = Modifier.size(22.dp)
                    )
                    Text("RAVEX HUD PANEL", color = accentCyan, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }

                Text(
                    text = "CLOSE [X]",
                    color = accentRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onToggleExpand() }
                )
            }

            // Real-Time Telemetry Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMetricBox("FPS", "${metrics.fps}", Color.White)
                TelemetryMetricBox("RAM", "${metrics.ramPercentage.toInt()}%", if (metrics.ramPercentage > 85) accentRed else accentCyan)
                TelemetryMetricBox("TEMP", "${metrics.batteryTempC.toInt()}°C", if (metrics.batteryTempC >= 40) accentRed else Color.Green)
                TelemetryMetricBox("PING", "${metrics.pingMs}ms", Color.Yellow)
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            // Crosshair Quick Customizer in Bubble Panel
            Text("IN-GAME CROSSHAIR SETTINGS", color = accentRed, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)

            activePreset?.let { preset ->
                // Preset Carousel Picker
                Text("Select Preset:", color = Color.Gray, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(presets.take(15)) { p ->
                        val isSelected = p.id == preset.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) accentCyan else Color(0xFF1A1D28))
                                .clickable { onPresetChanged(p) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = p.name.take(10),
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Size: ${preset.sizeDp.toInt()}dp", color = Color.White, fontSize = 10.sp)
                    Slider(
                        value = preset.sizeDp,
                        onValueChange = { newSize ->
                            onPresetChanged(preset.copy(sizeDp = newSize, isCustom = true))
                        },
                        valueRange = 10f..45f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                }

                // Opacity Slider (Supports 70% / 0.70 default option)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Opacity: ${(preset.opacity * 100).toInt()}%", color = Color.White, fontSize = 10.sp)
                    Slider(
                        value = preset.opacity,
                        onValueChange = { newOpacity ->
                            onPresetChanged(preset.copy(opacity = newOpacity, isCustom = true))
                        },
                        valueRange = 0.2f..1.0f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                }

                // Color Picker Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Color:", color = Color.White, fontSize = 10.sp)
                    val quickColors = listOf("#FF2A55", "#00F0FF", "#39FF14", "#FFD700", "#FFFFFF", "#FF5500")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        quickColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(com.akira.ravex.ui.components.parseColorHex(hex))
                                    .border(
                                        width = if (preset.colorHex == hex) 2.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onPresetChanged(preset.copy(colorHex = hex, isCustom = true))
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryMetricBox(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}
