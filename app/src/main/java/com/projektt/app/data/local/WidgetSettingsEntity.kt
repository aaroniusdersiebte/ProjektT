package com.projektt.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widget_list_settings")
data class WidgetListSettingEntity(
    @PrimaryKey val listId: String,
    val listTitle: String,
    val isEnabled: Boolean = true,
    val sortOrder: Int = 0
)
