package com.example.ididit.ui.screen.habit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ididit.ui.components.HabitHeatmap
import java.time.LocalDate
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ididit.data.local.AppDatabase
import com.example.ididit.data.local.CheckInEntity
import com.example.ididit.data.local.Frequency
import com.example.ididit.data.local.HabitEntity
import com.example.ididit.data.local.TopicEntity
import com.example.ididit.data.repository.CheckInRepository
import com.example.ididit.data.repository.HabitRepository
import com.example.ididit.data.repository.TopicRepository
import com.example.ididit.ui.components.CustomDialog
import com.example.ididit.ui.components.CustomTextField
import com.example.ididit.ui.theme.LocalExtendedColors

@Composable
fun HabitScreen(
    database: AppDatabase,
    viewModel: HabitViewModel = viewModel(
        factory = HabitViewModel.Factory(
            HabitRepository(database.habitDao()),
            CheckInRepository(database.checkInDao()),
            TopicRepository(database.topicDao())
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val extendedColors = LocalExtendedColors.current

    // Group habits by topic
    val groupedHabits = uiState.topics.mapNotNull { topic ->
        val topicHabits = uiState.habits.filter { it.habit.topicId == topic.id }
        if (topicHabits.isEmpty()) null
        else topic to topicHabits
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddHabitDialog() },
                containerColor = extendedColors.accent,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "习惯追踪",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = { viewModel.showAddTopicDialog() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "Manage Topics",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            groupedHabits.forEach { (topic, habits) ->
                item {
                    TopicSection(
                        topic = topic,
                        habits = habits,
                        onToggleHabit = { viewModel.toggleHabitCheckIn(it) },
                        onHabitClick = { viewModel.selectHabit(it.habit) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (uiState.showAddHabitDialog) {
        AddHabitDialog(
            topics = uiState.topics,
            onDismiss = { viewModel.hideAddHabitDialog() },
            onAdd = { name, topicId, frequency, weeklyTarget ->
                viewModel.addHabit(name, topicId, frequency, weeklyTarget)
            }
        )
    }

    if (uiState.showAddTopicDialog) {
        AddTopicDialog(
            onDismiss = { viewModel.hideAddTopicDialog() },
            onAdd = { name, color -> viewModel.addTopic(name, color) }
        )
    }

    uiState.selectedHabit?.let { habit ->
        val habitDisplay = uiState.habits.find { it.habit.id == habit.id }
        val checkIns = uiState.selectedHabitCheckIns
        HabitDetailDialog(
            habit = habit,
            habitDisplay = habitDisplay,
            checkIns = checkIns,
            onDismiss = { viewModel.clearSelectedHabit() },
            onDelete = {
                viewModel.deleteHabit(habit)
                viewModel.clearSelectedHabit()
            }
        )
    }
}

@Composable
private fun TopicSection(
    topic: TopicEntity,
    habits: List<HabitDisplay>,
    onToggleHabit: (Long) -> Unit,
    onHabitClick: (HabitDisplay) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(topic.color))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = topic.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        habits.forEach { habitDisplay ->
            HabitCard(
                habitDisplay = habitDisplay,
                onToggle = { onToggleHabit(habitDisplay.habit.id) },
                onClick = { onHabitClick(habitDisplay) }
            )
        }
    }
}

@Composable
private fun HabitCard(
    habitDisplay: HabitDisplay,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val habit = habitDisplay.habit
    var isChecked by remember { mutableStateOf(habitDisplay.todayCompleted) }
    val scale by animateFloatAsState(
        targetValue = if (isChecked) 0.98f else 1f,
        animationSpec = tween(150),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val frequencyText = when (habit.frequency) {
                        Frequency.DAILY -> "每日"
                        Frequency.WEEKLY_N -> "每周${habit.weeklyTarget}次"
                        Frequency.MONTHLY_N -> "每月${habit.monthlyTarget}次"
                    }
                    Text(
                        text = frequencyText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (habitDisplay.currentStreak > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🔥 ${habitDisplay.currentStreak}天",
                            style = MaterialTheme.typography.labelSmall,
                            color = extendedColors.accentCoral
                        )
                    }
                }
            }

            if (habitDisplay.thisWeekTarget > 0) {
                Text(
                    text = "${habitDisplay.thisWeekProgress}/${habitDisplay.thisWeekTarget}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isChecked) extendedColors.accentSage.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable {
                        isChecked = !isChecked
                        onToggle()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (isChecked) extendedColors.accentSage else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AddHabitDialog(
    topics: List<TopicEntity>,
    onDismiss: () -> Unit,
    onAdd: (String, Long, Frequency, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedTopicId by remember { mutableStateOf(topics.firstOrNull()?.id ?: 0L) }
    var frequency by remember { mutableStateOf(Frequency.DAILY) }
    var weeklyTarget by remember { mutableStateOf("3") }
    var showTopicWarning by remember { mutableStateOf(false) }
    val extendedColors = LocalExtendedColors.current

    CustomDialog(
        onDismiss = onDismiss,
        title = "添加习惯",
        confirmText = "添加",
        onConfirm = {
            if (name.isBlank()) return@CustomDialog
            if (selectedTopicId <= 0) {
                showTopicWarning = true
                return@CustomDialog
            }
            onAdd(name, selectedTopicId, frequency, weeklyTarget.toIntOrNull() ?: 3)
        },
        content = {
            Column {
                CustomTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "习惯名称"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "选择分组",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (topics.isNotEmpty()) {
                    topics.forEach { topic ->
                        val isSelected = selectedTopicId == topic.id
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSelected) extendedColors.accent.copy(alpha = 0.1f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    selectedTopicId = topic.id
                                    showTopicWarning = false
                                }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(topic.color))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = topic.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = extendedColors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "暂无分组，请先创建分组",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (showTopicWarning) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "请选择分组",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}

@Composable
private fun AddTopicDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(0xFF9A9A9A) }
    val extendedColors = LocalExtendedColors.current

    val colorOptions = listOf(
        0xFF9A9A9A.toLong(), // Gray
        0xFFE8B4A8.toLong(), // Coral
        0xFFA8C4B0.toLong(), // Sage
        0xFFA8B8C8.toLong(), // Blue
        0xFFD4C89C.toLong(), // Yellow
        0xFFB8A8C8.toLong()  // Purple
    )

    CustomDialog(
        onDismiss = onDismiss,
        title = "添加分组",
        confirmText = "添加",
        onConfirm = {
            if (name.isNotBlank()) {
                onAdd(name, color)
            }
        },
        content = {
            Column {
                CustomTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "分组名称"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "颜色",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colorOptions.forEach { colorOption ->
                        val isSelected = color == colorOption
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorOption))
                                .clickable { color = colorOption }
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.White.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun HabitDetailDialog(
    habit: HabitEntity,
    habitDisplay: HabitDisplay?,
    checkIns: List<CheckInEntity>,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    var currentMonth by remember { mutableStateOf(java.time.YearMonth.now()) }

    val completedDates = checkIns
        .filter { it.completed }
        .map { it.date }
        .toSet()

    val longestStreak = remember(checkIns) {
        calculateLongestStreak(checkIns)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (habit.frequency) {
                        Frequency.DAILY -> "每日"
                        Frequency.WEEKLY_N -> "每周${habit.weeklyTarget}次"
                        Frequency.MONTHLY_N -> "每月${habit.monthlyTarget}次"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        label = "当前连续",
                        value = "${habitDisplay?.currentStreak ?: 0}",
                        unit = "天",
                        color = extendedColors.accentCoral
                    )
                    StatItem(
                        label = "最长连续",
                        value = "$longestStreak",
                        unit = "天",
                        color = extendedColors.accentYellow
                    )
                    StatItem(
                        label = "本月完成",
                        value = "${habitDisplay?.thisMonthProgress ?: 0}",
                        unit = "次",
                        color = extendedColors.accentSage
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                HabitHeatmap(
                    yearMonth = currentMonth,
                    completedDates = completedDates,
                    onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                    onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "点击外部关闭",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Light,
                color = color
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            )
        }
    }
}

private fun calculateLongestStreak(checkIns: List<CheckInEntity>): Int {
    if (checkIns.isEmpty()) return 0

    val completedDates = checkIns
        .filter { it.completed }
        .map { it.date }
        .sorted()

    if (completedDates.isEmpty()) return 0

    var longest = 1
    var current = 1
    var prev = completedDates.first()

    for (i in 1 until completedDates.size) {
        val curr = completedDates[i]
        if (curr == prev.plusDays(1)) {
            current++
            longest = maxOf(longest, current)
        } else {
            current = 1
        }
        prev = curr
    }
    return longest
}
