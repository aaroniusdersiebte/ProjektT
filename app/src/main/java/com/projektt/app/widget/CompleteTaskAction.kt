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

        // 1. XP berechnen (DB-Arbeit vor dem Widget-Update)
        userProgressRepository.checkAndResetDailyStreak()
        val progress = userProgressRepository.getProgressOnce()
        val xpEarned = progress.calculateXpForTask()
        val (newProgress, _) = progress.addXp(xpEarned)
        val updatedProgress = newProgress.copy(
            lastCompletionDate = java.time.LocalDate.now().toString()
        )
        userProgressRepository.updateProgress(updatedProgress)
        taskSyncPrefs.addWidgetCompletedTask(taskId, xpEarned)

        // 2. Cache aktualisieren + Widget-Update
        ProjektTWidget.removeFromCache(taskId)
        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[ProjektTWidget.HIDDEN_TASKS_KEY] ?: emptySet()
            prefs[ProjektTWidget.HIDDEN_TASKS_KEY] = current + taskId
            prefs[ProjektTWidget.CURRENT_XP_KEY] = updatedProgress.currentXp
            prefs[ProjektTWidget.CURRENT_LEVEL_KEY] = updatedProgress.level
        }
        ProjektTWidget().update(context, glanceId)  // Expliziter Re-Render nötig

        // 3. API-Call
        if (!googleTasksService.isInitialized()) {
            if (!googleTasksService.initializeForWidget()) return
        }

        try {
            googleTasksService.completeTask(taskListId, taskId)
        } catch (e: Exception) {
            // Rollback bei API-Fehler
            updateAppWidgetState(context, glanceId) { prefs ->
                val current = prefs[ProjektTWidget.HIDDEN_TASKS_KEY] ?: emptySet()
                prefs[ProjektTWidget.HIDDEN_TASKS_KEY] = current - taskId
            }
            ProjektTWidget().update(context, glanceId)
        }
    }
}
