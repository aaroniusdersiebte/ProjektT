package com.projektt.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projektt.app.data.remote.GoogleTasksService
import com.projektt.app.data.repository.WidgetListSetting
import com.projektt.app.data.repository.WidgetSettingsRepository
import com.projektt.app.widget.ProjektTWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = true,
    val listSettings: List<WidgetListSetting> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val widgetSettingsRepository: WidgetSettingsRepository,
    private val googleTasksService: GoogleTasksService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Erst Task-Listen von API laden und mit DB synchronisieren
                val taskLists = googleTasksService.getTaskListsWithTasks()
                widgetSettingsRepository.syncWithTaskLists(taskLists)

                // Dann Settings beobachten
                widgetSettingsRepository.getAllSettings().collect { settings ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            listSettings = settings.sortedBy { s -> s.sortOrder }
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun toggleListEnabled(listId: String, enabled: Boolean) {
        viewModelScope.launch {
            widgetSettingsRepository.setEnabled(listId, enabled)
            updateWidget()
        }
    }

    fun moveList(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentList = _uiState.value.listSettings.toMutableList()
            if (fromIndex in currentList.indices && toIndex in currentList.indices) {
                val item = currentList.removeAt(fromIndex)
                currentList.add(toIndex, item)

                // Neue Reihenfolge speichern
                val orderedIds = currentList.map { it.listId }
                widgetSettingsRepository.reorderLists(orderedIds)
                updateWidget()
            }
        }
    }

    private suspend fun updateWidget() {
        ProjektTWidget.invalidateCache()
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(ProjektTWidget::class.java)
        glanceIds.forEach { glanceId ->
            ProjektTWidget().update(context, glanceId)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
