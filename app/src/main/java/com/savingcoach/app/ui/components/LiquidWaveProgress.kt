package com.savingcoach.app.ui.components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Sensor hook that listens to the accelerometer and smooths tilt values.
 * Returns Pair(tiltX, tiltY) normalized between roughly -1f and 1f.
 */
@Composable
fun rememberDeviceTilt(): State<Pair<Float, Float>> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(Pair(0f, 0f)) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || accelerometer == null) {
            return@DisposableEffect onDispose {}
        }

        var smoothedX = 0f
        var smoothedY = 0f
        val filterAlpha = 0.12f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val rawX = -it.values[0] / 9.8f
                    val rawY = it.values[1] / 9.8f

                    smoothedX += filterAlpha * (rawX.coerceIn(-1f, 1f) - smoothedX)
                    smoothedY += filterAlpha * (rawY.coerceIn(-1f, 1f) - smoothedY)

                    tiltState.value = Pair(smoothedX, smoothedY)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return tiltState
}

/**
 * Interactive liquid wave progress indicator for the challenge emoji bubble.
 * - Smoothly fills up with water from 0% to [progress].
 * - Dual-layer sinusoidal waves for fluid depth.
 * - Real-time physics sloshing & tilt in response to device movement.
 * - Floating emoji that naturally bobs on the water surface.
 */
@Composable
fun LiquidWaveProgress(
    progress: Float,
    emoji: String,
    primaryColor: Color,
    containerBg: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 82.dp,
    isDark: Boolean = false
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    // Animated water fill level
    val animatedFillLevel by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "bubble_water_fill_level"
    )

    // Infinite wave rolling motion
    val infiniteTransition = rememberInfiniteTransition(label = "bubble_wave_physics")
    val frontWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing)
        ),
        label = "bubble_front_wave_phase"
    )
    val backWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing)
        ),
        label = "bubble_back_wave_phase"
    )

    // Device accelerometer tilt
    val deviceTilt by rememberDeviceTilt()
    val tiltX = deviceTilt.first

    // Bobbing offset for the floating emoji
    val emojiBobbingOffset = if (animatedFillLevel > 0.05f) {
        (sin(frontWavePhase.toDouble()) * 2.5f).dp
    } else {
        0.dp
    }

    // Rich, vibrant medium-emerald green water colors (a little deeper)
    val backWaveColor = if (isDark) {
        Color(0xFF065F46).copy(alpha = 0.45f)
    } else {
        Color(0xFF6EE7B7).copy(alpha = 0.55f)
    }

    val frontWaveTopColor = if (isDark) {
        Color(0xFF10B981).copy(alpha = 0.90f)
    } else {
        Color(0xFF34D399).copy(alpha = 0.90f)
    }

    val frontWaveBottomColor = if (isDark) {
        Color(0xFF047857).copy(alpha = 0.95f)
    } else {
        Color(0xFF059669).copy(alpha = 0.95f)
    }

    val foamColor = if (isDark) Color(0xFFE8FDF4).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.90f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isDark) Color(0xFF16251D) else Color(0xFFEFF8F3))
            .border(1.5.dp, if (isDark) Color(0xFF264736) else Color(0xFFC7EBD5), CircleShape)
    ) {
        // 1. Canvas Liquid Waves
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = this.size.width
            val height = this.size.height

            if (animatedFillLevel > 0f) {
                val baseWaterY = height * (1f - animatedFillLevel)
                val tiltHeightOffset = tiltX * (width * 0.28f)
                val waveAmplitude = (height * 0.045f).coerceAtLeast(3f) * (if (animatedFillLevel > 0.95f) 0.3f else 1f)
                val waveFrequency = 1.25f

                // Draw Back Wave (Translucent)
                val backWavePath = Path().apply {
                    moveTo(0f, height)
                    for (x in 0..width.toInt() step 4) {
                        val xRatio = x.toFloat() / width
                        val tiltY = (xRatio - 0.5f) * tiltHeightOffset
                        val waveY = (sin((xRatio * waveFrequency * 2 * PI) - backWavePhase).toFloat() * waveAmplitude * 0.75f)
                        val currentY = (baseWaterY + tiltY + waveY).coerceIn(0f, height)

                        if (x == 0) {
                            lineTo(0f, currentY)
                        } else {
                            lineTo(x.toFloat(), currentY)
                        }
                    }
                    lineTo(width, height)
                    close()
                }
                drawPath(path = backWavePath, color = backWaveColor)

                // Draw Front Wave (Rich gradient)
                val frontWavePath = Path().apply {
                    moveTo(0f, height)
                    for (x in 0..width.toInt() step 4) {
                        val xRatio = x.toFloat() / width
                        val tiltY = (xRatio - 0.5f) * tiltHeightOffset
                        val waveY = (sin((xRatio * waveFrequency * 2 * PI) + frontWavePhase).toFloat() * waveAmplitude)
                        val currentY = (baseWaterY + tiltY + waveY).coerceIn(0f, height)

                        if (x == 0) {
                            lineTo(0f, currentY)
                        } else {
                            lineTo(x.toFloat(), currentY)
                        }
                    }
                    lineTo(width, height)
                    close()
                }

                drawPath(
                    path = frontWavePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(frontWaveTopColor, frontWaveBottomColor),
                        startY = (baseWaterY - waveAmplitude).coerceAtLeast(0f),
                        endY = height
                    )
                )

                // Water surface foam crest line
                if (animatedFillLevel in 0.03f..0.97f) {
                    val foamPath = Path().apply {
                        var first = true
                        for (x in 0..width.toInt() step 4) {
                            val xRatio = x.toFloat() / width
                            val tiltY = (xRatio - 0.5f) * tiltHeightOffset
                            val waveY = (sin((xRatio * waveFrequency * 2 * PI) + frontWavePhase).toFloat() * waveAmplitude)
                            val currentY = (baseWaterY + tiltY + waveY).coerceIn(0f, height)

                            if (first) {
                                moveTo(0f, currentY)
                                first = false
                            } else {
                                lineTo(x.toFloat(), currentY)
                            }
                        }
                    }
                    drawPath(
                        path = foamPath,
                        color = foamColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.5.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }
            }
        }

        // 2. Center Floating Emoji with water bobbing physics
        Text(
            text = emoji,
            fontSize = 30.sp,
            modifier = Modifier.offset(y = emojiBobbingOffset)
        )

        // 3. Subtle Glass Reflection Highlight
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (isDark) 0.12f else 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(canvasWidth * 0.35f, canvasHeight * 0.25f),
                    radius = canvasWidth * 0.45f
                )
            )
        }
    }
}

/**
 * Full card liquid wave background.
 * Fills up the entire card container with water from bottom to top based on [progress].
 * Reacts to device tilt physics in real-time.
 */
@Composable
fun CardLiquidWaveBackground(
    progress: Float,
    primaryColor: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    val clampedProgress = progress.coerceIn(0f, 1f)

    // Animated water fill level
    val animatedFillLevel by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "card_water_fill_level"
    )

    // Infinite wave rolling motion
    val infiniteTransition = rememberInfiniteTransition(label = "card_wave_physics")
    val frontWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing)
        ),
        label = "card_front_wave_phase"
    )
    val backWavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing)
        ),
        label = "card_back_wave_phase"
    )

    // Device accelerometer tilt
    val deviceTilt by rememberDeviceTilt()
    val tiltX = deviceTilt.first

    // Colors
    val backWaveColor = primaryColor.copy(alpha = if (isDark) 0.30f else 0.22f)
    val frontWaveTopColor = primaryColor.copy(alpha = if (isDark) 0.70f else 0.55f)
    val frontWaveBottomColor = if (isDark) {
        primaryColor.copy(alpha = 0.88f)
    } else {
        Color(
            red = (primaryColor.red * 0.85f).coerceIn(0f, 1f),
            green = (primaryColor.green * 0.85f).coerceIn(0f, 1f),
            blue = (primaryColor.blue * 0.85f).coerceIn(0f, 1f),
            alpha = 0.75f
        )
    }
    val foamColor = if (isDark) Color(0xFFE8F5E9).copy(alpha = 0.70f) else Color.White.copy(alpha = 0.85f)

    Canvas(modifier = modifier) {
        val width = this.size.width
        val height = this.size.height

        if (animatedFillLevel > 0f) {
            val baseWaterY = height * (1f - animatedFillLevel)
            val tiltHeightOffset = tiltX * (width * 0.28f)
            val waveAmplitude = (height * 0.04f).coerceAtLeast(3f) * (if (animatedFillLevel > 0.96f) 0.2f else 1f)
            val waveFrequency = 1.15f

            // Draw Back Wave
            val backWavePath = Path().apply {
                moveTo(0f, height)
                for (x in 0..width.toInt() step 4) {
                    val xRatio = x.toFloat() / width
                    val tiltY = (xRatio - 0.5f) * tiltHeightOffset
                    val waveY = (sin((xRatio * waveFrequency * 2 * PI) - backWavePhase).toFloat() * waveAmplitude * 0.75f)
                    val currentY = (baseWaterY + tiltY + waveY).coerceIn(0f, height)

                    if (x == 0) {
                        lineTo(0f, currentY)
                    } else {
                        lineTo(x.toFloat(), currentY)
                    }
                }
                lineTo(width, height)
                close()
            }
            drawPath(path = backWavePath, color = backWaveColor)

            // Draw Front Wave
            val frontWavePath = Path().apply {
                moveTo(0f, height)
                for (x in 0..width.toInt() step 4) {
                    val xRatio = x.toFloat() / width
                    val tiltY = (xRatio - 0.5f) * tiltHeightOffset
                    val waveY = (sin((xRatio * waveFrequency * 2 * PI) + frontWavePhase).toFloat() * waveAmplitude)
                    val currentY = (baseWaterY + tiltY + waveY).coerceIn(0f, height)

                    if (x == 0) {
                        lineTo(0f, currentY)
                    } else {
                        lineTo(x.toFloat(), currentY)
                    }
                }
                lineTo(width, height)
                close()
            }

            drawPath(
                path = frontWavePath,
                brush = Brush.verticalGradient(
                    colors = listOf(frontWaveTopColor, frontWaveBottomColor),
                    startY = (baseWaterY - waveAmplitude).coerceAtLeast(0f),
                    endY = height
                )
            )

            // Surface foam crest line
            if (animatedFillLevel in 0.02f..0.98f) {
                val foamPath = Path().apply {
                    var first = true
                    for (x in 0..width.toInt() step 4) {
                        val xRatio = x.toFloat() / width
                        val tiltY = (xRatio - 0.5f) * tiltHeightOffset
                        val waveY = (sin((xRatio * waveFrequency * 2 * PI) + frontWavePhase).toFloat() * waveAmplitude)
                        val currentY = (baseWaterY + tiltY + waveY).coerceIn(0f, height)

                        if (first) {
                            moveTo(0f, currentY)
                            first = false
                        } else {
                            lineTo(x.toFloat(), currentY)
                        }
                    }
                }
                drawPath(
                    path = foamPath,
                    color = foamColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }
        }
    }
}
