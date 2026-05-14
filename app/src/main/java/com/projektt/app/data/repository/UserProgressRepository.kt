package com.projektt.app.data.repository

import com.projektt.app.data.local.UserProgressDao
import com.projektt.app.data.local.UserProgressEntity
import com.projektt.app.domain.model.UserProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProgressRepository @Inject constructor(
    private val dao: UserProgressDao
) {
    fun getProgress(): Flow<UserProgress> = dao.getProgress().map { entity ->
        entity?.toDomain() ?: UserProgress()
    }

    suspend fun getProgressOnce(): UserProgress {
        return dao.getProgressOnce()?.toDomain() ?: UserProgress()
    }

    suspend fun updateProgress(progress: UserProgress) {
        dao.upsert(UserProgressEntity.fromDomain(progress))
    }

    suspend fun checkAndResetDailyStreak() {
        val current = dao.getProgressOnce()
        val today = LocalDate.now().toString()

        if (current?.lastCompletionDate != today) {
            dao.upsert(
                (current ?: UserProgressEntity()).copy(
                    tasksCompletedToday = 0,
                    lastCompletionDate = today
                )
            )
        }
    }

    // Debug: Level direkt setzen
    suspend fun setLevel(newLevel: Int) {
        val current = dao.getProgressOnce() ?: UserProgressEntity()
        dao.upsert(
            current.copy(
                level = newLevel.coerceAtLeast(1),
                currentXp = 0,
                totalXp = (newLevel - 1) * 100 // Approximation
            )
        )
    }
}
