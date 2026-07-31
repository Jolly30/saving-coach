package com.savingcoach.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton

import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savingcoach.app.ui.components.BudgetProgressBar
import com.savingcoach.app.ui.components.LoadingOverlay
import com.savingcoach.app.ui.theme.Green
import com.savingcoach.app.ui.theme.Orange
import com.savingcoach.app.ui.theme.Red
import com.savingcoach.app.ui.theme.Yellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToChallenges: () -> Unit = {},
    onNavigateToCalendarHistory: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            scope.launch {
                viewModel.loadDashboard()
                delay(500)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            if (uiState.isLoading && !isRefreshing) {
                LoadingOverlay(isLoading = true, message = "Loading dashboard…")
            } else {
                DashboardContent(
                    uiState = uiState,
                    onDateTap = viewModel::onDateTap,
                    onFilterChange = viewModel::onFilterChange,
                    onDismissTooltip = viewModel::dismissTooltip,
                    onNavigateToChallenges = onNavigateToChallenges,
                    onNavigateToCalendarHistory = onNavigateToCalendarHistory,
                    onNavigateToNotifications = onNavigateToNotifications
                )
            }
        }
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onDateTap: (String) -> Unit,
    onFilterChange: (CalendarFilter) -> Unit,
    onDismissTooltip: () -> Unit,
    onNavigateToChallenges: () -> Unit,
    onNavigateToCalendarHistory: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val currency = uiState.currency

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            androidx.compose.material3.IconButton(onClick = onNavigateToNotifications) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 1. Summary Cards (2×2 grid) ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(title = "Budget", value = "${uiState.monthlyBudget.toInt()} $currency", modifier = Modifier.weight(1f))
            SummaryCard(
                title = "Spent", value = "${uiState.totalSpent.toInt()} $currency",
                valueColor = when {
                    uiState.spentPercentage >= 100 -> Red
                    uiState.spentPercentage >= 90 -> Orange
                    uiState.spentPercentage >= 75 -> Yellow
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(title = "Remaining", value = "${uiState.remainingBudget.toInt()} $currency", modifier = Modifier.weight(1f))
            SummaryCard(title = "Savings", value = "${uiState.totalSaved.toInt()} $currency", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 2. Monthly Budget Progress ──
        Text("Monthly Budget Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        BudgetProgressBar(percentage = uiState.spentPercentage, label = "${uiState.spentPercentage.toInt()}% of ${uiState.monthlyBudget.toInt()} $currency")

        Spacer(modifier = Modifier.height(24.dp))

        // ── 3. Challenges Carousel (defaults + active) ──
        if (uiState.displayChallenges.isNotEmpty()) {
            val activeCount = uiState.activeChallenges.size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏆 Saving Challenges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "See All ➔",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onNavigateToChallenges() }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (activeCount > 0) {
                Text(
                    text = "$activeCount active",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                uiState.displayChallenges.forEach { challenge ->
                    ChallengeCarouselCard(
                        title = challenge.title,
                        currentAmount = challenge.currentAmount,
                        targetAmount = challenge.targetAmount,
                        progress = challenge.progress,
                        isActive = challenge.isActive,
                        currency = currency
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // ── 4. Spending Calendar ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🗓️ Monthly Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "See All Months ➔",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigateToCalendarHistory() }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))


        // Active filter indicator
        if (uiState.calendarFilter != CalendarFilter.ALL) {
            Text(
                text = when (uiState.calendarFilter) {
                    CalendarFilter.SAVINGS -> "Showing days with savings deposits"
                    CalendarFilter.EXPENSES -> "Showing days with expenses"
                    else -> ""
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Calendar with tooltip
        Box {
            CalendarHeatmap(
                dailySpending = uiState.dailySpending,
                dailySavings = uiState.dailySavings,
                monthlyBudget = uiState.monthlyBudget,
                filter = uiState.calendarFilter,
                selectedDate = uiState.selectedDate,
                tooltipData = uiState.tooltipData,
                onDateTap = onDateTap,
                onFilterChange = onFilterChange,
                onDismissTooltip = onDismissTooltip
            )
        }
    }
}

// ── Summary Card ──
@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.Start) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

// ── Challenge Carousel Card ──
@Composable
private fun ChallengeCarouselCard(
    title: String,
    currentAmount: Double,
    targetAmount: Double,
    progress: Double,
    isActive: Boolean,
    currency: String
) {
    Card(
        modifier = Modifier.width(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isActive) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = Green,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (isActive) {
                Text(
                    text = "${currentAmount.toInt()} / ${targetAmount.toInt()} $currency",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                BudgetProgressBar(percentage = progress, showLabel = false, height = 8.dp)
            } else {
                Text(
                    text = "Tap to activate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                BudgetProgressBar(percentage = 0.0, showLabel = false, height = 8.dp)
            }
        }
    }
}


