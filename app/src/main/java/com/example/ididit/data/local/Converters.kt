package com.example.ididit.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun fromFrequency(frequency: Frequency): String {
        return frequency.name
    }

    @TypeConverter
    fun toFrequency(value: String): Frequency {
        return Frequency.valueOf(value)
    }
}
