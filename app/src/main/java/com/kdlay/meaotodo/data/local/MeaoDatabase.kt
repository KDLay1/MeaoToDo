package com.kdlay.meaotodo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kdlay.meaotodo.data.local.dao.LedgerDao
import com.kdlay.meaotodo.data.local.dao.PomodoroDao
import com.kdlay.meaotodo.data.local.dao.PomodoroRunDao
import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao
import com.kdlay.meaotodo.data.local.dao.TaskDao
import com.kdlay.meaotodo.data.local.dao.TaskListDao
import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import com.kdlay.meaotodo.data.local.entity.PomodoroRunEntity
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.local.entity.TaskListEntity

@Database(
    entities = [
        TaskEntity::class,
        TaskListEntity::class,
        PomodoroSessionEntity::class,
        PomodoroRunEntity::class,
        LedgerEntryEntity::class,
        SyncOutboxEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class MeaoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskListDao(): TaskListDao
    abstract fun pomodoroDao(): PomodoroDao
    abstract fun pomodoroRunDao(): PomodoroRunDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun syncOutboxDao(): SyncOutboxDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN listId TEXT NOT NULL DEFAULT 'inbox'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_lists (
                        id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        deletedAt INTEGER,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO task_lists (id, name, sortOrder, createdAt, updatedAt, deletedAt)
                    VALUES ('inbox', '收集箱', -2147483648, 0, 0, NULL)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN hasDueTime INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pomodoro_sessions ADD COLUMN titleSnapshot TEXT")
                db.execSQL("ALTER TABLE pomodoro_sessions ADD COLUMN pausedAt INTEGER")
                db.execSQL("ALTER TABLE pomodoro_sessions ADD COLUMN accumulatedPausedSeconds INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pomodoro_sessions ADD COLUMN runId TEXT")
                db.execSQL("ALTER TABLE pomodoro_sessions ADD COLUMN roundIndex INTEGER NOT NULL DEFAULT 1")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS pomodoro_runs (
                        id TEXT NOT NULL,
                        taskId TEXT,
                        titleSnapshot TEXT,
                        focusDurationSeconds INTEGER NOT NULL,
                        breakDurationSeconds INTEGER NOT NULL,
                        targetFocusCount INTEGER NOT NULL,
                        completedFocusCount INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        startedAt INTEGER NOT NULL,
                        endedAt INTEGER,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        deletedAt INTEGER,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
            }
        }

        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5
        )
    }
}
