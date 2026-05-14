package com.projektt.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskSyncPrefs @Inject constructor(
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "task_sync", Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_WIDGET_COMPLETED_TASKS = "widget_completed_tasks"
        private const val KEY_TASKS_CHANGED = "tasks_changed_timestamp"
        private const val KEY_WIDGET_XP_EARNED = "widget_xp_earned"
        private const val KEY_INVALIDATE_CACHE = "invalidate_widget_cache"
        private const val KEY_LOGIN_STATE_CHANGED = "login_state_changed"
        private const val KEY_WAS_SIGNED_IN = "was_signed_in"
    }

    fun addWidgetCompletedTask(taskId: String, xpEarned: Int) {
        val current = getWidgetCompletedTasks()
        prefs.edit()
            .putStringSet(KEY_WIDGET_COMPLETED_TASKS, current + taskId)
            .putInt(KEY_WIDGET_XP_EARNED, xpEarned)
            .putLong(KEY_TASKS_CHANGED, System.currentTimeMillis())
            .commit() // commit statt apply für sofortige Persistenz
    }

    fun getWidgetXpEarned(): Int = prefs.getInt(KEY_WIDGET_XP_EARNED, 0)

    fun clearWidgetXpEarned() {
        prefs.edit().putInt(KEY_WIDGET_XP_EARNED, 0).apply()
    }

    fun getWidgetCompletedTasks(): Set<String> {
        return prefs.getStringSet(KEY_WIDGET_COMPLETED_TASKS, emptySet()) ?: emptySet()
    }

    fun clearWidgetCompletedTasks() {
        prefs.edit()
            .putStringSet(KEY_WIDGET_COMPLETED_TASKS, emptySet())
            .apply()
    }

    fun notifyTasksChanged() {
        prefs.edit()
            .putLong(KEY_TASKS_CHANGED, System.currentTimeMillis())
            .putBoolean(KEY_INVALIDATE_CACHE, true)
            .commit()
    }

    fun shouldInvalidateCache(): Boolean = prefs.getBoolean(KEY_INVALIDATE_CACHE, false)

    fun clearInvalidateFlag() {
        prefs.edit().putBoolean(KEY_INVALIDATE_CACHE, false).apply()
    }

    fun setSignedIn(signedIn: Boolean) {
        prefs.edit().putBoolean(KEY_WAS_SIGNED_IN, signedIn).commit()
    }

    fun wasSignedIn(): Boolean = prefs.getBoolean(KEY_WAS_SIGNED_IN, false)

    fun notifyLoginStateChanged() {
        prefs.edit()
            .putLong(KEY_LOGIN_STATE_CHANGED, System.currentTimeMillis())
            .putBoolean(KEY_INVALIDATE_CACHE, true)
            .putBoolean(KEY_WAS_SIGNED_IN, true)
            .commit()
    }

    fun observeChanges(): Flow<Long> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_TASKS_CHANGED || key == KEY_WIDGET_COMPLETED_TASKS) {
                trySend(System.currentTimeMillis())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getLong(KEY_TASKS_CHANGED, 0))
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
}
