package com.example.ididit.data.repository

import com.example.ididit.data.local.TodoDao
import com.example.ididit.data.local.TodoEntity
import kotlinx.coroutines.flow.Flow

class TodoRepository(private val todoDao: TodoDao) {
    fun getAllTodos(): Flow<List<TodoEntity>> = todoDao.getAllTodos()

    fun getTodosByTopic(topicId: Long): Flow<List<TodoEntity>> = todoDao.getTodosByTopic(topicId)

    suspend fun getTodoById(id: Long): TodoEntity? = todoDao.getTodoById(id)

    suspend fun insert(todo: TodoEntity): Long = todoDao.insert(todo)

    suspend fun update(todo: TodoEntity) = todoDao.update(todo)

    suspend fun delete(todo: TodoEntity) = todoDao.delete(todo)

    suspend fun toggleCompletion(id: Long, isCompleted: Boolean) {
        val completedAt = if (isCompleted) System.currentTimeMillis() else null
        todoDao.updateCompletionStatus(id, isCompleted, completedAt)
    }
}
