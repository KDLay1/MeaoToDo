package com.kdlay.meaotodo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kdlay.meaotodo.data.local.entity.PomodoroRunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PomodoroRunDao {
    @Query("SELECT * FROM pomodoro_runs WHERE status IN ('running', 'paused') AND deletedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveRun(): Flow<PomodoroRunEntity?>

    @Query("SELECT * FROM pomodoro_runs WHERE status IN ('running', 'paused') AND deletedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun findActiveRun(): PomodoroRunEntity?

    @Query("SELECT * FROM pomodoro_runs WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): PomodoroRunEntity?

    @Upsert
    suspend fun upsert(run: PomodoroRunEntity)
}
