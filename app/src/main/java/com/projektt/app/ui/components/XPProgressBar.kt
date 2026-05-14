package com.projektt.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.projektt.app.ui.theme.ProjektTTheme

@Composable
fun XPProgressBar(
    currentXp: Int,
    maxXp: Int,
    modifier: Modifier = Modifier
) {
    val progress = currentXp.toFloat() / maxXp
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "xp_progress"
    )

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ProjektTTheme.colors.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ProjektTTheme.colors.primary)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "$currentXp / $maxXp XP",
            style = ProjektTTheme.typography.labelSmall,
            color = ProjektTTheme.colors.onBackgroundDim
        )
    }
}
