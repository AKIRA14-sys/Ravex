package com.akira.ravex.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.akira.ravex.R
import com.akira.ravex.data.RavexPreferences
import com.akira.ravex.model.ThermalStatus
import com.akira.ravex.util.SystemMonitorUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ThermalGuardService : Service() {

    private lateinit var ravexPrefs: RavexPreferences
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitorJob: Job? = null

    companion object {
        private const val NOTIF_CHANNEL_ID = "ravex_thermal_guard_channel"
        private const val NOTIF_ID = 2002
    }

    override fun onCreate() {
        super.onCreate()
        ravexPrefs = RavexPreferences(this)
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification("Monitoring device thermal levels..."))

        startThermalMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startThermalMonitoring() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (true) {
                if (ravexPrefs.isThermalGuardEnabled) {
                    val metrics = SystemMonitorUtil.getSystemMetrics(this@ThermalGuardService)
                    val thermalStatus = SystemMonitorUtil.getThermalStatus(metrics.batteryTempC)

                    updateNotification(thermalStatus)
                }
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    private fun updateNotification(thermalStatus: ThermalStatus) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = buildNotification("${thermalStatus.temperatureC}°C - ${thermalStatus.statusMessage}")
        nm.notify(NOTIF_ID, notif)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "RAVEX Thermal Guard Protection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors device temperature and protects from thermal overheating."
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(message: String): Notification {
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle("RAVEX Thermal Guard Active")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_ravex_wolf)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
