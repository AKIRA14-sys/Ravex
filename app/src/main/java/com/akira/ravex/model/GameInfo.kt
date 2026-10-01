package com.akira.ravex.model

import android.graphics.drawable.Drawable

data class GameInfo(
    val packageName: String,
    val appName: String,
    val isSystemGame: Boolean = false,
    val category: String = "Game",
    val icon: Drawable? = null
)
