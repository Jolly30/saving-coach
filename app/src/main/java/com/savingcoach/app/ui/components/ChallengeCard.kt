package com.savingcoach.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savingcoach.app.data.model.SavingChallenge
import com.savingcoach.app.data.model.ChallengeTemplate
import com.savingcoach.app.data.model.ChallengeStatus
import com.savingcoach.app.ui.theme.CoralRed

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
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Original dynamic color & status styling based on challenge state
    val (statusColor, statusBg, statusBorder, statusLabel) = when (challenge.status) {
        ChallengeStatus.ACTIVE -> Quadruple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
            strings.daysLeftCount(challenge.daysLeft)
        )
        ChallengeStatus.COMPLETED -> Quadruple(
            Color(0xFF81C784),
            Color(0xFF81C784).copy(alpha = 0.15f),
            Color(0xFF81C784).copy(alpha = 0.3f),
            strings.completed
        )
        ChallengeStatus.FAILED -> Quadruple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
            strings.failed
        )
        ChallengeStatus.STOPPED -> Quadruple(
            Color(0xFFB0BEC5),
            Color(0xFFB0BEC5).copy(alpha = 0.15f),
            Color(0xFFB0BEC5).copy(alpha = 0.3f),
            strings.stopped
        )
    }

    val (emoji, rawTitle) = getEmojiAndTitle(challenge.title)
    val localizedTitle = strings.localizeChallengeTitle(rawTitle)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
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
                    color = statusBg,
                    border = BorderStroke(1.dp, statusBorder)
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

                // Progress %
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

            // 2. Center: Interactive Liquid Wave Progress with device tilt physics & floating emoji
            LiquidWaveProgress(
                progress = progressFloat,
                emoji = emoji,
                primaryColor = MaterialTheme.colorScheme.primary,
                containerBg = MaterialTheme.colorScheme.surfaceVariant,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                size = 80.dp,
                isDark = isDark
            )

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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
