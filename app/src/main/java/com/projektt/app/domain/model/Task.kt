package com.projektt.app.domain.model

import java.time.LocalDateTime

enum class RecurrenceType {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY
}

data class Task(
    val id: String,
    val title: String,
    val notes: String? = null,
    val dueDate: LocalDateTime? = null,
    val isCompleted: Boolean = false,
    val taskListId: String,
    val position: String? = null,
    val recurrence: RecurrenceType = RecurrenceType.NONE
) {
    val isOverdue: Boolean
        get() = dueDate?.let { it < LocalDateTime.now() && !isCompleted } ?: false

    fun getNextDueDate(): LocalDateTime? {
        val currentDue = dueDate ?: return null
        return when (recurrence) {
            RecurrenceType.NONE -> null
            RecurrenceType.DAILY -> currentDue.plusDays(1)
            RecurrenceType.WEEKLY -> currentDue.plusWeeks(1)
            RecurrenceType.MONTHLY -> currentDue.plusMonths(1)
        }
    }
}
