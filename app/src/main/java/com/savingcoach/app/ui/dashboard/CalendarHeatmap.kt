package com.savingcoach.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savingcoach.app.ui.theme.Green
import com.savingcoach.app.ui.theme.Orange
import com.savingcoach.app.ui.theme.Red
import com.savingcoach.app.ui.theme.Yellow
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class CalendarDay(
    val date: LocalDate?,
    val spending: Double = 0.0,
    val savings: Double = 0.0,
    val dailyBudget: Double = 0.0
) {
    /** Whether this day has a savings deposit */
    val hasSavings: Boolean get() = savings > 0

    /** Whether this day has expenses */
    val hasExpenses: Boolean get() = spending > 0
}

@Composable
fun CalendarHeatmap(
    dailySpending: Map<String, Double>,
    dailySavings: Map<String, Double> = emptyMap(),
    monthlyBudget: Double,
    month: YearMonth = YearMonth.from(LocalDate.now()),
    filter: CalendarFilter = CalendarFilter.ALL,
    selectedDate: String? = null,
    tooltipData: TooltipData? = null,
    onDateTap: (String) -> Unit = {},
    onFilterChange: (CalendarFilter) -> Unit = {},
    onDismissTooltip: () -> Unit = {},
    showDayHeaders: Boolean = true,
    showFilter: Boolean = true,
    monthFormat: String = "MMMM yyyy",
    modifier: Modifier = Modifier,
    cellSize: Dp = 32.dp
) {
    val today = LocalDate.now()
    val currentMonth = month
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
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val spending = dailySpending[dateStr] ?: 0.0
        val savings = dailySavings[dateStr] ?: 0.0
        days.add(CalendarDay(date, spending, savings, dailyBudget))
    }

    val remainder = days.size % 7
    if (remainder > 0) {
        repeat(7 - remainder) {
            days.add(CalendarDay(null))
        }
    }

    val maxSpending = days.maxOfOrNull { it.spending } ?: 0.0
    val maxSavings = days.maxOfOrNull { it.savings } ?: 0.0

    Column(modifier = modifier.fillMaxWidth()) {
        // Month header & Filter Dropdown
        // Month header & Filter Dropdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = if (showFilter) Arrangement.SpaceBetween else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern(monthFormat)),
                style = MaterialTheme.typography.titleMedium,
                textAlign = if (showFilter) TextAlign.Start else TextAlign.Center,
                modifier = if (showFilter) Modifier.weight(1f) else Modifier
            )

            var filterExpanded by remember { mutableStateOf(false) }
            if (showFilter) {
            val currentLabel = when (filter) {
                CalendarFilter.ALL -> "All"
                CalendarFilter.SAVINGS -> "💰 Savings"
                CalendarFilter.EXPENSES -> "🧾 Expenses"
            }

            Box {
                OutlinedButton(
                    onClick = { filterExpanded = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(currentLabel, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Filter", modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = filterExpanded,
                    onDismissRequest = { filterExpanded = false }
                ) {
                    CalendarFilter.entries.forEach { f ->
                        val label = when (f) {
                            CalendarFilter.ALL -> "All"
                            CalendarFilter.SAVINGS -> "💰 Savings"
                            CalendarFilter.EXPENSES -> "🧾 Expenses"
                        }
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                onFilterChange(f)
                                filterExpanded = false
                            }
                        )
                    }
                }
            }
            }
        }

        // Day of week headers
        if (showDayHeaders) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Calendar grid — using Column/Row to avoid nested scroll conflicts
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Chunk days into rows of 7
            days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    week.forEach { day ->
                        Box(modifier = Modifier.weight(1f)) {
                            val isSelected = day.date != null && day.date.format(DateTimeFormatter.ISO_LOCAL_DATE) == selectedDate
                            CalendarDayCell(
                                day = day,
                                selectedDate = if (isSelected) selectedDate else null,
                                filter = filter,
                                maxSpending = maxSpending,
                                maxSavings = maxSavings,
                                onDateTap = onDateTap
                            )
                            if (isSelected && tooltipData != null) {
                                TooltipPopup(date = selectedDate!!, tooltip = tooltipData, onDismiss = onDismissTooltip)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem("Normal", Color.Transparent)
            when (filter) {
                CalendarFilter.ALL -> {
                    LegendItem("Most Saved", Green.copy(alpha = 0.7f))
                    LegendItem("Most Spent", Red.copy(alpha = 0.7f))
                }
                CalendarFilter.SAVINGS -> {
                    LegendItem("Most Saved", Green.copy(alpha = 0.7f))
                }
                CalendarFilter.EXPENSES -> {
                    LegendItem("80-100%", Yellow.copy(alpha = 0.7f))
                    LegendItem("Over 100%", Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    selectedDate: String?,
    filter: CalendarFilter,
    maxSpending: Double,
    maxSavings: Double,
    onDateTap: (String) -> Unit
) {
    if (day.date == null) { Box(modifier = Modifier.aspectRatio(1f)); return }
    val dateStr = day.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val isSelected = dateStr == selectedDate
    val isToday = day.date == LocalDate.now()
    val isMuted = when (filter) {
        CalendarFilter.SAVINGS -> !day.hasSavings
        CalendarFilter.EXPENSES -> !day.hasExpenses
        CalendarFilter.ALL -> false
    }
    val ratingColor = getDayBackgroundColor(day, filter, maxSpending, maxSavings)
    val backgroundColor = if (isMuted) Color.LightGray.copy(alpha = 0.15f) else ratingColor
    val textAlpha = if (isMuted) 0.3f else 1.0f

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .then(
                when {
                    isSelected -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    isToday -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    else -> Modifier
                }
            )
            .clickable { onDateTap(dateStr) },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
                }
            )
        }
    }
}

fun getDayBackgroundColor(
    day: CalendarDay,
    filter: CalendarFilter,
    maxSpending: Double,
    maxSavings: Double
): Color {
    if (day.date == null) return Color.Transparent
    
    return when (filter) {
        CalendarFilter.ALL -> {
            if (day.hasSavings && day.savings >= maxSavings && maxSavings > 0) {
                Green.copy(alpha = 0.7f)
            } else if (day.hasExpenses && day.spending >= maxSpending && maxSpending > 0) {
                Red.copy(alpha = 0.7f)
            } else {
                Color.Transparent
            }
        }
        CalendarFilter.SAVINGS -> {
            if (day.hasSavings && day.savings >= maxSavings && maxSavings > 0) {
                Green.copy(alpha = 0.7f)
            } else {
                Color.Transparent
            }
        }
        CalendarFilter.EXPENSES -> {
            if (day.dailyBudget <= 0) return Color.LightGray.copy(alpha = 0.3f)
            val ratio = day.spending / day.dailyBudget
            when {
                ratio > 1.0 -> Red.copy(alpha = 0.7f)
                ratio > 0.8 -> Yellow.copy(alpha = 0.7f)
                else -> Color.Transparent
            }
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
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Tooltip Popup ──
@Composable
private fun TooltipPopup(
    date: String,
    tooltip: TooltipData,
    onDismiss: () -> Unit
) {
    Popup(
        onDismissRequest = onDismiss,
        alignment = Alignment.TopCenter,
        properties = PopupProperties(focusable = false)
    ) {
        Card(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .clickable { onDismiss() }
                .width(140.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                TooltipRow(icon = "🎯", label = "Budget", value = "${tooltip.dayBudget.toInt()} ${tooltip.currency}")
                TooltipRow(icon = "🧾", label = "Expense", value = "${tooltip.dayExpense.toInt()} ${tooltip.currency}")
                TooltipRow(icon = "💰", label = "Saving", value = "${tooltip.daySaving.toInt()} ${tooltip.currency}")
            }
        }
    }
}

@Composable
private fun TooltipRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$icon $label", style = MaterialTheme.typography.labelSmall, color = Color.Black)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun MiniCalendarHeatmap(
    month: YearMonth,
    dailySpending: Map<String, Double>,
    dailySavings: Map<String, Double>,
    monthlyBudget: Double,
    filter: CalendarFilter,
    modifier: Modifier = Modifier
) {
    val dailyBudget = monthlyBudget / month.lengthOfMonth()
    val daysInMonth = month.lengthOfMonth()
    val firstDayOfMonth = month.atDay(1)
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 0=Sun

    val days = mutableListOf<CalendarDay>()
    repeat(startDayOfWeek) { days.add(CalendarDay(null)) }
    for (day in 1..daysInMonth) {
        val date = month.atDay(day)
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val spending = dailySpending[dateStr] ?: 0.0
        val savings = dailySavings[dateStr] ?: 0.0
        days.add(CalendarDay(date, spending, savings, dailyBudget))
    }

    val remainder = days.size % 7
    if (remainder > 0) {
        repeat(7 - remainder) {
            days.add(CalendarDay(null))
        }
    }

    val maxSpending = days.maxOfOrNull { it.spending } ?: 0.0
    val maxSavings = days.maxOfOrNull { it.savings } ?: 0.0

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = month.format(DateTimeFormatter.ofPattern("MMMM")),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            days.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    week.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day.date != null) {
                                val isMuted = when (filter) {
                                    CalendarFilter.ALL -> false
                                    CalendarFilter.SAVINGS -> !day.hasSavings
                                    CalendarFilter.EXPENSES -> !day.hasExpenses
                                }
                                val ratingColor = getDayBackgroundColor(day, filter, maxSpending, maxSavings)
                                val backgroundColor = if (isMuted) Color.LightGray.copy(alpha = 0.15f) else ratingColor
                                val textAlpha = if (isMuted) 0.3f else 1.0f

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(backgroundColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
