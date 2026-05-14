package com.projektt.app.domain.model

data class TaskList(
    val id: String,
    val title: String,
    val tasks: List<Task> = emptyList()
)
