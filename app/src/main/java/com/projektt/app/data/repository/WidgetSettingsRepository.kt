package com.projektt.app.data.repository

import com.projektt.app.data.local.WidgetListSettingEntity
import com.projektt.app.data.local.WidgetSettingsDao
import com.projektt.app.domain.model.TaskList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class WidgetListSetting(
    val listId: String,
    val listTitle: String,
    val isEnabled: Boolean,
    val sortOrder: Int
)

@Singleton
class WidgetSettingsRepository @Inject constructor(
    private val dao: WidgetSettingsDao
) {
    fun getAllSettings(): Flow<List<WidgetListSetting>> =
        dao.getAllSettings().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getEnabledListIds(): List<String> = dao.getEnabledListIdsOnly()

    suspend fun setEnabled(listId: String, enabled: Boolean) {
        dao.setEnabled(listId, enabled)
    }

    suspend fun updateOrder(listId: String, newOrder: Int) {
        dao.setSortOrder(listId, newOrder)
    }

    suspend fun syncWithTaskLists(taskLists: List<TaskList>) {
        val existingIds = dao.getAllSettings().first().map { it.listId }

        taskLists.forEachIndexed { index, list ->
            if (list.id !in existingIds) {
                dao.upsert(
                    WidgetListSettingEntity(
                        listId = list.id,
                        listTitle = list.title,
                        isEnabled = true,
                        sortOrder = index
                    )
                )
            } else {
                // Update title if changed
                dao.upsert(
                    WidgetListSettingEntity(
                        listId = list.id,
                        listTitle = list.title,
                        isEnabled = true,
                        sortOrder = index
                    )
                )
            }
        }

        // Remove entries for lists that no longer exist
        val validIds = taskLists.map { it.id }
        dao.deleteStaleEntries(validIds)
    }

    suspend fun reorderLists(orderedIds: List<String>) {
        orderedIds.forEachIndexed { index, id ->
            dao.setSortOrder(id, index)
        }
    }

    private fun WidgetListSettingEntity.toDomain() = WidgetListSetting(
        listId = listId,
        listTitle = listTitle,
        isEnabled = isEnabled,
        sortOrder = sortOrder
    )
}
