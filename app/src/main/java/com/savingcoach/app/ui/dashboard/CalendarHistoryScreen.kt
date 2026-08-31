package com.savingcoach.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import java.time.YearMonth

import androidx.compose.material3.TopAppBarDefaults

enum class CalendarHistoryViewMode { MONTH, YEAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarHistoryScreen(
    onBack: () -> Unit = {},
    viewModel: CalendarHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var viewMode by remember { mutableStateOf(CalendarHistoryViewMode.MONTH) }
    var globalFilter by remember { mutableStateOf(CalendarFilter.ALL) }
    val monthListState = rememberLazyListState()
    val yearListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val strings = com.savingcoach.app.ui.localization.AppLocale.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                                .padding(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (viewMode == CalendarHistoryViewMode.MONTH) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .clickable { viewMode = CalendarHistoryViewMode.MONTH }
                                    .padding(horizontal = 18.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.monthView,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (viewMode == CalendarHistoryViewMode.MONTH) FontWeight.Bold else FontWeight.Medium,
                                    color = if (viewMode == CalendarHistoryViewMode.MONTH) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (viewMode == CalendarHistoryViewMode.YEAR) MaterialTheme.colorScheme.surface else Color.Transparent)
                                    .clickable { viewMode = CalendarHistoryViewMode.YEAR }
                                    .padding(horizontal = 18.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strings.yearView,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (viewMode == CalendarHistoryViewMode.YEAR) FontWeight.Bold else FontWeight.Medium,
                                    color = if (viewMode == CalendarHistoryViewMode.YEAR) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    var filterExpanded by remember { mutableStateOf(false) }
                    val currentLabel = when (globalFilter) {
                        CalendarFilter.ALL -> strings.calendarFilterAll
                        CalendarFilter.SAVINGS -> "💰 ${strings.savings}"
                        CalendarFilter.EXPENSES -> "🧾 ${strings.expenses}"
                        CalendarFilter.INVESTMENTS -> "📈 ${strings.investments}"
                    }
                    Box(modifier = Modifier.padding(end = 8.dp)) {
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
                                val isSelected = globalFilter == f
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
                                        globalFilter = f
                                        filterExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            if (uiState.months.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings.loading, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LaunchedEffect(uiState.months) {
                    // Pre-scroll Month view
                    val currentMonth = YearMonth.now()
                    val currentMonthIndex = uiState.months.indexOfFirst { it.month == currentMonth }
                    if (currentMonthIndex >= 0) {
                        monthListState.scrollToItem(currentMonthIndex)
                    }
                    
                    // Pre-scroll Year view
                    val currentYear = YearMonth.now().year
                    val years = uiState.months.map { it.month.year }.distinct()
                    val currentYearIndex = years.indexOf(currentYear)
                    if (currentYearIndex >= 0) {
                        yearListState.scrollToItem(currentYearIndex)
                    }
                }

                if (viewMode == CalendarHistoryViewMode.MONTH) {
                    // Global Day Headers
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
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

                    androidx.compose.foundation.lazy.LazyColumn(
                        state = monthListState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(uiState.months.size) { index ->
                            val monthData = uiState.months[index]
                            CalendarHeatmap(
                                dailySpending = monthData.dailySpending,
                                dailySavings = monthData.dailySavings,
                                savingTiers = monthData.savingTiers,
                                expenseTiers = monthData.expenseTiers,
                                investmentTiers = monthData.investmentTiers,
                                monthlyBudget = monthData.monthlyBudget,
                                month = monthData.month,
                                filter = globalFilter,
                                selectedDate = uiState.selectedDate,
                                tooltipData = uiState.tooltipData,
                                onDateTap = { date -> viewModel.onDateTap(date, monthData) },
                                onDismissTooltip = { viewModel.onDismissTooltip() },
                                showFilter = false,
                                showDayHeaders = false,
                                monthFormat = "MMMM yyyy"
                            )
                        }
                    }
                } else {
                    // YEAR View
                    val yearsList = uiState.months.map { it.month.year }.distinct()
                    val yearsGroups = uiState.months.groupBy { it.month.year }
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = yearListState,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(yearsList.size) { index ->
                            val year = yearsList[index]
                            val monthsInYear = yearsGroups[year] ?: emptyList()
                                Text(
                                    text = strings.formatNumber(year),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    textAlign = TextAlign.Center
                                )
                                
                                val sortedMonthsInYear = monthsInYear.sortedBy { it.month.monthValue }
                                val chunkedMonths = sortedMonthsInYear.chunked(3)
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    chunkedMonths.forEach { rowMonths ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            for (i in 0 until 3) {
                                                if (i < rowMonths.size) {
                                                    val mData = rowMonths[i]
                                                    MiniCalendarHeatmap(
                                                        month = mData.month,
                                                        dailySpending = mData.dailySpending,
                                                        dailySavings = mData.dailySavings,
                                                        savingTiers = mData.savingTiers,
                                                        expenseTiers = mData.expenseTiers,
                                                        investmentTiers = mData.investmentTiers,
                                                        monthlyBudget = mData.monthlyBudget,
                                                        filter = globalFilter,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable {
                                                                val index = uiState.months.indexOf(mData)
                                                                if (index >= 0) {
                                                                    coroutineScope.launch {
                                                                        monthListState.scrollToItem(index)
                                                                    }
                                                                    viewMode = CalendarHistoryViewMode.MONTH
                                                                }
                                                            }
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
