package com.projektt.app.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.projektt.app.ui.theme.ProjektTTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun LevelUpOverlay(
    level: Int,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(true) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    LaunchedEffect(Unit) {
        delay(2000)
        visible = false
        delay(300)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ProjektTTheme.colors.background.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        // Glitch effect text layers
        GlitchText(
            text = "LEVEL $level",
            modifier = Modifier.scale(scale)
        )
        
        // Particle effects
        ParticleSystem(
            modifier = Modifier.fillMaxSize(),
            particleCount = 30
        )
    }
}

@Composable
private fun GlitchText(
    text: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glitch")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX"
    )

    Box(modifier = modifier) {
        // Red layer (offset left)
        Text(
            text = text,
            style = ProjektTTheme.typography.displayLarge,
            color = Color.Red.copy(alpha = 0.7f),
            modifier = Modifier.offset(x = (-2 + offsetX).dp)
        )
        // Cyan layer (offset right)
        Text(
            text = text,
            style = ProjektTTheme.typography.displayLarge,
            color = Color.Cyan.copy(alpha = 0.7f),
            modifier = Modifier.offset(x = (2 - offsetX).dp)
        )
        // Main white text
        Text(
            text = text,
            style = ProjektTTheme.typography.displayLarge,
            color = ProjektTTheme.colors.onBackground
        )
    }
}

@Composable
private fun ParticleSystem(
    modifier: Modifier = Modifier,
    particleCount: Int = 20
) {
    val particles = remember {
        List(particleCount) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                speed = Random.nextFloat() * 0.02f + 0.01f,
                size = Random.nextFloat() * 8f + 4f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing)
        ),
        label = "time"
    )

    Box(modifier = modifier) {
        particles.forEach { particle ->
            val yOffset = (particle.y + time * particle.speed * 10) % 1f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.TopStart)
                    .offset(
                        x = (particle.x * 400).dp,
                        y = (yOffset * 800).dp
                    )
                    .size(particle.size.dp)
                    .alpha((1f - yOffset) * 0.8f)
                    .background(ProjektTTheme.colors.primary)
            )
        }
    }
}

private data class Particle(
    val x: Float,
    val y: Float,
    val speed: Float,
    val size: Float
)
