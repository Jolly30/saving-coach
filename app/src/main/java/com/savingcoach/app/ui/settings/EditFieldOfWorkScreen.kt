package com.savingcoach.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun EditFieldOfWorkScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    var selectedField by remember { mutableStateOf(if (uiState.fieldOfWork == "Unknown" || uiState.fieldOfWork == "Loading...") "" else uiState.fieldOfWork) }
    val backgroundColor = MaterialTheme.colorScheme.background
    
    val fieldOptions = listOf(
        Pair("Software Engineering", strings.fieldSoftware),
        Pair("Healthcare", strings.fieldHealthcare),
        Pair("Education", strings.fieldEducation),
        Pair("Finance", strings.fieldFinance),
        Pair("Marketing", strings.fieldMarketing),
        Pair("Design", strings.fieldDesign),
        Pair("Sales", strings.fieldSales),
        Pair("Business", strings.fieldBusiness),
        Pair("Other", strings.fieldOther)
    )

    LaunchedEffect(uiState.fieldOfWork) {
        if (selectedField.isEmpty() && uiState.fieldOfWork != "Unknown" && uiState.fieldOfWork != "Loading...") {
            selectedField = uiState.fieldOfWork
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
                text = strings.editFieldOfWorkTitle,
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = strings.selectFieldOfWork,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    fieldOptions.forEachIndexed { index, (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    selectedField = key 
                                    viewModel.clearError()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            RadioButton(
                                selected = (selectedField == key),
                                onClick = { 
                                    selectedField = key 
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
                        if (index < fieldOptions.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                thickness = 1.dp
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
                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = {
                    if (selectedField != null) {
                        viewModel.updateFieldOfWork(selectedField!!) {
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(24.dp),
                enabled = selectedField != null && selectedField != uiState.fieldOfWork
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
