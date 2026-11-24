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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
 * Selects the matching Kafka Succubus expression portrait from the model sheet.
 */
@DrawableRes
fun kafkaExpressionDrawable(
    isRunning: Boolean = false,
    isVerifiedSuccess: Boolean = false,
): Int = when {
    isRunning -> R.drawable.img_kafka_succubus_thinking
    isVerifiedSuccess -> R.drawable.img_kafka_succubus_smile
    else -> R.drawable.img_kafka_anime_icon
}

/**
 * Draws sharp gothic corner filigree ticks in crimson & amethyst for signature cards.
 */
fun Modifier.drawSuccubusCornerSigils(
    primaryColor: Color,
    secondaryColor: Color,
    accentLength: Dp = 10.dp,
): Modifier = drawBehind {
    val len = accentLength.toPx()
    val stroke = 1.5.dp.toPx()
    val inset = 2.dp.toPx()

    // Top-left sharp crimson corner bracket
    drawLine(primaryColor.copy(alpha = 0.85f), Offset(inset, inset + len), Offset(inset, inset), stroke)
    drawLine(primaryColor.copy(alpha = 0.85f), Offset(inset, inset), Offset(inset + len, inset), stroke)

    // Bottom-right sharp amethyst corner bracket
    val right = size.width - inset
    val bottom = size.height - inset
    drawLine(secondaryColor.copy(alpha = 0.78f), Offset(right - len, bottom), Offset(right, bottom), stroke)
    drawLine(secondaryColor.copy(alpha = 0.78f), Offset(right, bottom), Offset(right, bottom - len), stroke)
}

/**
 * Animated Kafka Succubus avatar with an orbital crimson-amethyst aura ring and
 * sharp sigil glow that accelerates while Kafka is actively researching.
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
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val infiniteTransition = rememberInfiniteTransition(label = "succubus_avatar_aura")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActive) 2200 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = if (isActive) 0.6f else 0.35f,
        targetValue = if (isActive) 1.0f else 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isActive) 850 else 2400, easing = FastOutSlowInEasing),
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
            val stroke = (size.toPx() * 0.085f).coerceAtLeast(2.2f)
            rotate(rotation) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = glowPulse),
                            tertiaryColor.copy(alpha = glowPulse * 0.8f),
                            secondaryColor.copy(alpha = glowPulse),
                            primaryColor.copy(alpha = glowPulse * 0.3f),
                            tertiaryColor.copy(alpha = glowPulse),
                            primaryColor.copy(alpha = glowPulse)
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
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
 * Razor-sharp Succubus Bat-Wing & 4-Point Crimson Star crest inspired directly by
 * Kafka's model sheet emblem & gothic wing accessories.
 */
@Composable
fun SuccubusBatWingCrest(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    wingColor: Color = MaterialTheme.colorScheme.tertiaryContainer,
    wingStrokeColor: Color = MaterialTheme.colorScheme.primary,
    starColor: Color = MaterialTheme.colorScheme.secondary,
) {
    Canvas(modifier = modifier.size(width = size * 1.5f, height = size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h * 0.54f

        val leftWing = Path().apply {
            moveTo(cx - w * 0.08f, cy)
            lineTo(w * 0.02f, h * 0.06f)
            lineTo(w * 0.14f, h * 0.44f)
            lineTo(w * 0.04f, h * 0.62f)
            lineTo(w * 0.24f, h * 0.62f)
            lineTo(w * 0.28f, h * 0.84f)
            lineTo(cx - w * 0.06f, cy + h * 0.16f)
            close()
        }
        val rightWing = Path().apply {
            moveTo(cx + w * 0.08f, cy)
            lineTo(w * 0.98f, h * 0.06f)
            lineTo(w * 0.86f, h * 0.44f)
            lineTo(w * 0.96f, h * 0.62f)
            lineTo(w * 0.76f, h * 0.62f)
            lineTo(w * 0.72f, h * 0.84f)
            lineTo(cx + w * 0.06f, cy + h * 0.16f)
            close()
        }

        drawPath(leftWing, color = wingColor)
        drawPath(leftWing, color = wingStrokeColor, style = Stroke(width = 1.35.dp.toPx()))
        drawPath(rightWing, color = wingColor)
        drawPath(rightWing, color = wingStrokeColor, style = Stroke(width = 1.35.dp.toPx()))

        // Center razor-sharp 4-point crimson gothic jewel
        val rOuter = h * 0.38f
        val rInner = rOuter * 0.28f
        val star = Path().apply {
            moveTo(cx, cy - rOuter)
            lineTo(cx + rInner, cy - rInner)
            lineTo(cx + rOuter, cy)
            lineTo(cx + rInner, cy + rInner)
            lineTo(cx, cy + rOuter)
            lineTo(cx - rInner, cy + rInner)
            lineTo(cx - rOuter, cy)
            lineTo(cx - rInner, cy - rInner)
            close()
        }
        drawPath(star, color = wingStrokeColor)
        drawCircle(color = starColor, radius = rInner * 0.65f, center = Offset(cx, cy))
    }
}

/**
 * Stylized twin Succubus horn + diamond sigil icon for headers & featured cards.
 */
@Composable
fun SuccubusHornCrest(
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
    hornColor: Color = MaterialTheme.colorScheme.tertiary,
    gemColor: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f

        val leftHorn = Path().apply {
            moveTo(w * 0.34f, h * 0.70f)
            lineTo(w * 0.08f, h * 0.10f)
            lineTo(w * 0.42f, h * 0.52f)
            close()
        }
        val rightHorn = Path().apply {
            moveTo(w * 0.66f, h * 0.70f)
            lineTo(w * 0.92f, h * 0.10f)
            lineTo(w * 0.58f, h * 0.52f)
            close()
        }
        drawPath(leftHorn, color = hornColor)
        drawPath(rightHorn, color = hornColor)

        val diamond = Path().apply {
            moveTo(cx, h * 0.34f)
            lineTo(cx + w * 0.20f, h * 0.64f)
            lineTo(cx, h * 0.94f)
            lineTo(cx - w * 0.20f, h * 0.64f)
            close()
        }
        drawPath(diamond, color = gemColor)
    }
}

/**
 * Razor-sharp 4-point Succubus star accent drawn in Compose Canvas.
 */
@Composable
fun SuccubusFourPointStar(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val r = minOf(cx, cy)
        val inner = r * 0.24f
        val star = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + inner, cy - inner)
            lineTo(cx + r, cy)
            lineTo(cx + inner, cy + inner)
            lineTo(cx, cy + r)
            lineTo(cx - inner, cy + inner)
            lineTo(cx - r, cy)
            lineTo(cx - inner, cy - inner)
            close()
        }
        drawPath(star, color = color)
    }
}

/**
 * Ornamental dark-fantasy divider featuring a purple-to-crimson hairline & centered sharp star.
 */
@Composable
fun SuccubusGothicDivider(
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, tertiary.copy(alpha = 0.55f), primary.copy(alpha = 0.8f))
                    )
                )
        )
        SuccubusFourPointStar(size = 12.dp, color = primary)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(primary.copy(alpha = 0.8f), tertiary.copy(alpha = 0.55f), Color.Transparent)
                    )
                )
        )
    }
}

/**
 * Staggered bouncing crimson-sakura-amethyst orbs for live agent reasoning / streaming feedback.
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
    val transition = rememberInfiniteTransition(label = "succubus_thinking_dots")

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
 * Ambient drifting crimson embers, amethyst sparks, and razor-sharp 4-point stars drawn on Canvas.
 */
@Composable
fun AnimeSparkleParticles(
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val transition = rememberInfiniteTransition(label = "succubus_embers")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_phase"
    )

    Canvas(modifier = modifier) {
        val motes = listOf(
            Triple(0.12f, 0.25f, primary),
            Triple(0.28f, 0.72f, secondary),
            Triple(0.45f, 0.36f, tertiary),
            Triple(0.62f, 0.18f, secondary),
            Triple(0.76f, 0.65f, primary),
            Triple(0.88f, 0.32f, Color(0xFFFFD8E8)),
            Triple(0.35f, 0.14f, primary),
            Triple(0.92f, 0.78f, tertiary),
        )
        motes.forEachIndexed { i, (rx, ry, color) ->
            val shift = (phase + i * 0.13f) % 1f
            val yPos = ((ry - shift * 0.45f + 1f) % 1f) * size.height
            val xPos = (rx + kotlin.math.sin(shift * Math.PI * 2 + i).toFloat() * 0.025f) * size.width
            val alpha = (kotlin.math.sin(shift * Math.PI).toFloat() * 0.78f).coerceIn(0f, 0.85f)
            val radius = (2.2f + (i % 3) * 1.15f).dp.toPx()

            if (i % 2 == 0) {
                val r = radius * 2.2f
                val inner = r * 0.26f
                val glint = Path().apply {
                    moveTo(xPos, yPos - r)
                    lineTo(xPos + inner, yPos - inner)
                    lineTo(xPos + r, yPos)
                    lineTo(xPos + inner, yPos + inner)
                    lineTo(xPos, yPos + r)
                    lineTo(xPos - inner, yPos + inner)
                    lineTo(xPos - r, yPos)
                    lineTo(xPos - inner, yPos - inner)
                    close()
                }
                drawPath(glint, color = color.copy(alpha = alpha))
            } else {
                drawCircle(
                    color = color.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(xPos, yPos)
                )
            }
            drawCircle(
                color = color.copy(alpha = alpha * 0.28f),
                radius = radius * 2.5f,
                center = Offset(xPos, yPos)
            )
        }
    }
}
