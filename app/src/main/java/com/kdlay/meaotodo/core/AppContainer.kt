package com.kdlay.meaotodo.core

import android.content.Context
import androidx.room.Room
import com.kdlay.meaotodo.data.local.MeaoDatabase
import com.kdlay.meaotodo.data.repository.LedgerRepository
import com.kdlay.meaotodo.data.repository.PomodoroRepository
import com.kdlay.meaotodo.data.repository.TaskListRepository
import com.kdlay.meaotodo.data.repository.TaskRepository
import com.kdlay.meaotodo.sync.AndroidNsdWifiDiscoveryService
import com.kdlay.meaotodo.sync.WifiSyncGateway

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: MeaoDatabase = Room.databaseBuilder(
        appContext,
        MeaoDatabase::class.java,
        "meao_todo.db"
    ).fallbackToDestructiveMigration().build()

    val taskRepository = TaskRepository(database.taskDao(), database.syncOutboxDao())
    val taskListRepository = TaskListRepository(database.taskListDao(), database.syncOutboxDao())
    val pomodoroRepository = PomodoroRepository(database.pomodoroDao(), database.pomodoroRunDao(), database.syncOutboxDao())
    val ledgerRepository = LedgerRepository(database.ledgerDao(), database.syncOutboxDao())

    val wifiDiscoveryService = AndroidNsdWifiDiscoveryService(appContext)
    val wifiSyncGateway = WifiSyncGateway(database.syncOutboxDao())
}
