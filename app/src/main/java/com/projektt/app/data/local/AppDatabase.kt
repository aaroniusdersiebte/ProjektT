package com.projektt.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserProgressEntity::class, WidgetListSettingEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProgressDao(): UserProgressDao
    abstract fun widgetSettingsDao(): WidgetSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "projektt_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
