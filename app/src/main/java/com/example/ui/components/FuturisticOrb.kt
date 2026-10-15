package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

@Composable
fun FuturisticOrb(
    state: OrbState = OrbState.IDLE,
    size: Dp = 160.dp,
    primaryAccent: Color = NeonMagenta,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")

    // Breathing pulse animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = when (state) {
            OrbState.SPEAKING -> 1.15f
            OrbState.LISTENING -> 1.10f
            OrbState.THINKING -> 1.05f
            OrbState.IDLE -> 1.02f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbState.SPEAKING -> 600
                    OrbState.LISTENING -> 800
                    OrbState.THINKING -> 400
                    OrbState.IDLE -> 1800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Continuous rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == OrbState.THINKING) 2500 else 8000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    // Wave ring ripple
    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleProgress"
    )

    Box(
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2.6f) * pulseScale

            // 1. Outer dynamic ripple ring when speaking or listening
            if (state == OrbState.LISTENING || state == OrbState.SPEAKING) {
                val rippleRadius = baseRadius + (rippleProgress * 30f)
                val rippleAlpha = (1f - rippleProgress).coerceIn(0f, 0.8f)
                drawCircle(
                    color = (if (state == OrbState.LISTENING) ElectricCyan else primaryAccent).copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // 2. Outer glowing aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryAccent.copy(alpha = 0.45f),
                        NeonPurple.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.55f
                ),
                radius = baseRadius * 1.55f,
                center = center
            )

            // 3. Orbiting neon rings
            val radAngle = Math.toRadians(rotationAngle.toDouble())
            val radAngleInv = Math.toRadians(-rotationAngle.toDouble() * 1.3)

            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        primaryAccent,
                        NeonPurple,
                        ElectricCyan,
                        primaryAccent
                    ),
                    center = center
                ),
                radius = baseRadius * 1.15f,
                center = center,
                style = Stroke(width = 2.5.dp.toPx())
            )

            drawCircle(
                color = ElectricCyan.copy(alpha = 0.6f),
                radius = baseRadius * 1.25f,
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )

            // 4. Orbiting satellite particles
            val satDistance = baseRadius * 1.2f
            val sat1X = center.x + (satDistance * cos(radAngle)).toFloat()
            val sat1Y = center.y + (satDistance * sin(radAngle)).toFloat()
            drawCircle(color = ElectricCyan, radius = 4.dp.toPx(), center = Offset(sat1X, sat1Y))

            val sat2X = center.x + (satDistance * cos(radAngleInv)).toFloat()
            val sat2Y = center.y + (satDistance * sin(radAngleInv)).toFloat()
            drawCircle(color = primaryAccent, radius = 5.dp.toPx(), center = Offset(sat2X, sat2Y))

            // 5. Core sphere gradient
            val sphereBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.95f),
                    primaryAccent,
                    NeonPurple,
                    Color(0xFF0F0B24)
                ),
                center = Offset(center.x - baseRadius * 0.25f, center.y - baseRadius * 0.25f),
                radius = baseRadius
            )
            drawCircle(brush = sphereBrush, radius = baseRadius, center = center)

            // 6. Inner spark highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = baseRadius * 0.22f,
                center = Offset(center.x - baseRadius * 0.35f, center.y - baseRadius * 0.35f)
            )
        }
    }
}
