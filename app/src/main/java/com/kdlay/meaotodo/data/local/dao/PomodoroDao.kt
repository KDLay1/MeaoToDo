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

    @Query("SELECT * FROM pomodoro_sessions WHERE deletedAt IS NULL ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int): Flow<List<PomodoroSessionEntity>>

    @Query("SELECT * FROM pomodoro_sessions WHERE status IN ('running', 'paused') AND deletedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<PomodoroSessionEntity?>

    @Query("SELECT * FROM pomodoro_sessions WHERE status IN ('running', 'paused') AND deletedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun findActiveSession(): PomodoroSessionEntity?

    @Query("SELECT * FROM pomodoro_sessions WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PomodoroSessionEntity?

    @Upsert
    suspend fun upsert(session: PomodoroSessionEntity)
}
