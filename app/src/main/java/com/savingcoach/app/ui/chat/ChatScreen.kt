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

val EmeraldGreen = Color(0xFF10B981)
val LightGreenBubble = Color(0xFFD9F5D6)
val LightGrayBg = Color(0xFFF7F7F7)
val BorderGray = Color(0xFFE5E5E5)
val TextBlack = Color(0xFF1A1A1A)
val TextGray = Color(0xFF737373)

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
    onClose: () -> Unit = {}
) {
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
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        ChatWindowContent(viewModel) { onClose() }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatWindowContent(
    viewModel: ChatViewModel,
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

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceInput()
        }
    }

    LaunchedEffect(messages.size, isTyping) {
        if (messages.isNotEmpty() || isTyping) {
            listState.animateScrollToItem(messages.size + if (isTyping) 1 else 0)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2FE))
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "AI",
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("AI Finance Assistant", color = TextBlack, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Your personal finance helper", color = TextGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(EmeraldGreen))
                }
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .background(LightGrayBg, CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextBlack, modifier = Modifier.size(18.dp))
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
                        WelcomeCard()
                    }
                    item {
                        DateSeparator()
                    }
                }

                items(messages) { message ->
                    val isSaved = message.expenseSaved
                    val isSaving = savingExpenseMessageIds.contains(message.id)
                    MessageItem(
                        message = message,
                        isSaved = isSaved,
                        isSaving = isSaving,
                        onSaveExpense = { viewModel.saveParsedExpense(message) }
                    )
                }
                if (isTyping) {
                    item {
                        TypingIndicator()
                    }
                }
            }

            // Scroll to Bottom Button
            val showScrollToBottom by remember {
                derivedStateOf { listState.canScrollForward }
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
                                val targetIndex = messages.size + if (isTyping) 1 else 0
                                if (targetIndex >= 0) {
                                    listState.animateScrollToItem(targetIndex)
                                }
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        containerColor = Color.White,
                        contentColor = EmeraldGreen,
                        elevation = FloatingActionButtonDefaults.elevation(4.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to bottom", modifier = Modifier.size(24.dp))
                    }
                }
            }
        }

        // Input Area
        AnimatedContent(targetState = isListening, label = "inputArea") { listening ->
            if (listening) {
                VoiceInputUi(
                    partialText = partialVoiceText,
                    rmsDb = rmsDb,
                    onCancel = { viewModel.cancelVoiceInput() },
                    onSend = { viewModel.stopVoiceInput() }
                )
            } else {
                NormalInputArea(
                    inputText = inputText,
                    isTyping = isTyping,
                    onInputChanged = { viewModel.updateInputText(it) },
                    onSend = {
                        viewModel.sendMessage(inputText)
                        viewModel.updateInputText("")
                    },
                    onMicClick = {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            viewModel.startVoiceInput()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NormalInputArea(
    inputText: String,
    isTyping: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text Input
        /*Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(LightGrayBg, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (inputText.isEmpty()) {
                Text("Ask anything about your finances...", color = Color.Gray, fontSize = 14.sp)
            }
            BasicTextField(
                value = inputText,
                onValueChange = onInputChanged,
                textStyle = TextStyle(color = TextBlack, fontSize = 14.sp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(EmeraldGreen)
            )
        }*/
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(LightGrayBg, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (inputText.isEmpty()) {
                // Updated to a short, clean placeholder
                Text("Ask AI...", color = Color.Gray, fontSize = 14.sp)
            }
            BasicTextField(
                value = inputText,
                onValueChange = onInputChanged,
                textStyle = TextStyle(color = TextBlack, fontSize = 14.sp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                cursorBrush = SolidColor(EmeraldGreen)
            )
        }


        Spacer(modifier = Modifier.width(8.dp))

        // Mic Button
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(EmeraldGreen, CircleShape)
                .clickable { onMicClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = Color.White, modifier = Modifier.size(24.dp))
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
                .background(Color(0xFFECFDF5), RoundedCornerShape(24.dp))
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

@Composable
fun WelcomeCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Hello!👋", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextBlack)
            Spacer(modifier = Modifier.height(8.dp))
            Text("I'm your AI finance assistant.\nHow can I help you today?", color = TextBlack, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}


@Composable
fun DateSeparator() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderGray)
        Text("Today", fontSize = 12.sp, color = TextGray, modifier = Modifier.padding(horizontal = 8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = BorderGray)
    }
}

@Composable
fun MessageItem(
    message: ChatMessage,
    isSaved: Boolean = false,
    isSaving: Boolean = false,
    onSaveExpense: () -> Unit = {}
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
                    // FIXED: Removed modifier = Modifier.fillMaxWidth()
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
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(end = 48.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2FE))
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "AI",
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Renders every AI response as a normal text bubble
            Box(
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
                    )
                    .border(1.dp, BorderGray, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 4.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    // FIXED: Removed modifier = Modifier.fillMaxWidth()
                    Text(text = message.content.trim(), color = TextBlack, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = timeString, color = TextGray, fontSize = 10.sp)
                    
                    if (message.parsedExpense != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val isMy = message.parsedExpense.language == "my"
                        val lblAmount = if (isMy) "ပမာဏ" else "Amount"
                        val lblCategory = if (isMy) "အမျိုးအစား" else "Category"
                        val lblMerchant = if (isMy) "ဆိုင်" else "Merchant"
                        val lblDate = if (isMy) "ရက်စွဲ" else "Date"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LightGrayBg),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                Text("$lblAmount: ${message.parsedExpense.amount} MMK", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                                Text("$lblCategory: ${message.parsedExpense.category}", fontSize = 13.sp, color = TextBlack)
                                Text("$lblMerchant: ${message.parsedExpense.merchant.ifEmpty { "—" }}", fontSize = 13.sp, color = TextBlack)
                                Text("$lblDate: ${message.parsedExpense.date}", fontSize = 13.sp, color = TextBlack)
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = onSaveExpense,
                                    enabled = !isSaved && !isSaving,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldGreen,
                                        disabledContainerColor = EmeraldGreen.copy(alpha = 0.5f)
                                    )
                                ) {
                                    val buttonText = when {
                                        isSaved -> if (isMy) "✓ အသုံးစရိတ်ထဲ ထည့်ပြီးပါပြီ" else "✓ Added to Expense"
                                        isSaving -> if (isMy) "သိမ်းဆည်းနေသည်..." else "Saving..."
                                        else -> if (isMy) "အသုံးစရိတ်ထဲ ထည့်မည်" else "Add to Expense"
                                    }
                                    Text(text = buttonText, color = Color.White)
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
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Gray.copy(alpha = alpha1)))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Gray.copy(alpha = alpha2)))
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Gray.copy(alpha = alpha3)))
    }
}

private fun formatTime(timestamp: Long): String {
    if (timestamp == 0L) return "Now"
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}