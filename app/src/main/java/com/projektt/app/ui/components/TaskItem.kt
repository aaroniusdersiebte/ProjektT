package com.projektt.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.projektt.app.domain.model.Task
import com.projektt.app.ui.theme.ProjektTTheme
import java.time.format.DateTimeFormatter

@Composable
fun TaskItem(
    task: Task,
    xpReward: Int,
    onComplete: (Task) -> Unit,
    onEdit: ((Task) -> Unit)? = null,
    showXpBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            showXpBadge -> ProjektTTheme.colors.success.copy(alpha = 0.15f)
            task.isOverdue -> ProjektTTheme.colors.primaryDim.copy(alpha = 0.2f)
            else -> ProjektTTheme.colors.surface
        },
        animationSpec = spring(),
        label = "bg_color"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .clickable(enabled = !showXpBadge && onEdit != null) { onEdit?.invoke(task) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted || showXpBadge,
            onCheckedChange = { if (!showXpBadge) onComplete(task) },
            enabled = !showXpBadge,
            colors = CheckboxDefaults.colors(
                checkedColor = ProjektTTheme.colors.success,
                uncheckedColor = ProjektTTheme.colors.onBackgroundDim,
                checkmarkColor = ProjektTTheme.colors.background
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = ProjektTTheme.typography.bodyLarge,
                color = ProjektTTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isCompleted || showXpBadge) TextDecoration.LineThrough else null
            )

            task.dueDate?.let { due ->
                Text(
                    text = due.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    style = ProjektTTheme.typography.labelSmall,
                    color = if (task.isOverdue)
                        ProjektTTheme.colors.primary
                    else
                        ProjektTTheme.colors.onBackgroundDim
                )
            }
        }

        // XP Badge - nur bei Completion anzeigen
        AnimatedVisibility(
            visible = showXpBadge,
            enter = fadeIn(tween(300)) + scaleIn(tween(300)),
            exit = fadeOut(tween(300)) + scaleOut(tween(300))
        ) {
            Text(
                text = "+$xpReward XP",
                style = ProjektTTheme.typography.titleMedium,
                color = ProjektTTheme.colors.success
            )
        }
    }
}
