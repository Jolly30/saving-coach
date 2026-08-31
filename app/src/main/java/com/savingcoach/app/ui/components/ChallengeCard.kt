package com.savingcoach.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.ChallengeTemplate
import com.savingcoach.app.data.model.ChallengeStatus

val SavingChallenge.daysLeft: Long get() = (durationDays - completedDaysCount).coerceAtLeast(0).toLong()

private fun getEmojiAndTitle(title: String): Pair<String, String> {
    if (title.isEmpty()) return Pair("🎯", "")
    
    val trimmed = title.trim()
    val firstSpace = trimmed.indexOf(' ')
    if (firstSpace > 0) {
        val firstPart = trimmed.substring(0, firstSpace)
        val hasEmoji = firstPart.any { char ->
            char.isSurrogate() || char.code in 0x2000..0x32FF || char.code in 0xFE00..0xFE0F
        }
        if (hasEmoji) {
            val rest = trimmed.substring(firstSpace + 1).trim()
            return Pair(firstPart, if (rest.isEmpty()) firstPart else rest)
        }
    }

    val firstChar = trimmed.first()
    val isEmoji = firstChar.isSurrogate() || 
                  firstChar.code in 0x2000..0x32FF || 
                  firstChar.code in 0xFE00..0xFE0F
    
    if (isEmoji) {
        var emojiLength = if (trimmed.length >= 2 && trimmed[0].isHighSurrogate() && trimmed[1].isLowSurrogate()) 2 else 1
        val emoji = trimmed.substring(0, emojiLength)
        val rest = trimmed.substring(emojiLength).trim()
        return Pair(emoji, if (rest.isEmpty()) emoji else rest)
    }
    
    val commonEmojis = listOf("🚫", "🎯", "🔥", "🏆", "❌", "🎉", "💰", "🐖", "🪙", "💡", "🏖️", "✈️", "🚗", "🏠", "💻", "📱")
    for (e in commonEmojis) {
        if (trimmed.startsWith(e)) {
            val rest = trimmed.substring(e.length).trim()
            return Pair(e, if (rest.isEmpty()) e else rest)
        }
    }
    
    return Pair("🎯", trimmed)
}

private fun formatAmount(amount: Double, currencyPreference: String): String {
    return com.savingcoach.app.utils.InvestmentCalculations.formatValue(amount, currencyPreference, 1.0, isInvestment = false)
}

@Composable
fun ChallengeCard(
    challenge: SavingChallenge,
    onClick: () -> Unit,
    currencyPreference: String = "MMK",
    modifier: Modifier = Modifier
) {
    val progressFloat = if (challenge.targetAmount > 0) {
        (challenge.currentAmount / challenge.targetAmount).toFloat().coerceIn(0f, 1f)
    } else if (challenge.durationDays > 0) {
        (challenge.completedDaysCount.toFloat() / challenge.durationDays.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val percentage = (progressFloat * 100).toInt().coerceIn(0, 100)

    val strings = com.savingcoach.app.ui.localization.AppLocale.current

    // Dynamic color & status styling based on challenge state
    val (statusColor, statusBg, statusLabel) = when (challenge.status) {
        ChallengeStatus.ACTIVE -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            strings.daysLeftCount(challenge.daysLeft)
        )
        ChallengeStatus.COMPLETED -> Triple(
            Color(0xFF81C784),
            Color(0xFF81C784).copy(alpha = 0.15f),
            strings.completed
        )
        ChallengeStatus.FAILED -> Triple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
            strings.failed
        )
        ChallengeStatus.STOPPED -> Triple(
            Color(0xFFB0BEC5),
            Color(0xFFB0BEC5).copy(alpha = 0.15f),
            strings.stopped
        )
    }

    val (emoji, rawTitle) = getEmojiAndTitle(challenge.title)
    val localizedTitle = strings.localizeChallengeTitle(rawTitle)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Badges: Days left / Status pill & Progress %
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status pill / Days left chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        if (challenge.status == ChallengeStatus.ACTIVE) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = statusColor
                        )
                    }
                }

                // Emoji Progress %
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "$percentage%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // 2. Center: Circular progress ring circling the user input emoji
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                // Subtle inner circular backdrop for emoji
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(statusBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 32.sp
                    )
                }

                // Circular Progress Indicator circling the emoji
                CircularProgressIndicator(
                    progress = { progressFloat },
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.size(80.dp)
                )
            }

            // 3. Bottom: Challenge Name
            Text(
                text = localizedTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
            )
        }
    }
}
