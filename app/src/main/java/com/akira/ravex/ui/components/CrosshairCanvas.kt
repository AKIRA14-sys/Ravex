package com.akira.ravex.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.akira.ravex.model.CrosshairPreset
import com.akira.ravex.model.CrosshairShape

@Composable
fun CrosshairCanvas(
    preset: CrosshairPreset,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "crosshair_anim")
    val pulseScale by if (preset.isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0.88f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
    } else {
        rememberUpdatedState(1.0f)
    }

    val animatedRotation by if (preset.isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
    } else {
        rememberUpdatedState(preset.rotationAngle)
    }

    val primaryColor = parseColorHex(preset.colorHex)
    val outlineColor = parseColorHex(preset.outlineColorHex)

    Canvas(
        modifier = modifier
            .size((preset.sizeDp * 2.5f).dp)
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val sizePx = preset.sizeDp * density * pulseScale
        val strokePx = preset.strokeWidthDp * density
        val gapPx = preset.gapDp * density
        val dotRadiusPx = preset.dotRadiusDp * density

        rotate(animatedRotation, center) {
            when (preset.shape) {
                CrosshairShape.CROSS -> drawCrossShape(
                    center, sizePx, strokePx, gapPx, primaryColor, outlineColor, preset.opacity
                )
                CrosshairShape.DOT -> drawDotShape(
                    center, dotRadiusPx, primaryColor, outlineColor, preset.opacity
                )
                CrosshairShape.CIRCLE_CROSS -> {
                    drawCircleCrossShape(
                        center, sizePx, strokePx, gapPx, primaryColor, outlineColor, preset.opacity
                    )
                }
                CrosshairShape.CHEVRON -> drawChevronShape(
                    center, sizePx, strokePx, primaryColor, outlineColor, preset.opacity
                )
                CrosshairShape.T_SHAPE -> drawTShape(
                    center, sizePx, strokePx, gapPx, primaryColor, outlineColor, preset.opacity
                )
                CrosshairShape.DIAMOND -> drawDiamondShape(
                    center, sizePx, strokePx, primaryColor, outlineColor, preset.opacity
                )
                CrosshairShape.TRIANGLE -> drawTriangleShape(
                    center, sizePx, strokePx, primaryColor, outlineColor, preset.opacity
                )
                CrosshairShape.SQUARE -> drawSquareShape(
                    center, sizePx, strokePx, primaryColor, outlineColor, preset.opacity
                )
                CrosshairShape.HOLOGRAM -> drawHologramShape(
                    center, sizePx, strokePx, gapPx, primaryColor, outlineColor, preset.opacity
                )
                CrosshairShape.DUAL_RING -> drawDualRingShape(
                    center, sizePx, strokePx, primaryColor, outlineColor, preset.opacity
                )
            }

            if (preset.showDot && preset.shape != CrosshairShape.DOT) {
                drawDotShape(center, dotRadiusPx, primaryColor, outlineColor, preset.opacity)
            }
        }
    }
}

private fun DrawScope.drawCrossShape(
    center: Offset,
    sizePx: Float,
    strokePx: Float,
    gapPx: Float,
    primaryColor: Color,
    outlineColor: Color,
    alpha: Float
) {
    val length = sizePx / 2f

    val lines = listOf(
        // Right
        Pair(Offset(center.x + gapPx, center.y), Offset(center.x + gapPx + length, center.y)),
        // Left
        Pair(Offset(center.x - gapPx, center.y), Offset(center.x - gapPx - length, center.y)),
        // Bottom
        Pair(Offset(center.x, center.y + gapPx), Offset(center.x, center.y + gapPx + length)),
        // Top
        Pair(Offset(center.x, center.y - gapPx), Offset(center.x, center.y - gapPx - length))
    )

    for (line in lines) {
        // Outline
        drawLine(
            color = outlineColor.copy(alpha = alpha),
            start = line.first,
            end = line.second,
            strokeWidth = strokePx + 2f,
            cap = StrokeCap.Round
        )
        // Main
        drawLine(
            color = primaryColor.copy(alpha = alpha),
            start = line.first,
            end = line.second,
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawDotShape(
    center: Offset,
    radiusPx: Float,
    primaryColor: Color,
    outlineColor: Color,
    alpha: Float
) {
    // Outline
    drawCircle(
        color = outlineColor.copy(alpha = alpha),
        radius = radiusPx + 1.5f,
        center = center
    )
    // Core
    drawCircle(
        color = primaryColor.copy(alpha = alpha),
        radius = radiusPx,
        center = center
    )
}

private fun DrawScope.drawCircleCrossShape(
    center: Offset,
    sizePx: Float,
    strokePx: Float,
    gapPx: Float,
    primaryColor: Color,
    outlineColor: Color,
    alpha: Float
) {
    drawCrossShape(center, sizePx, strokePx, gapPx, primaryColor, outlineColor, alpha)

    val radius = (sizePx / 2f) + gapPx / 2f
    // Outline Ring
    drawCircle(
        color = outlineColor.copy(alpha = alpha),
        radius = radius,
        center = center,
        style = Stroke(width = strokePx + 2f)
    )
    // Main Ring
    drawCircle(
        color = primaryColor.copy(alpha = alpha),
        radius = radius,
        center = center,
        style = Stroke(width = strokePx)
    )
}

private fun DrawScope.drawChevronShape(
    center: Offset,
    sizePx: Float,
    strokePx: Float,
    primaryColor: Color,
    outlineColor: Color,
    alpha: Float
) {
    val path = Path().apply {
        moveTo(center.x - sizePx / 2f, center.y + sizePx / 3f)
        lineTo(center.x, center.y - sizePx / 3f)
        lineTo(center.x + sizePx / 2f, center.y + sizePx / 3f)
    }

    drawPath(path, outlineColor.copy(alpha = alpha), style = Stroke(width = strokePx + 2f, cap = StrokeCap.Round))
    drawPath(path, primaryColor.copy(alpha = alpha), style = Stroke(width = strokePx, cap = StrokeCap.Round))
}

private fun DrawScope.drawTShape(
    center: Offset,
    sizePx: Float,
    strokePx: Float,
    gapPx: Float,
    primaryColor: Color,
    outlineColor: Color,
    alpha: Float
) {
    val length = sizePx / 2f
    val lines = listOf(
        Pair(Offset(center.x + gapPx, center.y), Offset(center.x + gapPx + length, center.y)),
        Pair(Offset(center.x - gapPx, center.y), Offset(center.x - gapPx - length, center.y)),
        Pair(Offset(center.x, center.y + gapPx), Offset(center.x, center.y + gapPx + length))
    )
    for (line in lines) {
        drawLine(outlineColor.copy(alpha = alpha), line.first, line.second, strokeWidth = strokePx + 2f, cap = StrokeCap.Round)
        drawLine(primaryColor.copy(alpha = alpha), line.first, line.second, strokeWidth = strokePx, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawDiamondShape(
    center: Offset,
    sizePx: Float,
    strokePx: Float,
    primaryColor: Color,
    outlineColor: Color,
    alpha: Float
) {
    val half = sizePx / 2f
    val path = Path().apply {
        moveTo(center.x, center.y - half)
        lineTo(center.x + half, center.y)
        lineTo(center.x, center.y + half)
        lineTo(center.x - half, center.y)
        close()
    }
    drawPath(path, outlineColor.copy(alpha = alpha), style = Stroke(width = strokePx + 2f))
    drawPath(path, primaryColor.copy(alpha = alpha), style = Stroke(width = strokePx))
}

private fun DrawScope.drawTriangleShape(
    center: Offset,
    sizePx: Float,
    strokePx: Float,
    primaryColor: Color,
    outlineColor: Color,
    alpha: Float
) {
    val half = sizePx / 2f
    val path = Path().apply {
        moveTo(center.x, center.y - half)
        lineTo(center.x + half, center.y + half)
        lineTo(center.x - half, center.y + half)
        close()
    }
    drawPath(path, outlineColor.copy(alpha = alpha), style = Stroke(width = strokePx + 2f))
    drawPath(path, primaryColor.copy(alpha = alpha), style = Stroke(width = strokePx))
}

private fun DrawScope.drawSquareShape(
    center: Offset,
    sizePx: Float,
    strokePx: Float,
    primaryColor: Color,
    outlineColor: Color,
    alpha: Float
) {
    val half = sizePx / 2f
    val topLeft = Offset(center.x - half, center.y - half)
    val size = Size(sizePx, sizePx)

    drawRect(outlineColor.copy(alpha = alpha), topLeft, size, style = Stroke(width = strokePx + 2f))
    drawRect(primaryColor.copy(alpha = alpha), topLeft, size, style = Stroke(width = strokePx))
}

private fun DrawScope.drawHologramShape(
    center: Offset,
    sizePx: Float,
    strokePx: Float,
    gapPx: Float,
    primaryColor: Color,
    outlineColor: Color,
    alpha: Float
) {
    drawSquareShape(center, sizePx, strokePx, primaryColor, outlineColor, alpha)
    drawCrossShape(center, sizePx * 0.7f, strokePx, gapPx, primaryColor, outlineColor, alpha)
}

private fun DrawScope.drawDualRingShape(
    center: Offset,
    sizePx: Float,
    strokePx: Float,
    primaryColor: Color,
    outlineColor: Color,
    alpha: Float
) {
    val r1 = sizePx / 3f
    val r2 = sizePx / 1.8f

    drawCircle(outlineColor.copy(alpha = alpha), r1, center, style = Stroke(width = strokePx + 2f))
    drawCircle(primaryColor.copy(alpha = alpha), r1, center, style = Stroke(width = strokePx))

    drawCircle(outlineColor.copy(alpha = alpha), r2, center, style = Stroke(width = strokePx + 2f))
    drawCircle(primaryColor.copy(alpha = alpha), r2, center, style = Stroke(width = strokePx))
}

fun parseColorHex(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val colorInt = when (cleaned.length) {
            6 -> (0xFF000000 or cleaned.toLong(16)).toInt()
            8 -> cleaned.toLong(16).toInt()
            else -> 0xFFFF2A55.toInt()
        }
        Color(colorInt)
    } catch (e: Exception) {
        Color(0xFFFF2A55)
    }
}
