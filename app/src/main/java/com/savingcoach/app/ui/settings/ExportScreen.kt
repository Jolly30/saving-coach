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
import androidx.compose.ui.graphics.Color
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
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryTextColor = MaterialTheme.colorScheme.onBackground
    val borderColor = MaterialTheme.colorScheme.outlineVariant

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
                    .background(backgroundColor)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                }
                Text(
                    text = strings.exportDataTitle, 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, 
                    color = primaryTextColor,
                    modifier = Modifier.weight(1f).offset(x = (-24).dp), // Offset by half the icon width to center it
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
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
                        .height(56.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(strings.exportToCsv, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Segmented Toggle
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                        .padding(4.dp)
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
                        text = strings.dateRange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                color = MaterialTheme.colorScheme.primary,
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
                            fontSize = 14.sp,
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
                            .height(50.dp),
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
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            cursorColor = MaterialTheme.colorScheme.primary
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
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "bg"
    )
    val textColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, label = "text"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text, 
            color = textColor, 
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
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
    
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = dateStr ?: selectDateText,
                fontSize = 15.sp,
                fontWeight = if (dateStr != null) FontWeight.Bold else FontWeight.Normal,
                color = if (dateStr != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun SelectableItemCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = if (isSelected) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp
            )
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
