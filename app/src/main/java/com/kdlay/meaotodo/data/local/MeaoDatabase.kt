package com.kdlay.meaotodo.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kdlay.meaotodo.data.local.dao.LedgerDao
import com.kdlay.meaotodo.data.local.dao.PomodoroDao
import com.kdlay.meaotodo.data.local.dao.SyncOutboxDao
import com.kdlay.meaotodo.data.local.dao.TaskDao
import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.SyncOutboxEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        PomodoroSessionEntity::class,
        LedgerEntryEntity::class,
        SyncOutboxEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MeaoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun pomodoroDao(): PomodoroDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun syncOutboxDao(): SyncOutboxDao
}
