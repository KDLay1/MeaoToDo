package com.kdlay.meaotodo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kdlay.meaotodo.data.local.entity.PomodoroSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroDao {
    @Query("SELECT * FROM pomodoro_sessions WHERE deletedAt IS NULL ORDER BY startedAt DESC")
    fun observeSessions(): Flow<List<PomodoroSessionEntity>>

    @Query("SELECT * FROM pomodoro_sessions WHERE status = 'running' AND deletedAt IS NULL LIMIT 1")
    fun observeRunningSession(): Flow<PomodoroSessionEntity?>

    @Upsert
    suspend fun upsert(session: PomodoroSessionEntity)

    @Query("UPDATE pomodoro_sessions SET status = :status, endedAt = :endedAt, actualDurationSeconds = :actualDurationSeconds, updatedAt = :updatedAt WHERE id = :id")
    suspend fun finishSession(
        id: String,
        status: String,
        endedAt: Long,
        actualDurationSeconds: Int,
        updatedAt: Long
    )
}
