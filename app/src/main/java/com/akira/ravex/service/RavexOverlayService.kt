package com.akira.ravex.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.akira.ravex.R
import com.akira.ravex.data.CrosshairPresetsRepository
import com.akira.ravex.data.RavexPreferences
import com.akira.ravex.ui.components.CrosshairCanvas
import com.akira.ravex.ui.screens.*
import com.akira.ravex.ui.theme.*

enum class HudTabModule(val label: String) {
    COPILOT("COPILOT"),
    LAG_DIAGNOSIS("LAG DIAG"),
    PROFILES("PROFILES"),
    CROSSHAIR("CROSSHAIR"),
    AI_CAMERA("CAMERA"),
    VOICE_CMD("VOICE"),
    COACH("COACH"),
    TROUBLESHOOT("REPAIR"),
    NETWORK("NETWORK"),
    PERFORMANCE("HEALTH"),
    HUD_SETTINGS("HUD SETTINGS")
}

class RavexOverlayService : Service() {

    companion object {
        const val ACTION_REFRESH_CROSSHAIR = "com.akira.ravex.ACTION_REFRESH_CROSSHAIR"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var ravexPrefs: RavexPreferences
    private lateinit var overlayLifecycleOwner: OverlayLifecycleOwner

    private var bubbleView: ComposeView? = null
    private var crosshairOverlayView: ComposeView? = null

    private var bubbleParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        ravexPrefs = RavexPreferences(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        overlayLifecycleOwner = OverlayLifecycleOwner()
        overlayLifecycleOwner.onCreate()
        overlayLifecycleOwner.onStart()
        overlayLifecycleOwner.onResume()

        startForegroundServiceNotification()
        setupCrosshairOverlay()
        setupFloatingBubbleOverlay()
    }

    private fun attachLifecycleToComposeView(composeView: ComposeView) {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setViewTreeLifecycleOwner(overlayLifecycleOwner)
        composeView.setViewTreeViewModelStoreOwner(overlayLifecycleOwner)
        composeView.setViewTreeSavedStateRegistryOwner(overlayLifecycleOwner)
    }

    private fun startForegroundServiceNotification() {
        val channelId = "ravex_hud_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "RAVEX Floating Gaming HUD",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AKIRA RAVEX HUD Active")
            .setContentText("Floating overlay & visual crosshair running")
            .setSmallIcon(R.drawable.ic_ravex_wolf)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(2001, notification)
    }

    private fun setupCrosshairOverlay() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val crosshairParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        crosshairOverlayView = ComposeView(this).apply {
            attachLifecycleToComposeView(this)
            setContent {
                AkiraRavexTheme {
                    var activeId by remember { mutableStateOf(ravexPrefs.activeCrosshairId) }
                    var isEnabled by remember { mutableStateOf(ravexPrefs.isCrosshairEnabled) }

                    LaunchedEffect(Unit) {
                        while (true) {
                            activeId = ravexPrefs.activeCrosshairId
                            isEnabled = ravexPrefs.isCrosshairEnabled
                            kotlinx.coroutines.delay(200)
                        }
                    }

                    if (isEnabled) {
                        val preset = CrosshairPresetsRepository.getPresetById(activeId) ?: CrosshairPresetsRepository.presets.first()
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CrosshairCanvas(preset = preset, modifier = Modifier.size((preset.sizeDp * 2.5f).dp))
                        }
                    }
                }
            }
        }

        try {
            windowManager.addView(crosshairOverlayView, crosshairParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupFloatingBubbleOverlay() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        bubbleParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        bubbleView = ComposeView(this).apply {
            attachLifecycleToComposeView(this)
            setContent {
                AkiraRavexTheme {
                    var isExpanded by remember { mutableStateOf(false) }
                    var activeTab by remember { mutableStateOf(HudTabModule.COPILOT) }

                    fun updateWindowFocus(expanded: Boolean) {
                        bubbleParams?.flags = if (expanded) {
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        } else {
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        }
                        windowManager.updateViewLayout(bubbleView, bubbleParams)
                    }

                    Box {
                        if (!isExpanded) {
                            // Collapsed Bubble with Official Wolf Identity Icon
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(RavexBlack.copy(alpha = 0.9f), CircleShape)
                                    .border(2.dp, RavexCyan, CircleShape)
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            bubbleParams?.let { p ->
                                                p.x += dragAmount.x.toInt()
                                                p.y += dragAmount.y.toInt()
                                                windowManager.updateViewLayout(bubbleView, p)
                                            }
                                        }
                                    }
                                    .clickable {
                                        isExpanded = true
                                        updateWindowFocus(true)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_ravex_wolf),
                                    contentDescription = "RAVEX HUD",
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                )
                            }
                        } else {
                            // Expanded Floating Gaming Panel
                            Card(
                                colors = CardDefaults.cardColors(containerColor = RavexSurface.copy(alpha = ravexPrefs.hudOpacityFloat)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RavexCyan),
                                modifier = Modifier
                                    .width(360.dp)
                                    .height(260.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    // Header Bar
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Image(
                                                painter = painterResource(id = R.drawable.ic_ravex_wolf),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("AKIRA RAVEX HUD", color = RavexCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        IconButton(
                                            onClick = {
                                                isExpanded = false
                                                updateWindowFocus(false)
                                            },
                                            modifier = Modifier.size(22.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Close", tint = RavexRed, modifier = Modifier.size(16.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Navigation Bar
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        items(HudTabModule.values()) { tab ->
                                            val isSelected = activeTab == tab
                                            Box(
                                                modifier = Modifier
                                                    .background(if (isSelected) RavexCyan.copy(alpha = 0.3f) else RavexSurfaceVariant, RoundedCornerShape(4.dp))
                                                    .border(1.dp, if (isSelected) RavexCyan else Color.Transparent, RoundedCornerShape(4.dp))
                                                    .clickable { activeTab = tab }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(tab.label, color = if (isSelected) RavexCyan else RavexTextMuted, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Active Panel Content
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        when (activeTab) {
                                            HudTabModule.COPILOT -> RavexCopilotScreen(ravexPrefs = ravexPrefs)
                                            HudTabModule.LAG_DIAGNOSIS -> SmartLagDetectionScreen(ravexPrefs = ravexPrefs)
                                            HudTabModule.PROFILES -> GameProfilesScreen(ravexPrefs = ravexPrefs)
                                            HudTabModule.CROSSHAIR -> CrosshairEngineScreen(ravexPrefs = ravexPrefs)
                                            HudTabModule.AI_CAMERA -> AiCameraScreen(ravexPrefs = ravexPrefs)
                                            HudTabModule.VOICE_CMD -> AiVoiceCommandsScreen(ravexPrefs = ravexPrefs)
                                            HudTabModule.COACH -> SessionCoachScreen(ravexPrefs = ravexPrefs)
                                            HudTabModule.TROUBLESHOOT -> AiTroubleshooterScreen(ravexPrefs = ravexPrefs)
                                            HudTabModule.NETWORK -> SmartNetworkScreen()
                                            HudTabModule.PERFORMANCE -> PhoneHealthLandscapeScreen()
                                            HudTabModule.HUD_SETTINGS -> PersonalizedHudSettingsScreen(ravexPrefs = ravexPrefs)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        try {
            windowManager.addView(bubbleView, bubbleParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        overlayLifecycleOwner.onDestroy()

        bubbleView?.let { try { windowManager.removeView(it) } catch (e: Exception) {} }
        crosshairOverlayView?.let { try { windowManager.removeView(it) } catch (e: Exception) {} }

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
