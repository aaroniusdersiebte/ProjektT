package com.projektt.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.projektt.app.MainActivity
import com.projektt.app.data.local.AppDatabase
import com.projektt.app.domain.model.Task
import com.projektt.app.domain.model.TaskList
import com.projektt.app.domain.model.UserProgress
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

data class WidgetData(
    val progress: UserProgress = UserProgress(),
    val allTasks: List<Task> = emptyList(),
    val taskLists: List<TaskList> = emptyList(),
    val overdueTasks: List<Task> = emptyList(),
    val isSignedIn: Boolean = false
)

class ProjektTWidget : GlanceAppWidget() {

    companion object {
        const val EXTRA_OPEN_ADD_DIALOG = "open_add_dialog"
        val HIDDEN_TASKS_KEY = stringSetPreferencesKey("hidden_tasks")
        val COLLAPSED_LISTS_KEY = stringSetPreferencesKey("collapsed_lists")
        val CURRENT_XP_KEY = androidx.datastore.preferences.core.intPreferencesKey("current_xp")
        val CURRENT_LEVEL_KEY = androidx.datastore.preferences.core.intPreferencesKey("current_level")

        @Volatile private var cachedWidgetData: WidgetData? = null
        @Volatile private var cacheTimestamp: Long = 0
        private const val CACHE_DURATION_MS = 30_000L // 30s

        fun invalidateCache() {
            cachedWidgetData = null
            cacheTimestamp = 0
        }

        fun getCachedWidgetData(): WidgetData? {
            val now = System.currentTimeMillis()
            return if (cachedWidgetData != null && (now - cacheTimestamp) < CACHE_DURATION_MS) {
                cachedWidgetData
            } else null
        }

        fun setCachedWidgetData(data: WidgetData) {
            cachedWidgetData = data
            cacheTimestamp = System.currentTimeMillis()
        }

        fun addToCache(task: Task) {
            val current = cachedWidgetData ?: return
            val updatedLists = current.taskLists.map { list ->
                if (list.id == task.taskListId) list.copy(tasks = list.tasks + task)
                else list
            }
            cachedWidgetData = current.copy(
                taskLists = updatedLists,
                allTasks = current.allTasks + task
            )
            cacheTimestamp = System.currentTimeMillis()
        }

        fun removeFromCache(taskId: String) {
            val current = cachedWidgetData ?: return
            cachedWidgetData = current.copy(
                allTasks = current.allTasks.filter { it.id != taskId },
                taskLists = current.taskLists.map { list ->
                    list.copy(tasks = list.tasks.filter { it.id != taskId })
                },
                overdueTasks = current.overdueTasks.filter { it.id != taskId }
            )
            cacheTimestamp = System.currentTimeMillis()
        }
    }

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(140.dp, 140.dp),
            DpSize(220.dp, 220.dp),
            DpSize(300.dp, 300.dp),
            DpSize(380.dp, 380.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData(context)

        provideContent {
            // State aus Glance Preferences lesen
            val prefs = currentState<Preferences>()
            val hiddenTasks = prefs[HIDDEN_TASKS_KEY] ?: emptySet()
            val collapsedLists = prefs[COLLAPSED_LISTS_KEY] ?: emptySet()

            // XP/Level aus Glance State (priorisiert) oder DB-Daten
            val currentXp = prefs[CURRENT_XP_KEY] ?: data.progress.currentXp
            val currentLevel = prefs[CURRENT_LEVEL_KEY] ?: data.progress.level
            val displayProgress = data.progress.copy(currentXp = currentXp, level = currentLevel)

            // Tasks filtern nach hidden
            val visibleOverdue = data.overdueTasks.filter { it.id !in hiddenTasks }
            val visibleLists = data.taskLists.map { list ->
                list.copy(tasks = list.tasks.filter { it.id !in hiddenTasks })
            }

            WidgetContent(
                progress = displayProgress,
                overdueTasks = visibleOverdue,
                taskLists = visibleLists,
                collapsedLists = collapsedLists,
                isSignedIn = data.isSignedIn
            )
        }
    }

    private suspend fun loadData(context: Context): WidgetData {
        return withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(context)
                val progressEntity = db.userProgressDao().getProgressOnce()
                val progress = progressEntity?.toDomain() ?: UserProgress()

                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WidgetEntryPoint::class.java
                )
                val taskSyncPrefs = entryPoint.taskSyncPrefs()

                // Cache-Invalidierung durch App-Signal prüfen
                if (taskSyncPrefs.shouldInvalidateCache()) {
                    invalidateCache()
                    taskSyncPrefs.clearInvalidateFlag()
                }

                // Fast-Path: gecachte WidgetData zurückgeben (vor Auth-Check)
                getCachedWidgetData()?.let { cached ->
                    return@withContext cached.copy(progress = progress)
                }

                // Slow-Path: Auth + vollständiger API-Fetch
                val googleTasksService = entryPoint.googleTasksService()
                val widgetSettingsRepo = entryPoint.widgetSettingsRepository()

                if (!googleTasksService.isInitialized()) {
                    if (!googleTasksService.initializeForWidget()) {
                        return@withContext if (taskSyncPrefs.wasSignedIn()) {
                            WidgetData(progress = progress, isSignedIn = true)
                        } else {
                            WidgetData(progress = progress, isSignedIn = false)
                        }
                    }
                }

                val taskListsWithTasks = googleTasksService.getTaskListsWithTasks()
                if (taskListsWithTasks.isEmpty()) {
                    val emptyData = WidgetData(progress = progress, isSignedIn = true)
                    setCachedWidgetData(emptyData)
                    return@withContext emptyData
                }

                // Enabled Lists aus Settings laden
                val enabledListIds = try {
                    widgetSettingsRepo.getEnabledListIds()
                } catch (e: Exception) {
                    emptyList()
                }

                val filteredLists = if (enabledListIds.isNotEmpty()) {
                    taskListsWithTasks.filter { it.id in enabledListIds }
                        .sortedBy { enabledListIds.indexOf(it.id) }
                } else {
                    taskListsWithTasks
                }

                val now = LocalDateTime.now()
                val allOverdue = filteredLists.flatMap { list ->
                    list.tasks.filter { task ->
                        !task.isCompleted && task.dueDate?.let { it.isBefore(now.toLocalDate().atStartOfDay()) } ?: false
                    }
                }

                val listsWithTasks = filteredLists.map { list ->
                    list.copy(tasks = list.tasks.filter { task ->
                        !task.isCompleted && !(task.dueDate?.let { it.isBefore(now.toLocalDate().atStartOfDay()) } ?: false)
                    })
                }.filter { it.tasks.isNotEmpty() }

                val allTasks = filteredLists.flatMap { it.tasks }.filter { !it.isCompleted }

                val result = WidgetData(
                    progress = progress,
                    allTasks = allTasks,
                    taskLists = listsWithTasks,
                    overdueTasks = allOverdue,
                    isSignedIn = true
                )
                setCachedWidgetData(result)
                result
            } catch (e: Exception) {
                WidgetData()
            }
        }
    }
}

@Composable
private fun WidgetContent(
    progress: UserProgress,
    overdueTasks: List<Task>,
    taskLists: List<TaskList>,
    collapsedLists: Set<String>,
    isSignedIn: Boolean
) {
    val size = LocalSize.current
    val isSmall = size.width < 200.dp

    val backgroundColor = Color(0xFF0D0D0D)
    val primaryColor = Color(0xFFFFB300)
    val textColor = Color(0xFFE0E0E0)
    val dimTextColor = Color(0xFF808080)
    val surfaceColor = Color(0xFF1A1A1A)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(if (isSmall) 8.dp else 12.dp)
    ) {
        // Header Row: Level + XP + Add Button
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity<MainActivity>()),
            horizontalAlignment = Alignment.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSmall) "L${progress.level}" else "LVL ${progress.level}",
                style = TextStyle(
                    color = ColorProvider(textColor),
                    fontSize = if (isSmall) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.width(6.dp))

            val blocks = if (isSmall) 4 else 6
            val filledBlocks = ((progress.currentXp.toFloat() / UserProgress.XP_PER_LEVEL) * blocks).toInt().coerceIn(0, blocks)
            Text(
                text = "█".repeat(filledBlocks) + "░".repeat(blocks - filledBlocks),
                style = TextStyle(color = ColorProvider(textColor), fontSize = if (isSmall) 10.sp else 12.sp)
            )

            Spacer(modifier = GlanceModifier.defaultWeight())

            Box(
                modifier = GlanceModifier
                    .size(if (isSmall) 24.dp else 28.dp)
                    .background(surfaceColor)
                    .clickable(actionRunCallback<OpenAddDialogAction>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = TextStyle(
                        color = ColorProvider(primaryColor),
                        fontSize = if (isSmall) 16.sp else 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        val hasAnyTasks = overdueTasks.isNotEmpty() || taskLists.any { it.tasks.isNotEmpty() }

        if (!isSignedIn) {
            NotSignedInContent()
        } else if (!hasAnyTasks) {
            NoTasksContent()
        } else {
            TaskListContent(
                overdueTasks = overdueTasks,
                taskLists = taskLists,
                collapsedLists = collapsedLists,
                isSmall = isSmall
            )
        }
    }
}

@Composable
private fun NotSignedInContent() {
    val dimTextColor = Color(0xFF808080)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "> Login erforderlich",
            style = TextStyle(color = ColorProvider(dimTextColor), fontSize = 12.sp)
        )
    }
}

@Composable
private fun NoTasksContent() {
    val dimTextColor = Color(0xFF808080)
    val successColor = Color(0xFF4CAF50)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "[OK]",
            style = TextStyle(color = ColorProvider(successColor), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "Keine Aufgaben",
            style = TextStyle(color = ColorProvider(dimTextColor), fontSize = 11.sp)
        )
    }
}

@Composable
private fun TaskListContent(
    overdueTasks: List<Task>,
    taskLists: List<TaskList>,
    collapsedLists: Set<String>,
    isSmall: Boolean
) {
    val primaryColor = Color(0xFFFFB300)
    val dimTextColor = Color(0xFF808080)
    val textColor = Color(0xFFE0E0E0)

    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        // Überfällige Section (immer zuerst, einklappbar)
        if (overdueTasks.isNotEmpty()) {
            val isOverdueCollapsed = "overdue" in collapsedLists
            item(itemId = -1L) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(
                            actionRunCallback<ToggleListAction>(
                                actionParametersOf(ToggleListAction.listIdKey to "overdue")
                            )
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOverdueCollapsed) ">" else "v",
                        style = TextStyle(color = ColorProvider(primaryColor), fontSize = if (isSmall) 10.sp else 11.sp)
                    )
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    Text(
                        text = "UEBERFAELLIG [${overdueTasks.size}]",
                        style = TextStyle(
                            color = ColorProvider(primaryColor),
                            fontSize = if (isSmall) 10.sp else 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            if (!isOverdueCollapsed) {
                items(overdueTasks, itemId = { task -> task.id.hashCode().toLong() }) { task ->
                    WidgetTaskItem(task = task, isOverdue = true, isSmall = isSmall)
                }
            }
            item(itemId = -2L) {
                Spacer(modifier = GlanceModifier.height(8.dp))
            }
        }

        // Pro TaskList eine einklappbare Section
        taskLists.forEachIndexed { index, taskList ->
            if (taskList.tasks.isNotEmpty()) {
                val isCollapsed = taskList.id in collapsedLists
                item(itemId = (index + 100).toLong()) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable(
                                actionRunCallback<ToggleListAction>(
                                    actionParametersOf(ToggleListAction.listIdKey to taskList.id)
                                )
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCollapsed) ">" else "v",
                            style = TextStyle(color = ColorProvider(dimTextColor), fontSize = if (isSmall) 10.sp else 11.sp)
                        )
                        Spacer(modifier = GlanceModifier.width(4.dp))
                        val maxTitleLen = if (isSmall) 12 else 20
                        val displayTitle = if (taskList.title.length > maxTitleLen)
                            taskList.title.take(maxTitleLen) + ".."
                        else
                            taskList.title
                        Text(
                            text = "${displayTitle.uppercase()} [${taskList.tasks.size}]",
                            style = TextStyle(
                                color = ColorProvider(dimTextColor),
                                fontSize = if (isSmall) 10.sp else 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                if (!isCollapsed) {
                    items(taskList.tasks, itemId = { task -> task.id.hashCode().toLong() + (index * 100000L) }) { task ->
                        WidgetTaskItem(task = task, isOverdue = false, isSmall = isSmall)
                    }
                }
                item(itemId = (index + 200).toLong()) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun WidgetTaskItem(task: Task, isOverdue: Boolean, isSmall: Boolean) {
    val backgroundColor = Color(0xFF1A1A1A)
    val textColor = Color(0xFFE0E0E0)
    val primaryColor = Color(0xFFFFB300)
    val dimTextColor = Color(0xFF808080)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clickable(
                actionRunCallback<CompleteTaskAction>(
                    actionParametersOf(
                        CompleteTaskAction.taskIdKey to task.id,
                        CompleteTaskAction.taskListIdKey to task.taskListId
                    )
                )
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "[  ]",
            style = TextStyle(
                color = ColorProvider(if (isOverdue) primaryColor else dimTextColor),
                fontSize = if (isSmall) 10.sp else 12.sp
            )
        )

        Spacer(modifier = GlanceModifier.width(6.dp))

        val maxChars = if (isSmall) 18 else 28
        Text(
            text = task.title.take(maxChars) + if (task.title.length > maxChars) ".." else "",
            style = TextStyle(
                color = ColorProvider(textColor),
                fontSize = if (isSmall) 11.sp else 12.sp
            ),
            maxLines = 1
        )
    }
}
