package com.projektt.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.projektt.app.domain.model.UserProgress

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val currentXp: Int = 0,
    val totalXp: Int = 0,
    val tasksCompletedToday: Int = 0,
    val lastCompletionDate: String? = null
) {
    fun toDomain(): UserProgress = UserProgress(
        level = level,
        currentXp = currentXp,
        totalXp = totalXp,
        tasksCompletedToday = tasksCompletedToday,
        lastCompletionDate = lastCompletionDate
    )

    companion object {
        fun fromDomain(progress: UserProgress): UserProgressEntity = UserProgressEntity(
            level = progress.level,
            currentXp = progress.currentXp,
            totalXp = progress.totalXp,
            tasksCompletedToday = progress.tasksCompletedToday,
            lastCompletionDate = progress.lastCompletionDate
        )
    }
}
