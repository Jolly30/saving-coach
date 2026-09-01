package com.savingcoach.app.ui.investment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.savingcoach.app.data.model.FinnhubNewsResponse
import com.savingcoach.app.ui.investment.components.AddAssetSheet
import com.savingcoach.app.ui.investment.components.HoldingCard
import com.savingcoach.app.ui.investment.components.InvestmentSearchScreen
import com.savingcoach.app.ui.investment.components.MarketNewsCard
import com.savingcoach.app.ui.investment.components.PortfolioSummaryCard
import com.savingcoach.app.ui.theme.AccentGreen
import com.savingcoach.app.ui.theme.PrimaryBlue
import com.savingcoach.app.utils.InvestmentCalculations

/**
 * Main Investment screen composable.
 * Displays portfolio summary, holdings list, and market news.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: InvestmentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Handle errors
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }



    if (uiState.isSearchScreenOpen) {
        InvestmentSearchScreen(
            uiState = uiState,
            onBackClick = { viewModel.setSearchScreenOpen(false) },
            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
            onSearchTabChange = { viewModel.updateSearchTab(it) },
            onDeleteHolding = { viewModel.deleteHolding(it) },
            onStopHolding = { holdingId, exitPrice -> viewModel.stopHolding(holdingId, exitPrice) }
        )
    } else {
        Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.investmentTitle,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {

                    IconButton(onClick = { viewModel.setSearchScreenOpen(true) }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowAddAssetSheet(true) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = strings.addAsset,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // Portfolio Summary Card
            PortfolioSummaryCard(summary = uiState.portfolioSummary, currencyPreference = uiState.currencyPreference)

            Spacer(modifier = Modifier.height(8.dp))

            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

            // Tab row
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (uiState.selectedTab.ordinal < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                            height = 3.dp,
                            color = if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F)
                        )
                    }
                },
                divider = {
                    HorizontalDivider(color = if (isDark) Color(0xFF38403A) else Color(0xFFE5E0CE))
                }
            ) {
                InvestmentTab.entries.forEach { tab ->
                    val isSelected = uiState.selectedTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                text = when (tab) {
                                    InvestmentTab.ALL -> strings.allHoldings
                                    InvestmentTab.STOCKS -> strings.stocksAndEtfs
                                    InvestmentTab.CRYPTO -> strings.crypto
                                    InvestmentTab.NEWS -> strings.marketNews
                                },
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) (if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F)) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            // Content based on selected tab
            when (uiState.selectedTab) {
                InvestmentTab.NEWS -> {
                    // Market News
                    if (uiState.marketNews.isEmpty()) {
                        EmptyState(
                            message = strings.loadingMarketNews,
                            isLoading = true
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(uiState.marketNews) { news ->
                                MarketNewsCard(news = news)
                            }
                        }
                    }
                }

                else -> {
                    // Holdings list
                    if (uiState.filteredHoldings.isEmpty() && !uiState.isRefreshing) {
                        EmptyState(
                            message = strings.noInvestmentsLogged,
                            isLoading = false
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = uiState.filteredHoldings,
                                key = { it.holding.id }
                            ) { computedHolding ->
                                HoldingCard(
                                    computedHolding = computedHolding,
                                    usdRate = uiState.usdRate,
                                    currencyPreference = uiState.currencyPreference,
                                    onDeleteClick = {
                                        viewModel.deleteHolding(computedHolding.holding.id)
                                    },
                                    onStopClick = { exitPrice ->
                                        viewModel.stopHolding(computedHolding.holding.id, exitPrice)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            }
        } // Close Column

        // Add Asset Bottom Sheet
        if (uiState.showAddAssetSheet) {
            AddAssetSheet(
                usdRate = uiState.usdRate,
                currencyPreference = uiState.currencyPreference,
                onDismiss = { viewModel.setShowAddAssetSheet(false) },
                onAssetAdded = { holding -> viewModel.addHolding(holding) },
                searchCrypto = { query ->
                    viewModel.searchCryptoDirect(query)
                },
                searchStocks = { query ->
                    viewModel.searchStocksDirect(query)
                },
                fetchCryptoPrice = { coinId ->
                    viewModel.fetchCryptoPrice(coinId)
                },
                fetchStockPrice = { symbol ->
                    viewModel.fetchStockPrice(symbol)
                }
            )
        }
    }
    }
}

@Composable
private fun EmptyState(
    message: String,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}


