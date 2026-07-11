package com.kdlay.meaotodo.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.kdlay.meaotodo.data.local.MeaoDatabase
import com.kdlay.meaotodo.data.local.entity.LedgerEntryEntity
import com.kdlay.meaotodo.data.local.entity.PomodoroRunEntity
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import com.kdlay.meaotodo.data.local.entity.TaskListEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class MeaoBackupEnvelope(
    val format: String = FORMAT,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long,
    val taskLists: List<TaskListEntity>,
    val tasks: List<TaskEntity>,
    val pomodoroRuns: List<PomodoroRunEntity>,
    val pomodoroSessions: List<PomodoroSessionEntity>,
    val ledgerEntries: List<LedgerEntryEntity>
) {
    companion object {
        const val FORMAT = "MeaoToDoBackup"
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

data class BackupResult(
    val taskLists: Int,
    val tasks: Int,
    val pomodoroRuns: Int,
    val pomodoroSessions: Int,
    val ledgerEntries: Int
) {
    val totalRecords: Int
        get() = taskLists + tasks + pomodoroRuns + pomodoroSessions + ledgerEntries
}

class DataBackupService(
    context: Context,
    private val database: MeaoDatabase,
    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }
) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun exportTo(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val envelope = database.withTransaction {
            MeaoBackupEnvelope(
                exportedAt = System.currentTimeMillis(),
                taskLists = database.taskListDao().allForBackup(),
                tasks = database.taskDao().allForBackup(),
                pomodoroRuns = database.pomodoroRunDao().allForBackup(),
                pomodoroSessions = database.pomodoroDao().allForBackup(),
                ledgerEntries = database.ledgerDao().allForBackup()
            )
        }
        resolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
            it.write(json.encodeToString(envelope))
        } ?: error("无法打开导出文件")
        envelope.toResult()
    }

    suspend fun importFrom(uri: Uri): BackupResult = withContext(Dispatchers.IO) {
        val text = resolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("无法读取备份文件")
        require(text.length <= MAX_BACKUP_CHARS) { "备份文件过大" }
        val envelope = runCatching { json.decodeFromString<MeaoBackupEnvelope>(text) }
            .getOrElse { throw IllegalArgumentException("不是有效的 MeaoToDo 备份文件") }
        require(envelope.format == MeaoBackupEnvelope.FORMAT) { "备份格式不匹配" }
        require(envelope.schemaVersion == MeaoBackupEnvelope.CURRENT_SCHEMA_VERSION) { "暂不支持此备份版本" }
        validate(envelope)
        database.withTransaction {
            database.taskListDao().upsertAll(envelope.taskLists)
            database.taskDao().upsertAll(envelope.tasks)
            database.pomodoroRunDao().upsertAll(envelope.pomodoroRuns)
            database.pomodoroDao().upsertAll(envelope.pomodoroSessions)
            database.ledgerDao().upsertAll(envelope.ledgerEntries)
        }
        envelope.toResult()
    }

    private fun validate(envelope: MeaoBackupEnvelope) {
        val allIds = buildList {
            addAll(envelope.taskLists.map { it.id })
            addAll(envelope.tasks.map { it.id })
            addAll(envelope.pomodoroRuns.map { it.id })
            addAll(envelope.pomodoroSessions.map { it.id })
            addAll(envelope.ledgerEntries.map { it.id })
        }
        require(allIds.none { it.isBlank() }) { "备份中存在空 ID" }
        require(envelope.tasks.size <= MAX_RECORDS_PER_TYPE) { "任务记录过多" }
        require(envelope.pomodoroSessions.size <= MAX_RECORDS_PER_TYPE) { "专注记录过多" }
        require(envelope.ledgerEntries.size <= MAX_RECORDS_PER_TYPE) { "账本记录过多" }
    }

    private fun MeaoBackupEnvelope.toResult() = BackupResult(
        taskLists = taskLists.size,
        tasks = tasks.size,
        pomodoroRuns = pomodoroRuns.size,
        pomodoroSessions = pomodoroSessions.size,
        ledgerEntries = ledgerEntries.size
    )

    private companion object {
        const val MAX_BACKUP_CHARS = 20_000_000
        const val MAX_RECORDS_PER_TYPE = 100_000
    }
}
