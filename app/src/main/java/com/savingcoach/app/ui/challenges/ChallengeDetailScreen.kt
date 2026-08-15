package com.savingcoach.app.ui.challenges

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.ChallengeTemplate
import com.savingcoach.app.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

data class DepositItem(val amount: Double, val note: String, val time: String)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChallengeDetailScreen(
    challengeId: String,
    onBackClick: () -> Unit,
    onSettingsClick: (SavingChallenge) -> Unit,
    viewModel: ChallengeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val challenge = uiState.challengesList.find { it.id == challengeId }

    androidx.compose.runtime.LaunchedEffect(challengeId) {
        viewModel.selectChallenge(challengeId)
    }

    val isPreset100 = challengeId == "preset_2"
    val isPreset7Day = challengeId == "preset_3"
    val isPresetNoSpend = challengeId == "preset_4"
    val isPreset1K = challengeId == "preset_1"
    
    val is100 = challenge?.template == ChallengeTemplate.ENVELOPE || challenge?.title?.contains("100", ignoreCase = true) == true || isPreset100
    val is7Day = challenge?.template == ChallengeTemplate.FLEXI || challenge?.title?.contains("7-Day", ignoreCase = true) == true || isPreset7Day
    val isNoSpend = challenge?.template == ChallengeTemplate.NO_SPEND || challenge?.title?.contains("No-Spend", ignoreCase = true) == true || isPresetNoSpend
    
    var completedSteps by rememberSaveable(challengeId) {
        mutableIntStateOf(challenge?.completedDaysCount ?: 0)
    }

    var currentAmount by rememberSaveable(challengeId) {
        mutableDoubleStateOf(challenge?.currentAmount ?: 0.0)
    }

    var currentDaysLeft by rememberSaveable(challengeId) {
        mutableIntStateOf(challenge?.let {
            try {
                val end = java.time.LocalDate.parse(it.endDate)
                java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), end).toInt().coerceAtLeast(0)
            } catch (e: Exception) { 30 }
        } ?: 30)
    }

    // Sync local state with Firestore when challenge changes externally (e.g., from another device)
    // Only reset if the challenge has been updated after our last local update
    androidx.compose.runtime.LaunchedEffect(challenge?.id, challenge?.currentAmount, challenge?.completedDaysCount) {
        challenge?.let {
            // Only sync from Firestore if we haven't made local changes
            // This prevents resetting local state after user actions
            val localCompletedSteps = completedSteps
            val localCurrentAmount = currentAmount
            if (it.completedDaysCount > localCompletedSteps || it.currentAmount > localCurrentAmount) {
                // Firestore has newer data - sync from it
                completedSteps = it.completedDaysCount
                currentAmount = it.currentAmount
                currentDaysLeft = try {
                    val end = java.time.LocalDate.parse(it.endDate)
                    java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), end).toInt().coerceAtLeast(0)
                } catch (e: Exception) { 30 }
            }
        }
    }

    val totalSteps = when {
        is100 && challengeId.startsWith("preset_") -> 100
        else -> try {
            val parts = challenge?.lastDepositDate?.split("|") ?: emptyList()
            if (parts.size > 2) {
                parts[2].toIntOrNull() ?: 30
            } else {
                val start = java.time.LocalDate.parse(challenge?.startDate ?: java.time.LocalDate.now().toString())
                val end = java.time.LocalDate.parse(challenge?.endDate ?: java.time.LocalDate.now().plusDays(30).toString())
                java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(1)
            }
        } catch (e: Exception) {
            30
        }
    }
    
    val targetAmount = challenge?.targetAmount ?: 30000.0
    
    val depositAmount = if (totalSteps > 0) {
        targetAmount / totalSteps
    } else 1000.0
    
    val depositHistory = uiState.selectedChallengeDeposits.map {
        val timeStr = java.text.SimpleDateFormat("yyyy-MM-dd • hh:mm a", java.util.Locale.US).format(java.util.Date(it.createdAt))
        DepositItem(amount = it.amount, note = it.note, time = timeStr)
    }

    var showDayDialog by remember { mutableStateOf(false) }
    var selectedDayNumber by remember { mutableIntStateOf(1) }
    
    var showEnterAmountDialog by remember { mutableStateOf(false) }
    var customAmountInput by remember { mutableStateOf("") }
    
    var showCompletionDialog by remember { mutableStateOf(false) }
    var showFullMapSheet by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(challenge) {
        val c = challenge ?: return@LaunchedEffect
        if (!c.isCompleted) {
            val parts = c.lastDepositDate.split("|")
            val duration = if (parts.size > 2) parts[2].toIntOrNull() ?: 30 else 30
            val isActuallyCompleted = when (c.template) {
                com.savingcoach.app.data.model.ChallengeTemplate.FLEXI -> c.currentAmount >= c.targetAmount
                else -> c.completedDaysCount >= duration
            }
            if (isActuallyCompleted) {
                viewModel.completeChallenge(c.id)
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(challenge?.isCompleted) {
        if (challenge?.isCompleted == true) {
            showCompletionDialog = true
        }
    }



    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    var animatingEnvelopeIndex by remember { mutableStateOf<Int?>(null) }
    var showSurpriseDialog by remember { mutableStateOf(false) }
    var calculatedSurpriseAmount by remember { mutableDoubleStateOf(0.0) }
    
    val outerFormatter = NumberFormat.getNumberInstance(Locale.US)
    
    val todayDateString = java.time.LocalDate.now().toString()
    val lastDepositDateOnly = challenge?.lastDepositDate?.substringBefore("|") ?: ""
    val hasDepositedToday = lastDepositDateOnly == todayDateString

    if (showDayDialog) {
        // Reset selectedDayNumber to current active step when dialog opens
        androidx.compose.runtime.LaunchedEffect(Unit) {
            selectedDayNumber = completedSteps + 1
        }
        Dialog(onDismissRequest = { showDayDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { showDayDialog = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(if (isNoSpend) "HABIT TRACKER" else "SAVING", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isNoSpend) {
                        Text(
                            text = "Zero Spend Day $selectedDayNumber",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        val formattedDepositAmount = NumberFormat.getNumberInstance(Locale.US).format(depositAmount)
                        Text(
                            text = "$formattedDepositAmount MMK",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (completedSteps < totalSteps) {
                                completedSteps++
                                if (currentDaysLeft > 0) currentDaysLeft--
                                if (!isNoSpend) {
                                    currentAmount += depositAmount
                                    val depositNote = when {
                                        is7Day -> "Deposit"
                                        is100 -> "Saved Envelope #$completedSteps"
                                        else -> "Deposit"
                                    }
                                    viewModel.addDepositMock(challengeId, depositAmount, completedSteps, 1, depositNote)
                                    if (completedSteps >= totalSteps) {
                                        viewModel.completeChallenge(challengeId)
                                        showCompletionDialog = true
                                    }
                                } else {
                                    viewModel.addDepositMock(challengeId, 0.0, completedSteps, 1, "Day $completedSteps saved")
                                    if (completedSteps >= totalSteps) {
                                        viewModel.completeChallenge(challengeId)
                                        showCompletionDialog = true
                                    }
                                }
                            }
                            showDayDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChallengeActive),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Confirm", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
    
    if (showSurpriseDialog) {
        Dialog(onDismissRequest = { showSurpriseDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { showSurpriseDialog = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "ENVELOPE SURPRISE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "You opened an envelope and found:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "${outerFormatter.format(calculatedSurpriseAmount)} MMK",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ChallengeActive
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (completedSteps < totalSteps) {
                                completedSteps++
                                if (currentDaysLeft > 0) currentDaysLeft--
                                currentAmount += calculatedSurpriseAmount
                                val depositNote = "Saved Envelope #$selectedDayNumber"
                                viewModel.addDepositMock(challengeId, calculatedSurpriseAmount, completedSteps, 1, depositNote)
                                if (completedSteps >= totalSteps) {
                                    viewModel.completeChallenge(challengeId)
                                    showCompletionDialog = true
                                }
                            }
                            showSurpriseDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChallengeActive),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Save", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showEnterAmountDialog) {
        Dialog(onDismissRequest = { showEnterAmountDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enter amount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(
                            onClick = { showEnterAmountDialog = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = customAmountInput,
                        onValueChange = { customAmountInput = it.filter { char -> char.isDigit() }.take(10) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            val amount = customAmountInput.toDoubleOrNull() ?: 10000.0
                            if (completedSteps < totalSteps) {
                                completedSteps++
                                if (currentDaysLeft > 0) currentDaysLeft--
                                currentAmount += amount
                                viewModel.addDepositMock(challengeId, amount, completedSteps, 1, "Deposit")
                                if (currentAmount >= targetAmount || completedSteps >= totalSteps) {
                                    viewModel.completeChallenge(challengeId)
                                    showCompletionDialog = true
                                }
                            }
                            showEnterAmountDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ChallengeActive),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Confirm", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    val handleStepClick: (Int) -> Unit = { stepNumber ->
        if (stepNumber == -1) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("You've already completed today's check-in! Come back tomorrow.")
            }
        } else {
            selectedDayNumber = stepNumber
            if (is100) {
                val todayStr = java.time.LocalDate.now().toString()
                val lastDepositDateOnly100 = challenge?.lastDepositDate?.substringBefore("|") ?: ""
                val alreadyDeposited = lastDepositDateOnly100 == todayStr
                if (alreadyDeposited) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("You've already opened an envelope today! Come back tomorrow.")
                    }
                } else {
                    animatingEnvelopeIndex = stepNumber
                    val remainingEnvelopes = (totalSteps - completedSteps).coerceAtLeast(1)
                    val remainingAmount = (targetAmount - currentAmount).coerceAtLeast(0.0)

                    val surprise = if (remainingEnvelopes == 1) {
                        remainingAmount
                    } else {
                        val average = remainingAmount / remainingEnvelopes
                        val randomFactor = 0.7 + (Math.random() * 0.6)
                        val rawSurprise = average * randomFactor
                        // Round to nearest 1000 if amount is large enough, otherwise use raw value
                        if (rawSurprise >= 1000.0) {
                            ((rawSurprise / 1000.0).toInt() * 1000.0).coerceAtLeast(1000.0)
                        } else if (rawSurprise >= 100.0) {
                            ((rawSurprise / 100.0).toInt() * 100.0).coerceAtLeast(100.0)
                        } else {
                            rawSurprise.coerceAtLeast(1.0)
                        }
                    }

                    calculatedSurpriseAmount = if (remainingEnvelopes == 1) {
                        remainingAmount.coerceAtLeast(0.0)
                    } else {
                        val maxAllowed = (remainingAmount - ((remainingEnvelopes - 1) * 1.0)).coerceAtLeast(0.0)
                        surprise.coerceAtLeast(1.0).coerceAtMost(maxAllowed)
                    }
                }
            } else if (is7Day) {
                customAmountInput = ""
                showEnterAmountDialog = true
            } else {
                showDayDialog = true
            }
        }
    }

    ChallengeDetailScreenContent(
        is100 = is100,
        is7Day = is7Day,
        isNoSpend = isNoSpend,
        completedSteps = completedSteps,
        currentAmount = currentAmount,
        targetAmount = targetAmount,
        totalSteps = totalSteps,
        depositAmount = depositAmount,
        daysLeft = currentDaysLeft,
        depositHistory = depositHistory,
        challengeTitle = challenge?.title ?: "",
        animatingEnvelopeIndex = animatingEnvelopeIndex,
        onAnimationFinished = {
            showSurpriseDialog = true
            animatingEnvelopeIndex = null
        },
        onBackClick = onBackClick,
        onStepClick = handleStepClick,
        onSettingsClick = { challenge?.let { onSettingsClick(it) } },
        onDeleteClick = {
            viewModel.deleteChallenge(challengeId)
            onBackClick()
        },
        onSeeMoreClick = { showFullMapSheet = true },
        snackbarHostState = snackbarHostState,
        hasDepositedToday = hasDepositedToday
    )
    
    if (showCompletionDialog) {
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
                color = DarkSlate,
                modifier = Modifier.padding(32.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.material3.Text(
                        "🎉 Challenge Completed! 🎉", 
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, 
                        color = Color.White, 
                        fontSize = 20.sp, 
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Text(
                        "Amazing job! You have successfully reached your saving goal.", 
                        color = MutedGray, 
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    androidx.compose.material3.Button(
                        onClick = { 
                            showCompletionDialog = false
                            onBackClick()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ChallengeActive),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        androidx.compose.material3.Text(
                            "OK", 
                            color = Color.White, 
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            com.savingcoach.app.ui.components.ConfettiView(modifier = Modifier.fillMaxSize())
        }
    }

    if (showFullMapSheet) {
        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = false)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showFullMapSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.onBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                androidx.compose.material3.Text(
                    text = "Full Progress Map",
                    fontSize = 18.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val outlineColor = MaterialTheme.colorScheme.outline
                val surfaceColor = MaterialTheme.colorScheme.onBackground

                LazyVerticalGrid(
                    columns = GridCells.Fixed(if (is100) 10 else 6),
                    horizontalArrangement = Arrangement.spacedBy(if (is100) 8.dp else 12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (is100) 8.dp else 12.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                ) {
                    items(totalSteps) { index ->
                        val isDone = index < completedSteps
                        val isActiveStep = index == completedSteps && !hasDepositedToday
                        val isDisabled = index > completedSteps || hasDepositedToday

                        if (is100) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        color = when {
                                            isDone -> ChallengeActive
                                            isActiveStep -> ChallengeActiveTrack
                                            else -> ChallengeInactive
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        if (isDone) {
                                            // Do nothing
                                        } else if (isDisabled) {
                                            showFullMapSheet = false
                                            handleStepClick(-1)
                                        } else {
                                            showFullMapSheet = false
                                            handleStepClick(index + 1)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!isDone) {
                                    Canvas(modifier = Modifier.matchParentSize()) {
                                        drawCircle(
                                            color = if (isActiveStep) outlineColor else MutedGray,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = 1.5.dp.toPx(),
                                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                            )
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isDone) Icons.Default.Check else Icons.Outlined.Email,
                                    contentDescription = null,
                                    tint = if (isDone) surfaceColor else MutedGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        color = when {
                                            isDone -> ChallengeActive
                                            isActiveStep -> ChallengeActiveTrack
                                            else -> ChallengeInactive
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        if (isDone) {
                                        } else if (isDisabled) {
                                            showFullMapSheet = false
                                            handleStepClick(-1)
                                        } else {
                                            showFullMapSheet = false
                                            handleStepClick(index + 1)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDone) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = surfaceColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Canvas(modifier = Modifier.matchParentSize()) {
                                        drawCircle(
                                            color = if (isActiveStep) outlineColor else MutedGray,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = 1.5.dp.toPx(),
                                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                            )
                                        )
                                    }
                                    androidx.compose.material3.Text(
                                        text = "${index + 1}",
                                        color = if (isActiveStep) MaterialTheme.colorScheme.onSurface else MutedGray,
                                        fontSize = 12.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChallengeDetailScreenContent(
    is100: Boolean,
    is7Day: Boolean,
    isNoSpend: Boolean,
    completedSteps: Int,
    currentAmount: Double,
    targetAmount: Double,
    totalSteps: Int,
    depositAmount: Double,
    daysLeft: Int,
    depositHistory: List<DepositItem>,
    challengeTitle: String,
    animatingEnvelopeIndex: Int?,
    onAnimationFinished: () -> Unit,
    onBackClick: () -> Unit,
    onStepClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSeeMoreClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    hasDepositedToday: Boolean
) {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    
    val firstWord = challengeTitle.split(" ").firstOrNull() ?: ""
    val isEmoji = firstWord.isNotEmpty() && firstWord.any { !it.isLetterOrDigit() }
    
    val emoji = if (isEmoji) {
        firstWord
    } else {
        when {
            is100 -> "✉️"
            is7Day -> "⚡"
            isNoSpend -> "🚫"
            else -> "🎯"
        }
    }
    
    val cleanTitle = if (isEmoji) {
        val stripped = challengeTitle.removePrefix(firstWord).trim()
        if (stripped.isEmpty()) challengeTitle else stripped
    } else {
        challengeTitle.ifBlank {
            when {
                is100 -> "100 Envelope"
                is7Day -> "7-Day Sprint"
                isNoSpend -> "No-Spend Day"
                else -> "1K a Day"
            }
        }
    }
    
    val description = when {
        is100 -> "Pick an envelope, save the number"
        is7Day -> "One intense week of saving"
        isNoSpend -> "Zero non-essentials for 7 days"
        else -> "A custom saving streak to hit your goal"
    }
    
    val formattedCurrent = formatter.format(currentAmount)
    val formattedTarget = formatter.format(targetAmount)
    
    val calculatedPercent = if (totalSteps > 0) {
        ((completedSteps.toFloat() / totalSteps.toFloat()) * 100).toInt()
    } else {
        0
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Header Section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(top = 8.dp, bottom = 24.dp)
                ) {
                        Column {
                            // Back arrow + Title + Settings — all same row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onBackClick,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Text(
                                    text = cleanTitle,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )

                                Box {
                                    var showMenu by remember { mutableStateOf(false) }
                                    var showDeleteDialog by remember { mutableStateOf(false) }

                                    IconButton(
                                        onClick = { showMenu = true },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit Challenge", color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                showMenu = false
                                                onSettingsClick()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete Challenge", color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                showMenu = false
                                                showDeleteDialog = true
                                            }
                                        )
                                    }

                                    if (showDeleteDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showDeleteDialog = false },
                                            title = { Text("Delete Challenge", color = MaterialTheme.colorScheme.onSurface) },
                                            text = { Text("Are you sure you want to delete this challenge? This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            confirmButton = {
                                                TextButton(onClick = {
                                                    showDeleteDialog = false
                                                    onDeleteClick()
                                                }) {
                                                    Text("Delete", color = MaterialTheme.colorScheme.error)
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showDeleteDialog = false }) {
                                                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Description
                            Text(
                                text = description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Progress text
                            if (isNoSpend) {
                                Text(
                                    text = "$completedSteps of $totalSteps days completed",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            } else {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)) {
                                            append(formattedCurrent)
                                        }
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = FontWeight.Bold)) {
                                            append("/$formattedTarget MMK")
                                        }
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Progress bar
                            val progressFloat = when {
                                isNoSpend && totalSteps > 0 -> (completedSteps.toFloat() / totalSteps.toFloat())
                                !isNoSpend && targetAmount > 0 -> (currentAmount / targetAmount).toFloat()
                                else -> 0f
                            }.coerceIn(0f, 1f)
                            
                            LinearProgressIndicator(
                                progress = { progressFloat },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (completedSteps >= totalSteps || currentAmount >= targetAmount) Orange else MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
                                strokeCap = StrokeCap.Round
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Subtext Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$calculatedPercent% complete",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (completedSteps >= totalSteps || currentAmount >= targetAmount) "Completed" else "$daysLeft days left",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                

                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )
                }

                // PROGRESS MAP
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                        Text(
                            text = "PROGRESS MAP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val windowSize = 30
                            val isCapped = totalSteps > windowSize
                            val startIndex = if (isCapped) {
                                val pageIndex = minOf(completedSteps, maxOf(0, totalSteps - 1)) / windowSize
                                pageIndex * windowSize
                            } else {
                                0
                            }
                            val remainingSteps = totalSteps - startIndex
                            val displayCount = if (isCapped) minOf(windowSize, remainingSteps) else totalSteps

                            if (is100) {
                                EnvelopeMap(
                                    completedSteps = completedSteps,
                                    totalSteps = totalSteps,
                                    animatingIndex = animatingEnvelopeIndex,
                                    onAnimationFinished = onAnimationFinished,
                                    hasDepositedToday = hasDepositedToday,
                                    onStepClick = onStepClick,
                                    startIndex = startIndex,
                                    displayCount = displayCount
                                )
                            } else if ((is7Day || isNoSpend) && totalSteps == 7) {
                                SevenDayMap(
                                    completedSteps = completedSteps,
                                    isNoSpend = isNoSpend,
                                    hasDepositedToday = hasDepositedToday,
                                    onStepClick = onStepClick
                                )
                            } else {
                                DotGridMap(
                                    completedSteps = completedSteps,
                                    totalSteps = totalSteps,
                                    hasDepositedToday = hasDepositedToday,
                                    onStepClick = onStepClick,
                                    startIndex = startIndex,
                                    displayCount = displayCount
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = isCapped) { if (isCapped) onSeeMoreClick() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$completedSteps of $totalSteps steps done",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isCapped) ChallengeActive else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }

                if (!isNoSpend) {
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                            thickness = 1.dp
                        )
                    }

                    // Deposit History List
                    item {
                        Text(
                            text = "DEPOSIT HISTORY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }

                    items(depositHistory) { deposit ->
                        DepositHistoryItem(
                            amount = deposit.amount,
                            note = deposit.note,
                            date = deposit.time
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

@Composable
fun DepositHistoryItem(amount: Double, note: String, date: String) {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Green dot indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(ChallengeActive, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = date,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
        Text(
            text = "+${formatter.format(amount)} MMK",
            color = ChallengeActive,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EnvelopeMap(
    completedSteps: Int,
    totalSteps: Int,
    animatingIndex: Int?,
    onAnimationFinished: () -> Unit,
    hasDepositedToday: Boolean,
    onStepClick: (Int) -> Unit,
    startIndex: Int = 0,
    displayCount: Int = totalSteps
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface

    FlowRow(
        maxItemsInEachRow = 10,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(displayCount) { i ->
            val index = startIndex + i
            if (index >= totalSteps) return@repeat
            val isDone = index < completedSteps
            val isActiveStep = index == completedSteps && !hasDepositedToday
            val isDisabled = index > completedSteps || hasDepositedToday
            val isCurrentlyAnimating = index + 1 == animatingIndex
            
            val animRotateY by animateFloatAsState(
                targetValue = if (isCurrentlyAnimating) 180f else 0f,
                animationSpec = tween(durationMillis = 600),
                finishedListener = {
                    if (isCurrentlyAnimating) {
                        onAnimationFinished()
                    }
                }
            )
            val animScale by animateFloatAsState(
                targetValue = if (isCurrentlyAnimating) 1.2f else 1.0f,
                animationSpec = tween(durationMillis = 600)
            )
            
            val scale = if (isCurrentlyAnimating) animScale else 1.0f
            val rotation = if (isCurrentlyAnimating) animRotateY else 0f
            
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .graphicsLayer {
                        this.scaleX = scale
                        this.scaleY = scale
                        this.rotationY = rotation
                        cameraDistance = 8 * density
                    }
                    .background(
                        color = when {
                            isDone -> ChallengeActive
                            isActiveStep -> ChallengeActiveTrack
                            else -> ChallengeInactive
                        },
                        shape = CircleShape
                    )
                    .clickable {
                        if (isDone) {
                            // Do nothing
                        } else if (isDisabled) {
                            onStepClick(-1)
                        } else {
                            onStepClick(index + 1)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!isDone) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = if (isActiveStep) outlineColor else MutedGray,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        )
                    }
                }
                Icon(
                    imageVector = if (isDone) Icons.Default.Check else Icons.Outlined.Email,
                    contentDescription = null,
                    tint = if (isDone) surfaceColor else MutedGray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun SevenDayMap(completedSteps: Int, isNoSpend: Boolean, hasDepositedToday: Boolean, onStepClick: (Int) -> Unit) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(7) { index ->
            val isDone = index < completedSteps
            val isActiveStep = index == completedSteps && !hasDepositedToday
            val isDisabled = index > completedSteps || hasDepositedToday
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        color = when {
                            isDone -> ChallengeActive
                            isActiveStep -> ChallengeActiveTrack
                            else -> ChallengeInactive
                        },
                        shape = CircleShape
                    )
                    .let {
                        if (isDone) {
                            it.border(2.dp, ChallengeActive, CircleShape)
                        } else {
                            it
                        }
                    }
                    .clickable {
                        if (isDone) {
                            // Do nothing
                        } else if (isDisabled) {
                            onStepClick(-1)
                        } else {
                            onStepClick(index + 1)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (!isDone) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = if (isActiveStep) outlineColor else MutedGray,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )
                    }
                }
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = surfaceColor,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    if (isNoSpend) {
                        Text(
                            text = "${index + 1}",
                            color = if (isActiveStep) MaterialTheme.colorScheme.surface else MutedGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "🔥",
                            fontSize = 16.sp,
                            modifier = Modifier.graphicsLayer { alpha = if (isActiveStep) 1f else 0.4f }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DotGridMap(
    completedSteps: Int,
    totalSteps: Int,
    hasDepositedToday: Boolean,
    onStepClick: (Int) -> Unit,
    startIndex: Int = 0,
    displayCount: Int = totalSteps
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceColor = MaterialTheme.colorScheme.surface

    FlowRow(
        maxItemsInEachRow = 6, // Match screenshot (6 columns x 5 rows = 30)
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(displayCount) { i ->
            val index = startIndex + i
            if (index >= totalSteps) return@repeat
            val isDone = index < completedSteps
            val isActiveStep = index == completedSteps && !hasDepositedToday
            val isDisabled = index > completedSteps || hasDepositedToday
            
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        color = when {
                            isDone -> ChallengeActive
                            isActiveStep -> ChallengeActiveTrack
                            else -> ChallengeInactive
                        },
                        shape = CircleShape
                    )
                    .clickable {
                        if (isDone) {
                            // Do nothing
                        } else if (isDisabled) {
                            onStepClick(-1)
                        } else {
                            onStepClick(index + 1)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = surfaceColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = if (isActiveStep) outlineColor else MutedGray,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )
                    }
                    Text(
                        text = "${index + 1}",
                        color = if (isActiveStep) MaterialTheme.colorScheme.onSurface else MutedGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
