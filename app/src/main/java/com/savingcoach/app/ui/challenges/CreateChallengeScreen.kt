package com.savingcoach.app.ui.challenges
 
import com.savingcoach.app.data.model.ChallengeTemplate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savingcoach.app.data.model.SavingChallenge
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateChallengeScreen(
    viewModel: ChallengeViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var targetAmount by remember { mutableStateOf("") }
    var durationDays by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("") }
    var selectedTemplate by remember { mutableStateOf(ChallengeTemplate.CONSTANT) }

    val uiState by viewModel.uiState.collectAsState()
    val isNameDuplicate = remember(title, uiState.challengesList) {
        val cleanInput = title.trim()
        cleanInput.isNotEmpty() && uiState.challengesList.any { challenge ->
            val cleanChallengeTitle = challenge.title.substringAfter(" ").trim()
            cleanChallengeTitle.equals(cleanInput, ignoreCase = true) || challenge.title.trim().equals(cleanInput, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        dragHandle = null // We will use our own header with close button
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "New Challenge",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Design your next savings streak",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
                IconButton(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp), tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "CHOOSE TEMPLATE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    ChallengeTemplate.CONSTANT to "Constant",
                    ChallengeTemplate.FLEXI to "Flexi",
                    ChallengeTemplate.ENVELOPE to "Envelope",
                    ChallengeTemplate.NO_SPEND to "No-Spend"
                ).forEach { (tmpl, label) ->
                    val isSelected = selectedTemplate == tmpl
                    Surface(
                        color = if (isSelected) Color(0xFF10B981) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTemplate = tmpl }
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Customization Fields
            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B),
                focusedBorderColor = Color(0xFF10B981),
                unfocusedBorderColor = Color(0xFF334155),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color(0xFF10B981),
                unfocusedLabelColor = Color(0xFF94A3B8)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedEmoji,
                    onValueChange = { input ->
                        // Allow empty or compound emoji characters (e.g. flags, skin tones have length up to 8 in UTF-16)
                        if (input.isEmpty() || input.length <= 8) {
                            selectedEmoji = input
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("CHALLENGE NAME") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = textFieldColors
                )
            }
            if (isNameDuplicate) {
                Text(
                    text = "Challenge name already exists",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (selectedTemplate == ChallengeTemplate.NO_SPEND) {
                OutlinedTextField(
                    value = durationDays,
                    onValueChange = { durationDays = it.filter { char -> char.isDigit() } },
                    label = { Text("DURATION (DAYS)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = targetAmount,
                        onValueChange = { targetAmount = it.filter { char -> char.isDigit() } },
                        label = { Text("TARGET AMOUNT (MMK)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )

                    OutlinedTextField(
                        value = durationDays,
                        onValueChange = { durationDays = it.filter { char -> char.isDigit() } },
                        label = { Text("DURATION (DAYS)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )
                }
            }

            // Dynamic Constant calculation preview
            val targetVal = targetAmount.toDoubleOrNull() ?: 0.0
            val durationVal = durationDays.toLongOrNull() ?: 0L
            if (selectedTemplate == ChallengeTemplate.CONSTANT && durationVal > 0) {
                val dailyAmount = targetVal / durationVal
                Text(
                    text = "Save ${String.format("%,.0f", dailyAmount)} MMK / day",
                    color = Color(0xFF10B981),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Action Button
            Button(
                onClick = {
                    val amount = if (selectedTemplate == ChallengeTemplate.NO_SPEND) 0.0 else (targetAmount.toDoubleOrNull() ?: 30000.0)
                    val days = durationDays.toLongOrNull() ?: 30L
                    
                    val finalTitle = if (title.isBlank()) "My Challenge" else title
                    val emojiPrefix = selectedEmoji.ifEmpty { "🎯" }
                    val displayTitle = "$emojiPrefix $finalTitle".trim()
                    
                    val newChallenge = SavingChallenge(
                        id = UUID.randomUUID().toString(),
                        title = displayTitle,
                        targetAmount = amount,
                        currentAmount = 0.0,
                        startDate = LocalDate.now().toString(),
                        endDate = LocalDate.now().plusDays(days).toString(),
                        isActive = true,
                        isCompleted = false,
                        template = selectedTemplate,
                        lastDepositDate = "|0|$days"
                    )
                    
                    viewModel.createChallenge(newChallenge)
                    
                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = title.isNotBlank() && !isNameDuplicate,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    disabledContainerColor = Color(0xFF1E293B)
                )
            ) {
                Text(
                    text = "Start Challenge",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
