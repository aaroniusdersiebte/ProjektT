package com.projektt.app.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.projektt.app.domain.model.Task
import com.projektt.app.domain.model.TaskList
import com.projektt.app.ui.animation.LevelUpOverlay
import com.projektt.app.ui.components.AddTaskDialog
import com.projektt.app.ui.components.EditTaskDialog
import com.projektt.app.ui.components.EncryptText
import com.projektt.app.ui.components.TaskItem
import com.projektt.app.ui.components.XPProgressBar
import com.projektt.app.ui.theme.ProjektTTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    openAddDialogOnStart: Boolean = false,
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val editingTask by viewModel.editingTask.collectAsState()
    var showAddDialog by remember { mutableStateOf(openAddDialogOnStart) }
    var isOverdueExpanded by remember { mutableStateOf(true) }
    var showLevelDialog by remember { mutableStateOf(false) }
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                viewModel.handleSignInResult(account)
            } catch (e: com.google.android.gms.common.api.ApiException) {
                viewModel.handleSignInError("OK-Pfad Fehler Code ${e.statusCode}")
                viewModel.trySilentSignIn()
            } catch (e: Exception) {
                viewModel.handleSignInError("OK-Pfad: ${e.javaClass.simpleName}: ${e.message?.take(60)}")
            }
        } else {
            viewModel.trySilentSignIn()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!uiState.isSignedIn) {
            SignInScreen(
                onSignIn = { signInLauncher.launch(viewModel.getSignInIntent()) },
                errorMessage = uiState.error
            )
        } else {
            Scaffold(
                containerColor = ProjektTTheme.colors.background,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddDialog = true },
                        containerColor = ProjektTTheme.colors.primary,
                        contentColor = ProjektTTheme.colors.background
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Neue Aufgabe")
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Settings Icon (top right)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .padding(top = 8.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Einstellungen",
                                tint = ProjektTTheme.colors.onBackgroundDim
                            )
                        }
                    }

                    // Sticky Level Header
                    LevelHeader(
                        level = uiState.progress.level,
                        currentXp = uiState.progress.currentXp,
                        maxXp = uiState.progress.xpNeededForNextLevel,
                        nextReward = viewModel.calculateNextXpReward(),
                        showXpGain = uiState.showCreateXp || uiState.showWidgetXp,
                        xpGainAmount = if (uiState.showWidgetXp) uiState.lastXpEarned else 5,
                        onLevelTap = {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime < 500) {
                                tapCount++
                                if (tapCount >= 5) {
                                    showLevelDialog = true
                                    tapCount = 0
                                }
                            } else {
                                tapCount = 1
                            }
                            lastTapTime = now
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ProjektTTheme.colors.background)
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    )

                    // Scrollable Task List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Overdue Section (collapsible)
                        if (uiState.overdueTasks.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "UEBERFAELLIG",
                                    count = uiState.overdueTasks.size,
                                    isWarning = true,
                                    isExpanded = isOverdueExpanded,
                                    onClick = { isOverdueExpanded = !isOverdueExpanded }
                                )
                            }
                            items(uiState.overdueTasks, key = { it.id }) { task ->
                                AnimatedVisibility(
                                    visible = isOverdueExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    TaskItem(
                                        task = task,
                                        xpReward = uiState.lastXpEarned,
                                        onComplete = { viewModel.completeTask(it) },
                                        onEdit = { viewModel.startEditTask(it) },
                                        showXpBadge = uiState.justCompletedTaskId == task.id
                                    )
                                }
                            }
                            if (isOverdueExpanded) {
                                item { Spacer(modifier = Modifier.height(16.dp)) }
                            }
                        }

                        // Task Lists (einklappbar pro Liste)
                        uiState.taskLists.forEach { taskList ->
                            val isListExpanded = taskList.id in uiState.expandedListIds

                            item(key = "header_${taskList.id}") {
                                SectionHeader(
                                    title = taskList.title.uppercase(),
                                    count = taskList.tasks.size,
                                    isExpanded = isListExpanded,
                                    onClick = { viewModel.toggleListExpanded(taskList.id) }
                                )
                            }

                            if (isListExpanded) {
                                if (taskList.tasks.isEmpty()) {
                                    item(key = "empty_${taskList.id}") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Keine Aufgaben",
                                                style = ProjektTTheme.typography.labelSmall,
                                                color = ProjektTTheme.colors.onBackgroundDim
                                            )
                                        }
                                    }
                                }

                                items(taskList.tasks, key = { it.id }) { task ->
                                    TaskItem(
                                        task = task,
                                        xpReward = uiState.lastXpEarned,
                                        onComplete = { viewModel.completeTask(it) },
                                        onEdit = { viewModel.startEditTask(it) },
                                        showXpBadge = uiState.justCompletedTaskId == task.id
                                    )
                                }

                                item(key = "spacer_${taskList.id}") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }

                        // Fallback wenn keine Listen vorhanden
                        if (uiState.taskLists.isEmpty() && uiState.overdueTasks.isEmpty() && !uiState.isLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EncryptText(
                                        text = "Keine Aufgaben",
                                        style = ProjektTTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Completed Tasks Section (eingeklappt)
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                        item {
                            SectionHeader(
                                title = "ERLEDIGT",
                                isExpanded = uiState.isCompletedExpanded,
                                onClick = { viewModel.toggleCompletedSection() }
                            )
                        }

                        if (uiState.isCompletedExpanded) {
                            if (uiState.isLoadingCompleted) {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = ProjektTTheme.colors.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            } else {
                                items(uiState.completedTasks, key = { "completed_${it.id}" }) { task ->
                                    CompletedTaskItem(
                                        task = task,
                                        onRepeat = { viewModel.uncompleteTask(it) }
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        // Level Up Overlay
        AnimatedVisibility(
            visible = uiState.showLevelUp,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LevelUpOverlay(
                level = uiState.newLevel,
                onDismiss = { viewModel.dismissLevelUp() }
            )
        }

        // Add Task Dialog
        if (showAddDialog) {
            AddTaskDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, notes, dueDate, recurrence ->
                    viewModel.createTask(title, notes, dueDate, recurrence)
                    showAddDialog = false
                }
            )
        }

        // Edit Task Dialog
        editingTask?.let { task ->
            EditTaskDialog(
                task = task,
                onDismiss = { viewModel.cancelEdit() },
                onSave = { title, notes, dueDate, recurrence ->
                    viewModel.updateTask(task, title, notes, dueDate, recurrence)
                },
                onDelete = { viewModel.deleteTask(task) }
            )
        }

        // Level Debug Dialog
        if (showLevelDialog) {
            LevelDebugDialog(
                currentLevel = uiState.progress.level,
                onDismiss = { showLevelDialog = false },
                onSetLevel = { newLevel ->
                    viewModel.setLevel(newLevel)
                    showLevelDialog = false
                }
            )
        }
    }

    // Load tasks on first composition
    LaunchedEffect(uiState.isSignedIn) {
        if (uiState.isSignedIn) {
            viewModel.loadTasks()
        }
    }
}

@Composable
private fun LevelHeader(
    level: Int,
    currentXp: Int,
    maxXp: Int,
    nextReward: Int,
    showXpGain: Boolean,
    xpGainAmount: Int,
    onLevelTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EncryptText(
            text = "LEVEL $level",
            style = ProjektTTheme.typography.displayLarge,
            modifier = Modifier.clickable { onLevelTap() }
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            XPProgressBar(
                currentXp = currentXp,
                maxXp = maxXp,
                modifier = Modifier.fillMaxWidth()
            )
            // XP Animation (Task-Erstellung oder Widget-Completion)
            androidx.compose.animation.AnimatedVisibility(
                visible = showXpGain,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it },
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
            ) {
                Text(
                    text = "+$xpGainAmount XP",
                    style = ProjektTTheme.typography.labelSmall,
                    color = ProjektTTheme.colors.success
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Naechste Aufgabe: +$nextReward XP",
            style = ProjektTTheme.typography.labelSmall,
            color = ProjektTTheme.colors.success
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int? = null,
    isWarning: Boolean = false,
    isExpanded: Boolean? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onClick != null && isExpanded != null) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = if (isExpanded) "Einklappen" else "Ausklappen",
                tint = if (isWarning) ProjektTTheme.colors.primary else ProjektTTheme.colors.onBackgroundDim,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            text = "> $title",
            style = ProjektTTheme.typography.titleMedium,
            color = if (isWarning) ProjektTTheme.colors.primary else ProjektTTheme.colors.onBackgroundDim
        )
        if (count != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isWarning) ProjektTTheme.colors.primary.copy(alpha = 0.2f)
                        else ProjektTTheme.colors.surface
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    style = ProjektTTheme.typography.labelSmall,
                    color = if (isWarning) ProjektTTheme.colors.primary else ProjektTTheme.colors.onBackgroundDim
                )
            }
        }
    }
}

@Composable
private fun SignInScreen(onSignIn: () -> Unit, errorMessage: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EncryptText(
            text = "PROJEKT_T",
            style = ProjektTTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Task Gamification System",
            style = ProjektTTheme.typography.bodyMedium,
            color = ProjektTTheme.colors.onBackgroundDim
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onSignIn,
            colors = ButtonDefaults.buttonColors(
                containerColor = ProjektTTheme.colors.surface,
                contentColor = ProjektTTheme.colors.onBackground
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = "> Mit Google anmelden",
                style = ProjektTTheme.typography.titleMedium
            )
        }
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                style = ProjektTTheme.typography.labelSmall,
                color = ProjektTTheme.colors.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}




@Composable
private fun CompletedTaskItem(
    task: com.projektt.app.domain.model.Task,
    onRepeat: (com.projektt.app.domain.model.Task) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ProjektTTheme.colors.surface.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = task.title,
                style = ProjektTTheme.typography.bodyMedium,
                color = ProjektTTheme.colors.onBackgroundDim,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onRepeat(task) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Wiederholen",
                    tint = ProjektTTheme.colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LevelDebugDialog(
    currentLevel: Int,
    onDismiss: () -> Unit,
    onSetLevel: (Int) -> Unit
) {
    var levelInput by remember { mutableStateOf(currentLevel.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ProjektTTheme.colors.surface,
        title = {
            Text(
                text = "Debug: Level setzen",
                style = ProjektTTheme.typography.titleLarge,
                color = ProjektTTheme.colors.primary
            )
        },
        text = {
            OutlinedTextField(
                value = levelInput,
                onValueChange = { levelInput = it.filter { c -> c.isDigit() } },
                label = { Text("Neues Level") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ProjektTTheme.colors.onBackground,
                    unfocusedTextColor = ProjektTTheme.colors.onBackground,
                    focusedBorderColor = ProjektTTheme.colors.primary,
                    unfocusedBorderColor = ProjektTTheme.colors.onBackgroundDim
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    levelInput.toIntOrNull()?.let { onSetLevel(it) }
                }
            ) {
                Text("Setzen", color = ProjektTTheme.colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = ProjektTTheme.colors.onBackgroundDim)
            }
        }
    )
}
