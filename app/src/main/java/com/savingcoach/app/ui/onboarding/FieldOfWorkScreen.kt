package com.savingcoach.app.ui.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldOfWorkScreen(
    onNavigateNext: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var selectedField by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val fieldOptions = listOf(
        "Technology / IT",
        "Healthcare",
        "Education",
        "Finance",
        "Business / Management",
        "Freelance / Self-employed",
        "Student",
        "Other",
        "Rather not answer"
    )

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "What is your field of work?",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedField ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { 
                        Text("Select field of work", color = MaterialTheme.colorScheme.onSurfaceVariant) 
                    },
                    trailingIcon = { 
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    fieldOptions.forEach { option ->
                        val isSelected = selectedField == option
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = option, 
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                ) 
                            },
                            onClick = {
                                selectedField = option
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    viewModel.saveFieldOfWork(selectedField) {
                        onNavigateNext()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = selectedField != null
            ) {
                Text("Next")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = {
                    viewModel.saveFieldOfWork(null) {
                        onNavigateNext()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip")
            }
        }
    }
}
