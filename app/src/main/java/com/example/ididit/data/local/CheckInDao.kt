package com.example.ididit.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_ins WHERE habitId = :habitId ORDER BY date DESC")
    fun getCheckInsByHabit(habitId: Long): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getCheckInByHabitAndDate(habitId: Long, date: LocalDate): CheckInEntity?

    @Query("SELECT * FROM check_ins WHERE date = :date")
    fun getCheckInsByDate(date: LocalDate): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE date BETWEEN :startDate AND :endDate")
    fun getCheckInsBetweenDates(startDate: LocalDate, endDate: LocalDate): Flow<List<CheckInEntity>>

    @Query("SELECT COUNT(*) FROM check_ins WHERE habitId = :habitId AND completed = 1")
    suspend fun getTotalCheckInCount(habitId: Long): Int

    @Query("SELECT COUNT(*) FROM check_ins WHERE habitId = :habitId AND date BETWEEN :startDate AND :endDate AND completed = 1")
    suspend fun getCheckInCountBetweenDates(habitId: Long, startDate: LocalDate, endDate: LocalDate): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checkIn: CheckInEntity): Long

    @Delete
    suspend fun delete(checkIn: CheckInEntity)

    @Query("DELETE FROM check_ins WHERE habitId = :habitId AND date = :date")
    suspend fun deleteByHabitAndDate(habitId: Long, date: LocalDate)
}
