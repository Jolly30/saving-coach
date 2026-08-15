package com.savingcoach.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.ChallengeTemplate
import com.savingcoach.app.ui.theme.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
    val percentage = if (challenge.template == ChallengeTemplate.NO_SPEND || challenge.targetAmount == 0.0) {
        // For NO_SPEND, use completedDaysCount / durationDays
        val parts = challenge.lastDepositDate.split("|")
        val duration = if (parts.size > 2) parts[2].toIntOrNull() ?: 30 else 30
        val completedDays = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
        if (duration > 0) ((completedDays.toFloat() / duration.toFloat()) * 100).toInt().coerceIn(0, 100) else 0
    } else if (challenge.targetAmount > 0) {
        ((challenge.currentAmount / challenge.targetAmount) * 100).toInt().coerceIn(0, 100)
    } else 0

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = null,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Challenge title
            Text(
                text = challenge.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar with percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val progressFloat = if (challenge.template == ChallengeTemplate.NO_SPEND || challenge.targetAmount == 0.0) {
                    val parts = challenge.lastDepositDate.split("|")
                    val duration = if (parts.size > 2) parts[2].toIntOrNull() ?: 30 else 30
                    val completedDays = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
                    if (duration > 0) (completedDays.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
                } else if (challenge.targetAmount > 0) {
                    (challenge.currentAmount / challenge.targetAmount).toFloat().coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { progressFloat },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (challenge.isCompleted) Orange else MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "$percentage%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (challenge.isCompleted) Orange else MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amount row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${formatAmount(challenge.currentAmount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = " / ${formatAmount(challenge.targetAmount)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Days left / Completed row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (challenge.isCompleted || percentage >= 100) {
                    Text(
                        text = "Completed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Orange
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${challenge.daysLeft} days left",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return String.format("%,.0f", amount)
}
