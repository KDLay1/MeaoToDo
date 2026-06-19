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

    @Upsert
    suspend fun upsert(taskList: TaskListEntity)
}
