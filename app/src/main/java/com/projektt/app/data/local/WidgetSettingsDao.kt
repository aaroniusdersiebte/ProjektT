package com.projektt.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetSettingsDao {
    @Query("SELECT * FROM widget_list_settings ORDER BY sortOrder ASC")
    fun getAllSettings(): Flow<List<WidgetListSettingEntity>>

    @Query("SELECT * FROM widget_list_settings WHERE isEnabled = 1 ORDER BY sortOrder ASC")
    suspend fun getEnabledListIds(): List<WidgetListSettingEntity>

    @Query("SELECT listId FROM widget_list_settings WHERE isEnabled = 1 ORDER BY sortOrder ASC")
    suspend fun getEnabledListIdsOnly(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: WidgetListSettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(settings: List<WidgetListSettingEntity>)

    @Query("UPDATE widget_list_settings SET isEnabled = :enabled WHERE listId = :listId")
    suspend fun setEnabled(listId: String, enabled: Boolean)

    @Query("UPDATE widget_list_settings SET sortOrder = :order WHERE listId = :listId")
    suspend fun setSortOrder(listId: String, order: Int)

    @Query("DELETE FROM widget_list_settings WHERE listId NOT IN (:validIds)")
    suspend fun deleteStaleEntries(validIds: List<String>)
}
