package com.savingcoach.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.savingcoach.app.ui.theme.ChallengeActive
import com.savingcoach.app.ui.theme.DarkMatchaPrimary
import com.savingcoach.app.ui.theme.MatchaPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import android.view.HapticFeedbackConstants

@Composable
fun EnvelopeAnimationDialog(
    amount: Double,
    isSkipped: Boolean = false,
    currencyPreference: String = "MMK",
    onDismiss: () -> Unit = {},
    onSaveClick: () -> Unit,
    onAnimationFinished: () -> Unit = {}
) {
    val view = LocalView.current
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Start animation loop
        isPlaying = true
        
        // Haptics timing
        delay(100)
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        
        delay(1300) // At 1.4s, card finishes expanding
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        
        delay(300) // Total 1.7s, envelope finishes disappearing
        onAnimationFinished()
    }

    val transition = updateTransition(targetState = isPlaying, label = "envelopeTransition")

    // 0.0s - 0.3s: Pop-in
    val popInScale by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(
                    durationMillis = 300,
                    easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
                )
            } else {
                snap()
            }
        },
        label = "popInScale"
    ) { state -> if (state) 1f else 0f }

    val popInRotation by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 300, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f))
            } else {
                snap()
            }
        },
        label = "popInRotation"
    ) { state -> if (state) -3f else 0f }

    // 0.3s - 0.6s: Flap opens
    val flapRotation by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(
                    durationMillis = 300,
                    delayMillis = 300,
                    easing = LinearOutSlowInEasing
                )
            } else {
                snap()
            }
        },
        label = "flapRotation"
    ) { state -> if (state) 180f else 0f }

    // 0.6s - 1.0s: Card emerges
    val cardTranslateY by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(
                    durationMillis = 400,
                    delayMillis = 600,
                    easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
                )
            } else {
                snap()
            }
        },
        label = "cardTranslateY"
    ) { state -> if (state) -190f else 0f }
    
    val containerTranslateY by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(
                    durationMillis = 400,
                    delayMillis = 600,
                    easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
                )
            } else {
                snap()
            }
        },
        label = "containerTranslateY"
    ) { state -> if (state) 90f else 0f }
    
    val showConfetti by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(durationMillis = 400, delayMillis = 600)
            } else snap()
        },
        label = "showConfetti"
    ) { state -> if (state) 1f else 0f }

    // 1.0s - 1.4s: Card Expands, Envelope fades down
    val finalCardScale by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(
                    durationMillis = 400,
                    delayMillis = 1000,
                    easing = FastOutSlowInEasing
                )
            } else snap()
        },
        label = "finalCardScale"
    ) { state -> if (state) 1.25f else 1f }

    // 1.3s - 1.7s: Envelope fades down after card is full
    val envelopeTranslateY by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(
                    durationMillis = 400,
                    delayMillis = 1300,
                    easing = FastOutLinearInEasing
                )
            } else snap()
        },
        label = "envelopeTranslateY"
    ) { state -> if (state) 100f else 0f }

    val envelopeOpacity by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                tween(
                    durationMillis = 300,
                    delayMillis = 1400,
                    easing = LinearEasing
                )
            } else snap()
        },
        label = "envelopeOpacity"
    ) { state -> if (state) 0f else 1f }

    val currentColorScheme = MaterialTheme.colorScheme
    val isDark = currentColorScheme.background.luminance() < 0.5f

    val envelopeBackColor = if (isDark) Color(0xFF262620) else Color(0xFFF9F6EE)
    val envelopeFlapColor = if (isDark) Color(0xFF303028) else Color(0xFFF0EBE1)
    val envelopeFrontSideColor = if (isDark) Color(0xFF2B2B23) else Color(0xFFFDFBF7)
    val envelopeFrontBottomColor = if (isDark) Color(0xFF22221C) else Color(0xFFFAF7F0)
    val envelopeGoldEdgeColor = Color(0xFFD4AF37)

    val cardBackground = if (isDark) Color(0xFF1E1E1A) else Color(0xFFFFFDF5)
    val cardBorderColor = Color(0xFFD4AF37).copy(alpha = if (isDark) 0.35f else 0.55f)
    val cardTitleColor = if (isDark) Color(0xFFB0B0A8) else Color(0xFF6B5E55)
    val cardAmountColor = if (isDark) DarkMatchaPrimary else ChallengeActive
    val cardButtonBg = if (isDark) DarkMatchaPrimary else Color(0xFF78BE8C)
    val cardButtonText = if (isDark) Color(0xFF141412) else Color(0xFF141412)
    val closeIconTint = if (isDark) Color(0xFFB0B0A8) else Color(0xFF6B5E55)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            
            // Confetti Burst behind envelope
            if (showConfetti > 0f && showConfetti < 1f) {
                EnvelopeConfettiBurst(progress = showConfetti)
            }

            // Envelope Container
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = popInScale
                        scaleY = popInScale
                        rotationZ = popInRotation
                        translationY = containerTranslateY
                    }
                    .width(280.dp)
                    .height(180.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // 1. Envelope Body (Back)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(0f)
                        .graphicsLayer {
                            translationY = envelopeTranslateY
                            alpha = envelopeOpacity
                        }
                        .background(envelopeBackColor, RoundedCornerShape(8.dp))
                )

                // 2. Envelope Flap
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                        .align(Alignment.TopCenter)
                        .zIndex(if (flapRotation > 90f) 1f else 5f)
                        .graphicsLayer {
                            translationY = envelopeTranslateY
                            alpha = envelopeOpacity
                            transformOrigin = TransformOrigin(0.5f, 0f)
                            rotationX = flapRotation
                            cameraDistance = 8 * density
                            // Drop z-index visually by manipulating shadow when flipped
                            shadowElevation = if (flapRotation > 90f) 0f else 8f
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2f, size.height)
                            close()
                        }
                        // Flap color slightly darker for depth
                        drawPath(path, color = envelopeFlapColor)
                        
                        // Metallic edge
                        drawPath(
                            path, 
                            color = envelopeGoldEdgeColor,
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // 3. Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.95f)
                        .zIndex(if (finalCardScale > 1.05f) 10f else 2f)
                        .graphicsLayer {
                            translationY = cardTranslateY
                            scaleX = finalCardScale
                            scaleY = finalCardScale
                        }
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBackground)
                        .border(
                            1.dp,
                            cardBorderColor,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Close Cross Icon
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = closeIconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSkipped) {
                            Text(
                                "SKIPPED",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                com.savingcoach.app.ui.localization.AppLocale.current.missedThisDay,
                                color = cardTitleColor,
                                fontSize = 16.sp
                            )
                        } else {
                            val strings = com.savingcoach.app.ui.localization.AppLocale.current
                            Text(
                                strings.youSaved,
                                color = cardTitleColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                strings.formatAmount(amount, currencyPreference, 1.0, isInvestment = false),
                                color = cardAmountColor,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            androidx.compose.material3.Button(
                                onClick = onSaveClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .graphicsLayer { alpha = if (finalCardScale > 1.1f) 1f else 0f },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = cardButtonBg,
                                    contentColor = cardButtonText
                                ),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text(
                                    strings.save, 
                                    fontSize = 16.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = cardButtonText
                                )
                            }
                        }
                    }
                }
                
                // 4. Envelope Body (Front cutouts to hide card bottom)

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (finalCardScale > 1.05f) 0f else 4f)
                        .graphicsLayer {
                            translationY = envelopeTranslateY
                            alpha = if (finalCardScale > 1.05f) 0f else envelopeOpacity
                        }
                ) {
                    val path1 = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(0f, 0f)
                        lineTo(size.width / 2f, size.height / 2f + 20f)
                        close()
                    }
                    val path2 = Path().apply {
                        moveTo(size.width, size.height)
                        lineTo(size.width, 0f)
                        lineTo(size.width / 2f, size.height / 2f + 20f)
                        close()
                    }
                    val path3 = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(size.width, size.height)
                        lineTo(size.width / 2f, size.height / 2f + 20f)
                        close()
                    }
                    
                    drawPath(path1, color = envelopeFrontSideColor)
                    drawPath(path2, color = envelopeFrontSideColor)
                    drawPath(path3, color = envelopeFrontBottomColor)
                    
                    // Wax Seal
                    drawCircle(
                        color = Color(0xFF8B0000), // Dark red metallic wax
                        radius = 24.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f + 20f)
                    )
                    // Wax Seal highlight
                    drawCircle(
                        color = Color(0x40FFFFFF),
                        radius = 20.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(size.width / 2f - 4f, size.height / 2f + 16f)
                    )
                }
            }
        }
    }
}

@Composable
fun EnvelopeConfettiBurst(progress: Float) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val particleCount = 40
        
        val random = kotlin.random.Random(42) // Fixed seed for stable particles during animation
        
        for (i in 0 until particleCount) {
            val angle = random.nextFloat() * 2 * PI
            val distance = (100f + random.nextFloat() * 300f) * progress
            
            val x = cx + cos(angle).toFloat() * distance
            val y = cy + sin(angle).toFloat() * distance
            
            val color = listOf(
                Color(0xFFD4AF37), // Gold
                Color(0xFFF9A826), // Orange-yellow
                Color.White,
                Color(0xFFFFD700)  // Yellow
            ).random(random)
            
            val alpha = (1f - progress).coerceIn(0f, 1f)
            
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = (4f + random.nextFloat() * 6f) * (1f - progress * 0.5f),
                center = androidx.compose.ui.geometry.Offset(x, y)
            )
        }
    }
}
