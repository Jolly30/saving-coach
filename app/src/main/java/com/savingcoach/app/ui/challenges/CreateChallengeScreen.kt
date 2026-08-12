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
import com.savingcoach.app.ui.theme.*
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
    val isNameDuplicate = title.isNotBlank() && uiState.challengesList.any {
        val existingTitle = it.title.substringAfter(" ").trim()
        existingTitle.equals(title.trim(), ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
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
                        color = MaterialTheme.colorScheme.surface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Design your next savings streak",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.surface)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "CHOOSE TEMPLATE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = if (isSelected) ChallengeActive else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTemplate = tmpl }
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = ChallengeActive,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedLabelColor = ChallengeActive,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "EMOJI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    OutlinedTextField(
                    value = selectedEmoji,
                    onValueChange = { input ->
                        // Allow empty or compound emoji characters (e.g. flags, skin tones have length up to 8 in UTF-16)
                        if (input.isEmpty() || input.length <= 8) {
                            selectedEmoji = input
                        }
                    },
                    modifier = Modifier.width(64.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChallengeActive,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CHALLENGE NAME",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = textFieldColors
                    )
                }
            }
            if (isNameDuplicate) {
                Text(
                    text = "Challenge name already exists",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 76.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedTemplate == ChallengeTemplate.NO_SPEND) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "DURATION (DAYS)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = durationDays,
                        onValueChange = { durationDays = it.filter { char -> char.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors
                    )
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TARGET AMOUNT (MMK)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = targetAmount,
                            onValueChange = { targetAmount = it.filter { char -> char.isDigit() }.take(10) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DURATION (DAYS)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        OutlinedTextField(
                            value = durationDays,
                            onValueChange = { durationDays = it.filter { char -> char.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors
                        )
                    }
                }
            }

            // Dynamic Constant calculation preview
            val targetVal = targetAmount.toDoubleOrNull() ?: 0.0
            val durationVal = durationDays.toLongOrNull() ?: 0L
            if (selectedTemplate == ChallengeTemplate.CONSTANT && durationVal > 0) {
                val dailyAmount = targetVal / durationVal
                Text(
                    text = "Save ${String.format("%,.0f", dailyAmount)} MMK / day",
                    color = ChallengeActive,
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
                        createdAt = System.currentTimeMillis(),
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
                    containerColor = ChallengeActive,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "Start Challenge",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (title.isNotBlank() && !isNameDuplicate) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
