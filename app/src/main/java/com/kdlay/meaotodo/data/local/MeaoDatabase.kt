package com.kdlay.meaotodo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
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
}
