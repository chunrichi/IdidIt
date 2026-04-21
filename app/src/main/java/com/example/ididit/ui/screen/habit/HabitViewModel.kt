package com.example.ididit.ui.screen.habit

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class HabitUiState(
    val topics: List<TopicEntity> = emptyList(),
    val habits: List<HabitDisplay> = emptyList(),
    val selectedHabit: HabitEntity? = null,
    val selectedHabitCheckIns: List<CheckInEntity> = emptyList(),
    val currentMonth: YearMonth = YearMonth.now(),
    val showAddHabitDialog: Boolean = false,
    val showAddTopicDialog: Boolean = false,
    val isLoading: Boolean = true
)

data class HabitDisplay(
    val habit: HabitEntity,
    val topic: TopicEntity?,
    val todayCompleted: Boolean,
    val thisWeekProgress: Int,
    val thisWeekTarget: Int,
    val thisMonthProgress: Int,
    val thisMonthTarget: Int,
    val currentStreak: Int
)

class HabitViewModel(
    private val habitRepository: HabitRepository,
    private val checkInRepository: CheckInRepository,
    private val topicRepository: TopicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitUiState())
    val uiState: StateFlow<HabitUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                habitRepository.getAllHabits(),
                topicRepository.getAllTopics(),
                checkInRepository.getCheckInsBetweenDates(
                    LocalDate.now().minusMonths(1),
                    LocalDate.now()
                )
            ) { habits, topics, checkIns ->
                Triple(habits, topics, checkIns)
            }.collect { (habits, topics, checkIns) ->
                updateHabitsData(habits, topics, checkIns)
            }
        }
    }

    private fun updateHabitsData(
        habits: List<HabitEntity>,
        topics: List<TopicEntity>,
        checkIns: List<CheckInEntity>
    ) {
        val today = LocalDate.now()
        val weekStart = today.with(DayOfWeek.MONDAY)
        val weekEnd = today.with(DayOfWeek.SUNDAY)
        val topicMap = topics.associateBy { it.id }

        val habitDisplays = habits.map { habit ->
            val habitCheckIns = checkIns.filter { it.habitId == habit.id }
            val topic = topicMap[habit.topicId]

            val todayCompleted = habitCheckIns.any { it.date == today && it.completed }

            val thisWeekProgress = habitCheckIns.count {
                it.date >= weekStart && it.date <= weekEnd && it.completed
            }

            val thisWeekTarget = when (habit.frequency) {
                Frequency.DAILY -> today.dayOfWeek.value
                Frequency.WEEKLY_N -> habit.weeklyTarget
                Frequency.MONTHLY_N -> (today.dayOfMonth / 7f).toInt().coerceAtLeast(1)
            }

            val thisMonthProgress = habitCheckIns.count {
                it.date.month == today.month && it.date.year == today.year && it.completed
            }

            val thisMonthTarget = when (habit.frequency) {
                Frequency.DAILY -> today.dayOfMonth
                Frequency.WEEKLY_N -> habit.weeklyTarget * 4
                Frequency.MONTHLY_N -> habit.monthlyTarget
            }

            val currentStreak = calculateStreak(habit, habitCheckIns, today)

            HabitDisplay(
                habit = habit,
                topic = topic,
                todayCompleted = todayCompleted,
                thisWeekProgress = thisWeekProgress,
                thisWeekTarget = thisWeekTarget,
                thisMonthProgress = thisMonthProgress,
                thisMonthTarget = thisMonthTarget,
                currentStreak = currentStreak
            )
        }

        // Group by topic
        val groupedHabits = topicMap.values.mapNotNull { topic ->
            val topicHabits = habitDisplays.filter { it.habit.topicId == topic.id }
            if (topicHabits.isEmpty()) null
            else topic to topicHabits
        }

        _uiState.value = _uiState.value.copy(
            topics = topics,
            habits = habitDisplays,
            isLoading = false
        )
    }

    private fun calculateStreak(habit: HabitEntity, habitCheckIns: List<CheckInEntity>, today: LocalDate): Int {
        if (habit.frequency != Frequency.DAILY) {
            return calculatePeriodStreak(habit, habitCheckIns, today)
        }

        var streak = 0
        var currentDate = today

        while (true) {
            val hasCheckIn = habitCheckIns.any { it.date == currentDate && it.completed }
            if (hasCheckIn) {
                streak++
                currentDate = currentDate.minusDays(1)
            } else if (currentDate == today) {
                currentDate = currentDate.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    private fun calculatePeriodStreak(habit: HabitEntity, habitCheckIns: List<CheckInEntity>, today: LocalDate): Int {
        var streak = 0
        var currentPeriod = if (habit.frequency == Frequency.WEEKLY_N) {
            today.with(DayOfWeek.MONDAY)
        } else {
            YearMonth.from(today).atDay(1)
        }

        while (true) {
            val periodEnd = if (habit.frequency == Frequency.WEEKLY_N) {
                currentPeriod.plusDays(6)
            } else {
                YearMonth.from(currentPeriod).atEndOfMonth()
            }

            val periodCount = habitCheckIns.count {
                it.date >= currentPeriod && it.date <= periodEnd && it.completed
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

    fun selectHabit(habit: HabitEntity) {
        viewModelScope.launch {
            val checkIns = checkInRepository.getCheckInsByHabit(habit.id).first()
            _uiState.value = _uiState.value.copy(
                selectedHabit = habit,
                selectedHabitCheckIns = checkIns
            )
        }
    }

    fun clearSelectedHabit() {
        _uiState.value = _uiState.value.copy(
            selectedHabit = null,
            selectedHabitCheckIns = emptyList()
        )
    }

    fun toggleHabitCheckIn(habitId: Long) {
        viewModelScope.launch {
            val today = LocalDate.now()
            checkInRepository.toggleCheckIn(habitId, today)
        }
    }

    fun addHabit(name: String, topicId: Long, frequency: Frequency, weeklyTarget: Int = 1, monthlyTarget: Int = 1) {
        viewModelScope.launch {
            val habit = HabitEntity(
                name = name,
                topicId = topicId,
                frequency = frequency,
                weeklyTarget = weeklyTarget,
                monthlyTarget = monthlyTarget
            )
            habitRepository.insert(habit)
            _uiState.value = _uiState.value.copy(showAddHabitDialog = false)
        }
    }

    fun addTopic(name: String, color: Long) {
        viewModelScope.launch {
            val topic = TopicEntity(name = name, color = color)
            topicRepository.insert(topic)
            _uiState.value = _uiState.value.copy(showAddTopicDialog = false)
        }
    }

    fun deleteHabit(habit: HabitEntity) {
        viewModelScope.launch {
            habitRepository.delete(habit)
            clearSelectedHabit()
        }
    }

    fun showAddHabitDialog() {
        _uiState.value = _uiState.value.copy(showAddHabitDialog = true)
    }

    fun hideAddHabitDialog() {
        _uiState.value = _uiState.value.copy(showAddHabitDialog = false)
    }

    fun showAddTopicDialog() {
        _uiState.value = _uiState.value.copy(showAddTopicDialog = true)
    }

    fun hideAddTopicDialog() {
        _uiState.value = _uiState.value.copy(showAddTopicDialog = false)
    }

    class Factory(
        private val habitRepository: HabitRepository,
        private val checkInRepository: CheckInRepository,
        private val topicRepository: TopicRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HabitViewModel(habitRepository, checkInRepository, topicRepository) as T
        }
    }
}
