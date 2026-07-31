package com.savingcoach.app.ui.dashboard

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
import java.time.YearMonth

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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Row(
                            modifier = Modifier
                                .background(Color(0xFFF3F4F6), RoundedCornerShape(24.dp))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(if (viewMode == CalendarHistoryViewMode.MONTH) Color.White else Color.Transparent)
                                    .clickable { viewMode = CalendarHistoryViewMode.MONTH }
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Month", style = MaterialTheme.typography.labelLarge, color = if (viewMode == CalendarHistoryViewMode.MONTH) Color.Black else Color.Gray)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(if (viewMode == CalendarHistoryViewMode.YEAR) Color.White else Color.Transparent)
                                    .clickable { viewMode = CalendarHistoryViewMode.YEAR }
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Year", style = MaterialTheme.typography.labelLarge, color = if (viewMode == CalendarHistoryViewMode.YEAR) Color.Black else Color.Gray)
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
                        CalendarFilter.ALL -> "All"
                        CalendarFilter.SAVINGS -> "💰"
                        CalendarFilter.EXPENSES -> "🧾"
                    }
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        OutlinedButton(
                            onClick = { filterExpanded = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
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
                    Text("Loading calendar...", style = MaterialTheme.typography.bodyMedium)
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
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
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
                                    text = year.toString(),
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
