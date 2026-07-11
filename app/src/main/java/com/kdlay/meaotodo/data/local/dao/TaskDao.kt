package com.kdlay.meaotodo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kdlay.meaotodo.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY isDone ASC, priority DESC, dueAt ASC, updatedAt DESC")
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE listId = :listId AND deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun findActiveByListId(listId: String): List<TaskEntity>

    @Query("SELECT * FROM tasks")
    suspend fun allForBackup(): List<TaskEntity>

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET isDone = :isDone, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setDone(id: String, isDone: Boolean, updatedAt: Long)

    @Query("UPDATE tasks SET listId = :targetListId, updatedAt = :updatedAt WHERE id = :id AND deletedAt IS NULL")
    suspend fun moveToList(id: String, targetListId: String, updatedAt: Long): Int

    @Query("UPDATE tasks SET listId = :targetListId, updatedAt = :updatedAt WHERE listId = :sourceListId AND deletedAt IS NULL")
    suspend fun moveTasksFromList(sourceListId: String, targetListId: String, updatedAt: Long): Int

    @Query("UPDATE tasks SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("UPDATE tasks SET actualPomodoros = actualPomodoros + :count, updatedAt = :updatedAt WHERE id = :id AND deletedAt IS NULL")
    suspend fun incrementActualPomodoros(id: String, count: Int, updatedAt: Long)
}
