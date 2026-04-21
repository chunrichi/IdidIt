package com.example.ididit.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ididit.data.local.CheckInEntity
import com.example.ididit.data.local.Frequency
import com.example.ididit.data.local.HabitEntity
import com.example.ididit.data.local.TopicEntity
import com.example.ididit.data.repository.CheckInRepository
import com.example.ididit.data.repository.HabitRepository
import com.example.ididit.data.repository.TopicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class HabitWithStats(
    val habit: HabitEntity,
    val topic: TopicEntity?,
    val todayCompleted: Boolean,
    val currentStreak: Int,
    val totalDays: Int,
    val thisWeekProgress: Int,
    val thisWeekTarget: Int
)

data class DashboardUiState(
    val todayCompleted: Int = 0,
    val todayTarget: Int = 0,
    val weekCompleted: Int = 0,
    val weekTarget: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val heatmapData: Map<LocalDate, Int> = emptyMap(), // date -> count
    val topics: List<TopicEntity> = emptyList(),
    val habitsWithStats: List<HabitWithStats> = emptyList(),
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val habitRepository: HabitRepository,
    private val checkInRepository: CheckInRepository,
    private val topicRepository: TopicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                habitRepository.getAllHabits(),
                checkInRepository.getCheckInsBetweenDates(
                    LocalDate.now().minusMonths(6),
                    LocalDate.now()
                ),
                topicRepository.getAllTopics()
            ) { habits, checkIns, topics ->
                Triple(habits, checkIns, topics)
            }.collect { (habits, checkIns, topics) ->
                updateDashboardData(habits, checkIns, topics)
            }
        }
    }

    private fun updateDashboardData(
        habits: List<HabitEntity>,
        checkIns: List<CheckInEntity>,
        topics: List<TopicEntity>
    ) {
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val topicMap = topics.associateBy { it.id }

        // Calculate heatmap data (last 6 months)
        val heatmapData = mutableMapOf<LocalDate, Int>()
        for (checkIn in checkIns) {
            if (checkIn.completed) {
                val existing = heatmapData[checkIn.date] ?: 0
                heatmapData[checkIn.date] = existing + 1
            }
        }

        // Calculate today's data
        val todayCheckIns = checkIns.count { it.date == today && it.completed }
        val todayTarget = calculateTodayTarget(habits)

        // Calculate week data
        val weekStart = today.with(DayOfWeek.MONDAY)
        val weekEnd = today.with(DayOfWeek.SUNDAY)
        val weekCheckIns = checkIns.count {
            it.date >= weekStart && it.date <= weekEnd && it.completed
        }
        val weekTarget = calculateWeekTarget(habits)

        // Calculate habits with stats
        val habitsWithStats = habits.map { habit ->
            val habitCheckIns = checkIns.filter { it.habitId == habit.id }
            val topic = topicMap[habit.topicId]

            // Today's completed
            val todayCompleted = habitCheckIns.any { it.date == today && it.completed }

            // Current streak
            val currentStreak = calculateStreak(habit, habitCheckIns, today, zoneId)

            // Total days
            val totalDays = habitCheckIns.count { it.completed }

            // This week progress
            val thisWeekProgress = habitCheckIns.count {
                it.date >= weekStart && it.date <= weekEnd && it.completed
            }

            // This week target
            val thisWeekTarget = when (habit.frequency) {
                Frequency.DAILY -> 7
                Frequency.WEEKLY_N -> habit.weeklyTarget
                Frequency.MONTHLY_N -> 4 // Approximate
            }

            HabitWithStats(
                habit = habit,
                topic = topic,
                todayCompleted = todayCompleted,
                currentStreak = currentStreak,
                totalDays = totalDays,
                thisWeekProgress = thisWeekProgress,
                thisWeekTarget = thisWeekTarget
            )
        }

        // Calculate overall streak (longest consecutive days with any check-in)
        val longestStreak = calculateLongestStreak(habitCheckIns = checkIns, today = today, zoneId = zoneId)

        // Overall current streak (consecutive days with at least one check-in)
        val currentStreak = calculateOverallStreak(checkIns, today, zoneId)

        _uiState.value = DashboardUiState(
            todayCompleted = todayCheckIns,
            todayTarget = todayTarget,
            weekCompleted = weekCheckIns,
            weekTarget = weekTarget,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            heatmapData = heatmapData,
            topics = topics,
            habitsWithStats = habitsWithStats,
            isLoading = false
        )
    }

    private fun calculateTodayTarget(habits: List<HabitEntity>): Int {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek
        val dayOfMonth = today.dayOfMonth

        return habits.count { habit ->
            when (habit.frequency) {
                Frequency.DAILY -> true
                Frequency.WEEKLY_N -> {
                    // For weekly habits, check if today counts toward the week
                    val weekProgress = today.dayOfWeek.value
                    true // Simplified: count every day
                }
                Frequency.MONTHLY_N -> {
                    // For monthly habits, every day counts
                    true
                }
            }
        }
    }

    private fun calculateWeekTarget(habits: List<HabitEntity>): Int {
        return habits.sumOf { habit ->
            when (habit.frequency) {
                Frequency.DAILY -> 7
                Frequency.WEEKLY_N -> habit.weeklyTarget
                Frequency.MONTHLY_N -> 4 // Approximate 4 weeks per month
            }
        }
    }

    private fun calculateStreak(
        habit: HabitEntity,
        habitCheckIns: List<CheckInEntity>,
        today: LocalDate,
        zoneId: ZoneId
    ): Int {
        if (habit.frequency == Frequency.WEEKLY_N || habit.frequency == Frequency.MONTHLY_N) {
            // For non-daily habits, streak = consecutive weeks/months with targets met
            return calculatePeriodStreak(habit, habitCheckIns, today, zoneId)
        }

        // For daily habits
        var streak = 0
        var currentDate = today

        while (true) {
            val hasCheckIn = habitCheckIns.any { it.date == currentDate && it.completed }
            if (hasCheckIn) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else if (currentDate == today) {
                // Today not checked in yet, check yesterday
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    private fun calculatePeriodStreak(
        habit: HabitEntity,
        habitCheckIns: List<CheckInEntity>,
        today: LocalDate,
        zoneId: ZoneId
    ): Int {
        // Simplified: count consecutive periods (weeks for WEEKLY_N, months for MONTHLY_N)
        var streak = 0
        var currentPeriod = if (habit.frequency == Frequency.WEEKLY_N) {
            today.with(DayOfWeek.MONDAY)
        } else {
            YearMonth.from(today).atDay(1)
        }

        while (true) {
            val periodStart = currentPeriod
            val periodEnd = if (habit.frequency == Frequency.WEEKLY_N) {
                currentPeriod.plusDays(6)
            } else {
                YearMonth.from(currentPeriod).atEndOfMonth()
            }

            val periodCount = habitCheckIns.count {
                it.date >= periodStart && it.date <= periodEnd && it.completed
            }

            val meetsTarget = when (habit.frequency) {
                Frequency.WEEKLY_N -> periodCount >= habit.weeklyTarget
                Frequency.MONTHLY_N -> periodCount >= habit.monthlyTarget
                else -> periodCount > 0
            }

            if (meetsTarget) {
                streak++
                currentPeriod = if (habit.frequency == Frequency.WEEKLY_N) {
                    currentPeriod.minusWeeks(1)
                } else {
                    currentPeriod.minusMonths(1)
                }
            } else {
                break
            }
        }
        return streak
    }

    private fun calculateOverallStreak(
        checkIns: List<CheckInEntity>,
        today: LocalDate,
        zoneId: ZoneId
    ): Int {
        val datesWithCheckIns = checkIns.filter { it.completed }.map { it.date }.toSet()

        var streak = 0
        var currentDate = today

        // Check if today has any check-in
        if (!datesWithCheckIns.contains(today)) {
            currentDate = currentDate.minusDays(1)
        }

        while (datesWithCheckIns.contains(currentDate)) {
            streak++
            currentDate = currentDate.minusDays(1)
        }

        return streak
    }

    private fun calculateLongestStreak(
        habitCheckIns: List<CheckInEntity>,
        today: LocalDate,
        zoneId: ZoneId
    ): Int {
        val datesWithCheckIns = habitCheckIns.filter { it.completed }
            .map { it.date }
            .sorted()

        if (datesWithCheckIns.isEmpty()) return 0

        var longest = 1
        var current = 1

        for (i in 1 until datesWithCheckIns.size) {
            if (datesWithCheckIns[i] == datesWithCheckIns[i - 1].plusDays(1)) {
                current++
                longest = maxOf(longest, current)
            } else {
                current = 1
            }
        }

        return longest
    }

    class Factory(
        private val habitRepository: HabitRepository,
        private val checkInRepository: CheckInRepository,
        private val topicRepository: TopicRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(habitRepository, checkInRepository, topicRepository) as T
        }
    }
}
