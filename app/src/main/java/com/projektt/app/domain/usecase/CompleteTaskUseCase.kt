package com.projektt.app.domain.usecase

import com.projektt.app.data.remote.GoogleTasksService
import com.projektt.app.data.repository.UserProgressRepository
import com.projektt.app.domain.model.RecurrenceType
import com.projektt.app.domain.model.Task
import java.time.LocalDate
import javax.inject.Inject

data class CompleteTaskResult(
    val xpEarned: Int,
    val leveledUp: Boolean,
    val newLevel: Int
)

class CompleteTaskUseCase @Inject constructor(
    private val googleTasksService: GoogleTasksService,
    private val userProgressRepository: UserProgressRepository
) {
    suspend operator fun invoke(task: Task): CompleteTaskResult {
        // Complete task in Google Tasks
        googleTasksService.completeTask(task.taskListId, task.id)

        // Falls Task wiederkehrend: neuen Task mit nächstem Datum erstellen
        if (task.recurrence != RecurrenceType.NONE) {
            val nextDueDate = task.getNextDueDate()
            if (nextDueDate != null) {
                // Notes mit Recurrence-Tag beibehalten
                googleTasksService.createTask(
                    taskListId = task.taskListId,
                    title = task.title,
                    notes = task.notes,
                    dueDate = nextDueDate
                )
            }
        }

        // Check if we need to reset daily streak
        userProgressRepository.checkAndResetDailyStreak()

        // Get current progress
        var progress = userProgressRepository.getProgressOnce()

        // Calculate XP with streak multiplier
        val xpEarned = progress.calculateXpForTask()

        // Add XP and check for level up
        val (newProgress, leveledUp) = progress.addXp(xpEarned)

        // Update with today's date
        val updatedProgress = newProgress.copy(lastCompletionDate = LocalDate.now().toString())

        // Save progress
        userProgressRepository.updateProgress(updatedProgress)

        return CompleteTaskResult(
            xpEarned = xpEarned,
            leveledUp = leveledUp,
            newLevel = updatedProgress.level
        )
    }
}
