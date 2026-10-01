package com.akira.ravex.model

data class GameProfile(
    val packageName: String,
    val gameName: String,
    val assignedCrosshairId: String = "preset_default_1",
    val targetFps: Int = 60,
    val customNotes: String = ""
)
