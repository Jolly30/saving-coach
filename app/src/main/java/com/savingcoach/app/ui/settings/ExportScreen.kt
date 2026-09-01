package com.savingcoach.app.ui.settings

import android.app.DatePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savingcoach.app.export.ShareManager
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var selectedType by remember { mutableStateOf("Spending") }
    var startDateStr by remember { mutableStateOf<String?>(null) }
    var endDateStr by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val selectedCategories = remember { mutableStateListOf<String>() }
    val selectedChallengeIds = remember { mutableStateListOf<String>() }
    val selectedHoldingIds = remember { mutableStateListOf<String>() }

    fun localizeCategory(cat: String): String = strings.localizeCategory(cat)

    LaunchedEffect(uiState.exportFile) {
        uiState.exportFile?.let { file ->
            ShareManager.shareCsvFile(context, file)
            viewModel.clearExportFile()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
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
                    text = strings.exportDataTitle, 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold, 
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        val exportType = when (selectedType) {
                            "Spending" -> "Expenses"
                            "Savings" -> "Savings"
                            else -> "Investments"
                        }
                        viewModel.exportData(
                            context = context,
                            type = exportType,
                            startDateStr = startDateStr,
                            endDateStr = endDateStr,
                            selectedCategories = selectedCategories.toList(),
                            selectedChallengeIds = selectedChallengeIds.toList(),
                            selectedHoldingIds = selectedHoldingIds.toList()
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF3B6E4A) else Color(0xFF336846),
                        disabledContainerColor = if (isDark) Color(0xFF222724) else Color(0xFFEDE9DF),
                        contentColor = Color.White
                    )
                ) {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(strings.exportToCsv, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Segmented Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) Color(0xFF161A17) else Color(0xFFECE7DB))
                        .border(1.dp, if (isDark) Color(0xFF2C342E) else Color(0xFFDDD7C8), RoundedCornerShape(18.dp))
                        .padding(3.dp)
                ) {
                    SegmentedButton(
                        text = strings.spendingTab,
                        isSelected = selectedType == "Spending",
                        modifier = Modifier.weight(1f),
                        onClick = { 
                            selectedType = "Spending"
                            searchQuery = ""
                        }
                    )
                    SegmentedButton(
                        text = strings.savingsTab,
                        isSelected = selectedType == "Savings",
                        modifier = Modifier.weight(1f),
                        onClick = { 
                            selectedType = "Savings"
                            searchQuery = ""
                        }
                    )
                    SegmentedButton(
                        text = strings.investmentsTab,
                        isSelected = selectedType == "Investments",
                        modifier = Modifier.weight(1f),
                        onClick = { 
                            selectedType = "Investments"
                            searchQuery = ""
                        }
                    )
                }
            }

            // Date Range
            item {
                Column {
                    Text(
                        text = strings.dateRange.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        letterSpacing = 1.2.sp,
                        color = if (isDark) Color(0xFF81C784).copy(alpha = 0.85f) else Color(0xFF336846),
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DatePickerCard(
                            label = strings.from,
                            dateStr = startDateStr,
                            selectDateText = strings.selectDate,
                            onDateSelected = { startDateStr = it },
                            modifier = Modifier.weight(1f)
                        )
                        DatePickerCard(
                            label = strings.to,
                            dateStr = endDateStr,
                            selectDateText = strings.selectDate,
                            onDateSelected = { endDateStr = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (startDateStr != null || endDateStr != null) {
                        TextButton(
                            onClick = { 
                                startDateStr = null
                                endDateStr = null
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                strings.clearDates,
                                color = if (isDark) Color(0xFF81C784) else Color(0xFF336846),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Filters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (selectedType) {
                                "Spending" -> strings.filterByCategory
                                "Savings" -> strings.filterByChallenge
                                else -> strings.filterByInvestment
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.leaveUncheckedHint,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .width(140.dp)
                            .height(48.dp),
                        placeholder = { 
                            Text(
                                strings.searchPlaceholder, 
                                fontSize = 12.sp, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            ) 
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) Color(0xFF222724) else Color(0xFFFAF7F0),
                            unfocusedContainerColor = if (isDark) Color(0xFF222724) else Color(0xFFFAF7F0),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = if (isDark) Color(0xFF81C784) else Color(0xFF336846),
                            unfocusedBorderColor = if (isDark) Color(0xFF38403A) else Color(0xFFE2DDD0),
                            cursorColor = if (isDark) Color(0xFF81C784) else Color(0xFF336846)
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                    )
                }
            }

            if (selectedType == "Spending") {
                val filteredCategories = uiState.availableCategories.filter {
                    val localized = localizeCategory(it)
                    it.contains(searchQuery, ignoreCase = true) || localized.contains(searchQuery, ignoreCase = true)
                }
                items(filteredCategories) { category ->
                    SelectableItemCard(
                        title = localizeCategory(category),
                        isSelected = selectedCategories.contains(category),
                        onClick = {
                            if (selectedCategories.contains(category)) selectedCategories.remove(category)
                            else selectedCategories.add(category)
                        }
                    )
                }
            } else if (selectedType == "Savings") {
                val filteredChallenges = uiState.availableChallenges.filter {
                    it.title.contains(searchQuery, ignoreCase = true)
                }
                items(filteredChallenges) { challenge ->
                    SelectableItemCard(
                        title = challenge.title,
                        isSelected = selectedChallengeIds.contains(challenge.id),
                        onClick = {
                            if (selectedChallengeIds.contains(challenge.id)) selectedChallengeIds.remove(challenge.id)
                            else selectedChallengeIds.add(challenge.id)
                        }
                    )
                }
            } else {
                val filteredHoldings = uiState.availableHoldings.filter {
                    it.displayTicker.contains(searchQuery, ignoreCase = true) ||
                    it.name.contains(searchQuery, ignoreCase = true)
                }
                items(filteredHoldings) { holding ->
                    SelectableItemCard(
                        title = "${holding.displayTicker} - ${holding.name} (${if (holding.isStoppedCompat) "Sold Out" else "Active"})",
                        isSelected = selectedHoldingIds.contains(holding.id),
                        onClick = {
                            if (selectedHoldingIds.contains(holding.id)) selectedHoldingIds.remove(holding.id)
                            else selectedHoldingIds.add(holding.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SegmentedButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val selectedBg = if (isDark) Color(0xFF2D4636) else Color(0xFF386848)
    val selectedText = if (isDark) Color(0xFF81C784) else Color.White
    val unselectedText = if (isDark) Color(0xFF7C8880) else Color(0xFF8C8578)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) selectedBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            color = if (isSelected) selectedText else unselectedText, 
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.5.sp
        )
    }
}

@Composable
fun DatePickerCard(
    label: String,
    dateStr: String?,
    selectDateText: String = "Select Date",
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
    
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                val calendar = Calendar.getInstance()
                if (dateStr != null) {
                    try {
                        val parts = dateStr.split("-")
                        calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                    } catch (e: Exception) {}
                }
                
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                        onDateSelected(formattedDate)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF38403A) else Color(0xFFE5E0CE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(16.dp)
        ) {
            Column {
                Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = dateStr ?: selectDateText,
                    fontSize = 14.5.sp,
                    fontWeight = if (dateStr != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (dateStr != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun SelectableItemCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
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

    val selectedBg = if (isDark) Color(0xFF233528) else Color(0xFFE8F3EB)
    val selectedBorder = if (isDark) Color(0xFF43704C) else Color(0xFF6CA37C)
    val unselectedBorder = if (isDark) Color(0xFF38403A) else Color(0xFFE5E0CE)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, if (isSelected) selectedBorder else unselectedBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 0.dp else 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) Brush.linearGradient(listOf(selectedBg, selectedBg)) else cardBrush)
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) (if (isDark) Color(0xFF81C784) else Color(0xFF2D693F)) else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.5.sp
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = if (isDark) Color(0xFF81C784) else Color(0xFF2D693F),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
