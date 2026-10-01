package com.akira.ravex.model

data class SystemMetrics(
    val usedRamMb: Long = 0,
    val totalRamMb: Long = 0,
    val ramPercentage: Float = 0f,
    val batteryLevel: Int = 100,
    val batteryTempC: Float = 25.0f,
    val batteryVoltage: Int = 0,
    val isCharging: Boolean = false,
    val thermalState: Int = 0, // 0: Normal, 1: Moderate, 2: Severe, 3: Critical
    val fps: Int = 60,
    val refreshRate: Int = 60,
    val pingMs: Int = 0,
    val isNetworkOnline: Boolean = true
)
