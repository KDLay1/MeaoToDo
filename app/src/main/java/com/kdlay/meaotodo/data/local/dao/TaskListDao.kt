package com.kdlay.meaotodo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.kdlay.meaotodo.data.local.entity.TaskListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {
    @Query("SELECT * FROM task_lists WHERE deletedAt IS NULL ORDER BY sortOrder ASC, createdAt ASC")
    fun observeActiveLists(): Flow<List<TaskListEntity>>

    @Query("SELECT * FROM task_lists WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): TaskListEntity?

    @Upsert
    suspend fun upsert(taskList: TaskListEntity)

    @Query("UPDATE task_lists SET name = :name, updatedAt = :updatedAt WHERE id = :id AND deletedAt IS NULL")
    suspend fun rename(id: String, name: String, updatedAt: Long)

    @Query("UPDATE task_lists SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id AND deletedAt IS NULL")
    suspend fun setSortOrder(id: String, sortOrder: Int, updatedAt: Long)

    @Query("UPDATE task_lists SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id AND deletedAt IS NULL")
    suspend fun softDelete(id: String, deletedAt: Long)
}
