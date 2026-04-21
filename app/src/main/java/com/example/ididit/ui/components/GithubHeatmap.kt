package com.example.ididit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.example.ididit.ui.theme.LocalExtendedColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun GithubHeatmap(
    data: List<Pair<LocalDate, Int>>,
    modifier: Modifier = Modifier
) {
    val extendedColors = LocalExtendedColors.current

    // Organize data by week
    val weeksData = data.chunked(7)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.width(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Spacer(modifier = Modifier.height(0.dp))
                DayOfWeek.values().forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.height(10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(2.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                for (dayIndex in 0..6) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        weeksData.forEachIndexed { weekIndex, week ->
                            if (dayIndex < week.size) {
                                val (date, count) = week[dayIndex]
                                var animated by remember { mutableStateOf(0f) }
                                val targetAlpha = getHeatmapAlpha(count)

                                LaunchedEffect(count) {
                                    animated = targetAlpha
                                }

                                val animatedAlpha by animateFloatAsState(
                                    targetValue = animated,
                                    animationSpec = tween(durationMillis = 300 + (weekIndex * 5))
                                )

                                HeatmapCell(
                                    count = count,
                                    alpha = animatedAlpha,
                                    extendedColors = extendedColors
                                )
                            } else {
                                EmptyCell()
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
    alpha: Float,
    extendedColors: com.example.ididit.ui.theme.ExtendedColors
) {
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
            .background(color.copy(alpha = alpha.coerceIn(0.5f, 1f)))
    )
}

@Composable
private fun EmptyCell() {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.background)
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
