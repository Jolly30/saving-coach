package com.savingcoach.app.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.savingcoach.app.data.model.NotificationItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val strings = com.savingcoach.app.ui.localization.AppLocale.current
    val isBurmese = strings is com.savingcoach.app.ui.localization.BurmeseStrings
    
    var isSelectMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showMenu by remember { mutableStateOf(false) }
    
    var showDeleteConfirmDialog by remember { mutableStateOf<NotificationItem?>(null) }

    // Deletion confirmation dialog
    if (showDeleteConfirmDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text(strings.deleteNotificationTitle) },
            text = { Text(strings.deleteNotificationConfirmMsg) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val item = showDeleteConfirmDialog
                        if (item != null) {
                            viewModel.deleteNotification(item.id)
                        }
                        showDeleteConfirmDialog = null
                    }
                ) {
                    Text(strings.delete, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(strings.notifications, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    if (isSelectMode) {
                        TextButton(
                            onClick = {
                                isSelectMode = false
                                selectedIds = emptySet()
                            }
                        ) {
                            Text(
                                text = strings.cancel,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    } else if (uiState.notifications.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.selectNotifications) },
                                    onClick = {
                                        isSelectMode = true
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (isSelectMode && uiState.notifications.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                val allSelected = selectedIds.size == uiState.notifications.size
                                selectedIds = if (allSelected) {
                                    emptySet()
                                } else {
                                    uiState.notifications.map { it.id }.toSet()
                                }
                            }
                        ) {
                            val allSelected = selectedIds.size == uiState.notifications.size && uiState.notifications.isNotEmpty()
                            CircularCheckbox(
                                checked = allSelected,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) {
                                        uiState.notifications.map { it.id }.toSet()
                                    } else {
                                        emptySet()
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = strings.selectAllNotifications,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.deleteNotifications(selectedIds)
                                isSelectMode = false
                                selectedIds = emptySet()
                            },
                            enabled = selectedIds.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(strings.delete, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage ?: "Unknown error occurred",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.notifications.isEmpty() -> {
                    EmptyNotificationsView(emptyMessage = strings.noNewNotifications)
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.notifications, key = { it.id }) { item ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                        showDeleteConfirmDialog = item
                                        false // snaps back so that dialog governs visual removal
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = !isSelectMode,
                                backgroundContent = {
                                    val isSwiping = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
                                    val color = if (isSwiping) MaterialTheme.colorScheme.error else Color.Transparent
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(color)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        if (isSwiping) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = strings.delete,
                                                tint = Color.White
                                            )
                                        }
                                    }
                                }
                            ) {
                                NotificationRow(
                                    item = item,
                                    isSelectMode = isSelectMode,
                                    isSelected = selectedIds.contains(item.id),
                                    isBurmese = isBurmese,
                                    onClick = {
                                        if (isSelectMode) {
                                            selectedIds = if (selectedIds.contains(item.id)) {
                                                selectedIds - item.id
                                            } else {
                                                selectedIds + item.id
                                            }
                                        } else {
                                            viewModel.markAsRead(item.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationRow(
    item: NotificationItem,
    isSelectMode: Boolean,
    isSelected: Boolean,
    isBurmese: Boolean,
    onClick: () -> Unit
) {
    val (icon, tint) = getNotificationIconAndColor(item.type)
    val (displayTitle, displayMessage) = localizeNotificationContent(item, isBurmese)
    
    val backgroundColor = if (item.isRead) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val borderStrokeColor = if (item.isRead) {
        Color.Transparent
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (item.isRead) null else androidx.compose.foundation.BorderStroke(1.dp, borderStrokeColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectMode) {
                CircularCheckbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            // Icon Container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.type,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (item.isRead) FontWeight.SemiBold else FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = displayMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = formatTimestamp(item.timestamp, isBurmese),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}

@Composable
fun CircularCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent)
            .border(2.dp, tint, CircleShape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun EmptyNotificationsView(emptyMessage: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.NotificationsOff,
            contentDescription = "No notifications",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = emptyMessage,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        )
    }
}

private fun getNotificationIconAndColor(type: String): Pair<ImageVector, Color> {
    return when (type) {
        "BUDGET_BREACH" -> Pair(Icons.Default.Warning, Color(0xFFF44336)) // Red
        "SAVING_MILESTONE" -> Pair(Icons.Default.EmojiEvents, Color(0xFFFFB300)) // Amber/Gold
        "SEVERE_INACTIVITY" -> Pair(Icons.Default.Info, Color(0xFF1E88E5)) // Blue
        "ABANDONED_CHALLENGE" -> Pair(Icons.Default.Warning, Color(0xFFFF7043)) // Orange
        "PORTFOLIO_RISK" -> Pair(Icons.AutoMirrored.Filled.TrendingDown, Color(0xFFE53935)) // Dark Red
        else -> Pair(Icons.Default.Notifications, Color(0xFF757575)) // Gray
    }
}

fun localizeNotificationContent(
    item: NotificationItem,
    isBurmese: Boolean
): Pair<String, String> {
    if (!isBurmese) {
        return when (item.type) {
            "BUDGET_BREACH" -> {
                val percentMatch = Regex("(\\d+)%").find(item.message)?.value ?: "100%"
                val title = "Budget Alert"
                val message = "⚠️ Budget Alert: You've reached $percentMatch of your monthly limit."
                Pair(title, message)
            }
            "SAVING_MILESTONE" -> {
                val percentMatch = Regex("(\\d+)%").find(item.message)?.value ?: "100%"
                val isComplete = percentMatch == "100%" || item.title.contains("Complete") || item.title.contains("အောင်မြင်")
                val title = if (isComplete) "Challenge Complete!" else "Saving Progress"
                val challengeName = if (item.message.contains(" goal")) {
                    item.message.substringAfter("your ").substringBefore(" goal")
                } else if (item.message.contains(" ရည်မှန်းချက်")) {
                    item.message.substringAfter("သင့် ").substringBefore(" ရည်မှန်းချက်")
                } else ""
                val msg = if (challengeName.isNotBlank()) {
                    "You've saved $percentMatch of your $challengeName goal"
                } else {
                    item.message
                }
                Pair(title, msg)
            }
            "ABANDONED_CHALLENGE" -> {
                val challengeName = if (item.message.contains(" alive!")) {
                    item.message.substringAfter("your ").substringBefore(" alive!")
                } else if (item.message.contains(" ကို ဆက်လက်လုပ်ဆောင်ပါ")) {
                    item.message.substringAfter("🎯 ").substringBefore(" ကို ဆက်လက်လုပ်ဆောင်ပါ")
                } else ""
                val title = "Challenge Alert"
                val msg = if (challengeName.isNotBlank()) {
                    "🎯 Keep your $challengeName alive! You're only a few check-ins away from this month's goal."
                } else {
                    item.message
                }
                Pair(title, msg)
            }
            "SEVERE_INACTIVITY" -> {
                Pair("We miss you!", "👋 We miss you! Take 30 seconds to catch up on your recent expenses.")
            }
            "PORTFOLIO_RISK" -> {
                Pair("Market Update", item.message)
            }
            else -> Pair(item.title, item.message)
        }
    } else {
        return when (item.type) {
            "BUDGET_BREACH" -> {
                val percentMatch = Regex("(\\d+)%").find(item.message)?.value ?: "100%"
                val title = "ဘတ်ဂျက် သတိပေးချက်"
                val message = "⚠️ သတိပေးချက်- သင်သည် လစဉ်ဘတ်ဂျက်၏ $percentMatch သုံးစွဲပြီးပါပြီ။"
                Pair(title, message)
            }
            "SAVING_MILESTONE" -> {
                val percentMatch = Regex("(\\d+)%").find(item.message)?.value ?: "100%"
                val isComplete = percentMatch == "100%" || item.title.contains("Complete") || item.title.contains("အောင်မြင်")
                val title = if (isComplete) "စိန်ခေါ်မှု အောင်မြင်ပါပြီ!" else "စုဆောင်းမှု တိုးတက်မှု"
                val challengeName = if (item.message.contains(" goal")) {
                    item.message.substringAfter("your ").substringBefore(" goal")
                } else if (item.message.contains(" ရည်မှန်းချက်")) {
                    item.message.substringAfter("သင့် ").substringBefore(" ရည်မှန်းချက်")
                } else ""
                val msg = if (challengeName.isNotBlank()) {
                    if (isComplete) {
                        "သင့် $challengeName ရည်မှန်းချက်၏ $percentMatch ကို အောင်မြင်စွာ စုဆောင်းနိုင်ခဲ့ပါပြီ။"
                    } else {
                        "သင့် $challengeName ရည်မှန်းချက်၏ $percentMatch ကို စုဆောင်းပြီးပါပြီ။"
                    }
                } else {
                    if (isComplete) {
                        "သင့် ရည်မှန်းချက်၏ $percentMatch ကို အောင်မြင်စွာ စုဆောင်းနိုင်ခဲ့ပါပြီ။"
                    } else {
                        "သင့် ရည်မှန်းချက်၏ $percentMatch ကို စုဆောင်းပြီးပါပြီ။"
                    }
                }
                Pair(title, msg)
            }
            "ABANDONED_CHALLENGE" -> {
                val challengeName = if (item.message.contains(" alive!")) {
                    item.message.substringAfter("your ").substringBefore(" alive!")
                } else if (item.message.contains(" ကို ဆက်လက်လုပ်ဆောင်ပါ")) {
                    item.message.substringAfter("🎯 ").substringBefore(" ကို ဆက်လက်လုပ်ဆောင်ပါ")
                } else ""
                val title = "စိန်ခေါ်မှု သတိပေးချက်"
                val msg = if (challengeName.isNotBlank()) {
                    "🎯 $challengeName ကို ဆက်လက်လုပ်ဆောင်ပါ! ပန်းတိုင်ပြည့်ရန် အနည်းငယ်သာ လိုပါတော့သည်။"
                } else {
                    "🎯 စိန်ခေါ်မှုကို ဆက်လက်လုပ်ဆောင်ပါ! ပန်းတိုင်ပြည့်ရန် အနည်းငယ်သာ လိုပါတော့သည်။"
                }
                Pair(title, msg)
            }
            "SEVERE_INACTIVITY" -> {
                Pair("သင့်ကို သတိရနေပါသည်!", "👋 မကြာသေးမီက အသုံးစရိတ်များကို စာရင်းသွင်းရန် စက္ကန့် ၃၀ ခန့် အချိန်ပေးပါ။")
            }
            "PORTFOLIO_RISK" -> {
                Pair("ဈေးကွက် သတင်း", item.message)
            }
            else -> Pair(item.title, item.message)
        }
    }
}

private fun formatTimestamp(timestamp: Long, isBurmese: Boolean): String {
    val date = Date(timestamp)
    return if (isBurmese) {
        val ymd = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US).format(date)
        "$ymd • $timeFormat"
    } else {
        val format = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.US)
        format.format(date)
    }
}
