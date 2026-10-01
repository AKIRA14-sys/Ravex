package com.akira.ravex.model

data class ThermalStatus(
    val temperatureC: Float = 25.0f,
    val warningLevel: WarningLevel = WarningLevel.NORMAL,
    val statusMessage: String = "Optimal Operating Temp"
) {
    enum class WarningLevel {
        NORMAL,
        WARM,
        HOT,
        CRITICAL
    }
}
