package com.projektt.app.data.remote

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.tasks.Tasks
import com.google.api.services.tasks.TasksScopes
import com.projektt.app.domain.model.RecurrenceType
import com.projektt.app.domain.model.Task
import com.projektt.app.domain.model.TaskList
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleTasksService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tasksService: Tasks? = null
    
    val signInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(TasksScopes.TASKS))
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun isSignedIn(): Boolean = GoogleSignIn.getLastSignedInAccount(context) != null

    /**
     * Initialisiert den Service für Widget-Kontext (ohne Activity).
     * Nutzt den zuletzt eingeloggten Account aus dem Credential Manager.
     * @return true wenn erfolgreich, false wenn kein Account vorhanden
     */
    fun initializeForWidget(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        initializeService(account)
        return true
    }

    fun isInitialized(): Boolean = tasksService != null

    fun initializeService(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(TasksScopes.TASKS)
        ).apply {
            selectedAccount = account.account
        }

        tasksService = Tasks.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("ProjektT")
            .build()
    }

    suspend fun getTaskLists(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        tasksService?.tasklists()?.list()?.execute()?.items?.map {
            it.id to it.title
        } ?: emptyList()
    }

    suspend fun getTaskListsWithTasks(): List<TaskList> = withContext(Dispatchers.IO) {
        val lists = tasksService?.tasklists()?.list()?.execute()?.items ?: emptyList()
        lists.map { list ->
            TaskList(
                id = list.id,
                title = list.title,
                tasks = getTasks(list.id)
            )
        }
    }

    suspend fun getTasks(taskListId: String): List<Task> = withContext(Dispatchers.IO) {
        tasksService?.tasks()?.list(taskListId)
            ?.setShowCompleted(false)
            ?.setShowHidden(false)
            ?.execute()
            ?.items
            ?.map { googleTask ->
                val (recurrence, cleanNotes) = parseRecurrenceFromNotes(googleTask.notes)
                Task(
                    id = googleTask.id,
                    title = googleTask.title ?: "",
                    notes = cleanNotes,
                    dueDate = googleTask.due?.let { parseGoogleDate(it) },
                    isCompleted = googleTask.status == "completed",
                    taskListId = taskListId,
                    position = googleTask.position,
                    recurrence = recurrence
                )
            } ?: emptyList()
    }

    suspend fun completeTask(taskListId: String, taskId: String) = withContext(Dispatchers.IO) {
        val task = tasksService?.tasks()?.get(taskListId, taskId)?.execute()
        task?.status = "completed"
        tasksService?.tasks()?.update(taskListId, taskId, task)?.execute()
    }

    suspend fun uncompleteTask(taskListId: String, taskId: String) = withContext(Dispatchers.IO) {
        val task = tasksService?.tasks()?.get(taskListId, taskId)?.execute()
        task?.status = "needsAction"
        task?.completed = null
        tasksService?.tasks()?.update(taskListId, taskId, task)?.execute()
    }

    suspend fun getCompletedTasks(taskListId: String, maxResults: Int = 20): List<Task> = withContext(Dispatchers.IO) {
        tasksService?.tasks()?.list(taskListId)
            ?.setShowCompleted(true)
            ?.setShowHidden(true)
            ?.setMaxResults(maxResults)
            ?.execute()
            ?.items
            ?.filter { it.status == "completed" }
            ?.take(maxResults)
            ?.map { googleTask ->
                val (recurrence, cleanNotes) = parseRecurrenceFromNotes(googleTask.notes)
                Task(
                    id = googleTask.id,
                    title = googleTask.title ?: "",
                    notes = cleanNotes,
                    dueDate = googleTask.due?.let { parseGoogleDate(it) },
                    isCompleted = true,
                    taskListId = taskListId,
                    position = googleTask.position,
                    recurrence = recurrence
                )
            } ?: emptyList()
    }

    suspend fun createTask(taskListId: String, title: String, notes: String? = null, dueDate: LocalDateTime? = null): Task = withContext(Dispatchers.IO) {
        val googleTask = com.google.api.services.tasks.model.Task().apply {
            this.title = title
            this.notes = notes
            dueDate?.let {
                this.due = it.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            }
        }
        val created = tasksService?.tasks()?.insert(taskListId, googleTask)?.execute()
            ?: throw IllegalStateException("Task creation failed")
        val (recurrence, cleanNotes) = parseRecurrenceFromNotes(created.notes)
        Task(
            id = created.id,
            title = created.title ?: title,
            notes = cleanNotes,
            dueDate = created.due?.let { parseGoogleDate(it) } ?: dueDate,
            isCompleted = false,
            taskListId = taskListId,
            position = created.position,
            recurrence = recurrence
        )
    }

    suspend fun updateTask(
        taskListId: String,
        taskId: String,
        title: String,
        notes: String? = null,
        dueDate: LocalDateTime? = null
    ) = withContext(Dispatchers.IO) {
        val task = tasksService?.tasks()?.get(taskListId, taskId)?.execute()
        task?.title = title
        task?.notes = notes
        task?.due = dueDate?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        tasksService?.tasks()?.update(taskListId, taskId, task)?.execute()
    }

    suspend fun deleteTask(taskListId: String, taskId: String) = withContext(Dispatchers.IO) {
        tasksService?.tasks()?.delete(taskListId, taskId)?.execute()
    }

    private fun parseGoogleDate(dateString: String): LocalDateTime? {
        return try {
            LocalDateTime.parse(dateString.removeSuffix("Z") + if (dateString.contains("T")) "" else "T00:00:00")
        } catch (e: Exception) {
            null
        }
    }

    private fun parseRecurrenceFromNotes(notes: String?): Pair<RecurrenceType, String?> {
        if (notes == null) return RecurrenceType.NONE to null

        val recurrencePattern = Regex("""\[RECUR:(DAILY|WEEKLY|MONTHLY)\]""")
        val match = recurrencePattern.find(notes)

        val recurrence = when (match?.groupValues?.getOrNull(1)) {
            "DAILY" -> RecurrenceType.DAILY
            "WEEKLY" -> RecurrenceType.WEEKLY
            "MONTHLY" -> RecurrenceType.MONTHLY
            else -> RecurrenceType.NONE
        }

        val cleanNotes = notes
            .replace(recurrencePattern, "")
            .trim()
            .ifEmpty { null }

        return recurrence to cleanNotes
    }
}
