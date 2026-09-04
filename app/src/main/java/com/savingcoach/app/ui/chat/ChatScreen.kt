package com.savingcoach.app.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.rounded.ChatBubble
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.savingcoach.app.data.model.ChatMessage
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.ExpenseCategoryEntity
import com.savingcoach.app.data.model.ParsedExpense
import androidx.compose.material.icons.filled.CheckCircle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.offset
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.savingcoach.app.ui.theme.*

val EmeraldGreen = MatchaPrimary
val LightGreenBubble = MatchaContainer
val LightGrayBg = CreamBackground
val BorderGray = CreamOutline
val TextBlack = DarkRoast
val TextGray = EarthySlate

/*@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onClose: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onClose() },
            contentAlignment = Alignment.Center        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(initialScale = 0.9f) + fadeIn(),
                    exit = scaleOut(targetScale = 0.9f) + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .fillMaxHeight(0.94f)      // was 0.90f
                            .offset(y = (-30).dp)      // move up a little
                            .clickable(enabled = false) {},
                        shape = RoundedCornerShape(36.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        ChatWindowContent(viewModel) { onClose() }
                    }
                }
            }
        }
    }
}*/

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    isDialog: Boolean = false,
    onClose: () -> Unit = {}
) {
    if (isDialog) {
        Dialog(
            onDismissRequest = onClose,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = true,      // Fixed: allows smooth keyboard window insets
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center        ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()              // Fixed: precisely tracks the keyboard height without dead spaces
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = scaleIn(initialScale = 0.9f) + fadeIn(),
                        exit = scaleOut(targetScale = 0.9f) + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .fillMaxHeight(0.94f)      // Kept your exact number (was 0.90f)
                                .offset(y = (-30).dp)      // Kept your exact offset
                                .clickable(enabled = false) {},
                            shape = RoundedCornerShape(36.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
                        ) {
                            ChatWindowContent(viewModel, isDialog = true, onClose = onClose)
                        }
                    }
                }
            }
        }
    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ChatWindowContent(viewModel, isDialog = false, onClose = onClose)
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatWindowContent(
    viewModel: ChatViewModel,
    isDialog: Boolean,
    onClose: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val isTyping by viewModel.isTyping.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val isListening by viewModel.isListening.collectAsState()
    val partialVoiceText by viewModel.partialVoiceText.collectAsState()
    val rmsDb by viewModel.rmsDb.collectAsState()
    val savingExpenseMessageIds by viewModel.savingExpenseMessageIds.collectAsState()
    val activeChallenges by viewModel.activeChallenges.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val error by viewModel.error.collectAsState()

    var showEnvelopeAnim by remember { mutableStateOf(false) }
    var envelopeAnimAmount by remember { mutableStateOf(0.0) }
    var envelopeAnimMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var envelopeAnimSwitchTarget by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceInput()
        }
    }

    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(isTyping) {
        if (isTyping) {
            keyboardController?.hide()
        }
    }

    var hasScrolledToInitial by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty()) {
            val lastUserIndex = messages.indexOfLast { it.role == "user" }
            val targetIndex = if (lastUserIndex != -1) lastUserIndex else (messages.size - 1)
            if (targetIndex >= 0) {
                if (!hasScrolledToInitial) {
                    hasScrolledToInitial = true
                    // On initial load, jump instantly to the latest turn so old messages never flash
                    kotlinx.coroutines.delay(50)
                    try {
                        listState.scrollToItem(targetIndex, 0)
                    } catch (_: Exception) {}
                } else {
                    // Smoothly animate latest turn to the top
                    kotlinx.coroutines.delay(80)
                    try {
                        listState.animateScrollToItem(targetIndex, 0)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val isImeVisible = androidx.compose.foundation.layout.WindowInsets.isImeVisible
    LaunchedEffect(isImeVisible) {
        if (isImeVisible && messages.isNotEmpty()) {
            val lastUserIndex = messages.indexOfLast { it.role == "user" }
            val targetIndex = if (lastUserIndex != -1) lastUserIndex else (messages.size - 1)
            if (targetIndex >= 0) {
                kotlinx.coroutines.delay(100)
                try {
                    listState.animateScrollToItem(targetIndex, 0)
                } catch (_: Exception) {}
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (isDialog) 24.dp else 16.dp, start = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("AI Finance Assistant", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your personal finance helper", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EmeraldGreen))
                }
            }
            if (isDialog) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
                }
            }
        }

        error?.let { errorMsg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMsg,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = { viewModel.clearError() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Chat Body and Floating Button
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        WelcomeCard(onSuggestionClick = { suggestion ->
                            viewModel.sendMessage(suggestion)
                        })
                    }
                    item {
                        DateSeparator()
                    }
                }

                items(messages) { message ->
                    MessageItem(
                        message = message,
                        onSaveExpenseAtIndex = { idx -> viewModel.saveParsedExpenseAtIndex(message, idx) },
                        onSaveExpenseWithCategoryAtIndex = { idx, cat -> viewModel.saveExpenseWithCategoryAtIndex(message, idx, cat) },
                        onUpdateExpenseCategoryAtIndex = { idx, cat -> viewModel.updateExpenseCategoryAtIndex(message, idx, cat) },
                        savingExpenseMessageIds = savingExpenseMessageIds,
                        onConfirmChallenge = { parsed ->
                             val title = parsed.challengeTitle.ifBlank { parsed.merchant }
                             val cleanTitle = title.filter { it.isLetterOrDigit() || it.isWhitespace() }.lowercase().trim()
                             val targetChallenge = activeChallenges.firstOrNull {
                                 val cleanDb = it.title.filter { c -> c.isLetterOrDigit() || c.isWhitespace() }.lowercase().trim()
                                 cleanDb == cleanTitle
                             }
                             val isEnvelope = targetChallenge?.template == com.savingcoach.app.data.model.ChallengeTemplate.ENVELOPE
                             if (isEnvelope && targetChallenge != null) {
                                 val parts = targetChallenge.lastDepositDate.split("|")
                                 val completedSteps = if (parts.size > 1) (parts[1].toIntOrNull() ?: 0) else 0
                                 val duration = if (parts.size > 2) parts[2] else "30"
                                 val totalSteps = duration.toIntOrNull() ?: 30
                                 val remainingEnvelopes = (totalSteps - completedSteps).coerceAtLeast(1)
                                 val remainingAmount = (targetChallenge.targetAmount - targetChallenge.currentAmount).coerceAtLeast(0.0)

                                 val surprise = if (remainingEnvelopes == 1) {
                                     remainingAmount
                                 } else {
                                     val average = remainingAmount / remainingEnvelopes
                                     val randomFactor = 0.7 + (Math.random() * 0.6)
                                     val rawSurprise = average * randomFactor
                                     if (rawSurprise >= 1000.0) {
                                         ((rawSurprise / 1000.0).toInt() * 1000.0).coerceAtLeast(1000.0)
                                     } else if (rawSurprise >= 100.0) {
                                         ((rawSurprise / 100.0).toInt() * 100.0).coerceAtLeast(100.0)
                                     } else {
                                         rawSurprise.coerceAtLeast(1.0)
                                     }
                                 }

                                 val surpriseAmount = if (remainingEnvelopes == 1) {
                                     remainingAmount.coerceAtLeast(0.0)
                                 } else {
                                     val maxAllowed = (remainingAmount - ((remainingEnvelopes - 1) * 1.0)).coerceAtLeast(0.0)
                                     surprise.coerceAtLeast(1.0).coerceAtMost(maxAllowed)
                                 }
                                 envelopeAnimAmount = surpriseAmount
                                 envelopeAnimMessage = message
                                 envelopeAnimSwitchTarget = null
                                 showEnvelopeAnim = true
                             } else {
                                 viewModel.confirmChallengeSaving(message)
                             }
                        },
                        onSwitchChallenge = { parsed, target ->
                            val targetChallenge = activeChallenges.firstOrNull { it.title.equals(target, ignoreCase = true) }
                            val isEnvelope = targetChallenge?.template == com.savingcoach.app.data.model.ChallengeTemplate.ENVELOPE
                            if (isEnvelope && targetChallenge != null) {
                                val parts = targetChallenge.lastDepositDate.split("|")
                                val completedSteps = if (parts.size > 1) (parts[1].toIntOrNull() ?: 0) else 0
                                val duration = if (parts.size > 2) parts[2] else "30"
                                val totalSteps = duration.toIntOrNull() ?: 30
                                val remainingEnvelopes = (totalSteps - completedSteps).coerceAtLeast(1)
                                val remainingAmount = (targetChallenge.targetAmount - targetChallenge.currentAmount).coerceAtLeast(0.0)

                                val surprise = if (remainingEnvelopes == 1) {
                                    remainingAmount
                                } else {
                                    val average = remainingAmount / remainingEnvelopes
                                    val randomFactor = 0.7 + (Math.random() * 0.6)
                                    val rawSurprise = average * randomFactor
                                    if (rawSurprise >= 1000.0) {
                                        ((rawSurprise / 1000.0).toInt() * 1000.0).coerceAtLeast(1000.0)
                                    } else if (rawSurprise >= 100.0) {
                                        ((rawSurprise / 100.0).toInt() * 100.0).coerceAtLeast(100.0)
                                    } else {
                                        rawSurprise.coerceAtLeast(1.0)
                                    }
                                }

                                val surpriseAmount = if (remainingEnvelopes == 1) {
                                    remainingAmount.coerceAtLeast(0.0)
                                } else {
                                    val maxAllowed = (remainingAmount - ((remainingEnvelopes - 1) * 1.0)).coerceAtLeast(0.0)
                                    surprise.coerceAtLeast(1.0).coerceAtMost(maxAllowed)
                                }
                                envelopeAnimAmount = surpriseAmount
                                envelopeAnimMessage = message
                                envelopeAnimSwitchTarget = target
                                showEnvelopeAnim = true
                            } else {
                                viewModel.switchChallengeSaving(message, target)
                            }
                        },
                        onCancelAction = { index -> viewModel.cancelAction(message, index) },
                        activeChallenges = activeChallenges,
                        categories = categories
                    )
                }
                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }
                if (messages.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.fillParentMaxHeight(0.85f))
                    }
                }
            }

            // Scroll to Bottom Button - only show when scrolled up reading older history
            val showScrollToBottom by remember {
                derivedStateOf {
                    val lastUser = messages.indexOfLast { it.role == "user" }
                    if (lastUser != -1) {
                        listState.firstVisibleItemIndex < lastUser
                    } else {
                        listState.canScrollForward
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollToBottom,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                val lastUser = messages.indexOfLast { it.role == "user" }
                                val targetIndex = if (lastUser != -1) lastUser else (messages.size - 1)
                                if (targetIndex >= 0) {
                                    listState.animateScrollToItem(targetIndex, 0)
                                }
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to bottom", modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Input Area
        NormalInputArea(
            inputText = inputText,
            isTyping = isTyping,
            onInputChanged = { viewModel.updateInputText(it) },
            onSend = {
                viewModel.sendMessage(inputText)
                viewModel.updateInputText("")
            }
        )

        if (showEnvelopeAnim && envelopeAnimMessage != null) {
            com.savingcoach.app.ui.components.EnvelopeAnimationDialog(
                amount = envelopeAnimAmount,
                currencyPreference = "MMK",
                onSaveClick = {
                    envelopeAnimMessage?.let { msg ->
                        val target = envelopeAnimSwitchTarget
                        if (target != null) {
                            viewModel.switchChallengeSaving(msg, target, envelopeAnimAmount)
                        } else {
                            viewModel.confirmChallengeSaving(msg, envelopeAnimAmount)
                        }
                    }
                    showEnvelopeAnim = false
                    envelopeAnimMessage = null
                    envelopeAnimSwitchTarget = null
                }
            )
        }
    }
}

@Composable
fun NormalInputArea(
    inputText: String,
    isTyping: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (inputText.isEmpty()) {
                Text("Ask AI...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
            BasicTextField(
                value = inputText,
                onValueChange = onInputChanged,
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(EmeraldGreen)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Send Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(if (inputText.isNotBlank() && !isTyping) EmeraldGreen else EmeraldGreen.copy(alpha = 0.5f), CircleShape)
                .clickable(enabled = inputText.isNotBlank() && !isTyping) { onSend() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun VoiceInputUi(
    partialText: String,
    rmsDb: Float,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = 1f + (rmsDb.coerceAtLeast(0f) / 10f) * 0.5f,
        animationSpec = tween(durationMillis = 100),
        label = "micScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cancel Button
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFFEE2E2), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFFEF4444))
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Center Area (Text + Mic Waveform)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (partialText.isNotBlank()) {
                Text(
                    text = partialText,
                    color = EmeraldGreen,
                    fontSize = 14.sp,
                    maxLines = 1,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen)
                            .scale(animatedScale)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Listening...", color = EmeraldGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Send Button
        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(48.dp)
                .background(EmeraldGreen, CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeCard(onSuggestionClick: (String) -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Hello!👋", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "I'm your AI finance assistant & saving coach.\nAsk me about your budget, expenses, or financial advice!",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                "Suggested questions / မေးမြန်းနိုင်သည်များ:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val suggestions = listOf(
                    "💰 How much do I have left this month?",
                    "🇲🇲 ဒီလအသုံးစရိတ်ဘယ်လောက်ကျန်သေးလဲ",
                    "💡 Give me tips to save more money",
                    "📊 ဒီလ ဘာတွေ အသုံးများလဲ?",
                    "🎯 How are my saving challenges doing?"
                )
                suggestions.forEach { text ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable {
                            // Extract clean query text
                            val clean = text.replace(Regex("^[\\p{So}\\p{Cn}]+\\s*"), "")
                            onSuggestionClick(clean)
                        }
                    ) {
                        Text(
                            text = text,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun DateSeparator() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        Text("Today", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun CustomCategoryDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Custom Category / အမျိုးအစား အသစ်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        focusedLabelColor = EmeraldGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (text.isNotBlank()) {
                                onConfirm(text.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Save", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SwitchChallengeDialog(
    challenges: List<SavingChallenge>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Switch Challenge / စုဘူးပြောင်းရန်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (challenges.isEmpty()) {
                    Text("No active challenges found.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        items(challenges) { challenge ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(challenge.title) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = challenge.title,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${challenge.currentAmount}/${challenge.targetAmount} USD",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun AmountInputDialog(
    currency: String = "MMK",
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Enter Amount / ငွေပမာဏ ထည့်ပါ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { newValue ->
                        // Only allow numbers and decimal point
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            amountText = newValue
                            isError = false
                        }
                    },
                    label = { Text("Amount") },
                    supportingText = { Text(currency) },
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Text(
                        text = "Please enter a valid amount",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull()
                            if (amount != null && amount > 0) {
                                onConfirm(amount)
                            } else {
                                isError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                    ) {
                        Text("Confirm", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SwitchCategoryDialog(
    categories: List<ExpenseCategoryEntity>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Switch Category / အမျိုးအစားပြောင်းရန်",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (categories.isEmpty()) {
                    Text("No categories found.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp)
                    ) {
                        items(categories) { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(category.name) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${category.emoji} ${category.name}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) EmeraldGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            .border(1.dp, if (enabled) EmeraldGreen else MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (enabled) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageItem(
    message: ChatMessage,
    onSaveExpenseAtIndex: (Int) -> Unit = { _ -> },
    onSaveExpenseWithCategoryAtIndex: (Int, String) -> Unit = { _, _ -> },
    onUpdateExpenseCategoryAtIndex: (Int, String) -> Unit = { _, _ -> },
    onConfirmChallenge: (ParsedExpense) -> Unit = { _ -> },
    onSwitchChallenge: (ParsedExpense, String) -> Unit = { _, _ -> },
    onCancelAction: (Int) -> Unit = {},
    activeChallenges: List<SavingChallenge> = emptyList(),
    categories: List<ExpenseCategoryEntity> = emptyList(),
    savingExpenseMessageIds: Set<String> = emptySet()
) {
    val isUser = message.role == "user"
    val timeString = formatTime(message.timestamp)

    if (isUser) {
        // User Message
        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp)
            , contentAlignment = Alignment.CenterEnd) {
            Box(
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .background(
                        color = LightGreenBubble,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
                    )
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 4.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = message.content.trim(), color = TextBlack, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = timeString, color = TextGray, fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "✓✓", color = EmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 32.dp, top = 6.dp, bottom = 6.dp)
        ) {
            val parsedText = parseMarkdown(message.content.trim())
            Text(
                text = parsedText,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeString,
                color = TextGray,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.End)
            )

            val parsedList = message.parsedExpenses ?: listOfNotNull(message.parsedExpense)
            
            for (index in parsedList.indices) {
                val parsed = parsedList[index]
                val isCardSaved = message.savedExpenseIndices.contains(index) || (index == 0 && message.expenseSaved)
                val isCardSaving = savingExpenseMessageIds.contains("${message.id}_$index") || (index == 0 && savingExpenseMessageIds.contains(message.id))
                val isCardCancelled = message.cancelledExpenseIndices.contains(index) || (index == 0 && message.expenseCancelled)
                
                val showCard = parsed.amount > 0.0 ||
                        parsed.action == "prompt_user_category_choice" ||
                        parsed.action == "prompt_challenge_confirmation" ||
                        parsed.action == "mark_challenge_saving"

                if (showCard) {
                    val isMy = parsed.language == "my"
                    val isChallenge = parsed.isChallenge || parsed.action == "prompt_challenge_confirmation" || parsed.action == "mark_challenge_saving"
                    
                    val lblAmount = if (isMy) "ပမာဏ" else "Amount"
                    val lblCategory = if (isChallenge) {
                        if (isMy) "စုဘူးအမျိုးအစား" else "Challenge"
                    } else {
                        if (isMy) "အမျိုးအစား" else "Category"
                    }
                    val lblMerchant = if (isChallenge) {
                        if (isMy) "စုဘူးအမည်" else "Challenge Title"
                    } else {
                        if (isMy) "ဆိုင်/ပစ္စည်း" else "Merchant/Item"
                    }
                    val lblDate = if (isMy) "ရက်စွဲ" else "Date"

                    var showCustomCategoryDialog by remember { mutableStateOf(false) }
                    var showSwitchChallengeDialog by remember { mutableStateOf(false) }
                    var showSwitchCategoryDialog by remember { mutableStateOf(false) }
                    var showAmountInputDialog by remember { mutableStateOf(false) }

                    if (showCustomCategoryDialog) {
                        CustomCategoryDialog(
                            initialValue = "",
                            onDismiss = { showCustomCategoryDialog = false },
                            onConfirm = { customCat ->
                                showCustomCategoryDialog = false
                                onSaveExpenseWithCategoryAtIndex(index, customCat)
                            }
                        )
                    }

                    if (showSwitchChallengeDialog) {
                        SwitchChallengeDialog(
                            challenges = activeChallenges,
                            onDismiss = { showSwitchChallengeDialog = false },
                            onSelect = { selectedTitle ->
                                showSwitchChallengeDialog = false
                                onSwitchChallenge(parsed, selectedTitle)
                            }
                        )
                    }

                    if (showSwitchCategoryDialog) {
                        SwitchCategoryDialog(
                            categories = categories,
                            onDismiss = { showSwitchCategoryDialog = false },
                            onSelect = { selectedCat ->
                                showSwitchCategoryDialog = false
                                onUpdateExpenseCategoryAtIndex(index, selectedCat)
                            }
                        )
                    }

                    if (showAmountInputDialog) {
                        AmountInputDialog(
                            currency = parsed.currency.ifBlank { "MMK" },
                            onDismiss = { showAmountInputDialog = false },
                            onConfirm = { amount ->
                                showAmountInputDialog = false
                                onConfirmChallenge(parsed.copy(amount = amount))
                            }
                        )
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                            val cardTitle = when (parsed.action) {
                                "prompt_user_category_choice" -> if (isMy) "အမျိုးအစား ရွေးချယ်ပါ" else "Select Category"
                                "prompt_challenge_confirmation", "mark_challenge_saving" -> if (isMy) "စုဘူးထဲထည့်ရန် အတည်ပြုပါ" else "Confirm Challenge Deposit"
                                else -> if (isChallenge) (if (isMy) "စုငွေ မှတ်တမ်းသစ်" else "New Saving Log") else (if (isMy) "အသုံးစရိတ်သစ် မှတ်တမ်း" else "New Expense Log")
                            }
                            Text(text = cardTitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldGreen)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)

                            val resolvedChallenge = if (isChallenge) {
                                val challengeTitle = parsed.challengeTitle.ifBlank { parsed.merchant }
                                val cleanQuery = challengeTitle.filter { it.isLetterOrDigit() || it.isWhitespace() }.lowercase().trim()
                                if (cleanQuery.isNotBlank()) {
                                    activeChallenges.firstOrNull {
                                        val cleanDb = it.title.filter { c -> c.isLetterOrDigit() || c.isWhitespace() }.lowercase().trim()
                                        cleanDb == cleanQuery
                                    }
                                } else {
                                    // Fallback: match from AI message content
                                    val cleanMsgContent = message.content.filter { it.isLetterOrDigit() || it.isWhitespace() }.lowercase()
                                    activeChallenges.firstOrNull {
                                        val cleanDb = it.title.filter { c -> c.isLetterOrDigit() || c.isWhitespace() }.lowercase().trim()
                                        cleanDb.isNotBlank() && cleanMsgContent.contains(cleanDb)
                                    }
                                }
                            } else null

                            val effectiveParsed = if (isChallenge && resolvedChallenge != null && parsed.challengeTitle.isBlank()) {
                                parsed.copy(challengeTitle = resolvedChallenge.title, merchant = resolvedChallenge.title)
                            } else parsed

                            val exists = if (isChallenge) {
                                resolvedChallenge != null
                            } else {
                                val categoryName = effectiveParsed.category
                                val cleanQuery = categoryName.filter { it.isLetterOrDigit() || it.isWhitespace() }.lowercase().trim()
                                categories.any {
                                    val cleanDb = it.name.filter { c -> c.isLetterOrDigit() || c.isWhitespace() }.lowercase().trim()
                                    cleanDb == cleanQuery
                                }
                            }

                            val matchedChallengeForAmount = resolvedChallenge

                            val shouldHideAmount = isChallenge && (
                                effectiveParsed.amount == 0.0 ||
                                matchedChallengeForAmount?.template == com.savingcoach.app.data.model.ChallengeTemplate.NO_SPEND ||
                                matchedChallengeForAmount?.template == com.savingcoach.app.data.model.ChallengeTemplate.ENVELOPE
                            )

                            if (!shouldHideAmount) {
                                Text("$lblAmount: ${effectiveParsed.amount} ${effectiveParsed.currency}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            
                            val merchantVal = if (isChallenge) {
                                val title = resolvedChallenge?.title ?: effectiveParsed.challengeTitle.ifBlank { effectiveParsed.merchant }
                                val suffix = if (exists || isCardSaved) "" else (if (isMy) " (မရှိသေးပါ)" else " (Non-existent)")
                                "$title$suffix"
                            } else {
                                effectiveParsed.item.ifEmpty { effectiveParsed.merchant.ifEmpty { "—" } }
                            }
                            Text("$lblMerchant: $merchantVal", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            if (!isChallenge && (effectiveParsed.action != "prompt_user_category_choice" || isCardSaved)) {
                                val suffix = if (exists || isCardSaved) "" else (if (isMy) " (မရှိသေးပါ)" else " (Non-existent)")
                                val catText = "${effectiveParsed.category}$suffix"
                                Text("$lblCategory: $catText", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (parsed.date.isNotBlank()) {
                                Text("$lblDate: ${parsed.date}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (!exists && !isCardSaved && parsed.action != "prompt_user_category_choice") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFFFFFBEB),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isChallenge) {
                                                if (isMy) "⚠️ စုဘူးမရှိသေးပါ။ ကျေးဇူးပြု၍ အခြားစုဘူးသို့ ပြောင်းလဲပေးပါ။"
                                                else "⚠️ Challenge does not exist. Please switch to an existing challenge."
                                            } else {
                                                if (isMy) "⚠️ အမျိုးအစား မရှိသေးပါ။ ကျေးဇူးပြု၍ အမျိုးအစားအသစ်သို့ ပြောင်းလဲပေးပါ။"
                                                else "⚠️ Category does not exist. Please switch to an existing category."
                                            },
                                            color = Color(0xFFB45309),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (isCardCancelled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancelled",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isMy) "\u1016\u103b\u1000\u103a\u101e\u102d\u1019\u103a\u1038\u1015\u103c\u102e\u1038\u1015\u102b\u1015\u103c\u102e" else "Cancelled",
                                        color = Color(0xFFEF4444),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (isCardSaved) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Done",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isMy) "အတည်ပြုပြီးပါပြီ (သိမ်းဆည်းပြီး)" else "Confirmed & Saved",
                                        color = EmeraldGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (isCardSaving) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = EmeraldGreen, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isMy) "သိမ်းဆည်းနေသည်..." else "Saving...",
                                        color = TextGray,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                when (parsed.action) {
                                    "prompt_user_category_choice" -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FlowRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                parsed.choices.forEach { choice ->
                                                    CategoryChip(
                                                        label = choice,
                                                        enabled = true,
                                                        onClick = { onSaveExpenseWithCategoryAtIndex(index, choice) }
                                                    )
                                                }
                                                CategoryChip(
                                                    label = if (isMy) "စိတ်ကြိုက်..." else "Custom...",
                                                    enabled = true,
                                                    onClick = { showCustomCategoryDialog = true }
                                                )
                                            }
                                        }
                                    }
                                    "prompt_challenge_confirmation" -> {
                                        val challengeTitle = effectiveParsed.challengeTitle.ifBlank { effectiveParsed.merchant }
                                        val cleanTitle = challengeTitle.filter { it.isLetterOrDigit() || it.isWhitespace() }.lowercase().trim()
                                        val matchedChallenge = activeChallenges.firstOrNull {
                                            val cleanDb = it.title.filter { c -> c.isLetterOrDigit() || c.isWhitespace() }.lowercase().trim()
                                            cleanDb == cleanTitle
                                        }
                                        val isFlexiWithNoAmount = matchedChallenge?.template == com.savingcoach.app.data.model.ChallengeTemplate.FLEXI &&
                                            effectiveParsed.amount == 0.0

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (isFlexiWithNoAmount) {
                                                        showAmountInputDialog = true
                                                    } else {
                                                        onConfirmChallenge(effectiveParsed)
                                                    }
                                                },
                                                enabled = exists,
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = EmeraldGreen,
                                                    disabledContainerColor = Color.LightGray
                                                ),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                Text(
                                                    if (isFlexiWithNoAmount) {
                                                        if (isMy) "ငွေထည့်ရန်" else "Enter Amount"
                                                    } else {
                                                        if (isMy) "အတည်ပြု" else "Confirm"
                                                    },
                                                    color = if (exists) Color.White else Color.Gray,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = { showSwitchChallengeDialog = true },
                                                modifier = Modifier.weight(1.1f),
                                                border = BorderStroke(1.dp, EmeraldGreen),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                Text(if (isMy) "စုဘူးပြောင်း" else "Switch", fontSize = 12.sp, maxLines = 1)
                                            }
                                            Button(
                                                onClick = { onCancelAction(index) },
                                                modifier = Modifier.weight(0.9f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                Text(if (isMy) "ဖျက်သိမ်း" else "Cancel", color = Color(0xFFEF4444), fontSize = 12.sp)
                                            }
                                        }
                                    }
                                    else -> {
                                        if (isChallenge) {
                                            val challengeTitle = effectiveParsed.challengeTitle.ifBlank { effectiveParsed.merchant }
                                            val cleanTitle = challengeTitle.filter { it.isLetterOrDigit() || it.isWhitespace() }.lowercase().trim()
                                            val matchedChallenge = activeChallenges.firstOrNull {
                                                val cleanDb = it.title.filter { c -> c.isLetterOrDigit() || c.isWhitespace() }.lowercase().trim()
                                                cleanDb == cleanTitle
                                            }
                                            val isFlexiWithNoAmount = isChallenge &&
                                                matchedChallenge?.template == com.savingcoach.app.data.model.ChallengeTemplate.FLEXI &&
                                                effectiveParsed.amount == 0.0

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        if (isFlexiWithNoAmount) {
                                                            showAmountInputDialog = true
                                                        } else {
                                                            onConfirmChallenge(effectiveParsed)
                                                        }
                                                    },
                                                    enabled = exists,
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = EmeraldGreen,
                                                        disabledContainerColor = Color.LightGray
                                                    ),
                                                    contentPadding = PaddingValues(vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        if (isFlexiWithNoAmount) {
                                                            if (isMy) "ငွေထည့်ရန်" else "Enter Amount"
                                                        } else {
                                                            if (isMy) "အတည်ပြု" else "Confirm"
                                                        },
                                                        color = if (exists) Color.White else Color.Gray,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                OutlinedButton(
                                                    onClick = { showSwitchChallengeDialog = true },
                                                    modifier = Modifier.weight(1.1f),
                                                    border = BorderStroke(1.dp, EmeraldGreen),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                                                    contentPadding = PaddingValues(vertical = 4.dp)
                                                ) {
                                                    Text(if (isMy) "စုဘူးပြောင်း" else "Switch", fontSize = 12.sp, maxLines = 1)
                                                }
                                                Button(
                                                    onClick = { onCancelAction(index) },
                                                    modifier = Modifier.weight(0.9f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                                                    contentPadding = PaddingValues(vertical = 4.dp)
                                                ) {
                                                    Text(if (isMy) "ဖျက်သိမ်း" else "Cancel", color = Color(0xFFEF4444), fontSize = 12.sp)
                                                }
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Button(
                                                    onClick = { onSaveExpenseAtIndex(index) },
                                                    enabled = exists,
                                                    modifier = Modifier.weight(1f),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = EmeraldGreen,
                                                        disabledContainerColor = Color.LightGray
                                                    ),
                                                    contentPadding = PaddingValues(vertical = 4.dp)
                                                ) {
                                                    Text(if (isMy) "အတည်ပြု" else "Confirm", color = if (exists) Color.White else Color.Gray, fontSize = 12.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = { showSwitchCategoryDialog = true },
                                                    modifier = Modifier.weight(1.1f),
                                                    border = BorderStroke(1.dp, EmeraldGreen),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldGreen),
                                                    contentPadding = PaddingValues(vertical = 4.dp)
                                                ) {
                                                    Text(if (isMy) "အမျိုးအစားပြောင်း" else "Switch", fontSize = 12.sp, maxLines = 1)
                                                }
                                                Button(
                                                    onClick = { onCancelAction(index) },
                                                    modifier = Modifier.weight(0.9f),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                                                    contentPadding = PaddingValues(vertical = 4.dp)
                                                ) {
                                                    Text(if (isMy) "ဖျက်သိမ်း" else "Cancel", color = Color(0xFFEF4444), fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val transition = rememberInfiniteTransition()
    val alpha1 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse)
    )
    val alpha2 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(500, delayMillis = 150), repeatMode = RepeatMode.Reverse)
    )
    val alpha3 by transition.animateFloat(
        initialValue = 0.2f, targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(500, delayMillis = 300), repeatMode = RepeatMode.Reverse)
    )

    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha1)))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha2)))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha3)))
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return "Now"
    val sdf = SimpleDateFormat("hh:mm a", Locale.US)
    return sdf.format(Date(timestamp))
}

private fun parseMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    val cleanText = text.lines().map { line ->
        val trimmed = line.trimStart()
        if (trimmed.startsWith("- ")) {
            line.replaceFirst("- ", "•  ")
        } else if (trimmed.startsWith("* ")) {
            line.replaceFirst("* ", "•  ")
        } else {
            line
        }
    }.joinToString("\n")

    return androidx.compose.ui.text.buildAnnotatedString {
        val parts = cleanText.split("**")
        for (i in parts.indices) {
            if (i % 2 == 1) {
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                append(parts[i])
                pop()
            } else {
                append(parts[i])
            }
        }
    }
}