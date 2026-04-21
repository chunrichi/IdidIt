package com.example.ididit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ididit.ui.theme.LocalExtendedColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun GithubHeatmap(
    data: List<Pair<LocalDate, Int>>,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current
    val today = LocalDate.now()
    val yearStart = LocalDate.of(today.year, 1, 1)
    val yearEnd = LocalDate.of(today.year, 12, 31)

    // Create a map for quick lookup
    val dataMap = data.toMap()

    // Build the grid for the full year
    val weeksInYear = mutableListOf<List<Pair<LocalDate, Int>>>()
    var currentDate = yearStart

    // Adjust to start from Sunday
    while (currentDate.dayOfWeek != DayOfWeek.SUNDAY) {
        currentDate = currentDate.minusDays(1)
    }

    while (currentDate <= yearEnd) {
        val week = mutableListOf<Pair<LocalDate, Int>>()
        for (day in 0..6) {
            val date = currentDate.plusDays(day.toLong())
            if (date.year == today.year) {
                if (date <= today) {
                    week.add(date to (dataMap[date] ?: 0))
                } else {
                    week.add(date to -1) // -1 means future
                }
            }
        }
        if (week.isNotEmpty()) {
            weeksInYear.add(week)
        }
        currentDate = currentDate.plusWeeks(1)
    }

    // Calculate month labels
    val monthLabels = Month.values().map { month ->
        val firstDayOfMonth = LocalDate.of(today.year, month, 1)
        val weekIndex = weeksInYear.indexOfFirst { week ->
            week.any { it.first == firstDayOfMonth || (it.first.month == month && it.first.dayOfMonth <= 7) }
        }
        month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) to weekIndex
    }.filter { it.second >= 0 }.toMutableList()

    // Shared scroll state for sync between month labels and grid
    val scrollState = rememberScrollState()

    // Calculate target scroll position to center current month
    val currentMonthWeekIndex = remember(today) {
        val currentMonth = today.month
        monthLabels.find { it.first == currentMonth.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }?.second ?: (today.dayOfYear / 7)
    }

    // Auto-scroll to current month on first composition
    LaunchedEffect(currentMonthWeekIndex) {
        // Each week cell is 12dp wide (10dp + 2dp spacing)
        val targetScroll = (currentMonthWeekIndex * 12).coerceAtLeast(0)
        scrollState.animateScrollTo(targetScroll)
    }

    Column(modifier = modifier) {
        // Month labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(24.dp))
            Box(
                modifier = Modifier
                    .horizontalScroll(scrollState)
                    .fillMaxWidth()
            ) {
                Row {
                    monthLabels.forEachIndexed { index, (month, weekIndex) ->
                        if (index > 0) {
                            val prevWeekIndex = monthLabels[index - 1].second
                            if (weekIndex > prevWeekIndex + 2) {
                                val gapWeeks = (weekIndex - prevWeekIndex - 1).coerceAtLeast(0)
                                Spacer(modifier = Modifier.width((gapWeeks * 12).dp))
                            }
                        }
                        Text(
                            text = month,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Day labels and heatmap grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Day of week labels
            Column(
                modifier = Modifier.width(20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                listOf("", "一", "", "三", "", "五", "").forEachIndexed { index, label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.height(10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            // Heatmap grid - uses same scroll state
            Box(
                modifier = Modifier
                    .horizontalScroll(scrollState)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    weeksInYear.forEachIndexed { weekIndex, week ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            week.forEach { (date, count) ->
                                val isFuture = count == -1

                                HeatmapCell(
                                    count = if (isFuture) 0 else count,
                                    extendedColors = extendedColors,
                                    delay = weekIndex * 3
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    count: Int,
    extendedColors: com.example.ididit.ui.theme.ExtendedColors,
    delay: Int = 0
) {
    var animated by remember { mutableStateOf(0f) }
    val targetAlpha = getHeatmapAlpha(count)

    LaunchedEffect(count) {
        animated = targetAlpha
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = animated,
        animationSpec = tween(durationMillis = 200 + delay)
    )

    val color = when {
        count == 0 -> extendedColors.heatmapLevel0
        count <= 1 -> extendedColors.heatmapLevel1
        count <= 2 -> extendedColors.heatmapLevel2
        count <= 3 -> extendedColors.heatmapLevel3
        else -> extendedColors.heatmapLevel4
    }

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color.copy(alpha = animatedAlpha.coerceIn(0.4f, 1f)))
    )
}

private fun getHeatmapAlpha(count: Int): Float {
    return when {
        count == 0 -> 0.4f
        count <= 1 -> 0.6f
        count <= 2 -> 0.8f
        count <= 3 -> 0.9f
        else -> 1.0f
    }
}
