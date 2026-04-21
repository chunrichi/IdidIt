package com.example.ididit.data.repository

import com.example.ididit.data.local.CheckInDao
import com.example.ididit.data.local.CheckInEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class CheckInRepository(private val checkInDao: CheckInDao) {
    fun getCheckInsByHabit(habitId: Long): Flow<List<CheckInEntity>> =
        checkInDao.getCheckInsByHabit(habitId)

    suspend fun getCheckInByHabitAndDate(habitId: Long, date: LocalDate): CheckInEntity? =
        checkInDao.getCheckInByHabitAndDate(habitId, date)

    fun getCheckInsByDate(date: LocalDate): Flow<List<CheckInEntity>> =
        checkInDao.getCheckInsByDate(date)

    fun getCheckInsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<CheckInEntity>> =
        checkInDao.getCheckInsBetweenDates(startDate, endDate)

    suspend fun getTotalCheckInCount(habitId: Long): Int =
        checkInDao.getTotalCheckInCount(habitId)

    suspend fun getCheckInCountBetweenDates(habitId: Long, startDate: LocalDate, endDate: LocalDate): Int =
        checkInDao.getCheckInCountBetweenDates(habitId, startDate, endDate)

    suspend fun insert(checkIn: CheckInEntity): Long = checkInDao.insert(checkIn)

    suspend fun delete(checkIn: CheckInEntity) = checkInDao.delete(checkIn)

    suspend fun toggleCheckIn(habitId: Long, date: LocalDate) {
        val existing = checkInDao.getCheckInByHabitAndDate(habitId, date)
        if (existing != null) {
            checkInDao.delete(existing)
        } else {
            checkInDao.insert(CheckInEntity(habitId = habitId, date = date, completed = true))
        }
    }
}
