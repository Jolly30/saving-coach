package com.savingcoach.app.ui.investment.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.savingcoach.app.data.model.CoinGeckoCoin
import com.savingcoach.app.data.model.FinnhubResult
import com.savingcoach.app.data.model.UserHolding
import com.savingcoach.app.ui.theme.AccentGreen
import com.savingcoach.app.ui.theme.PrimaryBlue
import com.savingcoach.app.utils.InvestmentCalculations
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Bottom sheet for adding a new asset to the portfolio.
 * Flow: Type toggle → Search → Select → Enter units/price → Preview → Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssetSheet(
    usdRate: Double,
    currencyPreference: String = "MMK",
    onDismiss: () -> Unit,
    onAssetAdded: (UserHolding) -> Unit,
    searchCrypto: suspend (String) -> List<CoinGeckoCoin>,
    searchStocks: suspend (String) -> List<FinnhubResult>,
    fetchCryptoPrice: suspend (String) -> Double?,
    fetchStockPrice: suspend (String) -> Double?,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // State
    var selectedType by remember { mutableStateOf("stock") } // "stock" or "crypto"
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Any>>(emptyList()) }
    var selectedAsset by remember { mutableStateOf<Any?>(null) }
    var units by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var currentPrice by remember { mutableStateOf<Double?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Derived state
    val unitsValue = units.toDoubleOrNull() ?: 0.0
    val buyPriceValue = buyPrice.toDoubleOrNull() ?: 0.0
    val costBasis = InvestmentCalculations.calculateCostBasis(unitsValue, buyPriceValue)
    val isFormValid = unitsValue > 0 && buyPriceValue > 0

    DebouncedSearch(
        query = searchQuery,
        onSearch = { query ->
            if (query.isNotEmpty() && selectedAsset == null) {
                scope.launch {
                    val results = if (selectedType == "crypto") {
                        searchCrypto(query)
                    } else {
                        searchStocks(query)
                    }
                    searchResults = results
                }
            } else if (query.isEmpty()) {
                searchResults = emptyList()
            }
        }
    )

    val strings = com.savingcoach.app.ui.localization.AppLocale.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = strings.addAsset,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Type selector
            Text(
                text = strings.assetType,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == "stock",
                    onClick = {
                        selectedType = "stock"
                        selectedAsset = null
                        searchQuery = ""
                        searchResults = emptyList()
                    },
                    label = { Text(strings.stockOrEtf) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.1f),
                        selectedLabelColor = PrimaryBlue
                    )
                )

                FilterChip(
                    selected = selectedType == "crypto",
                    onClick = {
                        selectedType = "crypto"
                        selectedAsset = null
                        searchQuery = ""
                        searchResults = emptyList()
                    },
                    label = { Text(strings.crypto) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentGreen.copy(alpha = 0.1f),
                        selectedLabelColor = AccentGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search input
            Text(
                text = strings.searchAsset,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            SearchableDropdown(
                query = searchQuery,
                onQueryChange = { query ->
                    searchQuery = query
                    selectedAsset = null
                },
                searchResults = searchResults,
                onItemSelected = { asset ->
                    selectedAsset = asset
                    // Set search query to selected asset name
                    when (asset) {
                        is CoinGeckoCoin -> searchQuery = "${asset.symbol.uppercase()} - ${asset.name}"
                        is FinnhubResult -> searchQuery = "${asset.symbol} - ${asset.description}"
                    }
                    searchResults = emptyList()
                    // Fetch current price
                    scope.launch {
                        isLoading = true
                        currentPrice = when (asset) {
                            is CoinGeckoCoin -> fetchCryptoPrice(asset.id)
                            is FinnhubResult -> fetchStockPrice(asset.symbol)
                            else -> null
                        }
                        isLoading = false
                    }
                },
                itemContent = { asset ->
                    when (asset) {
                        is CoinGeckoCoin -> CryptoSearchResultItem(
                            id = asset.id,
                            name = asset.name,
                            symbol = asset.symbol
                        )
                        is FinnhubResult -> StockSearchResultItem(
                            symbol = asset.symbol,
                            description = asset.description,
                            type = asset.type
                        )
                    }
                },
                placeholder = if (selectedType == "crypto") {
                    strings.typeCryptoPlaceholder
                } else {
                    strings.typeStockPlaceholder
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Units input
            OutlinedTextField(
                value = units,
                onValueChange = { units = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.unitsOrSharesOwned) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = selectedAsset != null
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Buy price input
            OutlinedTextField(
                value = buyPrice,
                onValueChange = { buyPrice = it.filter { char -> char.isDigit() || char == '.' }.take(15) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("${strings.avgBuyPrice} (${InvestmentCalculations.getCurrencyLabel(currencyPreference, isInvestment = true)})") },
                supportingText = {
                    if (currentPrice != null) {
                        val isMmk = currencyPreference == "MMK"
                        val displayPrice = if (isMmk) currentPrice!! * usdRate else currentPrice!!
                        val displayPriceText = InvestmentCalculations.formatValue(displayPrice, currencyPreference, 1.0, isInvestment = true)
                        val displayPriceUsd = if (isMmk) " (~$${String.format("%,.2f", currentPrice!!)})" else ""
                        Text("Current: $displayPriceText$displayPriceUsd")
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                enabled = selectedAsset != null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cost basis preview
            if (isFormValid) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = strings.totalCostBasis,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val totalCost = (units.toDoubleOrNull() ?: 0.0) * (buyPrice.toDoubleOrNull() ?: 0.0)
                    Text(
                        text = InvestmentCalculations.formatValue(totalCost, currencyPreference, 1.0, isInvestment = true),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(strings.cancel)
                }

                Button(
                    onClick = {
                        val holding = when (val asset = selectedAsset) {
                            is CoinGeckoCoin -> UserHolding(
                                id = UUID.randomUUID().toString(),
                                type = "crypto",
                                symbol = asset.id,
                                displayTicker = asset.symbol.uppercase(),
                                name = asset.name,
                                units = unitsValue,
                                buyPrice = buyPriceValue,
                                date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                            )
                            is FinnhubResult -> UserHolding(
                                id = UUID.randomUUID().toString(),
                                type = "stock",
                                symbol = asset.symbol,
                                displayTicker = asset.symbol,
                                name = asset.description,
                                units = unitsValue,
                                buyPrice = buyPriceValue,
                                date = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                            )
                            else -> return@Button
                        }
                        onAssetAdded(holding)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isFormValid && !isLoading
                ) {
                    Text(strings.saveToPortfolio)
                }
            }
        }
    }
}
