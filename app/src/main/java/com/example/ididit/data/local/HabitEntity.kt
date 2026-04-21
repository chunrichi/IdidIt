package com.example.ididit.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Frequency {
    DAILY,       // 每日
    WEEKLY_N,    // 每周N次
    MONTHLY_N    // 每月N次
}

@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("topicId")]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val topicId: Long,
    val frequency: Frequency = Frequency.DAILY,
    val weeklyTarget: Int = 1,  // 每周目标次数（针对 WEEKLY_N）
    val monthlyTarget: Int = 1, // 每月目标次数（针对 MONTHLY_N）
    val createdAt: Long = System.currentTimeMillis()
)
