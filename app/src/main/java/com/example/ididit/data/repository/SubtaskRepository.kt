package com.example.ididit.data.repository

import com.example.ididit.data.local.SubtaskDao
import com.example.ididit.data.local.SubtaskEntity
import kotlinx.coroutines.flow.Flow

class SubtaskRepository(private val subtaskDao: SubtaskDao) {
    fun getSubtasksByTodo(todoId: Long): Flow<List<SubtaskEntity>> =
        subtaskDao.getSubtasksByTodo(todoId)

    suspend fun getSubtaskById(id: Long): SubtaskEntity? = subtaskDao.getSubtaskById(id)

    suspend fun insert(subtask: SubtaskEntity): Long = subtaskDao.insert(subtask)

    suspend fun update(subtask: SubtaskEntity) = subtaskDao.update(subtask)

    suspend fun delete(subtask: SubtaskEntity) = subtaskDao.delete(subtask)

    suspend fun toggleCompletion(id: Long, isCompleted: Boolean) {
        subtaskDao.updateCompletionStatus(id, isCompleted)
    }
}
