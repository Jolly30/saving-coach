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
import androidx.compose.material.icons.filled.Lock
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

private val EMOJI_REGEX = Regex("[\\x{1F300}-\\x{1F5FF}\\x{1F900}-\\x{1F9FF}\\x{1F600}-\\x{1F64F}\\x{1F680}-\\x{1F6FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}\\x{1F1E6}-\\x{1F1FF}\\x{1F191}-\\x{1F251}\\x{1F004}\\x{1F0CF}\\x{1F170}-\\x{1F171}\\x{1F17E}-\\x{1F17F}\\x{1F18E}\\x{3030}\\x{2B50}\\x{2B55}\\x{2934}-\\x{2935}\\x{2B05}-\\x{2B07}\\x{2B1B}-\\x{2B1C}\\x{3297}\\x{3299}\\x{303D}\\x{00A9}\\x{00AE}\\x{2122}\\x{23F3}\\x{24C2}\\x{23E9}-\\x{23EF}\\x{25B6}\\x{23F8}-\\x{23FA}\\x{1FA70}-\\x{1FAFF}]")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChallengeScreen(
    challenge: SavingChallenge,
    viewModel: ChallengeViewModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onStop: (SavingChallenge) -> Unit
) {
    var title by remember { 
        val raw = challenge.title
        val firstWord = raw.split(" ").firstOrNull() ?: ""
        val isEmoji = firstWord.isNotEmpty() && EMOJI_REGEX.containsMatchIn(firstWord)
        mutableStateOf(if (isEmoji) raw.removePrefix(firstWord).trim() else raw)
    }
    var targetAmount by remember { mutableStateOf(String.format(java.util.Locale.US, "%.0f", challenge.targetAmount)) }
    
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
        val isEmoji = firstWord.isNotEmpty() && EMOJI_REGEX.containsMatchIn(firstWord)
        mutableStateOf(if (isEmoji) firstWord else "")
    }
    var selectedTemplate by remember { mutableStateOf(challenge.template) }

    val uiState by viewModel.uiState.collectAsState()
    val isNameDuplicate = title.isNotBlank() && uiState.challengesList.any {
        it.id != challenge.id && it.title.substringAfter(" ").trim().equals(title.trim(), ignoreCase = true)
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showStopConfirm by remember { mutableStateOf(false) }

    DeleteChallengeDialog(
        show = showDeleteConfirm,
        onDismiss = { showDeleteConfirm = false },
        onConfirm = {
            viewModel.deleteChallenge(challenge.id)
            showDeleteConfirm = false
            onDelete()
        }
    )

    StopChallengeDialog(
        show = showStopConfirm,
        currentAmount = challenge.currentAmount,
        currencyPreference = uiState.currencyPreference,
        onDismiss = { showStopConfirm = false },
        onConfirm = {
            viewModel.stopChallenge(challenge.id)
            showStopConfirm = false
            onStop(challenge)
        }
    )

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
            val strings = com.savingcoach.app.ui.localization.AppLocale.current

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
                        contentDescription = strings.back,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = strings.editChallengeTitle,
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

            val allowedTemplates = when (challenge.template) {
                ChallengeTemplate.FLEXI, ChallengeTemplate.ENVELOPE -> listOf(
                    ChallengeTemplate.FLEXI to strings.templateFlexi,
                    ChallengeTemplate.ENVELOPE to strings.templateEnvelope
                )
                ChallengeTemplate.CONSTANT -> listOf(
                    ChallengeTemplate.CONSTANT to strings.templateConstant
                )
                ChallengeTemplate.NO_SPEND -> listOf(
                    ChallengeTemplate.NO_SPEND to strings.templateNoSpend
                )
            }

            Text(
                text = if (allowedTemplates.size > 1) strings.chooseTemplate else strings.yourTemplate,
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
                allowedTemplates.forEach { (tmpl, label) ->
                    val isSelected = selectedTemplate == tmpl
                    Surface(
                        color = if (isSelected) ChallengeActive else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (allowedTemplates.size > 1) Modifier.clickable { selectedTemplate = tmpl }
                                else Modifier
                            )
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
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                focusedBorderColor = ChallengeActive,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                focusedLabelColor = ChallengeActive,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = strings.emojiLabel,
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
                        text = strings.challengeNameLabel,
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
                    text = strings.challengeNameExists,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 76.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (selectedTemplate == ChallengeTemplate.NO_SPEND) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = strings.durationDaysLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    val minDuration = challenge.completedDaysCount.coerceAtLeast(1)
                    val isDurationLower = (durationDays.toLongOrNull() ?: 0L) < minDuration
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
                            text = strings.mustBeAtLeast(minDuration.toLong()),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }
                }
            } else {
                val isTargetEnabled = selectedTemplate != ChallengeTemplate.NO_SPEND && challenge.template != ChallengeTemplate.CONSTANT
                val isDurationEnabled = selectedTemplate != ChallengeTemplate.CONSTANT && challenge.template != ChallengeTemplate.CONSTANT

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${strings.targetAmountLabel} (${com.savingcoach.app.utils.InvestmentCalculations.getCurrencyLabel(uiState.currencyPreference, isInvestment = false)})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        val isTargetLower = if (challenge.currentAmount == 0.0) {
                            (targetAmount.toDoubleOrNull() ?: 0.0) <= 0.0
                        } else {
                            (targetAmount.toDoubleOrNull() ?: 0.0) < challenge.currentAmount
                        }
                        OutlinedTextField(
                            value = targetAmount,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    targetAmount = newValue
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            isError = isTargetLower,
                            enabled = isTargetEnabled
                        )
                        if (isTargetLower) {
                            Text(
                                text = strings.mustBeGreaterThan(strings.formatAmount(challenge.currentAmount, uiState.currencyPreference, 1.0, isInvestment = false)),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.durationDaysLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        
                        val minDuration = challenge.completedDaysCount.coerceAtLeast(1)
                        val isDurationLower = (durationDays.toLongOrNull() ?: 0L) < minDuration

                        OutlinedTextField(
                            value = durationDays,
                            onValueChange = { durationDays = it.filter { char -> char.isDigit() } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            isError = isDurationLower,
                            enabled = isDurationEnabled
                        )
                        if (isDurationLower) {
                            Text(
                                text = strings.mustBeAtLeast(minDuration.toLong()),
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
                    text = strings.savePerDay(strings.formatAmount(dailyAmount, uiState.currencyPreference, 1.0, isInvestment = false)),
                    color = ChallengeActive,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Action Buttons
            val minDurationAllowed = challenge.completedDaysCount.coerceAtLeast(1)
            val isTargetLowerThanSaved = if (challenge.currentAmount == 0.0) {
                selectedTemplate != ChallengeTemplate.NO_SPEND && (targetAmount.toDoubleOrNull() ?: 0.0) <= 0.0
            } else {
                selectedTemplate != ChallengeTemplate.NO_SPEND && (targetAmount.toDoubleOrNull() ?: 0.0) < challenge.currentAmount
            }
            val isDurationLowerThanSaved = (durationDays.toLongOrNull() ?: 0L) < minDurationAllowed
            val isSaveEnabled = title.isNotBlank() && !isNameDuplicate && !isTargetLowerThanSaved && !isDurationLowerThanSaved && durationDays.isNotBlank() && durationVal > 0
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Save button
                Button(
                    onClick = {
                        val targetCurrency = com.savingcoach.app.utils.InvestmentCalculations.getTargetCurrency(uiState.currencyPreference, isInvestment = false)
                        val defaultAmount = if (targetCurrency == "USD") 10.0 else 30000.0
                        val amount = if (selectedTemplate == ChallengeTemplate.NO_SPEND) 0.0 else (targetAmount.toDoubleOrNull() ?: defaultAmount)
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
                            lastDepositDate = if (challenge.completedDaysCount == 0) "|0|$days" else challenge.lastDepositDate.substringBefore("|") + "|" + challenge.completedDaysCount + "|$days",
                            currency = targetCurrency
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
                    Text(strings.saveChanges, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isSaveEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Stop Challenge button
                if (challenge.isActive) {
                    OutlinedButton(
                        onClick = { showStopConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange),
                        border = BorderStroke(1.5.dp, AccentOrange),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text(strings.stopChallengeTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                    }
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
                    Text(strings.deleteChallengeTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun DeleteChallengeDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(strings.deleteChallengeTitle, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text(strings.deleteChallengeMsg, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.delete, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(strings.cancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun StopChallengeDialog(
    show: Boolean,
    currentAmount: Double,
    currencyPreference: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    if (show) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(strings.stopChallengeTitle, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        strings.stopChallengeMsg(com.savingcoach.app.utils.InvestmentCalculations.formatValue(currentAmount, currencyPreference, 1.0, isInvestment = false)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.stopChallengeConfirm, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(strings.stopChallengeKeep, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
