package com.projektt.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import com.projektt.app.domain.model.RecurrenceType
import com.projektt.app.domain.model.Task
import com.projektt.app.ui.theme.ProjektTTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, LocalDateTime?, RecurrenceType) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedRecurrence by remember { mutableStateOf(RecurrenceType.NONE) }
    var showDatePicker by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ProjektTTheme.colors.surface,
        title = {
            Text(
                text = "Neue Aufgabe",
                style = ProjektTTheme.typography.titleLarge,
                color = ProjektTTheme.colors.onBackground
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ProjektTTheme.colors.onBackground,
                        unfocusedTextColor = ProjektTTheme.colors.onBackground,
                        focusedBorderColor = ProjektTTheme.colors.primary,
                        unfocusedBorderColor = ProjektTTheme.colors.onBackgroundDim
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notizen (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ProjektTTheme.colors.onBackground,
                        unfocusedTextColor = ProjektTTheme.colors.onBackground,
                        focusedBorderColor = ProjektTTheme.colors.primary,
                        unfocusedBorderColor = ProjektTTheme.colors.onBackgroundDim
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Faellig:",
                    style = ProjektTTheme.typography.labelSmall,
                    color = ProjektTTheme.colors.onBackgroundDim
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Quick-Date Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickDateChip(
                        label = "Heute",
                        isSelected = selectedDate == today,
                        onClick = { selectedDate = if (selectedDate == today) null else today }
                    )
                    QuickDateChip(
                        label = "Morgen",
                        isSelected = selectedDate == tomorrow,
                        onClick = { selectedDate = if (selectedDate == tomorrow) null else tomorrow }
                    )
                    QuickDateChip(
                        label = "So.",
                        isSelected = selectedDate == endOfWeek,
                        onClick = { selectedDate = if (selectedDate == endOfWeek) null else endOfWeek }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Date Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showDatePicker = true }
                    ) {
                        Text(
                            text = if (selectedDate != null && selectedDate != today && selectedDate != tomorrow && selectedDate != endOfWeek) {
                                selectedDate!!.format(dateFormatter)
                            } else {
                                "Datum waehlen..."
                            },
                            color = ProjektTTheme.colors.primary
                        )
                    }
                    if (selectedDate != null) {
                        TextButton(
                            onClick = { selectedDate = null }
                        ) {
                            Text(
                                text = "X",
                                color = ProjektTTheme.colors.onBackgroundDim
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Wiederholung:",
                    style = ProjektTTheme.typography.labelSmall,
                    color = ProjektTTheme.colors.onBackgroundDim
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecurrenceChip(
                        label = "Taegl.",
                        isSelected = selectedRecurrence == RecurrenceType.DAILY,
                        onClick = {
                            selectedRecurrence = if (selectedRecurrence == RecurrenceType.DAILY)
                                RecurrenceType.NONE else RecurrenceType.DAILY
                        }
                    )
                    RecurrenceChip(
                        label = "Woech.",
                        isSelected = selectedRecurrence == RecurrenceType.WEEKLY,
                        onClick = {
                            selectedRecurrence = if (selectedRecurrence == RecurrenceType.WEEKLY)
                                RecurrenceType.NONE else RecurrenceType.WEEKLY
                        }
                    )
                    RecurrenceChip(
                        label = "Monatl.",
                        isSelected = selectedRecurrence == RecurrenceType.MONTHLY,
                        onClick = {
                            selectedRecurrence = if (selectedRecurrence == RecurrenceType.MONTHLY)
                                RecurrenceType.NONE else RecurrenceType.MONTHLY
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val dueDateTime = selectedDate?.atTime(LocalTime.of(23, 59))
                        onConfirm(title, notes.ifBlank { null }, dueDateTime, selectedRecurrence)
                    }
                }
            ) {
                Text(
                    text = "Erstellen",
                    color = ProjektTTheme.colors.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Abbrechen",
                    color = ProjektTTheme.colors.onBackgroundDim
                )
            }
        }
    )

    // DatePicker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = ProjektTTheme.colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen", color = ProjektTTheme.colors.onBackgroundDim)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = ProjektTTheme.colors.surface
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = ProjektTTheme.colors.surface,
                    titleContentColor = ProjektTTheme.colors.onBackground,
                    headlineContentColor = ProjektTTheme.colors.onBackground,
                    weekdayContentColor = ProjektTTheme.colors.onBackgroundDim,
                    dayContentColor = ProjektTTheme.colors.onBackground,
                    selectedDayContainerColor = ProjektTTheme.colors.primary,
                    selectedDayContentColor = ProjektTTheme.colors.background,
                    todayContentColor = ProjektTTheme.colors.primary,
                    todayDateBorderColor = ProjektTTheme.colors.primary
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onSave: (String, String?, LocalDateTime?, RecurrenceType) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var notes by remember { mutableStateOf(task.notes ?: "") }
    var selectedDate by remember { mutableStateOf(task.dueDate?.toLocalDate()) }
    var selectedRecurrence by remember { mutableStateOf(task.recurrence) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd.MM.yyyy") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ProjektTTheme.colors.surface,
        title = {
            Text(
                text = "Aufgabe bearbeiten",
                style = ProjektTTheme.typography.titleLarge,
                color = ProjektTTheme.colors.onBackground
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ProjektTTheme.colors.onBackground,
                        unfocusedTextColor = ProjektTTheme.colors.onBackground,
                        focusedBorderColor = ProjektTTheme.colors.primary,
                        unfocusedBorderColor = ProjektTTheme.colors.onBackgroundDim
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notizen (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = ProjektTTheme.colors.onBackground,
                        unfocusedTextColor = ProjektTTheme.colors.onBackground,
                        focusedBorderColor = ProjektTTheme.colors.primary,
                        unfocusedBorderColor = ProjektTTheme.colors.onBackgroundDim
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Faellig:",
                    style = ProjektTTheme.typography.labelSmall,
                    color = ProjektTTheme.colors.onBackgroundDim
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickDateChip(
                        label = "Heute",
                        isSelected = selectedDate == today,
                        onClick = { selectedDate = if (selectedDate == today) null else today }
                    )
                    QuickDateChip(
                        label = "Morgen",
                        isSelected = selectedDate == tomorrow,
                        onClick = { selectedDate = if (selectedDate == tomorrow) null else tomorrow }
                    )
                    QuickDateChip(
                        label = "So.",
                        isSelected = selectedDate == endOfWeek,
                        onClick = { selectedDate = if (selectedDate == endOfWeek) null else endOfWeek }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(
                            text = if (selectedDate != null && selectedDate != today && selectedDate != tomorrow && selectedDate != endOfWeek) {
                                selectedDate!!.format(dateFormatter)
                            } else {
                                "Datum waehlen..."
                            },
                            color = ProjektTTheme.colors.primary
                        )
                    }
                    if (selectedDate != null) {
                        TextButton(onClick = { selectedDate = null }) {
                            Text(text = "X", color = ProjektTTheme.colors.onBackgroundDim)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Wiederholung:",
                    style = ProjektTTheme.typography.labelSmall,
                    color = ProjektTTheme.colors.onBackgroundDim
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RecurrenceChip(
                        label = "Taegl.",
                        isSelected = selectedRecurrence == RecurrenceType.DAILY,
                        onClick = {
                            selectedRecurrence = if (selectedRecurrence == RecurrenceType.DAILY)
                                RecurrenceType.NONE else RecurrenceType.DAILY
                        }
                    )
                    RecurrenceChip(
                        label = "Woech.",
                        isSelected = selectedRecurrence == RecurrenceType.WEEKLY,
                        onClick = {
                            selectedRecurrence = if (selectedRecurrence == RecurrenceType.WEEKLY)
                                RecurrenceType.NONE else RecurrenceType.WEEKLY
                        }
                    )
                    RecurrenceChip(
                        label = "Monatl.",
                        isSelected = selectedRecurrence == RecurrenceType.MONTHLY,
                        onClick = {
                            selectedRecurrence = if (selectedRecurrence == RecurrenceType.MONTHLY)
                                RecurrenceType.NONE else RecurrenceType.MONTHLY
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Loeschen", color = ProjektTTheme.colors.primary)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        val dueDateTime = selectedDate?.atTime(LocalTime.of(23, 59))
                        onSave(title, notes.ifBlank { null }, dueDateTime, selectedRecurrence)
                    }
                }
            ) {
                Text(text = "Speichern", color = ProjektTTheme.colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Abbrechen", color = ProjektTTheme.colors.onBackgroundDim)
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = ProjektTTheme.colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Abbrechen", color = ProjektTTheme.colors.onBackgroundDim)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = ProjektTTheme.colors.surface)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = ProjektTTheme.colors.surface,
                    titleContentColor = ProjektTTheme.colors.onBackground,
                    headlineContentColor = ProjektTTheme.colors.onBackground,
                    weekdayContentColor = ProjektTTheme.colors.onBackgroundDim,
                    dayContentColor = ProjektTTheme.colors.onBackground,
                    selectedDayContainerColor = ProjektTTheme.colors.primary,
                    selectedDayContentColor = ProjektTTheme.colors.background,
                    todayContentColor = ProjektTTheme.colors.primary,
                    todayDateBorderColor = ProjektTTheme.colors.primary
                )
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = ProjektTTheme.colors.surface,
            title = { Text("Aufgabe loeschen?", color = ProjektTTheme.colors.onBackground) },
            text = { Text("Diese Aktion kann nicht rueckgaengig gemacht werden.", color = ProjektTTheme.colors.onBackgroundDim) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("Loeschen", color = ProjektTTheme.colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Abbrechen", color = ProjektTTheme.colors.onBackgroundDim)
                }
            }
        )
    }
}

@Composable
fun QuickDateChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) ProjektTTheme.colors.primary else ProjektTTheme.colors.surface,
        contentColor = if (isSelected) ProjektTTheme.colors.background else ProjektTTheme.colors.onBackgroundDim
    ) {
        Text(
            text = label,
            style = ProjektTTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun RecurrenceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) ProjektTTheme.colors.primary else ProjektTTheme.colors.surface,
        contentColor = if (isSelected) ProjektTTheme.colors.background else ProjektTTheme.colors.onBackgroundDim
    ) {
        Text(
            text = label,
            style = ProjektTTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
