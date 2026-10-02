package com.akira.ravex.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SharinganEyeView(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sharingan_anim")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val crimsonRed = Color(0xFFE50914)
    val darkCrimson = Color(0xFF660000)
    val cyanGlow = Color(0xFF00F0FF)

    Canvas(modifier = modifier.size(sizeDp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2.2f

        val eyeWidth = size.width * 0.95f
        val eyeHeight = size.height * 0.55f

        val eyePath = Path().apply {
            moveTo(center.x - eyeWidth / 2f, center.y)
            quadraticBezierTo(center.x, center.y - eyeHeight, center.x + eyeWidth / 2f, center.y)
            quadraticBezierTo(center.x, center.y + eyeHeight, center.x - eyeWidth / 2f, center.y)
            close()
        }

        drawPath(
            path = eyePath,
            brush = Brush.radialGradient(
                colors = listOf(crimsonRed.copy(alpha = 0.3f * pulseGlow), Color.Transparent),
                center = center,
                radius = radius * 1.5f
            )
        )

        drawPath(
            path = eyePath,
            color = crimsonRed.copy(alpha = 0.8f),
            style = Stroke(width = 2.5f)
        )

        drawPath(
            path = eyePath,
            color = cyanGlow.copy(alpha = 0.4f * pulseGlow),
            style = Stroke(width = 1f)
        )

        // Center Red Iris Circle
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(crimsonRed, darkCrimson, Color(0xFF200000)),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Inner Black Ring
        val ringRadius = radius * 0.62f
        drawCircle(
            color = Color.Black.copy(alpha = 0.85f),
            radius = ringRadius,
            center = center,
            style = Stroke(width = 3.5f)
        )

        // Center Pupil
        val pupilRadius = radius * 0.22f
        drawCircle(
            color = Color.Black,
            radius = pupilRadius,
            center = center
        )

        // 3 Tomoe Markings Rotated
        rotate(rotationAngle, center) {
            for (i in 0..2) {
                val angleRad = Math.toRadians((i * 120.0))
                val tomoeX = center.x + (ringRadius * cos(angleRad)).toFloat()
                val tomoeY = center.y + (ringRadius * sin(angleRad)).toFloat()

                drawTomoe(
                    center = Offset(tomoeX, tomoeY),
                    radius = radius * 0.12f,
                    rotationDegrees = (i * 120f) + 45f
                )
            }
        }
    }
}

private fun DrawScope.drawTomoe(
    center: Offset,
    radius: Float,
    rotationDegrees: Float
) {
    rotate(rotationDegrees, center) {
        drawCircle(
            color = Color.Black,
            radius = radius,
            center = center
        )

        val tailPath = Path().apply {
            moveTo(center.x, center.y - radius)
            quadraticBezierTo(
                center.x + radius * 1.8f, center.y - radius * 0.5f,
                center.x + radius * 0.8f, center.y + radius * 1.5f
            )
            quadraticBezierTo(
                center.x, center.y + radius * 0.5f,
                center.x, center.y - radius
            )
            close()
        }

        drawPath(tailPath, Color.Black)
    }
}
