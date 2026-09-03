package com.victorkirui.module_features.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ProCelebrationOverlay(
    modifier: Modifier = Modifier
) {
    // 1. Two-Phase Burst Animation (0ms-1000ms: Central orb hold & pulse, 1000ms-3600ms: Outward blast to corners)
    val burstAnimatable = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        burstAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = keyframes {
                durationMillis = 3600
                0.0f at 0
                0.08f at 1000 using FastOutSlowInEasing // Hold and pulse in center for 1 full second
                1.0f at 3600 using CubicBezierEasing(0.1f, 0.7f, 0.1f, 1.0f) // Blast outward to corners
            }
        )
    }

    // 2. Continuous Pulsing Edge Glow (smooth wave)
    val infiniteTransition = rememberInfiniteTransition(label = "edge_glow_pulse")
    val edgePulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "edge_pulse_alpha"
    )

    // Particle Data (60 particles for rich coverage)
    val particles = remember {
        List(60) { index ->
            val angle = (index * (360f / 60f)) * (Math.PI.toFloat() / 180f)
            val speedFactor = Random.nextFloat() * 0.35f + 0.85f
            val particleColor = when (index % 4) {
                0 -> Color(0xFFD4AF37) // Gold
                1 -> Color(0xFF2D6A4F) // Evergreen
                2 -> Color(0xFFFFD700) // Bright Yellow-Gold
                else -> Color(0xFFE76F51) // Warm Coral Accent
            }
            val radiusSize = Random.nextFloat() * 10f + 8f
            ParticleData(angle, speedFactor, particleColor, radiusSize)
        }
    }

    val displayCornerRadius = 28.dp
    val burstProgress = burstAnimatable.value

    // Edge glow opacity builds as particles blast outward toward the borders
    val edgeGlowOpacity = if (burstProgress < 0.08f) {
        burstProgress * 4f * edgePulseAlpha // Subtle entrance during central hold
    } else {
        ((burstProgress - 0.08f) * 2.5f).coerceIn(0.4f, 1.0f) * edgePulseAlpha
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // --- EDGE GLOW OVERLAY (Hugging rounded display corners) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    border = BorderStroke(
                        width = 6.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFD4AF37).copy(alpha = edgeGlowOpacity),
                                Color(0xFF2D6A4F).copy(alpha = edgeGlowOpacity),
                                Color(0xFFD4AF37).copy(alpha = edgeGlowOpacity)
                            )
                        )
                    ),
                    shape = RoundedCornerShape(displayCornerRadius)
                )
        )

        // Inner Corner Glow Highlights
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    border = BorderStroke(
                        width = 2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF2D6A4F).copy(alpha = edgeGlowOpacity * 0.8f),
                                Color.Transparent,
                                Color(0xFFD4AF37).copy(alpha = edgeGlowOpacity * 0.8f)
                            )
                        )
                    ),
                    shape = RoundedCornerShape(displayCornerRadius)
                )
        )

        // --- CENTRAL PARTICLE BURST & CORE ORB ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxDistance = kotlin.math.hypot(size.width / 2f, size.height / 2f)

            // 1. Central Orb Pulse during Phase 1 (0ms - 1000ms)
            if (burstProgress <= 0.25f) {
                val orbPulseAlpha = (1f - (burstProgress / 0.25f)).coerceIn(0f, 1f)
                val orbRadius = 36.dp.toPx() * (1f + (burstProgress * 2f))

                // Inner Glowing Core
                drawCircle(
                    color = Color(0xFFFFD700).copy(alpha = orbPulseAlpha * 0.9f),
                    radius = orbRadius * 0.5f,
                    center = center
                )

                // Outer Glowing Halo
                drawCircle(
                    color = Color(0xFFD4AF37).copy(alpha = orbPulseAlpha * 0.4f),
                    radius = orbRadius,
                    center = center
                )
            }

            // 2. Radiating Particle Explosion
            if (burstProgress < 1f) {
                particles.forEach { particle ->
                    val currentDistance = maxDistance * burstProgress * particle.speedFactor
                    val x = center.x + cos(particle.angle.toDouble()).toFloat() * currentDistance
                    val y = center.y + sin(particle.angle.toDouble()).toFloat() * currentDistance

                    val alpha = if (burstProgress < 0.08f) {
                        0.9f // Fully visible during central hold
                    } else {
                        (1f - ((burstProgress - 0.08f) / 0.92f)).coerceIn(0f, 1f)
                    }

                    val currentRadius = particle.radiusSize * (1f - burstProgress * 0.3f)

                    // Particle Circle
                    drawCircle(
                        color = particle.color.copy(alpha = alpha),
                        radius = currentRadius,
                        center = Offset(x, y)
                    )

                    // Outer Particle Glow Trail
                    drawCircle(
                        color = particle.color.copy(alpha = alpha * 0.4f),
                        radius = currentRadius * 1.8f,
                        center = Offset(x, y)
                    )
                }

                // Outward Expanding Shockwave Ring (Phase 2)
                if (burstProgress > 0.08f) {
                    val shockProgress = (burstProgress - 0.08f) / 0.92f
                    val shockwaveRadius = maxDistance * 0.75f * shockProgress
                    val shockwaveAlpha = (1f - shockProgress * 1.2f).coerceAtLeast(0f)

                    if (shockwaveAlpha > 0f) {
                        drawCircle(
                            color = Color(0xFFD4AF37).copy(alpha = shockwaveAlpha),
                            radius = shockwaveRadius,
                            center = center,
                            style = Stroke(width = 6.dp.toPx() * (1f - shockProgress))
                        )
                    }
                }
            }
        }
    }
}

private data class ParticleData(
    val angle: Float,
    val speedFactor: Float,
    val color: Color,
    val radiusSize: Float
)
