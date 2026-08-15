package com.savingcoach.app.ui.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.ChallengeTemplate
import com.savingcoach.app.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChallengeScreen(
    challenge: SavingChallenge,
    viewModel: ChallengeViewModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { 
        val raw = challenge.title
        val firstWord = raw.split(" ").firstOrNull() ?: ""
        val isEmoji = firstWord.isNotEmpty() && firstWord.any { !it.isLetterOrDigit() }
        mutableStateOf(if (isEmoji) raw.removePrefix(firstWord).trim() else raw)
    }
    var targetAmount by remember { mutableStateOf(challenge.targetAmount.toInt().toString()) }
    
    // Parse duration from lastDepositDate suffix
    val durationFromMetadata = remember(challenge) {
        val parts = challenge.lastDepositDate.split("|")
        if (parts.size > 2) parts[2] else null
    }
    var durationDays by remember { 
        mutableStateOf(
            durationFromMetadata ?: try {
                val start = LocalDate.parse(challenge.startDate)
                val end = LocalDate.parse(challenge.endDate)
                java.time.temporal.ChronoUnit.DAYS.between(start, end).toString()
            } catch (e: Exception) { "30" }
        ) 
    }
    
    var selectedEmoji by remember { 
        val raw = challenge.title
        val firstWord = raw.split(" ").firstOrNull() ?: ""
        val isEmoji = firstWord.isNotEmpty() && firstWord.any { !it.isLetterOrDigit() }
        mutableStateOf(if (isEmoji) firstWord else "")
    }
    var selectedTemplate by remember { mutableStateOf(challenge.template) }

    val uiState by viewModel.uiState.collectAsState()
    val isNameDuplicate = title.isNotBlank() && uiState.challengesList.any {
        it.id != challenge.id && it.title.substringAfter(" ").trim().equals(title.trim(), ignoreCase = true)
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Challenge", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this challenge? This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChallenge(challenge.id)
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    BackHandler {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "Edit Challenge",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Spacer to balance back button
                Spacer(modifier = Modifier.size(48.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "CHOOSE TEMPLATE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            val allowedTemplates = when (challenge.template) {
                ChallengeTemplate.FLEXI, ChallengeTemplate.ENVELOPE -> listOf(
                    ChallengeTemplate.FLEXI to "Flexi",
                    ChallengeTemplate.ENVELOPE to "Envelope"
                )
                ChallengeTemplate.CONSTANT -> listOf(
                    ChallengeTemplate.CONSTANT to "Constant",
                    ChallengeTemplate.NO_SPEND to "No-Spend"
                )
                ChallengeTemplate.NO_SPEND -> listOf(
                    ChallengeTemplate.NO_SPEND to "No-Spend",
                    ChallengeTemplate.CONSTANT to "Constant"
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                allowedTemplates.forEach { (tmpl, label) ->
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

            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = ChallengeActive,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedLabelColor = ChallengeActive,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                        color = Color.White
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
                    val isDurationLower = (durationDays.toLongOrNull() ?: 0L) < challenge.completedDaysCount
                    OutlinedTextField(
                        value = durationDays,
                        onValueChange = { durationDays = it.filter { char -> char.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = textFieldColors,
                        isError = isDurationLower
                    )
                    if (isDurationLower) {
                        Text(
                            text = "Must be >= ${challenge.completedDaysCount}",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
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
                        val isTargetLower = if (challenge.currentAmount == 0.0) {
                            (targetAmount.toDoubleOrNull() ?: 0.0) <= 0.0
                        } else {
                            (targetAmount.toDoubleOrNull() ?: 0.0) <= challenge.currentAmount
                        }
                        OutlinedTextField(
                            value = targetAmount,
                            onValueChange = { targetAmount = it.filter { char -> char.isDigit() }.take(10) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            isError = isTargetLower,
                            enabled = selectedTemplate != ChallengeTemplate.CONSTANT && selectedTemplate != ChallengeTemplate.NO_SPEND
                        )
                        if (isTargetLower) {
                            Text(
                                text = "Must be > ${String.format("%,.0f", challenge.currentAmount)}",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DURATION (DAYS)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        
                        val isDurationLower = (durationDays.toLongOrNull() ?: 0L) <= challenge.completedDaysCount
                        val minDuration = challenge.completedDaysCount + 1

                        OutlinedTextField(
                            value = durationDays,
                            onValueChange = { durationDays = it.filter { char -> char.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            isError = isDurationLower,
                            enabled = selectedTemplate != ChallengeTemplate.CONSTANT
                        )
                        if (isDurationLower) {
                            Text(
                                text = "Must be >= $minDuration",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }
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

            // Action Buttons
            val isTargetLowerThanSaved = if (challenge.currentAmount == 0.0) {
                selectedTemplate != ChallengeTemplate.NO_SPEND && (targetAmount.toDoubleOrNull() ?: 0.0) <= 0.0
            } else {
                selectedTemplate != ChallengeTemplate.NO_SPEND && (targetAmount.toDoubleOrNull() ?: 0.0) <= challenge.currentAmount
            }
            val isDurationLowerThanSaved = (durationDays.toLongOrNull() ?: 0L) <= challenge.completedDaysCount
            val isSaveEnabled = title.isNotBlank() && !isNameDuplicate && !isTargetLowerThanSaved && !isDurationLowerThanSaved
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Save button
                Button(
                    onClick = {
                        val amount = if (selectedTemplate == ChallengeTemplate.NO_SPEND) 0.0 else (targetAmount.toDoubleOrNull() ?: 30000.0)
                        val days = durationDays.toLongOrNull() ?: 30L

                        val finalTitle = if (title.isBlank()) "My Challenge" else title
                        val emojiPrefix = selectedEmoji.ifEmpty { "🎯" }
                        val displayTitle = "$emojiPrefix $finalTitle".trim()

                        // Parse existing start date and shift end date accordingly
                        val start = try {
                            LocalDate.parse(challenge.startDate)
                        } catch (e: Exception) {
                            LocalDate.now()
                        }
                        val newEndDate = start.plusDays(days).toString()

                        val updatedChallenge = challenge.copy(
                            title = displayTitle,
                            targetAmount = amount,
                            endDate = newEndDate,
                            template = selectedTemplate,
                            lastDepositDate = challenge.lastDepositDate.substringBefore("|") + "|" + challenge.completedDaysCount + "|$days"
                        )

                        viewModel.updateChallengeMock(updatedChallenge)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    enabled = isSaveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChallengeActive,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isSaveEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Delete button
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Delete Challenge", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
