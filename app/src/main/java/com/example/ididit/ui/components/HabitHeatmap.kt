package com.example.ididit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ididit.ui.theme.LocalExtendedColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitHeatmap(
    yearMonth: YearMonth,
    completedDates: Set<LocalDate>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${yearMonth.year}年 ${yearMonth.monthValue}月",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = onNextMonth) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DayOfWeek.values().forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val startDate = yearMonth.atDay(1)
        val firstDayOfWeek = startDate.dayOfWeek.value % 7
        val daysInMonth = yearMonth.lengthOfMonth()

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            var dayCounter = 1
            for (week in 0..5) {
                if (dayCounter > daysInMonth) break

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dayOfWeek in 0..6) {
                        if (week == 0 && dayOfWeek < firstDayOfWeek) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else if (dayCounter > daysInMonth) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = yearMonth.atDay(dayCounter)
                            val isCompleted = completedDates.contains(date)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(1.dp)
                            ) {
                                HeatmapCell(
                                    isCompleted = isCompleted,
                                    extendedColors = extendedColors
                                )
                            }
                            dayCounter++
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    isCompleted: Boolean,
    extendedColors: com.example.ididit.ui.theme.ExtendedColors
) {
    var animated by remember { mutableStateOf(0f) }
    val targetAlpha = if (isCompleted) 1f else 0.3f

    LaunchedEffect(isCompleted) {
        animated = targetAlpha
    }

    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = animated,
        animationSpec = tween(300),
        label = "alpha"
    )

    val color = if (isCompleted) extendedColors.accentSage else extendedColors.heatmapLevel0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .background(color.copy(alpha = alpha)),
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Text(
                text = "✓",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = alpha)
            )
        }
    }
}
