package com.savingcoach.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCurrencyScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    var selectedCurrency by remember { mutableStateOf("") }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val cardBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF242925),
                Color(0xFF1D211E),
                Color(0xFF161917)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFBF9F2),
                Color(0xFFF5F1E6)
            )
        )
    }

    val options = listOf(
        Pair("MMK", strings.currencyMMKLabel),
        Pair("USD", strings.currencyUSDLabel),
        Pair("mixed", strings.currencyMixedLabel)
    )

    LaunchedEffect(uiState.currencyPreference) {
        if (selectedCurrency.isEmpty() && uiState.currencyPreference.isNotEmpty()) {
            selectedCurrency = uiState.currencyPreference
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack, 
                    contentDescription = strings.back,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = strings.editCurrencyTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = strings.selectCurrency,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF38403A) else Color(0xFFE5E0CE))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardBrush)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        options.forEachIndexed { index, (value, label) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        selectedCurrency = value
                                        viewModel.clearError() 
                                    }
                                    .padding(vertical = 15.dp, horizontal = 18.dp)
                            ) {
                                RadioButton(
                                    selected = (selectedCurrency == value),
                                    onClick = { 
                                        selectedCurrency = value 
                                        viewModel.clearError()
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = if (isDark) Color(0xFF81C784) else Color(0xFF336846),
                                        unselectedColor = if (isDark) Color(0xFF6E7B73) else Color(0xFFA59F91)
                                    )
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                            if (index < options.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 18.dp),
                                    color = if (isDark) Color(0xFF2B322D) else Color(0xFFEDE8DD),
                                    thickness = 0.8.dp
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(28.dp))
            }

            Button(
                onClick = {
                    if (selectedCurrency.isNotEmpty()) {
                        viewModel.updateCurrencyPreference(selectedCurrency) {
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF3B6E4A) else Color(0xFF336846),
                    disabledContainerColor = if (isDark) Color(0xFF222724) else Color(0xFFEDE9DF),
                    contentColor = Color.White,
                    disabledContentColor = if (isDark) Color(0xFF5A665E) else Color(0xFFA39E92)
                ),
                shape = RoundedCornerShape(20.dp),
                enabled = selectedCurrency.isNotEmpty() && selectedCurrency != uiState.currencyPreference
            ) {
                Text(
                    text = strings.save,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
