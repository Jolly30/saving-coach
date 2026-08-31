package com.savingcoach.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.savingcoach.app.export.ShareManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.savingcoach.app.data.repository.AppThemeMode
import com.savingcoach.app.data.repository.AppLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToEditUsername: () -> Unit = {},
    onNavigateToEditAge: () -> Unit = {},
    onNavigateToEditGender: () -> Unit = {},
    onNavigateToEditFieldOfWork: () -> Unit = {},
    onNavigateToEditSalary: () -> Unit = {},
    onNavigateToEditEmail: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToExportData: () -> Unit = {},
    onNavigateToEditCurrency: () -> Unit = {},
    onNavigateToEditLanguage: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    val context = LocalContext.current
    var showSignOutConfirm by remember { mutableStateOf(false) }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text(strings.signOutConfirmTitle) },
            text = { Text(strings.signOutConfirmMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        viewModel.signOut()
                        onNavigateToAuth()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.signOut, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    val notSetText = if (strings.save == "သိမ်းမည်") "မသတ်မှတ်ရသေးပါ" else "Not set"

    val displayUsername = if (uiState.username.isEmpty() || uiState.username == "Not set" || uiState.username == "Loading..." || uiState.username == "Unknown") {
        notSetText
    } else uiState.username

    val displayAge = if (uiState.age.isEmpty() || uiState.age == "Not set" || uiState.age == "Loading..." || uiState.age == "Unknown") {
        notSetText
    } else uiState.age

    val displayGender = when (uiState.gender) {
        "Male" -> strings.male
        "Female" -> strings.female
        "Prefer not to say" -> strings.preferNotToSay
        "Not set", "Unknown", "Loading..." -> notSetText
        else -> uiState.gender.ifEmpty { notSetText }
    }

    val displayFieldOfWork = when (uiState.fieldOfWork) {
        "Software Engineering" -> strings.fieldSoftware
        "Healthcare" -> strings.fieldHealthcare
        "Education" -> strings.fieldEducation
        "Finance" -> strings.fieldFinance
        "Marketing" -> strings.fieldMarketing
        "Design" -> strings.fieldDesign
        "Sales" -> strings.fieldSales
        "Business" -> strings.fieldBusiness
        "Other" -> strings.fieldOther
        "Not set", "Unknown", "Loading..." -> notSetText
        else -> uiState.fieldOfWork.ifEmpty { notSetText }
    }

    val displaySalary = when (uiState.salaryRange) {
        "Under 1,000,000 MMK" -> strings.salaryUnder1M
        "1,000,000 - 3,000,000 MMK" -> strings.salary1MTo3M
        "3,000,000 - 5,400,000 MMK" -> strings.salary3MTo54M
        "5,400,000 - 10,000,000 MMK" -> strings.salary54MTo10M
        "Above 10,000,000 MMK" -> strings.salaryAbove10M
        "Not set", "Unknown", "Loading..." -> notSetText
        else -> uiState.salaryRange.ifEmpty { notSetText }
    }

    val displayEmail = if (uiState.email.isEmpty() || uiState.email == "Not set" || uiState.email == "Loading...") {
        notSetText
    } else uiState.email

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.settingsTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.close,
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
            
            item {
                SettingsSection(title = strings.account) {
                    SettingsItem(
                        label = strings.username, 
                        value = displayUsername, 
                        onClick = onNavigateToEditUsername
                    )
                    SettingsDivider()
                    SettingsItem(label = strings.age, value = displayAge, onClick = onNavigateToEditAge)
                    SettingsDivider()
                    SettingsItem(label = strings.gender, value = displayGender, onClick = onNavigateToEditGender)
                    SettingsDivider()
                    SettingsItem(label = strings.fieldOfWork, value = displayFieldOfWork, onClick = onNavigateToEditFieldOfWork)
                    SettingsDivider()
                    SettingsItem(label = strings.salaryRange, value = displaySalary, onClick = onNavigateToEditSalary)
                    SettingsDivider()
                    SettingsItem(label = strings.email, value = displayEmail, onClick = onNavigateToEditEmail)
                    SettingsDivider()
                    SettingsItem(label = strings.resetPassword, value = "", onClick = onNavigateToChangePassword)
                }
                if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = if (uiState.error == strings.passwordResetSent) Color(0xFF009688) else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            item {
                SettingsSection(title = strings.data) {
                    SettingsItem(
                        label = strings.exportDataCsv,
                        value = "",
                        onClick = { onNavigateToExportData() }
                    )
                }
            }

            item {
                SettingsSection(title = strings.preferences) {
                    SettingsThemeItem(
                        selectedMode = uiState.themeMode,
                        themeLabel = strings.theme,
                        lightLabel = strings.light,
                        darkLabel = strings.dark,
                        onSelectMode = { viewModel.setThemeMode(it) }
                    )
                    SettingsDivider()
                    val displayCurrency = when (uiState.currencyPreference) {
                        "USD" -> strings.currencyUSDLabel
                        "mixed" -> strings.currencyMixedLabel
                        else -> strings.currencyMMKLabel
                    }
                    SettingsItem(label = strings.currency, value = displayCurrency, onClick = onNavigateToEditCurrency)
                    SettingsDivider()
                    val displayLanguage = when (uiState.language) {
                        AppLanguage.MY -> "မြန်မာ (Burmese)"
                        else -> "English"
                    }
                    SettingsItem(label = strings.language, value = displayLanguage, onClick = onNavigateToEditLanguage)
                    SettingsDivider()
                    SettingsSwitchItem(label = strings.notifications, initialValue = true, onCheckedChange = {})
                }
            }

            item {
                SettingsSection(title = strings.aboutAndInfo) {
                    SettingsItem(label = strings.about, value = "", onClick = {})
                    SettingsDivider()
                    SettingsItem(label = strings.version, value = "v1.0.0", showArrow = false, onClick = {})
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = {
                            showSignOutConfirm = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDB3A3E) // Red button color
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 48.dp, vertical = 14.dp)
                    ) {
                        Text(strings.signOut, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsItem(
    label: String,
    value: String = "",
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (showArrow) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsThemeItem(
    selectedMode: AppThemeMode,
    themeLabel: String = "Theme",
    lightLabel: String = "Light",
    darkLabel: String = "Dark",
    onSelectMode: (AppThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = themeLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        // Segmented Control
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeOption(
                label = lightLabel,
                icon = Icons.Default.LightMode,
                selected = selectedMode == AppThemeMode.LIGHT,
                onClick = { onSelectMode(AppThemeMode.LIGHT) }
            )
            ThemeOption(
                label = darkLabel,
                icon = Icons.Default.DarkMode,
                selected = selectedMode == AppThemeMode.DARK,
                onClick = { onSelectMode(AppThemeMode.DARK) }
            )
        }
    }
}

@Composable
fun ThemeOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun SettingsSwitchItem(
    label: String,
    initialValue: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    var checked by remember { mutableStateOf(initialValue) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.surface,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        thickness = 0.8.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
