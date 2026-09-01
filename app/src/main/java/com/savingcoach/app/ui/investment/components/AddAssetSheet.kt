package com.savingcoach.app.ui.investment.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.savingcoach.app.data.model.CoinGeckoCoin
import com.savingcoach.app.data.model.FinnhubResult
import com.savingcoach.app.data.model.UserHolding
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // State
    var selectedType by remember { mutableStateOf("stock") } // "stock" or "crypto"
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Any>>(emptyList()) }
    var selectedAsset by remember { mutableStateOf<Any?>(null) }
    var units by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }
    var currentPrice by remember { mutableStateOf<Double?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Dynamic accent color based on active asset type
    val activeColor = if (selectedType == "stock") {
        if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    } else {
        if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
    }
    val textFieldBorder = if (isDark) Color(0xFF38403A) else Color(0xFFE2DDD0)
    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = activeColor,
        unfocusedBorderColor = textFieldBorder,
        focusedLabelColor = activeColor,
        cursorColor = activeColor,
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
    )

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
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = if (isDark) Color(0xFF222724) else Color(0xFFFCFBF7)
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
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Type selector
            Text(
                text = strings.assetType,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val stockColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                val stockBg = if (isDark) Color(0xFF1E2A38) else Color(0xFFEFF6FF)
                val stockBorder = if (isDark) Color(0xFF2E4057) else Color(0xFFBFDBFE)

                val cryptoColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                val cryptoBg = if (isDark) Color(0xFF382C1E) else Color(0xFFFFFBEB)
                val cryptoBorder = if (isDark) Color(0xFF57412A) else Color(0xFFFDE68A)

                val inactiveBg = if (isDark) Color(0xFF272D29) else Color(0xFFEFECE2)
                val inactiveBorder = if (isDark) Color(0xFF38403A) else Color(0xFFE2DDD0)

                Surface(
                    onClick = {
                        selectedType = "stock"
                        selectedAsset = null
                        searchQuery = ""
                        searchResults = emptyList()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedType == "stock") stockBg else inactiveBg,
                    border = BorderStroke(
                        1.dp,
                        if (selectedType == "stock") stockBorder else inactiveBorder
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "📈 " + strings.stockOrEtf,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedType == "stock") FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedType == "stock") stockColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    onClick = {
                        selectedType = "crypto"
                        selectedAsset = null
                        searchQuery = ""
                        searchResults = emptyList()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedType == "crypto") cryptoBg else inactiveBg,
                    border = BorderStroke(
                        1.dp,
                        if (selectedType == "crypto") cryptoBorder else inactiveBorder
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "🪙 " + strings.crypto,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedType == "crypto") FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedType == "crypto") cryptoColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Search input
            Text(
                text = strings.searchAsset,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
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
                    when (asset) {
                        is CoinGeckoCoin -> searchQuery = "${asset.symbol.uppercase()} - ${asset.name}"
                        is FinnhubResult -> searchQuery = "${asset.symbol} - ${asset.description}"
                    }
                    searchResults = emptyList()
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
                },
                accentColor = activeColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Units input
            OutlinedTextField(
                value = units,
                onValueChange = { units = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.unitsOrSharesOwned) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = customTextFieldColors,
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
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = customTextFieldColors,
                enabled = selectedAsset != null
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Cost basis preview
            if (isFormValid) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF272D29) else Color(0xFFEFECE2)
                    ),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF38403A) else Color(0xFFE2DDD0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = strings.totalCostBasis,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val totalCost = (units.toDoubleOrNull() ?: 0.0) * (buyPrice.toDoubleOrNull() ?: 0.0)
                        Text(
                            text = InvestmentCalculations.formatValue(totalCost, currencyPreference, 1.0, isInvestment = true),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = activeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = isFormValid && !isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedType == "stock") {
                            if (isDark) Color(0xFF2563EB) else Color(0xFF2563EB)
                        } else {
                            if (isDark) Color(0xFFD97706) else Color(0xFFD97706)
                        },
                        disabledContainerColor = if (isDark) Color(0xFF272D29) else Color(0xFFEFECE2),
                        disabledContentColor = if (isDark) Color(0xFF555F57) else Color(0xFFA09B90)
                    )
                ) {
                    Text(strings.saveToPortfolio, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
