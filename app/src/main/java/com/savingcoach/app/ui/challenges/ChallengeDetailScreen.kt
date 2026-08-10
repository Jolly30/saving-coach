package com.savingcoach.app.ui.challenges

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.text.NumberFormat
import java.util.Locale

data class DepositItem(val amount: Double, val note: String, val time: String)

@Composable
fun ChallengeDetailScreen(
    challengeId: String,
    onBackClick: () -> Unit,
    onSettingsClick: (SavingChallenge) -> Unit,
    viewModel: ChallengeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val challenge = uiState.challengesList.find { it.id == challengeId }

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

    androidx.compose.runtime.LaunchedEffect(challenge) {
        challenge?.let {
            completedSteps = it.completedDaysCount
            currentAmount = it.currentAmount
            currentDaysLeft = try {
                val end = java.time.LocalDate.parse(it.endDate)
                java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), end).toInt().coerceAtLeast(0)
            } catch (e: Exception) { 30 }
        }
    }

    val totalSteps = when {
        is100 && challengeId.startsWith("preset_") -> 100
        is7Day && challengeId.startsWith("preset_") -> 7
        isNoSpend && challengeId.startsWith("preset_") -> 7
        else -> try {
            val start = java.time.LocalDate.parse(challenge?.startDate ?: java.time.LocalDate.now().toString())
            val end = java.time.LocalDate.parse(challenge?.endDate ?: java.time.LocalDate.now().plusDays(30).toString())
            val currentDiff = java.time.temporal.ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(1)
            if (challengeId.startsWith("preset_")) {
                currentDiff
            } else {
                currentDiff + completedSteps
            }
        } catch (e: Exception) {
            if (is100) 100 else if (is7Day || isNoSpend) 7 else 30
        }
    }
    
    val targetAmount = challenge?.targetAmount ?: 30000.0
    
    val depositAmount = if (totalSteps > 0) {
        if (isPreset100 && targetAmount == 505000.0) 5000.0
        else targetAmount / totalSteps
    } else 1000.0
    
    val depositHistory = remember(challengeId) {
        val list = mutableStateListOf<DepositItem>()
        if (isPreset7Day) {
            list.add(DepositItem(amount = depositAmount, note = "Deposit", time = "Today, 06:55"))
            list.add(DepositItem(amount = depositAmount, note = "Freelance payout", time = "Yesterday, 21:10"))
        } else if (isPreset100) {
            list.add(DepositItem(amount = depositAmount, note = "Saved Envelope #34", time = "Today, 08:12"))
            list.add(DepositItem(amount = depositAmount, note = "Deposit", time = "Yesterday, 21:40"))
        } else if (isPresetNoSpend) {
            list.add(DepositItem(amount = depositAmount, note = "Day 4 saved", time = "Yesterday, 20:00"))
            list.add(DepositItem(amount = depositAmount, note = "Day 3 saved", time = "Jul 26, 19:15"))
        } else if (isPreset1K) {
            list.add(DepositItem(amount = depositAmount, note = "Morning coffee skipped", time = "Today, 08:12"))
            list.add(DepositItem(amount = depositAmount, note = "Deposit", time = "Yesterday, 21:40"))
        }
        list
    }

    var showDayDialog by remember { mutableStateOf(false) }
    var selectedDayNumber by remember { mutableIntStateOf(1) }
    
    var showEnterAmountDialog by remember { mutableStateOf(false) }
    var customAmountInput by remember { mutableStateOf("10000") }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    var animatingEnvelopeIndex by remember { mutableStateOf<Int?>(null) }
    var showSurpriseDialog by remember { mutableStateOf(false) }
    var calculatedSurpriseAmount by remember { mutableDoubleStateOf(0.0) }
    
    val outerFormatter = NumberFormat.getNumberInstance(Locale.US)
    
    val todayDateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    val lastDepositDateOnly = challenge?.lastDepositDate?.substringBefore("|") ?: ""
    val hasDepositedToday = lastDepositDateOnly == todayDateString

    if (showDayDialog) {
        Dialog(onDismissRequest = { showDayDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
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
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(if (isNoSpend) "HABIT TRACKER" else "SAVING", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isNoSpend) {
                        Text(
                            text = "Zero Spend Day $selectedDayNumber", 
                            fontSize = 22.sp, 
                            fontWeight = FontWeight.ExtraBold, 
                            color = Color(0xFF0F172A)
                        )
                    } else {
                        val formattedDepositAmount = NumberFormat.getNumberInstance(Locale.US).format(depositAmount)
                        Text(
                            text = "$formattedDepositAmount MMK", 
                            fontSize = 24.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = Color(0xFF0F172A)
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
                                    viewModel.addDepositMock(challengeId, depositAmount, completedSteps, 1)
                                    val depositNote = when {
                                        is7Day -> "Deposit"
                                        is100 -> "Saved Envelope #$completedSteps"
                                        else -> "Deposit"
                                    }
                                    depositHistory.add(
                                        0, 
                                        DepositItem(amount = depositAmount, note = depositNote, time = "Just now")
                                    )
                                } else {
                                    viewModel.addDepositMock(challengeId, 0.0, completedSteps, 1)
                                }
                            }
                            showDayDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
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
                color = Color(0xFF0F172A), // Theme navy
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
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "ENVELOPE SURPRISE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "You opened an envelope and found:",
                        fontSize = 14.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "${outerFormatter.format(calculatedSurpriseAmount)} MMK",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10B981) // Emerald accent
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            completedSteps++
                            if (currentDaysLeft > 0) currentDaysLeft--
                            currentAmount += calculatedSurpriseAmount
                            viewModel.addDepositMock(challengeId, calculatedSurpriseAmount, completedSteps, 1)
                            
                            val depositNote = "Saved Envelope #$selectedDayNumber"
                            depositHistory.add(
                                0,
                                DepositItem(amount = calculatedSurpriseAmount, note = depositNote, time = "Just now")
                            )
                            showSurpriseDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
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
                color = Color.White,
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
                        Text("Enter amount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        IconButton(
                            onClick = { showEnterAmountDialog = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = customAmountInput,
                        onValueChange = { customAmountInput = it.filter { char -> char.isDigit() } },
                        placeholder = { Text("10000") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF10B981).copy(alpha = 0.5f)
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
                                viewModel.addDepositMock(challengeId, amount, completedSteps, 1)
                                depositHistory.add(
                                    0, 
                                    DepositItem(amount = amount, note = "Deposit", time = "Just now")
                                )
                            }
                            showEnterAmountDialog = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Confirm", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
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
        onStepClick = { stepNumber ->
            if (stepNumber == -1) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("You've already completed today's check-in! Come back tomorrow.")
                }
            } else {
                selectedDayNumber = stepNumber
                if (is100) {
                    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
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
                            ((average * randomFactor) / 10).toInt() * 10.0
                        }
                        calculatedSurpriseAmount = surprise.coerceAtLeast(100.0)
                    }
                } else if (is7Day) {
                    customAmountInput = depositAmount.toInt().toString()
                    showEnterAmountDialog = true
                } else {
                    showDayDialog = true
                }
            }
        },
        onSettingsClick = { challenge?.let { onSettingsClick(it) } },
        snackbarHostState = snackbarHostState,
        hasDepositedToday = hasDepositedToday
    )
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        // Wrap everything in a light surface explicitly to avoid black screens
        Surface(color = Color(0xFFF8FAFC), modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Header Section
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A))
                            .padding(top = 16.dp, bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. Back Arrow Button
                                IconButton(
                                    onClick = onBackClick,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                // 2. Circular Emoji Badge
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emoji,
                                        fontSize = 22.sp
                                    )
                                }

                                // 3. Title & Subtitle Column
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    IconButton(
                                        onClick = onSettingsClick,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = cleanTitle,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = description,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Progress text
                            if (isNoSpend) {
                                Text(
                                    text = "$completedSteps of $totalSteps days completed",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            } else {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)) {
                                            append(formattedCurrent)
                                        }
                                        withStyle(SpanStyle(color = Color(0xFFA5D6A7), fontSize = 16.sp, fontWeight = FontWeight.Bold)) {
                                            append("/$formattedTarget MMK")
                                        }
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF0F172A))
                            ) {
                                LinearProgressIndicator(
                                    progress = { if (isNoSpend) (completedSteps.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f) else (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                    color = if (completedSteps >= totalSteps || currentAmount >= targetAmount) Color(0xFFF97316) else Color(0xFF10B981),
                                    trackColor = Color(0xFF0F172A),
                                    strokeCap = StrokeCap.Butt
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Subtext Row
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = "$calculatedPercent% complete",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(if (completedSteps >= totalSteps || currentAmount >= targetAmount) "Completed" else "$daysLeft days left", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                
                // PROGRESS MAP Card
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
                            color = Color.Gray,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (is100) {
                                    EnvelopeMap(
                                        completedSteps = completedSteps,
                                        totalSteps = totalSteps,
                                        animatingIndex = animatingEnvelopeIndex,
                                        onAnimationFinished = onAnimationFinished,
                                        hasDepositedToday = hasDepositedToday,
                                        onStepClick = onStepClick
                                    )
                                } else if (is7Day || isNoSpend) {
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
                                        onStepClick = onStepClick
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "$completedSteps of $totalSteps steps done",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                if (!isNoSpend) {
                    // Deposit History List
                    item {
                        Text(
                            text = "DEPOSIT HISTORY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
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
}

@Composable
fun DepositHistoryItem(amount: Double, note: String, date: String) {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
                val iconBg = Color(0xFFDCFCE7) // Soft emerald background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF16A34A))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${formatter.format(amount)} MMK",
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Bold, // Bold text for amount
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = note,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = date,
                color = Color.Gray,
                fontSize = 12.sp, // date timestamp
                fontWeight = FontWeight.Medium
            )
        }
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
    onStepClick: (Int) -> Unit
) {
    FlowRow(
        maxItemsInEachRow = 10,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(totalSteps) { index ->
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
                            isDone -> Color(0xFF2563EB)
                            isActiveStep -> Color(0xFFF1F5F9)
                            else -> Color(0xFFE2E8F0)
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
                            color = if (isActiveStep) Color(0xFFE0E0E0) else Color(0xFF94A3B8),
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
                    tint = if (isDone) Color.White else Color(0xFF9E9E9E), // gray tint
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun SevenDayMap(completedSteps: Int, isNoSpend: Boolean, hasDepositedToday: Boolean, onStepClick: (Int) -> Unit) {
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
                            isDone -> Color(0xFF2563EB)
                            isActiveStep -> Color(0xFFF1F5F9)
                            else -> Color(0xFFE2E8F0)
                        },
                        shape = CircleShape
                    )
                    .let {
                        if (isDone) {
                            it.border(2.dp, Color(0xFF2563EB), CircleShape)
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
                            color = if (isActiveStep) Color(0xFFE0E0E0) else Color(0xFF94A3B8),
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
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    if (isNoSpend) {
                        Text(
                            text = "${index + 1}",
                            color = if (isActiveStep) Color(0xFF0F172A) else Color(0xFF94A3B8),
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
fun DotGridMap(completedSteps: Int, totalSteps: Int, hasDepositedToday: Boolean, onStepClick: (Int) -> Unit) {
    FlowRow(
        maxItemsInEachRow = 6, // Match screenshot (6 columns x 5 rows = 30)
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(totalSteps) { index ->
            val isDone = index < completedSteps
            val isActiveStep = index == completedSteps && !hasDepositedToday
            val isDisabled = index > completedSteps || hasDepositedToday
            
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        color = when {
                            isDone -> Color(0xFF2563EB)
                            isActiveStep -> Color(0xFFF1F5F9)
                            else -> Color(0xFFE2E8F0)
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
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = if (isActiveStep) Color(0xFFE0E0E0) else Color(0xFF94A3B8),
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )
                    }
                    Text(
                        text = "${index + 1}",
                        color = if (isActiveStep) Color(0xFF0F172A) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
