package com.savingcoach.app.ui.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.ChallengeTemplate
import com.savingcoach.app.ui.components.ChallengeCard
import java.time.LocalDate
import java.time.temporal.ChronoUnit

val SavingChallenge.durationDays: Int get() {
    val parts = lastDepositDate.split("|")
    return if (parts.size > 2) parts[2].toIntOrNull() ?: 30 else {
        try {
            val start = LocalDate.parse(startDate)
            val end = LocalDate.parse(endDate)
            ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(1)
        } catch (e: Exception) { 30 }
    }
}

val SavingChallenge.completedDaysCount: Int get() {
    val parts = lastDepositDate.split("|")
    return if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
}

@Composable
fun ChallengesScreen(
    viewModel: ChallengeViewModel = hiltViewModel(),
    onChallengeClick: (String) -> Unit = {},
    onCreateChallengeClick: () -> Unit = {}
) {
    var selectedChallengeId by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var editingChallenge by remember { mutableStateOf<SavingChallenge?>(null) }
    val uiState by viewModel.uiState.collectAsState()


    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedChallengeId != null) {
            ChallengeDetailScreen(
                challengeId = selectedChallengeId!!,
                onBackClick = { selectedChallengeId = null },
                onSettingsClick = { challenge -> editingChallenge = challenge }
            )
        } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column {
                        Text(
                            text = "Saving Challenges",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = "TOTAL SAVED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "260,500",
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1C1B1F)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "MMK",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFE0F2FE)
                                    ) {
                                        Text(
                                            text = "🔥 3 Active",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0284C7)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFEF3C7)
                                    ) {
                                        Text(
                                            text = "🏆 1 Completed",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "YOUR CHALLENGES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 160.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.challengesList) { challenge ->
                        val percent = if (challenge.template == ChallengeTemplate.NO_SPEND || challenge.targetAmount == 0.0) {
                            if (challenge.durationDays > 0) {
                                ((challenge.completedDaysCount.toFloat() / challenge.durationDays.toFloat()) * 100).toInt()
                            } else 0
                        } else {
                            if (challenge.targetAmount > 0) {
                                ((challenge.currentAmount / challenge.targetAmount) * 100).toInt()
                            } else 0
                        }

                        val displayChallenge = if (challenge.template == ChallengeTemplate.NO_SPEND || challenge.targetAmount == 0.0) {
                            val cleanTitle = if (challenge.title.contains("No-Spend Week", ignoreCase = true)) {
                                "🚫 No-Spend Day"
                            } else challenge.title
                            challenge.copy(
                                title = cleanTitle,
                                currentAmount = challenge.completedDaysCount.toDouble(),
                                targetAmount = challenge.durationDays.toDouble()
                            )
                        } else {
                            if (challenge.title.contains("No-Spend Week", ignoreCase = true)) {
                                challenge.copy(title = "🚫 No-Spend Day")
                            } else challenge
                        }
                        
                        ChallengeCard(
                            challenge = displayChallenge,
                            onClick = { selectedChallengeId = challenge.id }
                        )
                    }
                }
            }

            // Floating Action Button pinned to Bottom-Right
            FloatingActionButton(
                onClick = { 
                    showCreateSheet = true
                    onCreateChallengeClick()
                },
                containerColor = Color(0xFFFFA000),
                contentColor = Color.White,
                shape = androidx.compose.foundation.shape.CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 96.dp, end = 20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Challenge",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

            if (showCreateSheet) {
                CreateChallengeScreen(
                    viewModel = viewModel,
                    onDismiss = { showCreateSheet = false }
                )
            }
        }

        if (editingChallenge != null) {
            EditChallengeScreen(
                challenge = editingChallenge!!,
                viewModel = viewModel,
                onDismiss = { editingChallenge = null },
                onDelete = {
                    editingChallenge = null
                    selectedChallengeId = null
                }
            )
        }
    }
}
