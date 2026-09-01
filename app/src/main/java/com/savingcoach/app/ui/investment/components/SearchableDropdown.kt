package com.savingcoach.app.ui.investment.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Reusable searchable dropdown with debounced input.
 * Triggers search callback after 300ms of user inactivity.
 */
@Composable
fun <T> SearchableDropdown(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<T>,
    onItemSelected: (T) -> Unit,
    itemContent: @Composable (T) -> Unit,
    placeholder: String = "Search...",
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val borderColor = if (isDark) Color(0xFF38403A) else Color(0xFFE2DDD0)
    val cardBg = if (isDark) Color(0xFF272D29) else Color(0xFFFFFFFF)
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Search input
        OutlinedTextField(
            value = query,
            onValueChange = { newValue ->
                onQueryChange(newValue)
                isDropdownExpanded = newValue.isNotEmpty()
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        onQueryChange("")
                        isDropdownExpanded = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = borderColor,
                focusedLabelColor = accentColor,
                cursorColor = accentColor,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        // Dropdown results
        if (isDropdownExpanded && searchResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBg
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    searchResults.take(15).forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider(color = if (isDark) Color(0xFF333A35) else Color(0xFFF0ECE1))
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onItemSelected(item)
                                    isDropdownExpanded = false
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            itemContent(item)
                        }
                    }
                }
            }
        }

        // Show "No results" message
        if (isDropdownExpanded && query.isNotEmpty() && searchResults.isEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBg
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, borderColor)
            ) {
                Text(
                    text = "No results found for \"$query\"",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Debounced search effect that triggers after specified delay.
 */
@Composable
fun DebouncedSearch(
    query: String,
    delayMs: Long = 300L,
    onSearch: (String) -> Unit
) {
    LaunchedEffect(query) {
        delay(delayMs)
        onSearch(query)
    }
}

/**
 * Search result item for crypto assets.
 */
@Composable
fun CryptoSearchResultItem(
    id: String,
    name: String,
    symbol: String,
    thumbnailUrl: String? = null
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (isDark) Color(0xFF382C1E) else Color(0xFFFFFBEB),
                    RoundedCornerShape(10.dp)
                )
                .border(
                    1.dp,
                    if (isDark) Color(0xFF57412A) else Color(0xFFFDE68A),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🪙",
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = symbol.uppercase(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Search result item for stock/ETF assets.
 */
@Composable
fun StockSearchResultItem(
    symbol: String,
    description: String,
    type: String
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (isDark) Color(0xFF1E2A38) else Color(0xFFEFF6FF),
                    RoundedCornerShape(10.dp)
                )
                .border(
                    1.dp,
                    if (isDark) Color(0xFF2E4057) else Color(0xFFBFDBFE),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (type == "ETP") "📊" else "📈",
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = symbol,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
