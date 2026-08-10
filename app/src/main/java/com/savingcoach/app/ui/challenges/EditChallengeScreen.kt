package com.savingcoach.app.ui.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    val isNameDuplicate = remember(title, uiState.challengesList) {
        val cleanInput = title.trim()
        cleanInput.isNotEmpty() && uiState.challengesList.any { c ->
            c.id != challenge.id && (
                c.title.substringAfter(" ").trim().equals(cleanInput, ignoreCase = true) || 
                c.title.trim().equals(cleanInput, ignoreCase = true)
            )
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Challenge", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this challenge? This action cannot be undone.", color = Color(0xFF94A3B8)) },
            containerColor = Color(0xFF1E293B),
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChallenge(challenge.id)
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF1E293B), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "Back", 
                        modifier = Modifier.size(20.dp), 
                        tint = Color.White
                    )
                }

                Text(
                    text = "Edit Challenge",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Spacer to balance back button
                Spacer(modifier = Modifier.size(36.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

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

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Delete button
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444)
                    )
                }

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
                        .weight(1f)
                        .height(48.dp),
                    enabled = title.isNotBlank() && !isNameDuplicate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        disabledContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
