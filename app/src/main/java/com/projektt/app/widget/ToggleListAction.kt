package com.projektt.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState

class ToggleListAction : ActionCallback {
    companion object {
        val listIdKey = ActionParameters.Key<String>("list_id")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val listId = parameters[listIdKey] ?: return

        updateAppWidgetState(context, glanceId) { prefs ->
            val current = prefs[ProjektTWidget.COLLAPSED_LISTS_KEY] ?: emptySet()
            prefs[ProjektTWidget.COLLAPSED_LISTS_KEY] = if (listId in current) {
                current - listId
            } else {
                current + listId
            }
        }

        ProjektTWidget().update(context, glanceId)
    }
}
