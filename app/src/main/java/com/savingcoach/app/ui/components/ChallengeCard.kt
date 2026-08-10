package com.savingcoach.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savingcoach.app.data.model.SavingChallenge
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Extension properties to support the new UI bindings without modifying data model
val SavingChallenge.emoji: String get() = when {
    title.contains("1K", ignoreCase = true) -> "🎯"
    title.contains("7-Day", ignoreCase = true) -> "⚡"
    title.contains("100", ignoreCase = true) -> "✉️"
    title.contains("Spend", ignoreCase = true) -> "🚫"
    else -> "🎯"
}
val SavingChallenge.percentage: Int get() = progress.toInt()
val SavingChallenge.daysLeft: Long get() = try {
    if (endDate.isNotEmpty()) {
        val end = LocalDate.parse(endDate)
        val now = LocalDate.now()
        ChronoUnit.DAYS.between(now, end).coerceAtLeast(0)
    } else 0L
} catch (e: Exception) { 0L }

@Composable
fun ChallengeCard(
    challenge: SavingChallenge,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percentage = if (challenge.targetAmount > 0) {
        ((challenge.currentAmount / challenge.targetAmount) * 100).toInt().coerceIn(0, 100)
    } else 0

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = null,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {

            Text(
                text = challenge.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            LinearProgressIndicator(
                progress = { (challenge.currentAmount / challenge.targetAmount).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (challenge.isCompleted) Color(0xFFF97316) else Color(0xFF10B981),
                trackColor = Color(0xFFF1F5F9),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${formatAmount(challenge.currentAmount)} ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "/ ${formatAmount(challenge.targetAmount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (challenge.isCompleted) "Completed" else "${challenge.daysLeft} days left",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return String.format("%,.0f", amount)
}


