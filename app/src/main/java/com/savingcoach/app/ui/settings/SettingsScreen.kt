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
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
    onNavigateToAbout: () -> Unit = {},
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
                        viewModel.signOut {
                            onNavigateToAuth()
                        }
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
                    SettingsItem(label = strings.about, value = "", onClick = onNavigateToAbout)
                    SettingsDivider()
                    SettingsItem(label = strings.version, value = "v1.0.0", showArrow = false, onClick = onNavigateToAbout)
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

    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = if (isDark) Color(0xFF81C784).copy(alpha = 0.85f) else Color(0xFF336846),
            fontWeight = FontWeight.Bold,
            fontSize = 11.5.sp,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 8.dp, start = 6.dp)
        )
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, if (isDark) Color(0xFF38403A) else Color(0xFFE5E0CE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBrush)
            ) {
                Column(content = content)
            }
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (showArrow) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF6E7B73) else Color(0xFFA59F91),
                    modifier = Modifier.size(18.dp)
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = themeLabel,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        // Segmented Control
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDark) Color(0xFF161A17) else Color(0xFFECE7DB))
                .border(1.dp, if (isDark) Color(0xFF2C342E) else Color(0xFFDDD7C8), RoundedCornerShape(18.dp))
                .padding(3.dp),
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val selectedBg = if (isDark) Color(0xFF2D4636) else Color(0xFF386848)
    val selectedContent = if (isDark) Color(0xFF81C784) else Color.White
    val unselectedContent = if (isDark) Color(0xFF7C8880) else Color(0xFF8C8578)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) selectedBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = if (selected) selectedContent else unselectedContent
        )
        Text(
            text = label,
            fontSize = 12.5.sp,
            color = if (selected) selectedContent else unselectedContent,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = it
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = if (isDark) Color(0xFF43704C) else Color(0xFF336846),
                uncheckedThumbColor = if (isDark) Color(0xFF7C8880) else Color(0xFFB5AFA4),
                uncheckedTrackColor = if (isDark) Color(0xFF1E2420) else Color(0xFFE5DEC8)
            )
        )
    }
}

@Composable
fun SettingsDivider() {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    HorizontalDivider(
        color = if (isDark) Color(0xFF2B322D) else Color(0xFFEDE8DD),
        thickness = 0.8.dp,
        modifier = Modifier.padding(horizontal = 18.dp)
    )
}
