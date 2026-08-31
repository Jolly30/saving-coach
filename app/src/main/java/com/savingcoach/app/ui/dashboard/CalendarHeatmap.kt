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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.savingcoach.app.ui.theme.MatchaRampTier0
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class CalendarDay(
    val date: LocalDate?,
    val spending: Double = 0.0,
    val savings: Double = 0.0,
    val investments: Double = 0.0,
    val dailyBudget: Double = 0.0,
    val savingTier: com.savingcoach.app.utils.ActivityTier = com.savingcoach.app.utils.ActivityTier.NEUTRAL,
    val expenseTier: com.savingcoach.app.utils.ActivityTier = com.savingcoach.app.utils.ActivityTier.NEUTRAL,
    val investmentTier: com.savingcoach.app.utils.ActivityTier = com.savingcoach.app.utils.ActivityTier.NEUTRAL
) {
    /** Whether this day has a savings deposit */
    val hasSavings: Boolean get() = savings > 0

    /** Whether this day has expenses */
    val hasExpenses: Boolean get() = spending > 0

    /** Whether this day has investments */
    val hasInvestments: Boolean get() = investments > 0
}

@Composable
fun CalendarHeatmap(
    dailySpending: Map<String, Double>,
    dailySavings: Map<String, Double> = emptyMap(),
    savingTiers: Map<String, com.savingcoach.app.utils.ActivityTier> = emptyMap(),
    expenseTiers: Map<String, com.savingcoach.app.utils.ActivityTier> = emptyMap(),
    investmentTiers: Map<String, com.savingcoach.app.utils.ActivityTier> = emptyMap(),
    dailyInvestments: Map<String, Double> = emptyMap(),
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
        val investments = dailyInvestments[dateStr] ?: 0.0
        val savingTier = savingTiers[dateStr] ?: com.savingcoach.app.utils.ActivityTier.NEUTRAL
        val expenseTier = expenseTiers[dateStr] ?: com.savingcoach.app.utils.ActivityTier.NEUTRAL
        val investmentTier = investmentTiers[dateStr] ?: com.savingcoach.app.utils.ActivityTier.NEUTRAL
        days.add(CalendarDay(date, spending, savings, investments, dailyBudget, savingTier, expenseTier, investmentTier))
    }

    val remainder = days.size % 7
    if (remainder > 0) {
        repeat(7 - remainder) {
            days.add(CalendarDay(null))
        }
    }

    val maxSpending = days.maxOfOrNull { it.spending } ?: 0.0
    val maxSavings = days.maxOfOrNull { it.savings } ?: 0.0
    val maxInvestments = days.maxOfOrNull { it.investments } ?: 0.0

    val strings = com.savingcoach.app.ui.localization.AppLocale.current

    Column(modifier = modifier.fillMaxWidth()) {
        // Month header & Filter Dropdown
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = if (showFilter) Arrangement.SpaceBetween else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.formatMonthYear(currentMonth),
                style = MaterialTheme.typography.titleMedium,
                textAlign = if (showFilter) TextAlign.Start else TextAlign.Center,
                modifier = if (showFilter) Modifier.weight(1f) else Modifier
            )

            var filterExpanded by remember { mutableStateOf(false) }
            if (showFilter) {
                val currentLabel = when (filter) {
                    CalendarFilter.ALL -> strings.calendarFilterAll
                    CalendarFilter.SAVINGS -> "💰 ${strings.savings}"
                    CalendarFilter.EXPENSES -> "🧾 ${strings.expenses}"
                    CalendarFilter.INVESTMENTS -> "📈 ${strings.investments}"
                }

                Box {
                    Surface(
                        onClick = { filterExpanded = true },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Filter",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = filterExpanded,
                        onDismissRequest = { filterExpanded = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.widthIn(min = 160.dp)
                    ) {
                        CalendarFilter.entries.forEach { f ->
                            val isSelected = filter == f
                            val (icon, label) = when (f) {
                                CalendarFilter.ALL -> "📅" to strings.calendarFilterAllCategories
                                CalendarFilter.SAVINGS -> "💰" to strings.savings
                                CalendarFilter.EXPENSES -> "🧾" to strings.expenses
                                CalendarFilter.INVESTMENTS -> "📈" to strings.investments
                            }
                            DropdownMenuItem(
                                modifier = Modifier
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent),
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(icon, fontSize = 15.sp)
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
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
                strings.dayHeaders.forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = if (day.length > 4) 9.sp else 10.sp,
                            maxLines = 1,
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
                                maxInvestments = maxInvestments,
                                onDateTap = onDateTap
                            )
                            if (isSelected && tooltipData != null) {
                                TooltipPopup(date = selectedDate!!, tooltip = tooltipData, filter = filter, onDismiss = onDismissTooltip)
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
            val strings = com.savingcoach.app.ui.localization.AppLocale.current
            when (filter) {
                CalendarFilter.ALL -> {
                    LegendItem(strings.investments, Orange.copy(alpha = 0.7f))
                    LegendItem(strings.savings, Green.copy(alpha = 0.7f))
                    LegendItem(strings.expenses, Red.copy(alpha = 0.7f))
                }
                CalendarFilter.SAVINGS -> {
                    LegendItem(strings.highSaver, Green)
                    LegendItem(strings.lowSaver, Yellow.copy(alpha = 0.7f))
                }
                CalendarFilter.EXPENSES -> {
                    LegendItem(strings.highExpense, Red)
                    LegendItem(strings.lowExpense, Green)
                }
                CalendarFilter.INVESTMENTS -> {
                    LegendItem(strings.highInvestment, Orange.copy(alpha = 0.7f))
                    LegendItem(strings.lowInvestment, Yellow.copy(alpha = 0.7f))
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
    maxInvestments: Double,
    onDateTap: (String) -> Unit
) {
    if (day.date == null) { Box(modifier = Modifier.aspectRatio(1f)); return }
    val dateStr = day.date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val isSelected = dateStr == selectedDate
    val isToday = day.date == LocalDate.now()
    val isMuted = when (filter) {
        CalendarFilter.SAVINGS -> !day.hasSavings
        CalendarFilter.EXPENSES -> !day.hasExpenses
        CalendarFilter.INVESTMENTS -> !day.hasInvestments
        CalendarFilter.ALL -> false
    }
    val inactiveColor = MaterialTheme.colorScheme.surfaceVariant
    val ratingColor = getDayBackgroundColor(day, filter, maxSpending, maxSavings, maxInvestments, inactiveColor)
    val backgroundColor = if (isMuted) inactiveColor.copy(alpha = 0.4f) else ratingColor
    val textAlpha = if (isMuted) 0.35f else 1.0f
    val isColored = backgroundColor != inactiveColor && backgroundColor != inactiveColor.copy(alpha = 0.4f)

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .then(
                when {
                    isSelected -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                    isToday -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                    else -> Modifier
                }
            )
            .clickable { onDateTap(dateStr) },
        contentAlignment = Alignment.Center
    ) {
        val strings = com.savingcoach.app.ui.localization.AppLocale.current
        Text(
            text = strings.formatNumber(day.date.dayOfMonth),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            fontWeight = if (isColored || isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                isToday && !isColored -> MaterialTheme.colorScheme.primary
                isColored -> Color.White
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha)
            }
        )
    }
}

fun getDayBackgroundColor(
    day: CalendarDay,
    filter: CalendarFilter,
    maxSpending: Double,
    maxSavings: Double,
    maxInvestments: Double,
    defaultInactiveColor: Color = Color.Transparent
): Color {
    if (day.date == null) return Color.Transparent
    
    return when (filter) {
        CalendarFilter.ALL -> {
            when {
                day.hasInvestments -> Orange.copy(alpha = 0.85f)
                day.hasSavings -> Green.copy(alpha = 0.85f)
                day.hasExpenses -> Red.copy(alpha = 0.85f)
                else -> defaultInactiveColor
            }
        }
        CalendarFilter.SAVINGS -> {
            when (day.savingTier) {
                com.savingcoach.app.utils.ActivityTier.HIGH -> Green
                com.savingcoach.app.utils.ActivityTier.NEUTRAL -> defaultInactiveColor
                com.savingcoach.app.utils.ActivityTier.LOW -> Yellow.copy(alpha = 0.85f)
            }
        }
        CalendarFilter.EXPENSES -> {
            when (day.expenseTier) {
                com.savingcoach.app.utils.ActivityTier.HIGH -> Red
                com.savingcoach.app.utils.ActivityTier.NEUTRAL -> defaultInactiveColor
                com.savingcoach.app.utils.ActivityTier.LOW -> Green
            }
        }
        CalendarFilter.INVESTMENTS -> {
            when (day.investmentTier) {
                com.savingcoach.app.utils.ActivityTier.HIGH -> Orange.copy(alpha = 0.85f)
                com.savingcoach.app.utils.ActivityTier.NEUTRAL -> defaultInactiveColor
                com.savingcoach.app.utils.ActivityTier.LOW -> Yellow.copy(alpha = 0.85f)
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
    filter: CalendarFilter,
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
                .widthIn(min = 140.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp).width(IntrinsicSize.Max)) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                val pref = tooltip.currency
                val strings = com.savingcoach.app.ui.localization.AppLocale.current
                if (filter == CalendarFilter.ALL || filter == CalendarFilter.EXPENSES) {
                    TooltipRow(icon = "🧾", label = strings.expense, value = com.savingcoach.app.utils.InvestmentCalculations.formatValue(tooltip.dayExpense, pref, 1.0, isInvestment = false))
                }
                if (filter == CalendarFilter.ALL || filter == CalendarFilter.SAVINGS) {
                    TooltipRow(icon = "💰", label = strings.savings, value = com.savingcoach.app.utils.InvestmentCalculations.formatValue(tooltip.daySaving, pref, 1.0, isInvestment = false))
                }
                if (filter == CalendarFilter.ALL || filter == CalendarFilter.INVESTMENTS) {
                    TooltipRow(icon = "📈", label = strings.investments, value = com.savingcoach.app.utils.InvestmentCalculations.formatValue(tooltip.dayInvestment, pref, 1.0, isInvestment = true))
                }
            }
        }
    }
}

@Composable
private fun TooltipRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$icon $label", 
            style = MaterialTheme.typography.labelSmall, 
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value, 
            style = MaterialTheme.typography.labelSmall, 
            fontWeight = FontWeight.Bold, 
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MiniCalendarHeatmap(
    month: YearMonth,
    dailySpending: Map<String, Double>,
    dailySavings: Map<String, Double>,
    savingTiers: Map<String, com.savingcoach.app.utils.ActivityTier> = emptyMap(),
    expenseTiers: Map<String, com.savingcoach.app.utils.ActivityTier> = emptyMap(),
    investmentTiers: Map<String, com.savingcoach.app.utils.ActivityTier> = emptyMap(),
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
        val savingTier = savingTiers[dateStr] ?: com.savingcoach.app.utils.ActivityTier.NEUTRAL
        val expenseTier = expenseTiers[dateStr] ?: com.savingcoach.app.utils.ActivityTier.NEUTRAL
        val investmentTier = investmentTiers[dateStr] ?: com.savingcoach.app.utils.ActivityTier.NEUTRAL
        days.add(CalendarDay(date, spending, savings, 0.0, dailyBudget, savingTier, expenseTier, investmentTier))
    }

    val remainder = days.size % 7
    if (remainder > 0) {
        repeat(7 - remainder) {
            days.add(CalendarDay(null))
        }
    }

    val maxSpending = days.maxOfOrNull { it.spending } ?: 0.0
    val maxSavings = days.maxOfOrNull { it.savings } ?: 0.0

    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = strings.formatMonthName(month),
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
                                    CalendarFilter.INVESTMENTS -> !day.hasInvestments
                                }
                                val inactiveColor = MaterialTheme.colorScheme.surfaceVariant
                                val ratingColor = getDayBackgroundColor(day, filter, maxSpending, maxSavings, 0.0, inactiveColor)
                                val backgroundColor = if (isMuted) inactiveColor.copy(alpha = 0.4f) else ratingColor
                                val isColored = backgroundColor != inactiveColor && backgroundColor != inactiveColor.copy(alpha = 0.4f)

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(backgroundColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val strings = com.savingcoach.app.ui.localization.AppLocale.current
                                    Text(
                                        text = strings.formatNumber(day.date.dayOfMonth),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 8.sp),
                                        fontWeight = if (isColored) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isColored) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
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
