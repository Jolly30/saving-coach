package com.savingcoach.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlinx.coroutines.delay
import kotlin.random.Random

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var color: Color,
    var rotation: Float,
    var rotationSpeed: Float,
    var size: Float
)

@Composable
fun ConfettiView(modifier: Modifier = Modifier) {
    val particles = remember {
        List(100) {
            ConfettiParticle(
                x = Random.nextFloat(), // relative 0..1
                y = Random.nextFloat() * -1.5f, // start above screen, spread out
                velocityX = (Random.nextFloat() - 0.5f) * 0.005f,
                velocityY = Random.nextFloat() * 0.015f + 0.005f,
                color = listOf(
                    Color(0xFFE91E63), Color(0xFF2196F3), Color(0xFF4CAF50), 
                    Color(0xFFFFEB3B), Color(0xFFFF9800), Color(0xFF9C27B0)
                ).random(),
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 15f,
                size = Random.nextFloat() * 20f + 15f
            )
        }
    }

    var ticks by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // ~60fps
            ticks++
            particles.forEach {
                it.x += it.velocityX
                it.y += it.velocityY
                it.rotation += it.rotationSpeed
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        
        // Reading ticks forces recomposition
        ticks.let {
            particles.forEach { p ->
                val px = p.x * w
                val py = p.y * h
                // Only draw if within screen vertically
                if (py > -100f && py < h + 100f) {
                    rotate(degrees = p.rotation, pivot = Offset(px, py)) {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(px - p.size / 2, py - p.size / 2),
                            size = Size(p.size, p.size)
                        )
                    }
                }
            }
        }
    }
}
