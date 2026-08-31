package com.savingcoach.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DecimalFormat

import androidx.compose.ui.text.style.TextAlign

@Composable
fun AutoScalingText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    minTextSize: TextUnit = 16.sp,
    maxTextSize: TextUnit = 30.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    textAlign: TextAlign = TextAlign.Start
) {
    var textStyle by remember(text, minTextSize, maxTextSize, fontWeight, textAlign) { 
        mutableStateOf(TextStyle(fontSize = maxTextSize, fontWeight = fontWeight, textAlign = textAlign)) 
    }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        color = color,
        maxLines = 1,
        softWrap = false,
        textAlign = textAlign,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        style = textStyle,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.hasVisualOverflow) {
                if (textStyle.fontSize > minTextSize) {
                    textStyle = textStyle.copy(fontSize = textStyle.fontSize * 0.9f)
                } else {
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        }
    )
}

@Composable
fun MetricItem(
    title: String,
    amount: Double,
    currency: String,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    val maxFontSize = if (isPrimary) 36.sp else 24.sp
    val minFontSize = if (isPrimary) 18.sp else 14.sp
    
    val formatter = DecimalFormat("#,###")
    val formattedAmount = formatter.format(amount)

    Column(
        modifier = modifier,
        horizontalAlignment = if (isPrimary) Alignment.CenterHorizontally else Alignment.Start
    ) {
        // Faded currency label at the top, e.g. "SAVINGS (MMK)"
        Text(
            text = "${title.uppercase()} ($currency)",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Purely clean digits below, auto-scaling, no currency symbol
        AutoScalingText(
            text = formattedAmount,
            maxTextSize = maxFontSize,
            minTextSize = minFontSize,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HeroCard(
    mainTitle: String,
    mainAmount: Double,
    leftTitle: String,
    leftAmount: Double,
    rightTitle: String,
    rightAmount: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "1" (Top Primary Metric)
            MetricItem(
                title = mainTitle,
                amount = mainAmount,
                currency = currency,
                isPrimary = true
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // "2" (Bottom Secondary Metrics)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    title = leftTitle,
                    amount = leftAmount,
                    currency = currency,
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                MetricItem(
                    title = rightTitle,
                    amount = rightAmount,
                    currency = currency,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
