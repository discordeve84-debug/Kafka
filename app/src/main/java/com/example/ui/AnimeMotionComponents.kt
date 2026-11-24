package com.example.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Animated anime girl avatar with an orbital cyber-sakura energy ring that smoothly pulses
 * when the agent or voice session is active.
 */
@Composable
fun AnimePulseAvatar(
    @DrawableRes drawableRes: Int = R.drawable.img_kafka_anime_icon,
    contentDescription: String?,
    isActive: Boolean = false,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val infiniteTransition = rememberInfiniteTransition(label = "anime_avatar_aura")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActive) 2200 else 7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.55f else 0.25f,
        targetValue = if (isActive) 1.0f else 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActive) 900 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_glow"
    )
    val activeScale by animateFloatAsState(
        targetValue = if (isActive) 1.07f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "active_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = activeScale
                scaleY = activeScale
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = (size.toPx() * 0.08f).coerceAtLeast(2f)
            rotate(rotation) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = glowPulse),
                            secondaryColor.copy(alpha = glowPulse),
                            primaryColor.copy(alpha = glowPulse * 0.25f),
                            secondaryColor.copy(alpha = glowPulse),
                            primaryColor.copy(alpha = glowPulse)
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
                .clip(CircleShape)
        )
    }
}

/**
 * Staggered bouncing neon orbs for live agent reasoning / streaming feedback.
 */
@Composable
fun AnimeThinkingIndicator(
    modifier: Modifier = Modifier,
    dotSize: Dp = 7.dp,
) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )
    val transition = rememberInfiniteTransition(label = "anime_thinking_dots")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEachIndexed { idx, color ->
            val offsetY by transition.animateFloat(
                initialValue = 0f,
                targetValue = -5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 480,
                        delayMillis = idx * 140,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_bounce_$idx"
            )
            Box(
                modifier = Modifier
                    .offset(y = offsetY.dp)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/**
 * Ambient drifting starlight and sakura motes drawn smoothly on Canvas over hero headers.
 */
@Composable
fun AnimeSparkleParticles(
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val transition = rememberInfiniteTransition(label = "anime_sparkles")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_phase"
    )

    Canvas(modifier = modifier) {
        val motes = listOf(
            Triple(0.14f, 0.25f, primary),
            Triple(0.28f, 0.72f, secondary),
            Triple(0.45f, 0.36f, tertiary),
            Triple(0.62f, 0.18f, secondary),
            Triple(0.76f, 0.65f, primary),
            Triple(0.88f, 0.32f, Color.White),
            Triple(0.35f, 0.14f, Color.White),
            Triple(0.92f, 0.78f, tertiary),
        )
        motes.forEachIndexed { i, (rx, ry, color) ->
            val shift = (phase + i * 0.13f) % 1f
            val yPos = ((ry - shift * 0.45f + 1f) % 1f) * size.height
            val xPos = (rx + kotlin.math.sin(shift * Math.PI * 2 + i).toFloat() * 0.025f) * size.width
            val alpha = (kotlin.math.sin(shift * Math.PI).toFloat() * 0.75f).coerceIn(0f, 0.8f)
            val radius = (2.2f + (i % 3) * 1.1f).dp.toPx()

            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = Offset(xPos, yPos)
            )
            drawCircle(
                color = color.copy(alpha = alpha * 0.28f),
                radius = radius * 2.4f,
                center = Offset(xPos, yPos)
            )
        }
    }
}
