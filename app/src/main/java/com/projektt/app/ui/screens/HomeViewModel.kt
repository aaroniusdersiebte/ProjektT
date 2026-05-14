package com.projektt.app.ui.screens

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.projektt.app.data.remote.GoogleTasksService
import com.projektt.app.data.repository.UserProgressRepository
import com.projektt.app.data.TaskSyncPrefs
import com.projektt.app.domain.model.RecurrenceType
import com.projektt.app.domain.model.Task
import com.projektt.app.domain.model.TaskList
import com.projektt.app.domain.model.UserProgress
import com.projektt.app.domain.usecase.CompleteTaskResult
import com.projektt.app.domain.usecase.CompleteTaskUseCase
import com.projektt.app.ui.animation.HapticFeedback
import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.projektt.app.widget.ProjektTWidget
import java.time.LocalDateTime
import javax.inject.Inject

data class HomeUiState(
    val isSignedIn: Boolean = false,
    val isLoading: Boolean = false,
    val progress: UserProgress = UserProgress(),
    val tasks: List<Task> = emptyList(),
    val overdueTasks: List<Task> = emptyList(),
    val todayTasks: List<Task> = emptyList(),
    val taskLists: List<TaskList> = emptyList(),
    val expandedListIds: Set<String> = emptySet(),
    val completedTasks: List<Task> = emptyList(),
    val isCompletedExpanded: Boolean = false,
    val isLoadingCompleted: Boolean = false,
    val showLevelUp: Boolean = false,
    val newLevel: Int = 0,
    val lastXpEarned: Int = 0,
    val showCreateXp: Boolean = false,
    val showWidgetXp: Boolean = false,
    val justCompletedTaskId: String? = null,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val googleTasksService: GoogleTasksService,
    private val userProgressRepository: UserProgressRepository,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val hapticFeedback: HapticFeedback,
    private val taskSyncPrefs: TaskSyncPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _editingTask = MutableStateFlow<Task?>(null)
    val editingTask: StateFlow<Task?> = _editingTask.asStateFlow()

    private var currentTaskListId: String? = null

    init {
        checkSignInStatus()
        observeProgress()
        observeWidgetSync()
        checkPendingWidgetXp()
    }

    // Prüft beim App-Start ob Widget-XP verdient wurden
    private fun checkPendingWidgetXp() {
        viewModelScope.launch {
            delay(500) // Kurz warten bis UI bereit
            val widgetXp = taskSyncPrefs.getWidgetXpEarned()
            val completedByWidget = taskSyncPrefs.getWidgetCompletedTasks()

            if (widgetXp > 0 || completedByWidget.isNotEmpty()) {
                _uiState.update { state ->
                    state.copy(
                        lastXpEarned = widgetXp,
                        showWidgetXp = widgetXp > 0
                    )
                }
                taskSyncPrefs.clearWidgetCompletedTasks()
                taskSyncPrefs.clearWidgetXpEarned()

                if (widgetXp > 0) {
                    delay(2000)
                    _uiState.update { it.copy(showWidgetXp = false) }
                }
            }
        }
    }

    private fun checkSignInStatus() {
        val isSignedIn = googleTasksService.isSignedIn()
        if (isSignedIn && !googleTasksService.isInitialized()) {
            // Nach App-Neustart: Service re-initialisieren
            googleTasksService.initializeForWidget()
        }
        _uiState.update { it.copy(isSignedIn = isSignedIn) }
    }

    private fun observeProgress() {
        viewModelScope.launch {
            userProgressRepository.getProgress().collect { progress ->
                _uiState.update { it.copy(progress = progress) }
            }
        }
    }

    private fun observeWidgetSync() {
        viewModelScope.launch {
            taskSyncPrefs.observeChanges()
                .drop(1) // Initial emit ignorieren
                .collect {
                    // Widget hat Tasks geändert → lokal filtern + XP anzeigen
                    val completedByWidget = taskSyncPrefs.getWidgetCompletedTasks()
                    val widgetXp = taskSyncPrefs.getWidgetXpEarned()

                    if (completedByWidget.isNotEmpty()) {
                        _uiState.update { state ->
                            state.copy(
                                tasks = state.tasks.filter { it.id !in completedByWidget },
                                overdueTasks = state.overdueTasks.filter { it.id !in completedByWidget },
                                todayTasks = state.todayTasks.filter { it.id !in completedByWidget },
                                lastXpEarned = widgetXp,
                                showWidgetXp = widgetXp > 0
                            )
                        }
                        taskSyncPrefs.clearWidgetCompletedTasks()
                        taskSyncPrefs.clearWidgetXpEarned()

                        // XP-Badge nach 2s ausblenden
                        if (widgetXp > 0) {
                            delay(2000)
                            _uiState.update { it.copy(showWidgetXp = false) }
                        }
                    }
                }
        }
    }

    fun getSignInIntent(): Intent = googleTasksService.signInClient.signInIntent

    fun handleSignInError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun trySilentSignIn() {
        viewModelScope.launch {
            try {
                val account = googleTasksService.signInClient.silentSignIn().await()
                handleSignInResult(account)
            } catch (e: com.google.android.gms.common.api.ApiException) {
                _uiState.update {
                    it.copy(error = "Silent-Login Code ${e.statusCode}. Tippe nochmal 'Anmelden'.")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Silent-Login: ${e.javaClass.simpleName}: ${e.message?.take(80)}")
                }
            }
        }
    }

    fun handleSignInResult(account: GoogleSignInAccount) {
        try {
            googleTasksService.initializeService(account)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "Service-Init Fehler: ${e.message}") }
            return
        }
        _uiState.update { it.copy(isSignedIn = true) }
        taskSyncPrefs.setSignedIn(true)
        loadTasks()

        // Widget sofort nach Login updaten
        viewModelScope.launch {
            try {
                taskSyncPrefs.notifyLoginStateChanged()
                ProjektTWidget.invalidateCache()
                ProjektTWidget().updateAll(context)
            } catch (e: Exception) {
                // Widget-Fehler darf Login-Flow nicht blockieren
            }
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val taskLists = googleTasksService.getTaskListsWithTasks()

                // Alle überfälligen Tasks aggregieren (aus allen Listen)
                val allOverdue = taskLists.flatMap { list ->
                    list.tasks.filter { it.isOverdue }
                }

                // Listen mit nicht-überfälligen Tasks (überfällige werden separat angezeigt)
                val listsWithTasks = taskLists.map { list ->
                    list.copy(tasks = list.tasks.filter { !it.isOverdue && !it.isCompleted })
                }

                // Alle IDs als expanded setzen beim ersten Laden
                val currentExpanded = _uiState.value.expandedListIds
                val expandedIds = if (currentExpanded.isEmpty()) {
                    taskLists.map { it.id }.toSet()
                } else {
                    currentExpanded
                }

                // Für Abwärtskompatibilität: erste Liste als currentTaskListId setzen
                if (taskLists.isNotEmpty()) {
                    currentTaskListId = taskLists.first().id
                }

                // todayTasks für Abwärtskompatibilität (alle nicht-überfälligen Tasks)
                val allToday = listsWithTasks.flatMap { it.tasks }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        taskLists = listsWithTasks,
                        expandedListIds = expandedIds,
                        overdueTasks = allOverdue,
                        todayTasks = allToday,
                        tasks = taskLists.flatMap { list -> list.tasks }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Fehler beim Laden"
                    )
                }
            }
        }
    }

    fun toggleListExpanded(listId: String) {
        _uiState.update { state ->
            val current = state.expandedListIds
            val newExpanded = if (listId in current) current - listId else current + listId
            state.copy(expandedListIds = newExpanded)
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            try {
                val result = completeTaskUseCase(task)
                hapticFeedback.taskComplete()

                _uiState.update { state ->
                    state.copy(
                        lastXpEarned = result.xpEarned,
                        justCompletedTaskId = task.id,
                        showLevelUp = result.leveledUp,
                        newLevel = result.newLevel
                    )
                }

                // Badge nach 2s ausblenden, dann Task entfernen
                delay(2000)

                _uiState.update { state ->
                    state.copy(
                        tasks = state.tasks.filter { it.id != task.id },
                        overdueTasks = state.overdueTasks.filter { it.id != task.id },
                        todayTasks = state.todayTasks.filter { it.id != task.id },
                        justCompletedTaskId = null
                    )
                }

                // Widget aktualisieren
                ProjektTWidget().updateAll(context)

                if (result.leveledUp) {
                    hapticFeedback.levelUp()
                }
            } catch (e: Exception) {
                hapticFeedback.error()
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun createTask(
        title: String,
        notes: String? = null,
        dueDate: LocalDateTime? = null,
        recurrence: RecurrenceType = RecurrenceType.NONE
    ) {
        viewModelScope.launch {
            currentTaskListId?.let { listId ->
                try {
                    // Recurrence-Tag zu Notes hinzufügen
                    val notesWithRecurrence = buildNotesWithRecurrence(notes, recurrence)
                    val createdTask = googleTasksService.createTask(listId, title, notesWithRecurrence, dueDate)

                    // Widget SOFORT updaten – vor Animation und loadTasks()
                    ProjektTWidget.addToCache(createdTask)
                    ProjektTWidget().updateAll(context)

                    // 5 XP für Task-Erstellung
                    val progress = userProgressRepository.getProgressOnce()
                    val (newProgress, _) = progress.addXpWithoutStreak(UserProgress.TASK_CREATE_XP)
                    userProgressRepository.updateProgress(newProgress)

                    // XP-Animation anzeigen
                    _uiState.update { it.copy(showCreateXp = true) }
                    delay(1500)
                    _uiState.update { it.copy(showCreateXp = false) }

                    loadTasks()
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
        }
    }

    private fun buildNotesWithRecurrence(notes: String?, recurrence: RecurrenceType): String? {
        val recurrenceTag = when (recurrence) {
            RecurrenceType.NONE -> null
            RecurrenceType.DAILY -> "[RECUR:DAILY]"
            RecurrenceType.WEEKLY -> "[RECUR:WEEKLY]"
            RecurrenceType.MONTHLY -> "[RECUR:MONTHLY]"
        }
        return when {
            recurrenceTag == null && notes == null -> null
            recurrenceTag == null -> notes
            notes == null -> recurrenceTag
            else -> "$recurrenceTag\n$notes"
        }
    }

    fun dismissLevelUp() {
        _uiState.update { it.copy(showLevelUp = false) }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun calculateNextXpReward(): Int {
        return _uiState.value.progress.calculateXpForTask()
    }

    fun toggleCompletedSection() {
        val willExpand = !_uiState.value.isCompletedExpanded
        _uiState.update { it.copy(isCompletedExpanded = willExpand) }

        // On-demand laden
        if (willExpand && _uiState.value.completedTasks.isEmpty()) {
            loadCompletedTasks()
        }
    }

    private fun loadCompletedTasks() {
        viewModelScope.launch {
            currentTaskListId?.let { listId ->
                _uiState.update { it.copy(isLoadingCompleted = true) }
                try {
                    val completed = googleTasksService.getCompletedTasks(listId, 20)
                    _uiState.update { it.copy(completedTasks = completed, isLoadingCompleted = false) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoadingCompleted = false, error = e.message) }
                }
            }
        }
    }

    fun uncompleteTask(task: Task) {
        viewModelScope.launch {
            try {
                googleTasksService.uncompleteTask(task.taskListId, task.id)
                // Aus completed entfernen, Tasks neu laden
                _uiState.update { state ->
                    state.copy(completedTasks = state.completedTasks.filter { it.id != task.id })
                }
                loadTasks()
                ProjektTWidget().updateAll(context)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // Debug: Level direkt setzen
    fun setLevel(newLevel: Int) {
        viewModelScope.launch {
            userProgressRepository.setLevel(newLevel)
        }
    }

    // Edit Task
    fun startEditTask(task: Task) {
        _editingTask.value = task
    }

    fun cancelEdit() {
        _editingTask.value = null
    }

    fun updateTask(
        task: Task,
        title: String,
        notes: String?,
        dueDate: LocalDateTime?,
        recurrence: RecurrenceType = RecurrenceType.NONE
    ) {
        viewModelScope.launch {
            try {
                val notesWithRecurrence = buildNotesWithRecurrence(notes, recurrence)
                googleTasksService.updateTask(task.taskListId, task.id, title, notesWithRecurrence, dueDate)
                _editingTask.value = null
                loadTasks()
                ProjektTWidget.invalidateCache()
                ProjektTWidget().updateAll(context)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                googleTasksService.deleteTask(task.taskListId, task.id)
                _editingTask.value = null
                loadTasks()
                ProjektTWidget.invalidateCache()
                ProjektTWidget().updateAll(context)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
