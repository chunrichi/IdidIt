package com.example.ididit.ui.screen.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ididit.data.local.AppDatabase
import com.example.ididit.data.repository.CheckInRepository
import com.example.ididit.data.repository.HabitRepository
import com.example.ididit.data.repository.TopicRepository
import com.example.ididit.ui.components.GithubHeatmap
import com.example.ididit.ui.theme.LocalExtendedColors
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    database: AppDatabase,
    viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            HabitRepository(database.habitDao()),
            CheckInRepository(database.checkInDao()),
            TopicRepository(database.topicDao())
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val extendedColors = LocalExtendedColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "看板",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Medium
            )
        }

        // Today's stats
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "今日",
                        value = "${uiState.todayCompleted}/${uiState.todayTarget}",
                        subtitle = "已完成",
                        accentColor = extendedColors.accentSage,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "本周",
                        value = "${uiState.weekCompleted}/${uiState.weekTarget}",
                        subtitle = "已完成",
                        accentColor = extendedColors.accentBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Streak stats
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(400, delayMillis = 100)) + slideInVertically(tween(400, delayMillis = 100))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "当前连续",
                        value = "${uiState.currentStreak}",
                        subtitle = "天",
                        accentColor = extendedColors.accentCoral,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "最长连续",
                        value = "${uiState.longestStreak}",
                        subtitle = "天",
                        accentColor = extendedColors.accentYellow,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Heatmap
        item {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(500, delayMillis = 200)) + slideInVertically(tween(500, delayMillis = 200))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(0.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "完成热力图",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GithubHeatmap(
                            data = uiState.heatmapData.map { it.key to it.value },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Today's habits
        if (uiState.habitsWithStats.isNotEmpty()) {
            item {
                Text(
                    text = "今日习惯",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            itemsIndexed(uiState.habitsWithStats.take(5)) { index, habitWithStats ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(300, delayMillis = 300 + index * 50)) +
                            slideInVertically(tween(300, delayMillis = 300 + index * 50))
                ) {
                    HabitQuickCard(
                        habitWithStats = habitWithStats,
                        onToggle = {}
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = accentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.dp)
                    .background(accentColor, RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
private fun HabitQuickCard(
    habitWithStats: HabitWithStats,
    onToggle: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current
    val habit = habitWithStats.habit
    val topic = habitWithStats.topic

    var isChecked by remember { mutableStateOf(habitWithStats.todayCompleted) }
    val scale by animateFloatAsState(
        targetValue = if (isChecked) 0.98f else 1f,
        animationSpec = tween(150),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                isChecked = !isChecked
                onToggle()
            },
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
            if (topic != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(topic.color))
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (habitWithStats.currentStreak > 0) {
                    Text(
                        text = "🔥 ${habitWithStats.currentStreak}天",
                        style = MaterialTheme.typography.labelSmall,
                        color = extendedColors.accentCoral
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (isChecked) extendedColors.accentSage else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
