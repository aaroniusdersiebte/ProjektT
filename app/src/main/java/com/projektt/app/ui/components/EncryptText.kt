package com.projektt.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.projektt.app.ui.theme.ProjektTTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

private val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"

@Composable
fun EncryptText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = ProjektTTheme.typography.bodyLarge,
    animationDurationMs: Int = 800,
    scrambleIterations: Int = 8
) {
    var displayText by remember(text) { mutableStateOf(scrambleString(text)) }
    val revealProgress = remember(text) { Animatable(0f) }

    LaunchedEffect(text) {
        revealProgress.snapTo(0f)
        
        repeat(scrambleIterations) { iteration ->
            val progress = (iteration + 1).toFloat() / scrambleIterations
            val revealedCount = (text.length * progress).toInt()
            
            displayText = buildString {
                text.forEachIndexed { index, char ->
                    if (index < revealedCount) {
                        append(char)
                    } else {
                        append(CHARS.random())
                    }
                }
            }
            delay((animationDurationMs / scrambleIterations).toLong())
        }
        
        displayText = text
        revealProgress.animateTo(1f, tween(100))
    }

    Text(
        text = displayText,
        modifier = modifier,
        style = style,
        color = ProjektTTheme.colors.onBackground
    )
}

private fun scrambleString(text: String): String {
    return text.map { if (it.isWhitespace()) it else CHARS.random() }.joinToString("")
}
