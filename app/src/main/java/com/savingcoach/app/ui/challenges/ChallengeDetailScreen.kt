package com.savingcoach.app.ui.challenges

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.savingcoach.app.R
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
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.cos
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
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

private val EMOJI_REGEX = Regex("[\\x{1F300}-\\x{1F5FF}\\x{1F900}-\\x{1F9FF}\\x{1F600}-\\x{1F64F}\\x{1F680}-\\x{1F6FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}\\x{1F1E6}-\\x{1F1FF}\\x{1F191}-\\x{1F251}\\x{1F004}\\x{1F0CF}\\x{1F170}-\\x{1F171}\\x{1F17E}-\\x{1F17F}\\x{1F18E}\\x{3030}\\x{2B50}\\x{2B55}\\x{2934}-\\x{2935}\\x{2B05}-\\x{2B07}\\x{2B1B}-\\x{2B1C}\\x{3297}\\x{3299}\\x{303D}\\x{00A9}\\x{00AE}\\x{2122}\\x{23F3}\\x{24C2}\\x{23E9}-\\x{23EF}\\x{25B6}\\x{23F8}-\\x{23FA}\\x{1FA70}-\\x{1FAFF}]")

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
    val strings = com.savingcoach.app.ui.localization.AppLocale.current

    androidx.compose.runtime.LaunchedEffect(challengeId) {
        viewModel.selectChallenge(challengeId)
        viewModel.autoSkipMissedDays(challengeId)
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
            (it.durationDays - it.completedDaysCount).coerceAtLeast(0)
        } ?: 30)
    }

    // Sync local state with Firestore when challenge changes
    androidx.compose.runtime.LaunchedEffect(challenge?.id, challenge?.currentAmount, challenge?.completedDaysCount) {
        challenge?.let {
            completedSteps = it.completedDaysCount
            currentAmount = it.currentAmount
            currentDaysLeft = (it.durationDays - it.completedDaysCount).coerceAtLeast(0)
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
        val timeStr = if (it.note.equals("Skipped", ignoreCase = true)) {
            "${it.date} • ${strings.missed}"
        } else {
            strings.formatExpenseDateTime(it.createdAt, it.date)
        }
        DepositItem(amount = it.amount, note = it.note, time = timeStr)
    }

    val skippedIndices = remember(uiState.selectedChallengeDeposits) {
        uiState.selectedChallengeDeposits
            .sortedWith(compareBy<com.savingcoach.app.data.model.SavingsDeposit> { it.date }.thenBy { it.createdAt })
            .mapIndexedNotNull { index, deposit ->
                if (deposit.note.contains("Skipped", ignoreCase = true)) index else null
            }.toSet()
    }

    var showDayDialog by remember { mutableStateOf(false) }
    var selectedDayNumber by remember { mutableIntStateOf(1) }
    
    var showEnterAmountDialog by remember { mutableStateOf(false) }
    var customAmountInput by remember { mutableStateOf("") }
    
    var showCompletionDialog by remember { mutableStateOf(false) }
    var isFailedLocal by remember { mutableStateOf(false) }
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
    var hasShownDialogForThisChallenge by remember(challenge?.id) { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(challenge?.id, challenge?.isCompleted, challenge?.status) {
        if (!hasShownDialogForThisChallenge) {
            if (challenge?.status == com.savingcoach.app.data.model.ChallengeStatus.COMPLETED || challenge?.isCompleted == true) {
                showCompletionDialog = true
                isFailedLocal = false
                hasShownDialogForThisChallenge = true
            } else if (challenge?.status == com.savingcoach.app.data.model.ChallengeStatus.FAILED) {
                showCompletionDialog = true
                isFailedLocal = true
                hasShownDialogForThisChallenge = true
            }
        }
    }



    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    var showEnvelopeAnimation by remember { mutableStateOf(false) }
    var calculatedSurpriseAmount by remember { mutableDoubleStateOf(0.0) }
    
    val outerFormatter = NumberFormat.getNumberInstance(Locale.US)
    
    val todayDateString = java.time.LocalDate.now().toString()
    val lastDepositDateOnly = challenge?.lastDepositDate?.substringBefore("|") ?: ""
    val hasDepositedToday = completedSteps > 0 && lastDepositDateOnly == todayDateString

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
                            Icon(Icons.Default.Close, contentDescription = strings.close, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(if (isNoSpend) strings.habitTracker else strings.savings.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isNoSpend) {
                        Text(
                            text = strings.zeroSpendDayTitle(selectedDayNumber),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        Text(
                            text = strings.formatAmount(depositAmount, uiState.currencyPreference, 1.0, isInvestment = false),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            val nextSteps = completedSteps + 1
                            if (nextSteps <= totalSteps) {
                                if (!isNoSpend) {
                                    val depositNote = when {
                                        is7Day -> strings.deposit
                                        is100 -> strings.savedEnvelopeNumber(nextSteps)
                                        else -> strings.deposit
                                    }
                                    viewModel.addDepositMock(challengeId, depositAmount, nextSteps, 1, depositNote)
                                    val willReachTarget = (currentAmount + depositAmount) >= (targetAmount - 0.01)
                                    val willCompleteSteps = nextSteps >= totalSteps
                                    if (willReachTarget || willCompleteSteps) {
                                        isFailedLocal = if (isNoSpend || is100) false else (!willReachTarget && willCompleteSteps)
                                        viewModel.completeChallenge(challengeId)
                                        showCompletionDialog = true
                                    }
                                } else {
                                    viewModel.addDepositMock(challengeId, 0.0, nextSteps, 1, strings.zeroSpendDayTitle(nextSteps))
                                    if (nextSteps >= totalSteps) {
                                        isFailedLocal = false
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
                        Text(strings.confirm, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
    
    if (showEnvelopeAnimation) {
        com.savingcoach.app.ui.components.EnvelopeAnimationDialog(
            amount = calculatedSurpriseAmount,
            currencyPreference = uiState.currencyPreference,
            onDismiss = { showEnvelopeAnimation = false },
            onSaveClick = {
                val nextSteps = completedSteps + 1
                if (nextSteps <= totalSteps) {
                    val depositNote = strings.savedEnvelopeNumber(selectedDayNumber)
                    viewModel.addDepositMock(challengeId, calculatedSurpriseAmount, nextSteps, 1, depositNote)
                    val willReachTarget = (currentAmount + calculatedSurpriseAmount) >= (targetAmount - 0.01)
                    val willCompleteSteps = nextSteps >= totalSteps
                    if (willReachTarget || willCompleteSteps) {
                        isFailedLocal = false // Completing envelopes reaches target successfully
                        viewModel.completeChallenge(challengeId)
                        showCompletionDialog = true
                    }
                }
                showEnvelopeAnimation = false
            }
        )
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
                        Text(strings.enterAmount, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(
                            onClick = { showEnterAmountDialog = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = strings.close, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = customAmountInput,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                customAmountInput = newValue
                            }
                        },
                        label = { Text("${strings.amount} (${com.savingcoach.app.utils.InvestmentCalculations.getCurrencyLabel(uiState.currencyPreference, isInvestment = false)})") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    val parsedAmount = customAmountInput.toDoubleOrNull()
                    val isConfirmEnabled = parsedAmount != null && parsedAmount > 0.0

                    Button(
                        onClick = {
                            val amount = parsedAmount ?: 0.0
                            val nextSteps = completedSteps + 1
                            if (nextSteps <= totalSteps) {
                                viewModel.addDepositMock(challengeId, amount, nextSteps, 1, strings.deposit)
                                val willReachTarget = (currentAmount + amount) >= (targetAmount - 0.01)
                                val willCompleteSteps = nextSteps >= totalSteps
                                if (willReachTarget || willCompleteSteps) {
                                    isFailedLocal = !willReachTarget && willCompleteSteps
                                    viewModel.completeChallenge(challengeId)
                                    showCompletionDialog = true
                                }
                            }
                            showEnterAmountDialog = false
                        },
                        enabled = isConfirmEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChallengeActive,
                            disabledContainerColor = ChallengeActive.copy(alpha = 0.5f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(strings.confirm, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    val handleStepClick: (Int) -> Unit = { stepNumber ->
        if (stepNumber == -1) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(strings.alreadyCheckedInMsg)
            }
        } else {
            selectedDayNumber = stepNumber
            if (is100) {
                val todayStr = java.time.LocalDate.now().toString()
                val lastDepositDateOnly100 = challenge?.lastDepositDate?.substringBefore("|") ?: ""
                val alreadyDeposited = completedSteps > 0 && lastDepositDateOnly100 == todayStr
                if (alreadyDeposited) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(strings.alreadyOpenedEnvelopeMsg)
                    }
                } else {
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
                    showEnvelopeAnimation = true
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
        currencyPreference = uiState.currencyPreference,
        status = challenge?.status ?: com.savingcoach.app.data.model.ChallengeStatus.ACTIVE,
        onBackClick = onBackClick,
        onStepClick = handleStepClick,
        onSettingsClick = { challenge?.let { onSettingsClick(it) } },
        onDeleteClick = {
            viewModel.deleteChallenge(challengeId)
            onBackClick()
        },
        onSeeMoreClick = { showFullMapSheet = true },
        snackbarHostState = snackbarHostState,
        hasDepositedToday = hasDepositedToday,
        skippedIndices = skippedIndices
    )
    
    if (showCompletionDialog) {
        val isFailed = when {
            challenge?.status == com.savingcoach.app.data.model.ChallengeStatus.COMPLETED || challenge?.isCompleted == true -> false
            challenge?.status == com.savingcoach.app.data.model.ChallengeStatus.FAILED -> true
            !isNoSpend && targetAmount > 0 && currentAmount >= (targetAmount - 0.01) -> false
            isNoSpend && totalSteps > 0 && completedSteps >= totalSteps -> false
            else -> isFailedLocal
        }
        var showMessageBox by remember { mutableStateOf(false) }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            if (isFailed) {
                kotlinx.coroutines.delay(2200) // Wait 2.2 seconds
                showMessageBox = true
            } else {
                showMessageBox = true
            }
        }

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .pointerInput(Unit) {
                    detectTapGestures { }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isFailed && !showMessageBox) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    BrokenPiggyBankAnimation()
                }
            }

            val messageBoxAlpha by animateFloatAsState(
                targetValue = if (showMessageBox) 1f else 0f,
                animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
                label = "messageBoxAlpha"
            )
            val messageBoxScale by animateFloatAsState(
                targetValue = if (showMessageBox) 1f else 0.8f,
                animationSpec = tween(durationMillis = 400, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)),
                label = "messageBoxScale"
            )

            if (showMessageBox) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = messageBoxAlpha
                            scaleX = messageBoxScale
                            scaleY = messageBoxScale
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isFailed) {
                            androidx.compose.material3.Text(
                                strings.challengeFailedTitle, 
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.error, 
                                fontSize = 20.sp, 
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Text(
                                strings.challengeFailedDetail, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                textAlign = TextAlign.Center
                            )
                        } else {
                            androidx.compose.material3.Text(
                                strings.challengeCompletedTitle, 
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.onSurface, 
                                fontSize = 20.sp, 
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            androidx.compose.material3.Text(
                                strings.challengeCompletedMsg, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        androidx.compose.material3.Button(
                            onClick = { 
                                showCompletionDialog = false
                                onBackClick()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            androidx.compose.material3.Text(
                                strings.ok, 
                                color = Color.White, 
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, 
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            if (!isFailed) {
                com.savingcoach.app.ui.components.ConfettiView(modifier = Modifier.fillMaxSize())
            }
        }
    }

    if (showFullMapSheet) {
        val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = false)
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showFullMapSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                androidx.compose.material3.Text(
                    text = strings.fullProgressMap,
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
                        val isSkipped = skippedIndices.contains(index)
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
                                if (isSkipped) {
                                    Icon(
                                        imageVector = Icons.Outlined.Email,
                                        contentDescription = null,
                                        tint = surfaceColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else if (!isDone) {
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
                                if (!isSkipped) {
                                    Icon(
                                        imageVector = if (isDone) Icons.Default.Check else Icons.Outlined.Email,
                                        contentDescription = null,
                                        tint = if (isDone) surfaceColor else MutedGray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        } else {
                            ProgressPiggyItem(
                                stepNumber = index + 1,
                                isDone = isDone,
                                isSkipped = isSkipped,
                                isActiveStep = isActiveStep,
                                isDisabled = isDisabled,
                                size = 44.dp,
                                onClick = {
                                    if (isDone) {
                                        // Do nothing
                                    } else if (isDisabled) {
                                        showFullMapSheet = false
                                        handleStepClick(-1)
                                    } else {
                                        showFullMapSheet = false
                                        handleStepClick(index + 1)
                                    }
                                }
                            )
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
    currencyPreference: String,
    status: com.savingcoach.app.data.model.ChallengeStatus,
    onBackClick: () -> Unit,
    onStepClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSeeMoreClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    hasDepositedToday: Boolean,
    skippedIndices: Set<Int>
) {
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    
    val firstWord = challengeTitle.split(" ").firstOrNull() ?: ""
    val isEmoji = firstWord.isNotEmpty() && EMOJI_REGEX.containsMatchIn(firstWord)
    
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
                is100 -> strings.challenge100Envelope
                is7Day -> strings.challenge7DaySprint
                isNoSpend -> strings.challengeNoSpendWeek
                else -> strings.challenge1KADay
            }
        }
    }
    
    val description = when {
        is100 -> strings.challengeEnvelopeDesc
        is7Day && totalSteps == 7 -> strings.challenge7DayDesc
        isNoSpend && totalSteps == 7 -> strings.challengeNoSpendDesc
        else -> strings.challengeCustomDesc
    }
    
    val formattedCurrent = formatter.format(currentAmount)
    val formattedTarget = formatter.format(targetAmount)
    
    val calculatedPercent = if (isNoSpend || targetAmount == 0.0) {
        if (totalSteps > 0) ((completedSteps.toFloat() / totalSteps.toFloat()) * 100).toInt() else 0
    } else {
        if (targetAmount > 0) ((currentAmount / targetAmount) * 100).toInt() else 0
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
                                        contentDescription = strings.back,
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Text(
                                    text = strings.localizeChallengeTitle(cleanTitle),
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
                                            contentDescription = strings.settingsTitle,
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
                                            text = { Text(strings.editChallengeTitle, color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                showMenu = false
                                                onSettingsClick()
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(strings.deleteChallengeTitle, color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                showMenu = false
                                                showDeleteDialog = true
                                            }
                                        )
                                    }

                                    if (showDeleteDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showDeleteDialog = false },
                                            title = { Text(strings.deleteChallengeTitle, color = MaterialTheme.colorScheme.onSurface) },
                                            text = { Text(strings.deleteChallengeMsg, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            confirmButton = {
                                                TextButton(onClick = {
                                                    showDeleteDialog = false
                                                    onDeleteClick()
                                                }) {
                                                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showDeleteDialog = false }) {
                                                    Text(strings.cancel, color = MaterialTheme.colorScheme.onSurface)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Progress text
                            if (isNoSpend) {
                                Text(
                                    text = strings.daysCompletedCount(completedSteps, totalSteps),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            } else {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)) {
                                            append(strings.formatAmount(currentAmount, currencyPreference, 1.0, isInvestment = false))
                                        }
                                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), fontSize = 16.sp, fontWeight = FontWeight.Bold)) {
                                            append(" / " + strings.formatAmount(targetAmount, currencyPreference, 1.0, isInvestment = false))
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
                                targetAmount > 0 -> (currentAmount / targetAmount).toFloat()
                                else -> 0f
                            }.coerceIn(0f, 1f)
                            
                            LinearProgressIndicator(
                                progress = { progressFloat },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = when (status) {
                                    com.savingcoach.app.data.model.ChallengeStatus.FAILED -> MaterialTheme.colorScheme.error
                                    com.savingcoach.app.data.model.ChallengeStatus.STOPPED -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
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
                                    text = strings.percentComplete(calculatedPercent),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = when (status) {
                                        com.savingcoach.app.data.model.ChallengeStatus.COMPLETED -> strings.completed
                                        com.savingcoach.app.data.model.ChallengeStatus.FAILED -> strings.failed
                                        com.savingcoach.app.data.model.ChallengeStatus.STOPPED -> strings.stopped
                                        else -> strings.daysLeftCount(daysLeft.toLong())
                                    },
                                    color = when (status) {
                                        com.savingcoach.app.data.model.ChallengeStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                                        com.savingcoach.app.data.model.ChallengeStatus.FAILED -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
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
                            text = strings.progressMap,
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
                                    hasDepositedToday = hasDepositedToday,
                                    onStepClick = onStepClick,
                                    startIndex = startIndex,
                                    displayCount = displayCount,
                                    skippedIndices = skippedIndices
                                )
                            } else if ((is7Day || isNoSpend) && totalSteps == 7) {
                                SevenDayMap(
                                    completedSteps = completedSteps,
                                    isNoSpend = isNoSpend,
                                    hasDepositedToday = hasDepositedToday,
                                    onStepClick = onStepClick,
                                    skippedIndices = skippedIndices
                                )
                            } else {
                                DotGridMap(
                                    completedSteps = completedSteps,
                                    totalSteps = totalSteps,
                                    hasDepositedToday = hasDepositedToday,
                                    onStepClick = onStepClick,
                                    startIndex = startIndex,
                                    displayCount = displayCount,
                                    skippedIndices = skippedIndices
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
                                    text = strings.stepsDoneCount(completedSteps, totalSteps),
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
                            text = strings.depositHistory,
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
                            date = deposit.time,
                            currencyPreference = currencyPreference
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
fun DepositHistoryItem(amount: Double, note: String, date: String, currencyPreference: String = "MMK") {
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    val isSkipped = note.equals("Skipped", ignoreCase = true)
    val dotColor = if (isSkipped) CoralRed else ChallengeActive
    val amountColor = if (isSkipped) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f) else ChallengeActive

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dot indicator (Coral Red for skipped, Matcha Green for deposit)
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
        val displayNote = when {
            isSkipped -> strings.skipped
            note.equals("Deposit", ignoreCase = true) -> strings.deposit
            note.startsWith("Saved Envelope #") -> {
                val num = note.substringAfter("#").toIntOrNull()
                if (num != null) strings.savedEnvelopeNumber(num) else note
            }
            note.startsWith("Day ") && note.endsWith(" saved") -> {
                val num = note.removePrefix("Day ").removeSuffix(" saved").toIntOrNull()
                if (num != null) strings.zeroSpendDayTitle(num) else note
            }
            else -> strings.localizeChallengeTitle(note)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayNote,
                color = if (isSkipped) CoralRed else MaterialTheme.colorScheme.onBackground,
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
            text = if (isSkipped) "+0" else "+" + strings.formatAmount(amount, currencyPreference, 1.0, isInvestment = false),
            color = amountColor,
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
    hasDepositedToday: Boolean,
    onStepClick: (Int) -> Unit,
    startIndex: Int = 0,
    displayCount: Int = totalSteps,
    skippedIndices: Set<Int> = emptySet()
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val errorColor = MaterialTheme.colorScheme.error

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
            val isSkipped = skippedIndices.contains(index)
            val isActiveStep = index == completedSteps && !hasDepositedToday
            val isDisabled = index > completedSteps || hasDepositedToday
            
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .scale(if (isActiveStep) 1.15f else 1f)
                    .alpha(if (isDisabled) 0.6f else 1f)
                    .clip(CircleShape)
                    .background(
                        color = when {
                            isSkipped -> errorColor.copy(alpha = 0.15f)
                            isDone -> primaryColor
                            isActiveStep -> primaryColor.copy(alpha = 0.15f)
                            else -> surfaceVariantColor
                        },
                        shape = CircleShape
                    )
                    .border(
                        width = if (isActiveStep) 1.5.dp else 1.dp,
                        color = when {
                            isSkipped -> errorColor
                            isActiveStep -> primaryColor
                            isDone -> primaryColor
                            else -> outlineVariantColor
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
                if (isSkipped) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = errorColor,
                        modifier = Modifier.size(16.dp)
                    )
                } else if (!isDone) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = if (isActiveStep) outlineColor else outlineVariantColor,
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        )
                    }
                }
                
                if (!isSkipped) {
                    Icon(
                        imageVector = if (isDone) Icons.Default.Check else Icons.Outlined.Email,
                        contentDescription = null,
                        tint = if (isDone) onPrimaryColor else if (isActiveStep) primaryColor else onSurfaceVariantColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressPiggyItem(
    stepNumber: Int,
    isDone: Boolean,
    isSkipped: Boolean,
    isActiveStep: Boolean,
    isDisabled: Boolean,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            // 1. MISSED / SKIPPED PIGGY (sad pastel-gray piggy)
            isSkipped -> {
                Image(
                    painter = painterResource(id = R.drawable.piggy_missed),
                    contentDescription = "Skipped step $stepNumber",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // 2. SUCCESSFUL DEPOSIT PIGGY (happy pink piggy with crown and gold coins)
            isDone -> {
                Image(
                    painter = painterResource(id = R.drawable.piggy_success),
                    contentDescription = "Completed step $stepNumber",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // 3. ACTIVE / READY TODAY PIGGY (alert pink piggy with soft highlight)
            isActiveStep -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Subtle soft highlight halo
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.2f),
                            radius = size.toPx() / 2.05f
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.piggy_active),
                        contentDescription = "Active step $stepNumber",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            // 4. UPCOMING / LOCKED PIGGY (grayed out)
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.piggy_active),
                        contentDescription = "Upcoming step $stepNumber",
                        contentScale = ContentScale.Fit,
                        colorFilter = ColorFilter.colorMatrix(
                            ColorMatrix().apply { setToSaturation(0f) }
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.35f)
                    )
                }
            }
        }
    }
}

@Composable
fun SevenDayMap(completedSteps: Int, isNoSpend: Boolean, hasDepositedToday: Boolean, onStepClick: (Int) -> Unit, skippedIndices: Set<Int> = emptySet()) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(7) { index ->
            val isDone = index < completedSteps
            val isSkipped = skippedIndices.contains(index)
            val isActiveStep = index == completedSteps && !hasDepositedToday
            val isDisabled = index > completedSteps || hasDepositedToday

            ProgressPiggyItem(
                stepNumber = index + 1,
                isDone = isDone,
                isSkipped = isSkipped,
                isActiveStep = isActiveStep,
                isDisabled = isDisabled,
                size = 42.dp,
                onClick = {
                    if (isDone) {
                        // Do nothing
                    } else if (isDisabled) {
                        onStepClick(-1)
                    } else {
                        onStepClick(index + 1)
                    }
                }
            )
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
    displayCount: Int = totalSteps,
    skippedIndices: Set<Int> = emptySet()
) {
    FlowRow(
        maxItemsInEachRow = 6, // 6 columns
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(displayCount) { i ->
            val index = startIndex + i
            if (index >= totalSteps) return@repeat
            val isDone = index < completedSteps
            val isSkipped = skippedIndices.contains(index)
            val isActiveStep = index == completedSteps && !hasDepositedToday
            val isDisabled = index > completedSteps || hasDepositedToday

            ProgressPiggyItem(
                stepNumber = index + 1,
                isDone = isDone,
                isSkipped = isSkipped,
                isActiveStep = isActiveStep,
                isDisabled = isDisabled,
                size = 48.dp,
                onClick = {
                    if (isDone) {
                        // Do nothing
                    } else if (isDisabled) {
                        onStepClick(-1)
                    } else {
                        onStepClick(index + 1)
                    }
                }
            )
        }
    }
}

@Composable
fun BrokenPiggyBankAnimation() {
    val animProgress = remember { Animatable(0f) }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        // 2.5 seconds total duration
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2500, easing = LinearEasing)
        )
    }

    val progress = animProgress.value

    // Phase 1 (0.0s - 0.4s): Enter & Float (progress 0.0 to 0.22)
    // Scale goes 0% -> 105% -> 100% (overshoot)
    val entranceProgress = (progress / 0.22f).coerceIn(0f, 1f)
    val coinScale = if (entranceProgress < 0.8f) {
        (entranceProgress / 0.8f) * 1.05f
    } else {
        1.05f - ((entranceProgress - 0.8f) / 0.2f) * 0.05f
    }

    // Phase 2 (0.4s - 0.6s): Wiggle / Shake (progress 0.22 to 0.33)
    val isShaking = progress in 0.22f..0.33f
    val shakeRotation = if (isShaking) {
        val cycleProgress = ((progress - 0.22f) / 0.11f) * 3f // 3 cycles
        sin(cycleProgress * 2 * PI.toFloat()) * 3.5f // amplitude
    } else 0f

    // Phase 2 & 3: Color desaturation (No desaturation - stays golden)
    val desaturationProgress = 0f

    // Phase 3 (0.6s - 1.4s): Shatter & Fall (progress 0.33 to 0.78)
    val shatterProgress = ((progress - 0.33f) / 0.45f).coerceIn(0f, 1f)

    // Shimmer path translation sweep (Phase 1: 0.0s - 0.4s)
    val shimmerOffset = -150f + (300f * entranceProgress)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = minOf(size.width, size.height) * 0.38f

        // 1. Shards / debris bursting radially outward (Phase 3)
        if (shatterProgress > 0f && shatterProgress < 1f) {
            val random = java.util.Random(999)
            val debrisAlpha = 1f - shatterProgress
            val burstDistance = 120f * shatterProgress
            for (i in 0..4) {
                val angle = (i * (360f / 5f)) * (PI.toFloat() / 180f)
                val dx = cos(angle) * burstDistance
                val dy = sin(angle) * burstDistance
                val shardSize = 8f + random.nextFloat() * 10f
                
                val shardPath = Path().apply {
                    moveTo(cx + dx, cy + dy)
                    lineTo(cx + dx + shardSize, cy + dy - shardSize * 0.3f)
                    lineTo(cx + dx + shardSize * 0.5f, cy + dy + shardSize)
                    close()
                }
                
                val shardColor = lerp(Color(0xFFD4AF37), Color(0xFF9E9E9E), desaturationProgress)
                drawPath(shardPath, color = shardColor.copy(alpha = debrisAlpha))
            }
        }

        // Draw Left & Right halves inside the transform scopes
        val crackPoints = listOf(
            androidx.compose.ui.geometry.Offset(cx, cy - radius),
            androidx.compose.ui.geometry.Offset(cx - 8f, cy - radius * 0.5f),
            androidx.compose.ui.geometry.Offset(cx + 10f, cy),
            androidx.compose.ui.geometry.Offset(cx - 12f, cy + radius * 0.5f),
            androidx.compose.ui.geometry.Offset(cx, cy + radius)
        )

        // Left Piece half path
        val leftPath = Path().apply {
            addArc(
                androidx.compose.ui.geometry.Rect(cx - radius, cy - radius, cx + radius, cy + radius),
                90f,
                180f
            )
            crackPoints.forEach { point ->
                lineTo(point.x, point.y)
            }
            close()
        }

        // Right Piece half path
        val rightPath = Path().apply {
            addArc(
                androidx.compose.ui.geometry.Rect(cx - radius, cy - radius, cx + radius, cy + radius),
                -90f,
                180f
            )
            crackPoints.asReversed().forEach { point ->
                lineTo(point.x, point.y)
            }
            close()
        }

        // Interpolated color brushes from bright Gold to Stone Grey
        val goldLeft1 = lerp(Color(0xFFFFDF00), Color(0xFFB0B0B0), desaturationProgress)
        val goldLeft2 = lerp(Color(0xFFD4AF37), Color(0xFF8E8E8E), desaturationProgress)
        val goldLeft3 = lerp(Color(0xFF996515), Color(0xFF6E6E6E), desaturationProgress)

        val goldRight1 = lerp(Color(0xFFFFF066), Color(0xFFC8C8C8), desaturationProgress)
        val goldRight2 = lerp(Color(0xFFE5C158), Color(0xFF9E9E9E), desaturationProgress)
        val goldRight3 = lerp(Color(0xFFB58024), Color(0xFF7C7C7C), desaturationProgress)

        val brushLeft = Brush.linearGradient(
            colors = listOf(goldLeft1, goldLeft2, goldLeft3),
            start = androidx.compose.ui.geometry.Offset(cx - radius, cy - radius),
            end = androidx.compose.ui.geometry.Offset(cx, cy + radius)
        )

        val brushRight = Brush.linearGradient(
            colors = listOf(goldRight1, goldRight2, goldRight3),
            start = androidx.compose.ui.geometry.Offset(cx, cy - radius),
            end = androidx.compose.ui.geometry.Offset(cx + radius, cy + radius)
        )

        // Wiggle/Shake applied to whole coin group
        withTransform({
            scale(coinScale, coinScale, pivot = androidx.compose.ui.geometry.Offset(cx, cy))
            rotate(shakeRotation, pivot = androidx.compose.ui.geometry.Offset(cx, cy))
        }) {
            
            // Left half Piece motion (Position X -40px, Position Y +60px, Rotation -25 degrees, Opacity 100% -> 0%)
            val leftShiftX = -40.dp.toPx() * shatterProgress
            val leftShiftY = 60.dp.toPx() * shatterProgress
            val leftRotation = -25f * shatterProgress
            val pieceAlpha = (1f - shatterProgress).coerceIn(0f, 1f)

            withTransform({
                translate(left = leftShiftX, top = leftShiftY)
                rotate(leftRotation, pivot = androidx.compose.ui.geometry.Offset(cx - radius / 2f, cy))
            }) {
                drawPath(leftPath, brush = brushLeft, alpha = pieceAlpha)
                
                // Outer outline border
                drawPath(
                    leftPath,
                    color = Color.Black.copy(alpha = 0.15f * (1f - desaturationProgress * 0.5f)),
                    style = Stroke(width = 4.dp.toPx()),
                    alpha = pieceAlpha
                )

                // Coin inner rim
                val innerLeftPath = Path().apply {
                    addArc(
                        androidx.compose.ui.geometry.Rect(cx - radius + 12f, cy - radius + 12f, cx + radius - 12f, cy + radius - 12f),
                        90f,
                        180f
                    )
                }
                drawPath(innerLeftPath, color = Color.White.copy(alpha = 0.25f * (1f - desaturationProgress)), style = Stroke(width = 2.dp.toPx()), alpha = pieceAlpha)

                // Draw currency symbol ($) on left side of coin
                val dollarPath = Path().apply {
                    moveTo(cx - 30f, cy - 25f)
                    lineTo(cx - 15f, cy - 25f)
                    // Simple S outline
                    lineTo(cx - 15f, cy - 10f)
                    lineTo(cx - 30f, cy - 10f)
                    lineTo(cx - 30f, cy + 10f)
                    lineTo(cx - 15f, cy + 10f)
                }
                drawPath(
                    dollarPath,
                    color = lerp(Color(0x40FFFFFF), Color(0x1A000000), desaturationProgress),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    alpha = pieceAlpha
                )
            }

            // Right half Piece motion (Position X +40px, Position Y +60px, Rotation +25 degrees, Opacity 100% -> 0%)
            val rightShiftX = 40.dp.toPx() * shatterProgress
            val rightShiftY = 60.dp.toPx() * shatterProgress
            val rightRotation = 25f * shatterProgress

            withTransform({
                translate(left = rightShiftX, top = rightShiftY)
                rotate(rightRotation, pivot = androidx.compose.ui.geometry.Offset(cx + radius / 2f, cy))
            }) {
                drawPath(rightPath, brush = brushRight, alpha = pieceAlpha)
                
                drawPath(
                    rightPath,
                    color = Color.Black.copy(alpha = 0.15f * (1f - desaturationProgress * 0.5f)),
                    style = Stroke(width = 4.dp.toPx()),
                    alpha = pieceAlpha
                )

                // Coin inner rim
                val innerRightPath = Path().apply {
                    addArc(
                        androidx.compose.ui.geometry.Rect(cx - radius + 12f, cy - radius + 12f, cx + radius - 12f, cy + radius - 12f),
                        -90f,
                        180f
                    )
                }
                drawPath(innerRightPath, color = Color.White.copy(alpha = 0.25f * (1f - desaturationProgress)), style = Stroke(width = 2.dp.toPx()), alpha = pieceAlpha)
            }

            // 2. Shimmer shine path sweep (Phase 1)
            if (progress < 0.22f) {
                val shinePath = Path().apply {
                    moveTo(cx + shimmerOffset - 30f, cy - radius)
                    lineTo(cx + shimmerOffset + 10f, cy - radius)
                    lineTo(cx + shimmerOffset + 40f, cy + radius)
                    lineTo(cx + shimmerOffset, cy + radius)
                    close()
                }
                // Clip shine to the circular coin shape
                withTransform({
                    // Simple circular clip logic inside drawing
                }) {
                    drawPath(
                        shinePath,
                        color = Color.White.copy(alpha = 0.28f)
                    )
                }
            }
        }

        // Draw growing black fracture zig-zag line (Phase 2 & 3)
        if (progress > 0.22f && shatterProgress < 0.5f) {
            val crackVisible = ((progress - 0.22f) / 0.25f).coerceIn(0f, 1f)
            val crackStrokePath = Path().apply {
                moveTo(crackPoints[0].x, crackPoints[0].y)
                val segmentCount = crackPoints.size - 1
                val activeSegments = (crackVisible * segmentCount).toInt()
                val lastSegmentProgress = (crackVisible * segmentCount) - activeSegments

                for (i in 0 until activeSegments) {
                    lineTo(crackPoints[i + 1].x, crackPoints[i + 1].y)
                }
                
                if (activeSegments < segmentCount) {
                    val start = crackPoints[activeSegments]
                    val end = crackPoints[activeSegments + 1]
                    val currentX = start.x + (end.x - start.x) * lastSegmentProgress
                    val currentY = start.y + (end.y - start.y) * lastSegmentProgress
                    lineTo(currentX, currentY)
                }
            }
            
            drawPath(
                crackStrokePath,
                color = Color(0xFF263238), // dark zig-zag crack
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round
                ),
                alpha = 1f - (shatterProgress * 2f).coerceIn(0f, 1f)
            )
        }
    }
}

