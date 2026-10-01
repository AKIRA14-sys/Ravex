package com.akira.ravex.model

enum class CrosshairCategory(val label: String) {
    TACTICAL("Tactical FPS"),
    CYBERPUNK("Cyberpunk / Sci-Fi"),
    MINIMAL("Minimal Dot & Clean"),
    DYNAMIC("Dynamic & Animated"),
    CUSTOM("Custom Created")
}

enum class CrosshairShape {
    CROSS,
    DOT,
    CIRCLE_CROSS,
    CHEVRON,
    T_SHAPE,
    DIAMOND,
    TRIANGLE,
    SQUARE,
    HOLOGRAM,
    DUAL_RING
}

data class CrosshairPreset(
    val id: String,
    val name: String,
    val category: CrosshairCategory,
    val shape: CrosshairShape = CrosshairShape.CROSS,
    val colorHex: String = "#FF0000",
    val outlineColorHex: String = "#000000",
    val sizeDp: Float = 24f,
    val strokeWidthDp: Float = 2.5f,
    val gapDp: Float = 4f,
    val opacity: Float = 1.0f,
    val dotRadiusDp: Float = 2f,
    val showDot: Boolean = true,
    val isAnimated: Boolean = false,
    val rotationAngle: Float = 0f,
    val shadowEffect: Boolean = true,
    val isCustom: Boolean = false
)
