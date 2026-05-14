package com.projektt.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE id = 1")
    fun getProgress(): Flow<UserProgressEntity?>

    @Query("SELECT * FROM user_progress WHERE id = 1")
    suspend fun getProgressOnce(): UserProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: UserProgressEntity)

    @Query("UPDATE user_progress SET tasksCompletedToday = 0 WHERE id = 1")
    suspend fun resetDailyStreak()
}
