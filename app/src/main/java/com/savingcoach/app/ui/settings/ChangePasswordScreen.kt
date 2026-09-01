package com.savingcoach.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var oldPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

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
                text = strings.changePasswordTitle,
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
                text = "Keep your account secure by using a strong password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = if (isDark) Color(0xFF222724) else Color(0xFFFAF7F0),
                unfocusedContainerColor = if (isDark) Color(0xFF222724) else Color(0xFFFAF7F0),
                focusedBorderColor = if (isDark) Color(0xFF81C784) else Color(0xFF336846),
                unfocusedBorderColor = if (isDark) Color(0xFF38403A) else Color(0xFFE2DDD0),
                focusedLabelColor = if (isDark) Color(0xFF81C784) else Color(0xFF336846),
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = if (isDark) Color(0xFF81C784) else Color(0xFF336846)
            )

            OutlinedTextField(
                value = oldPassword,
                onValueChange = { 
                    oldPassword = it
                    viewModel.clearError()
                },
                label = { Text(strings.currentPassword) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    val image = if (oldPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                        Icon(
                            imageVector = image, 
                            contentDescription = if (oldPasswordVisible) "Hide password" else "Show password",
                            tint = if (isDark) Color(0xFF6E7B73) else Color(0xFFA59F91)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = { 
                    newPassword = it
                    viewModel.clearError()
                },
                label = { Text(strings.newPassword) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                trailingIcon = {
                    val image = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(
                            imageVector = image, 
                            contentDescription = if (newPasswordVisible) "Hide password" else "Show password",
                            tint = if (isDark) Color(0xFF6E7B73) else Color(0xFFA59F91)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors
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

            val isButtonEnabled = oldPassword.isNotBlank() && newPassword.isNotBlank() && !uiState.isLoading
            Button(
                onClick = {
                    if (oldPassword.isNotBlank() && newPassword.isNotBlank()) {
                        viewModel.changePassword(oldPassword, newPassword) {
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
                enabled = isButtonEnabled
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = strings.save,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onNavigateToForgotPassword,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Forgot old password?",
                    color = if (isDark) Color(0xFF81C784) else Color(0xFF336846),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
