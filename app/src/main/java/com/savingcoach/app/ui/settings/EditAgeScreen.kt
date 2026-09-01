package com.savingcoach.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAgeScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    var editingAge by remember { mutableStateOf(if (uiState.age == "Unknown" || uiState.age == "Loading...") "" else uiState.age) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    LaunchedEffect(uiState.age) {
        if (editingAge.isEmpty() && uiState.age != "Unknown" && uiState.age != "Loading...") {
            editingAge = uiState.age
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
                text = strings.editAgeTitle,
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
                text = strings.ageDesc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            OutlinedTextField(
                value = editingAge,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        editingAge = newValue
                        viewModel.clearError() 
                    }
                },
                label = { Text(strings.ageLabel) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedContainerColor = if (isDark) Color(0xFF222724) else Color(0xFFFAF7F0),
                    unfocusedContainerColor = if (isDark) Color(0xFF222724) else Color(0xFFFAF7F0),
                    focusedBorderColor = if (isDark) Color(0xFF81C784) else Color(0xFF336846),
                    unfocusedBorderColor = if (isDark) Color(0xFF38403A) else Color(0xFFE2DDD0),
                    focusedLabelColor = if (isDark) Color(0xFF81C784) else Color(0xFF336846),
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

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
                    val ageInt = editingAge.toIntOrNull()
                    if (ageInt != null) {
                        viewModel.updateAge(ageInt) {
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
                enabled = editingAge.isNotBlank() && editingAge != uiState.age
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
