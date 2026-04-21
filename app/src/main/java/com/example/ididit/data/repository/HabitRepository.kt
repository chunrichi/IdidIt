package com.example.ididit.data.repository

import com.example.ididit.data.local.HabitDao
import com.example.ididit.data.local.HabitEntity
import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {
    fun getAllHabits(): Flow<List<HabitEntity>> = habitDao.getAllHabits()

    fun getHabitsByTopic(topicId: Long): Flow<List<HabitEntity>> = habitDao.getHabitsByTopic(topicId)

    suspend fun getHabitById(id: Long): HabitEntity? = habitDao.getHabitById(id)

    suspend fun insert(habit: HabitEntity): Long = habitDao.insert(habit)

    suspend fun update(habit: HabitEntity) = habitDao.update(habit)

    suspend fun delete(habit: HabitEntity) = habitDao.delete(habit)
}
