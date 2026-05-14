package com.projektt.app.widget

import com.projektt.app.data.remote.GoogleTasksService
import com.projektt.app.data.repository.UserProgressRepository
import com.projektt.app.data.repository.WidgetSettingsRepository
import com.projektt.app.data.TaskSyncPrefs
import com.projektt.app.domain.usecase.CompleteTaskUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun completeTaskUseCase(): CompleteTaskUseCase
    fun googleTasksService(): GoogleTasksService
    fun userProgressRepository(): UserProgressRepository
    fun taskSyncPrefs(): TaskSyncPrefs
    fun widgetSettingsRepository(): WidgetSettingsRepository
}
