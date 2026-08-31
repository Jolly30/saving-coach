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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSalaryScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    var selectedRange by remember { mutableStateOf(if (uiState.salaryRange == "Unknown" || uiState.salaryRange == "Loading...") "" else uiState.salaryRange) }
    val backgroundColor = MaterialTheme.colorScheme.background
    
    val salaryOptions = listOf(
        Pair("Under 1,000,000 MMK", strings.salaryUnder1M),
        Pair("1,000,000 - 3,000,000 MMK", strings.salary1MTo3M),
        Pair("3,000,000 - 5,400,000 MMK", strings.salary3MTo54M),
        Pair("5,400,000 - 10,000,000 MMK", strings.salary54MTo10M),
        Pair("Above 10,000,000 MMK", strings.salaryAbove10M)
    )

    LaunchedEffect(uiState.salaryRange) {
        if (selectedRange.isEmpty() && uiState.salaryRange != "Unknown" && uiState.salaryRange != "Loading...") {
            selectedRange = uiState.salaryRange
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
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
                text = strings.editSalaryTitle,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = strings.selectSalaryRange,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    salaryOptions.forEachIndexed { index, (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedRange = key
                                    viewModel.clearError() 
                                }
                                .padding(vertical = 14.dp, horizontal = 16.dp)
                        ) {
                            RadioButton(
                                selected = (selectedRange == key),
                                onClick = { 
                                    selectedRange = key 
                                    viewModel.clearError()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                        if (index < salaryOptions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 0.8.dp
                            )
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
                Spacer(modifier = Modifier.height(32.dp))
            }

            Button(
                onClick = {
                    if (selectedRange != null) {
                        viewModel.updateSalaryRange(selectedRange!!) {
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(24.dp),
                enabled = selectedRange != null && selectedRange != uiState.salaryRange
            ) {
                Text(
                    text = strings.save,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            }
        }
    }
}
