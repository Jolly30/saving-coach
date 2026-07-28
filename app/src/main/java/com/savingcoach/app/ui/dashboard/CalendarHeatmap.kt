package com.savingcoach.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.savingcoach.app.ui.theme.Green
import com.savingcoach.app.ui.theme.Orange
import com.savingcoach.app.ui.theme.Red
import com.savingcoach.app.ui.theme.Yellow
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class CalendarDay(
    val date: LocalDate?,
    val spending: Double = 0.0,
    val dailyBudget: Double = 0.0
) {
    val color: Color
        get() {
            if (date == null) return Color.Transparent
            if (dailyBudget <= 0) return Color.LightGray.copy(alpha = 0.3f)
            val ratio = spending / dailyBudget
            return when {
                ratio <= 0.5 -> Green.copy(alpha = 0.7f)
                ratio <= 0.8 -> Yellow.copy(alpha = 0.7f)
                ratio <= 1.0 -> Orange.copy(alpha = 0.7f)
                else -> Red.copy(alpha = 0.7f)
            }
        }
}

@Composable
fun CalendarHeatmap(
    dailySpending: Map<String, Double>,
    monthlyBudget: Double,
    modifier: Modifier = Modifier,
    cellSize: Dp = 32.dp
) {
    val today = LocalDate.now()
    val currentMonth = YearMonth.from(today)
    val dailyBudget = monthlyBudget / currentMonth.lengthOfMonth()

    // Build calendar days
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1)
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 0=Sun

    val days = mutableListOf<CalendarDay>()

    // Empty cells before first day
    repeat(startDayOfWeek) {
        days.add(CalendarDay(null))
    }

    // Actual days
    for (day in 1..daysInMonth) {
        val date = currentMonth.atDay(day)
        val dateStr = date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        val spending = dailySpending[dateStr] ?: 0.0
        days.add(CalendarDay(date, spending, dailyBudget))
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Month and year header
        Text(
            text = currentMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Day of week headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            userScrollEnabled = false
        ) {
            items(days) { day ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(day.color),
                    contentAlignment = Alignment.Center
                ) {
                    day.date?.let {
                        Text(
                            text = it.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = if (day.date == today)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem("Under 50%", Green.copy(alpha = 0.7f))
            LegendItem("50-80%", Yellow.copy(alpha = 0.7f))
            LegendItem("80-100%", Orange.copy(alpha = 0.7f))
            LegendItem("Over 100%", Red.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
                .padding(6.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
