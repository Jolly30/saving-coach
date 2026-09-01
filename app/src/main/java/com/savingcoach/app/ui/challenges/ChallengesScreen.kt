package com.savingcoach.app.ui.challenges

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.TextStyle
import androidx.hilt.navigation.compose.hiltViewModel

import com.savingcoach.app.ui.theme.*
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.ChallengeTemplate
import com.savingcoach.app.data.model.ChallengeStatus
import com.savingcoach.app.ui.components.ChallengeCard
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

enum class ChallengeFilter(val displayName: String) {
    ALL("All"),
    ACTIVE("Active"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    STOPPED("Stopped")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(
    initialChallengeId: String? = null,
    viewModel: ChallengeViewModel = hiltViewModel(),
    onChallengeClick: (String) -> Unit = {},
    onCreateChallengeClick: () -> Unit = {}
) {
    var selectedChallengeId by rememberSaveable { mutableStateOf(initialChallengeId) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var editingChallenge by remember { mutableStateOf<SavingChallenge?>(null) }
    var showCompletedPopup by remember { mutableStateOf(false) }
    var showStoppedPopup by remember { mutableStateOf(false) }
    var stoppedChallengeData by remember { mutableStateOf<SavingChallenge?>(null) }
    var showFailedPopup by remember { mutableStateOf(false) }
    var failedChallengeData by remember { mutableStateOf<SavingChallenge?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(ChallengeFilter.ALL) }
    var isFilterMenuExpanded by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    val strings = com.savingcoach.app.ui.localization.AppLocale.current

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.reconcileAllActiveChallenges()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedChallengeId != null) {
            ChallengeDetailScreen(
                challengeId = selectedChallengeId!!,
                onBackClick = { selectedChallengeId = null },
                onSettingsClick = { challenge -> editingChallenge = challenge }
            )
        } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(
                            text = strings.challengesTitle,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    windowInsets = WindowInsets(0.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                val heroBrush = if (isDark) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF38403A) else Color(0xFFE5E0CE))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(heroBrush)
                            .padding(22.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${strings.totalSaved.uppercase()} (${com.savingcoach.app.utils.InvestmentCalculations.getCurrencyLabel(uiState.currencyPreference, isInvestment = false)})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0xFF2E3830) else Color(0xFFE8EFE8),
                                    border = BorderStroke(1.dp, if (isDark) Color(0xFF3F4E42) else Color(0xFFD0E0D2))
                                ) {
                                    Text(
                                        text = "🏆 ${strings.allTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                com.savingcoach.app.ui.components.AutoScalingText(
                                    text = strings.formatAmount(uiState.totalSaved, uiState.currencyPreference, 1.0, isInvestment = false),
                                    maxTextSize = 38.sp,
                                    minTextSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = strings.yourChallenges.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        placeholder = {
                            Text(
                                strings.searchChallenges,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) Color(0xFF242925) else Color(0xFFEFECE2),
                            unfocusedContainerColor = if (isDark) Color(0xFF242925) else Color(0xFFEFECE2),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        textStyle = TextStyle(fontSize = 14.sp)
                    )

                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        val currentFilterLabel = when (selectedFilter) {
                            ChallengeFilter.ALL -> strings.all
                            ChallengeFilter.ACTIVE -> strings.active
                            ChallengeFilter.COMPLETED -> strings.completed
                            ChallengeFilter.FAILED -> strings.failed
                            ChallengeFilter.STOPPED -> strings.stopped
                        }
                        Surface(
                            onClick = { isFilterMenuExpanded = true },
                            modifier = Modifier.fillMaxHeight(),
                            shape = RoundedCornerShape(18.dp),
                            color = if (isDark) Color(0xFF242925) else Color(0xFFEFECE2),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF38403A) else Color(0xFFE2DDD0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = currentFilterLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Filter",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isFilterMenuExpanded,
                            onDismissRequest = { isFilterMenuExpanded = false },
                            containerColor = if (isDark) Color(0xFF242925) else Color(0xFFFCFBF7),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF38403A) else Color(0xFFE5E0CE)),
                            modifier = Modifier.widthIn(min = 170.dp)
                        ) {
                            ChallengeFilter.values().forEach { filterOption ->
                                val isSelected = selectedFilter == filterOption
                                val optionLabel = when (filterOption) {
                                    ChallengeFilter.ALL -> strings.all
                                    ChallengeFilter.ACTIVE -> strings.active
                                    ChallengeFilter.COMPLETED -> strings.completed
                                    ChallengeFilter.FAILED -> strings.failed
                                    ChallengeFilter.STOPPED -> strings.stopped
                                }
                                DropdownMenuItem(
                                    modifier = Modifier
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) (if (isDark) Color(0xFF2F3831) else Color(0xFFE8EFE8)) else Color.Transparent),
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = optionLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) (if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F)) else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = if (isDark) Color(0xFF81C784) else Color(0xFF2E6B4F),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedFilter = filterOption
                                        isFilterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.challengesList.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        ) {
                            Text(
                                text = "🎯",
                                fontSize = 64.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = strings.noChallengesYet,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = strings.noChallengesDesc,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    showCreateSheet = true
                                    onCreateChallengeClick()
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.createChallenge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    val displayList = uiState.challengesList.filter { challenge ->
                        val matchesSearch = searchQuery.isBlank() || challenge.title.contains(searchQuery, ignoreCase = true)
                        val matchesStatus = when (selectedFilter) {
                            ChallengeFilter.ALL -> true
                            ChallengeFilter.ACTIVE -> challenge.status == ChallengeStatus.ACTIVE
                            ChallengeFilter.COMPLETED -> challenge.status == ChallengeStatus.COMPLETED
                            ChallengeFilter.FAILED -> challenge.status == ChallengeStatus.FAILED
                            ChallengeFilter.STOPPED -> challenge.status == ChallengeStatus.STOPPED
                        }
                        matchesSearch && matchesStatus
                    }.let { filtered ->
                        if (selectedFilter == ChallengeFilter.ALL) {
                            filtered.sortedBy { it.status != ChallengeStatus.ACTIVE }
                        } else {
                            filtered
                        }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 160.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayList) { challenge ->
                        val percent = if (challenge.template == ChallengeTemplate.NO_SPEND || challenge.targetAmount == 0.0) {
                            if (challenge.durationDays > 0) {
                                ((challenge.completedDaysCount.toFloat() / challenge.durationDays.toFloat()) * 100).toInt()
                            } else 0
                        } else {
                            if (challenge.targetAmount > 0) {
                                ((challenge.currentAmount / challenge.targetAmount) * 100).toInt()
                            } else 0
                        }

                        val displayChallenge = if (challenge.template == ChallengeTemplate.NO_SPEND || challenge.targetAmount == 0.0) {
                            val cleanTitle = if (challenge.title.contains("No-Spend Week", ignoreCase = true)) {
                                "🚫 No-Spend Day"
                            } else challenge.title
                            challenge.copy(
                                title = cleanTitle,
                                currentAmount = challenge.completedDaysCount.toDouble(),
                                targetAmount = challenge.durationDays.toDouble()
                            )
                        } else {
                            if (challenge.title.contains("No-Spend Week", ignoreCase = true)) {
                                challenge.copy(title = "🚫 No-Spend Day")
                            } else challenge
                        }
                        
                        ChallengeCard(
                            challenge = displayChallenge,
                            currencyPreference = uiState.currencyPreference,
                            onClick = {
                                when (challenge.status) {
                                    ChallengeStatus.COMPLETED -> showCompletedPopup = true
                                    ChallengeStatus.FAILED -> {
                                        failedChallengeData = challenge
                                        showFailedPopup = true
                                    }
                                    ChallengeStatus.STOPPED -> {
                                        stoppedChallengeData = challenge
                                        showStoppedPopup = true
                                    }
                                    ChallengeStatus.ACTIVE -> selectedChallengeId = challenge.id
                                }
                            }
                        )
                    }
                }
                }
            }

            // Floating Action Button pinned to Bottom-Right
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        showCreateSheet = true
                        onCreateChallengeClick()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Challenge",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

            if (showCreateSheet) {
                CreateChallengeScreen(
                    viewModel = viewModel,
                    onDismiss = { showCreateSheet = false }
                )
            }
        }

        if (editingChallenge != null) {
            EditChallengeScreen(
                challenge = editingChallenge!!,
                viewModel = viewModel,
                onDismiss = { editingChallenge = null },
                onDelete = {
                    editingChallenge = null
                    selectedChallengeId = null
                },
                onStop = { challenge ->
                    editingChallenge = null
                    selectedChallengeId = null
                    stoppedChallengeData = challenge
                    showStoppedPopup = true
                }
            )
        }
        if (showCompletedPopup) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {
                        detectTapGestures { }
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(32.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            strings.challengeCompletedTitle,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            strings.challengeCompletedMsg,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showCompletedPopup = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                strings.ok,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Stopped Challenge Dialog
        if (showStoppedPopup && stoppedChallengeData != null) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {
                        detectTapGestures { }
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(32.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            strings.challengeStoppedTitle,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            strings.localizeChallengeTitle(stoppedChallengeData!!.title),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            strings.stoppedChallengeRecorded(strings.formatAmount(stoppedChallengeData!!.currentAmount, uiState.currencyPreference, 1.0, isInvestment = false)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            strings.cannotDepositStoppedMsg,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                showStoppedPopup = false
                                stoppedChallengeData = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                strings.ok,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Failed Challenge Dialog
        if (showFailedPopup && failedChallengeData != null) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {
                        detectTapGestures { }
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(32.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            strings.challengeFailedTitle,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            strings.localizeChallengeTitle(failedChallengeData!!.title),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            strings.challengeFailedMsg,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                showFailedPopup = false
                                failedChallengeData = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                strings.ok,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
