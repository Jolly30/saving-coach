package com.savingcoach.app.ui.challenges

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.text.TextStyle
import androidx.hilt.navigation.compose.hiltViewModel
import com.savingcoach.app.ui.theme.*
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.ChallengeTemplate
import com.savingcoach.app.ui.components.ChallengeCard
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch
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
    var showCompletedPopup by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Saving Challenges",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "TOTAL SAVED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format("%,.0f", uiState.totalSaved),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "MMK",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Surface(
                                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "🔥 ${uiState.activeCount} Active",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            
                            Surface(
                                color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "🏆 ${uiState.completedCount} Completed",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "YOUR CHALLENGES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .height(32.dp)
                                    .width(140.dp)
                                    .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.challengesList.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 40.dp)
                        ) {
                            Text(
                                text = "🎯",
                                fontSize = 64.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No challenges yet",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start your first saving challenge and watch your savings grow!",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    showCreateSheet = true
                                    onCreateChallengeClick()
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Challenge", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    val displayList = if (searchQuery.isBlank()) {
                        uiState.challengesList
                    } else {
                        uiState.challengesList.filter { it.title.contains(searchQuery, ignoreCase = true) }
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 160.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayList) { challenge ->
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
                            onClick = {
                                if (challenge.isCompleted || percent >= 100 || (challenge.targetAmount == 0.0 && challenge.completedDaysCount >= challenge.durationDays)) {
                                    showCompletedPopup = true
                                } else {
                                    selectedChallengeId = challenge.id
                                }
                            }
                        )
                    }
                }
                }
            }

            // Floating Action Button pinned to Bottom-Right
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 96.dp, end = 20.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        showCreateSheet = true
                        onCreateChallengeClick()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Challenge",
                        modifier = Modifier.size(28.dp)
                    )
                }
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
        if (showCompletedPopup) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .pointerInput(Unit) {
                        detectTapGestures { }
                    },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1E1E24),
                    modifier = Modifier.padding(32.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🎉 Challenge Completed! 🎉", 
                            fontWeight = FontWeight.Bold, 
                            color = Color.White, 
                            fontSize = 20.sp, 
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Amazing job! You have successfully reached your saving goal.", 
                            color = Color(0xFFA0A0A0), 
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { showCompletedPopup = false },
                            colors = ButtonDefaults.buttonColors(containerColor = com.savingcoach.app.ui.theme.Orange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text(
                                "OK", 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
