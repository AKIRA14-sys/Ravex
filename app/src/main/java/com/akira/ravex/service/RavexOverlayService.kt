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
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

class RavexOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var ravexPrefs: RavexPreferences
    private val lifecycleOwner = OverlayLifecycleOwner()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var hudView: ComposeView? = null
    private var crosshairView: ComposeView? = null

    private var metricsState by mutableStateOf(SystemMetrics())
    private var activePresetState by mutableStateOf<CrosshairPreset?>(null)
    private var isExpanded by mutableStateOf(true)

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
            x = 50
            y = 200
        }

        hudView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                HudOverlayComposable(
                    metrics = metricsState,
                    isExpanded = isExpanded,
                    onToggleExpand = { isExpanded = !isExpanded }
                )
            }
        }

        // Drag listener for HUD Widget
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        hudView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    try {
                        windowManager.updateViewLayout(hudView, params)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    true
                }
                else -> false
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
fun HudOverlayComposable(
    metrics: SystemMetrics,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val darkBg = Color(0xFF10121A)
    val accentCyan = Color(0xFF00F0FF)
    val accentRed = Color(0xFFFF2A55)

    if (!isExpanded) {
        // Compact Wolf Badge
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(darkBg)
                .border(2.dp, accentCyan, CircleShape)
                .clickable { onToggleExpand() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "RX",
                color = accentCyan,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    } else {
        // Expanded Tactical HUD Bar
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(darkBg.copy(alpha = 0.92f))
                .border(1.5.dp, accentCyan, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accentRed.copy(alpha = 0.2f))
                    .clickable { onToggleExpand() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "RX", color = accentRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Column {
                Text(text = "FPS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${metrics.fps}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Column {
                Text(text = "RAM", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${metrics.ramPercentage.toInt()}%",
                    color = if (metrics.ramPercentage > 85f) accentRed else accentCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column {
                Text(text = "TEMP", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${metrics.batteryTempC.toInt()}°C",
                    color = if (metrics.batteryTempC >= 40f) accentRed else Color.Green,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column {
                Text(text = "PING", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${metrics.pingMs}ms",
                    color = Color.Yellow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
