package com.projektt.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import dagger.hilt.android.EntryPointAccessors

class CompleteTaskAction : ActionCallback {

    companion object {
        val taskIdKey = ActionParameters.Key<String>("task_id")
        val taskListIdKey = ActionParameters.Key<String>("task_list_id")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val taskId = parameters[taskIdKey] ?: return
        val taskListId = parameters[taskListIdKey] ?: return

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WidgetEntryPoint::class.java
        )
        val userProgressRepository = entryPoint.userProgressRepository()
        val googleTasksService = entryPoint.googleTasksService()
        val taskSyncPrefs = entryPoint.taskSyncPrefs()

        // 1. XP SOFORT berechnen und speichern (optimistisch)
        userProgressRepository.checkAndResetDailyStreak()
        val progress = userProgressRepository.getProgressOnce()
        val xpEarned = progress.calculateXpForTask()
        val (newProgress, _) = progress.addXp(xpEarned)
        val updatedProgress = newProgress.copy(
            lastCompletionDate = java.time.LocalDate.now().toString()
        )
        userProgressRepository.updateProgress(updatedProgress)
        taskSyncPrefs.addWidgetCompletedTask(taskId, xpEarned)

        // 2. Task aus In-Memory-Cache entfernen
        ProjektTWidget.removeFromCache(taskId)

        // 3. Task ausblenden + XP/Level in Glance-State → löst reaktives Re-Render sofort aus
        // Kein update() nötig: updateAppWidgetState triggert Glance-Rerender ohne API-Call
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[ProjektTWidget.HIDDEN_TASKS_KEY] ?: emptySet()
            prefs[ProjektTWidget.HIDDEN_TASKS_KEY] = current + taskId
            prefs[ProjektTWidget.CURRENT_XP_KEY] = updatedProgress.currentXp
            prefs[ProjektTWidget.CURRENT_LEVEL_KEY] = updatedProgress.level
        }

        // 4. API-Call im Hintergrund (kann dauern)
        if (!googleTasksService.isInitialized()) {
            if (!googleTasksService.initializeForWidget()) {
                return
            }
        }

        try {
            googleTasksService.completeTask(taskListId, taskId)

        } catch (e: Exception) {
            // Rollback bei Fehler
            updateAppWidgetState(context, glanceId) { prefs ->
                val current = prefs[ProjektTWidget.HIDDEN_TASKS_KEY] ?: emptySet()
                prefs[ProjektTWidget.HIDDEN_TASKS_KEY] = current - taskId
            }
            ProjektTWidget().update(context, glanceId)
        }
    }
}
