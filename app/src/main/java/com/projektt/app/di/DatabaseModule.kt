package com.projektt.app.di

import android.content.Context
import androidx.room.Room
import com.projektt.app.data.local.AppDatabase
import com.projektt.app.data.local.UserProgressDao
import com.projektt.app.data.local.WidgetSettingsDao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.projektt.app.data.TaskSyncPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS widget_list_settings (
                    listId TEXT PRIMARY KEY NOT NULL,
                    listTitle TEXT NOT NULL,
                    isEnabled INTEGER NOT NULL DEFAULT 1,
                    sortOrder INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "projektt_db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideUserProgressDao(db: AppDatabase): UserProgressDao = db.userProgressDao()

    @Provides
    fun provideWidgetSettingsDao(db: AppDatabase): WidgetSettingsDao = db.widgetSettingsDao()

    @Provides
    @Singleton
    fun provideTaskSyncPrefs(@ApplicationContext context: Context): TaskSyncPrefs {
        return TaskSyncPrefs(context)
    }
}
