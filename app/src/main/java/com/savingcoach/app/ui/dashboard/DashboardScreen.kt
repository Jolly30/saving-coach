package com.savingcoach.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import com.savingcoach.app.utils.InvestmentCalculations
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.unit.sp
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

import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.FloatingActionButton
import com.savingcoach.app.ui.chat.ChatScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToChallenges: (String?) -> Unit = {},
    onNavigateToCalendarHistory: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
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
            DashboardContent(
                uiState = uiState,
                onDateTap = viewModel::onDateTap,
                onFilterChange = viewModel::onFilterChange,
                onDismissTooltip = viewModel::dismissTooltip,
                onNavigateToChallenges = onNavigateToChallenges,
                onNavigateToCalendarHistory = onNavigateToCalendarHistory,
                onNavigateToNotifications = onNavigateToNotifications,
                onNavigateToSettings = onNavigateToSettings
            )
        }
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onDateTap: (String) -> Unit,
    onFilterChange: (CalendarFilter) -> Unit,
    onDismissTooltip: () -> Unit,
    onNavigateToChallenges: (String?) -> Unit,
    onNavigateToCalendarHistory: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val currency = uiState.currency
    val currencyPreference = uiState.currencyPreference
    val strings = com.savingcoach.app.ui.localization.AppLocale.current

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(onClick = onNavigateToSettings) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = strings.settingsTitle
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.dashboardTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            androidx.compose.material3.IconButton(onClick = onNavigateToNotifications) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = strings.notifications
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── 1. Summary Cards (2×2 grid) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "${strings.budget} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = false)})",
                value = strings.formatAmount(uiState.monthlyBudget, currencyPreference, 1.0, isInvestment = false),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            SummaryCard(
                title = "${strings.spent} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = false)})",
                value = strings.formatAmount(uiState.totalSpent, currencyPreference, 1.0, isInvestment = false),
                valueColor = when {
                    uiState.spentPercentage >= 100 -> Red
                    uiState.spentPercentage >= 90 -> Orange
                    uiState.spentPercentage >= 75 -> Yellow
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "${strings.remaining} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = false)})",
                value = strings.formatAmount(uiState.remainingBudget, currencyPreference, 1.0, isInvestment = false),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                valueColor = if (uiState.remainingBudget < 0) Red else MaterialTheme.colorScheme.onSurface
            )
            SummaryCard(
                title = "${strings.savings} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = false)})",
                value = strings.formatAmount(uiState.totalSaved, currencyPreference, 1.0, isInvestment = false),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── 2. Investment Portfolio ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "${strings.investmentValue} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = true)})",
                value = strings.formatAmount(uiState.investmentTotalCostBasis, currencyPreference, 1.0, isInvestment = true),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            SummaryCard(
                title = "${strings.rate} (MMK)",
                value = strings.formatNumber(InvestmentCalculations.formatCurrency(uiState.usdToMmkRate)),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 3. Challenges Carousel ──
        val activeCount = uiState.activeChallenges.size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🏆 ${strings.savingChallenges}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${strings.seeAll} ➔",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigateToChallenges(null) }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${strings.formatNumber(activeCount)} ${strings.activeLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.displayChallenges.isNotEmpty()) {
                uiState.displayChallenges.forEach { challenge ->
                    Box(modifier = Modifier.clickable { onNavigateToChallenges(challenge.id) }) {
                        ChallengeCarouselCard(
                            title = challenge.title,
                            currentAmount = challenge.currentAmount,
                            targetAmount = challenge.targetAmount,
                            progress = challenge.progress,
                            isActive = challenge.isActive,
                            currencyPreference = currencyPreference
                        )
                    }
                }
            } else if (uiState.hasCreatedAnyChallenge) {
                // Old user, no active challenges -> "Create new challenge"
                Box(modifier = Modifier.clickable { onNavigateToChallenges(null) }) {
                    ChallengeCarouselCard(
                        title = strings.createChallenge,
                        currentAmount = 0.0,
                        targetAmount = 0.0,
                        progress = 0.0,
                        isActive = false,
                        currencyPreference = ""
                    )
                }
            } else {
                // New user -> Default cards
                val emergencyTarget = if (currencyPreference == "USD" && uiState.usdToMmkRate > 0.0) 500000.0 / uiState.usdToMmkRate else 500000.0
                val vacationTarget = if (currencyPreference == "USD" && uiState.usdToMmkRate > 0.0) 300000.0 / uiState.usdToMmkRate else 300000.0
                Box(modifier = Modifier.clickable { onNavigateToChallenges(null) }) {
                    ChallengeCarouselCard(
                        title = strings.challengeEmergencyFund,
                        currentAmount = 0.0,
                        targetAmount = emergencyTarget,
                        progress = 0.0,
                        isActive = false,
                        currencyPreference = currencyPreference
                    )
                }
                Box(modifier = Modifier.clickable { onNavigateToChallenges(null) }) {
                    ChallengeCarouselCard(
                        title = strings.challengeVacation,
                        currentAmount = 0.0,
                        targetAmount = vacationTarget,
                        progress = 0.0,
                        isActive = false,
                        currencyPreference = currencyPreference
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // ── 4. Spending Calendar ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🗓️ ${strings.monthlyOverview}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${strings.seeAll} ➔",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigateToCalendarHistory() }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))


        Spacer(modifier = Modifier.height(8.dp))

        // Calendar with tooltip
        Box {
            CalendarHeatmap(
                dailySpending = uiState.dailySpending,
                dailySavings = uiState.dailySavings,
                savingTiers = uiState.savingTiers,
                expenseTiers = uiState.expenseTiers,
                investmentTiers = uiState.investmentTiers,
                dailyInvestments = uiState.dailyInvestments,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            com.savingcoach.app.ui.components.AutoScalingText(
                text = title,
                maxTextSize = 12.sp,
                minTextSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            com.savingcoach.app.ui.components.AutoScalingText(
                text = value,
                maxTextSize = 16.sp,
                minTextSize = 10.sp,
                color = valueColor,
                fontWeight = FontWeight.Bold
            )
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
    currencyPreference: String
) {
    Card(
        modifier = Modifier
            .requiredWidth(270.dp)
            .requiredHeight(135.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                if (isActive) {
                    val strings = com.savingcoach.app.ui.localization.AppLocale.current
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = strings.active,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (isActive) {
                val strings = com.savingcoach.app.ui.localization.AppLocale.current
                val displayCurrent = if (currencyPreference.isEmpty()) "" else strings.formatAmount(currentAmount, currencyPreference, 1.0, isInvestment = false)
                val displayTarget = if (currencyPreference.isEmpty()) "" else strings.formatAmount(targetAmount, currencyPreference, 1.0, isInvestment = false)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = displayCurrent,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "/ $displayTarget",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${strings.formatNumber(progress.toInt())}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                BudgetProgressBar(percentage = progress, showLabel = false, height = 8.dp)
            } else {
                val strings = com.savingcoach.app.ui.localization.AppLocale.current
                Text(
                    text = if (strings.save == "သိမ်းမည်") "စတင်ရန် နှိပ်ပါ" else "Tap to activate",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                BudgetProgressBar(percentage = 0.0, showLabel = false, height = 8.dp)
            }
        }
    }
}


