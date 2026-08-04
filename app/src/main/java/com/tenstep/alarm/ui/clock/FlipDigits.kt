package com.tenstep.alarm.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tenstep.alarm.ui.theme.isLight
import java.time.LocalDateTime

/**
 * Pure time -> cells math (unit-testable). A cell is a single character:
 * digits become number cards, ':' becomes a static separator.
 */
object FlipTime {

    data class Cells(val digits: List<Char>, val amPm: String?)

    fun cells(now: LocalDateTime, is24Hour: Boolean, showSeconds: Boolean): Cells {
        val hour12 = now.hour % 12
        val hour = if (is24Hour) now.hour else if (hour12 == 0) 12 else hour12
        val text = buildString {
            append(hour.toString().padStart(2, '0'))
            append(':')
            append(now.minute.toString().padStart(2, '0'))
            if (showSeconds) {
                append(':')
                append(now.second.toString().padStart(2, '0'))
            }
        }
        val amPm = if (is24Hour) null else if (now.hour < 12) "AM" else "PM"
        return Cells(text.toList(), amPm)
    }
}

/**
 * Flip-clock digits rendered as plain number cards (no fold/hinge animation):
 * each digit is one rounded card on the clock face color, so the time reads
 * like a normal digital clock and scales to fit the available space.
 *
 * @param face card background color
 * @param digitColor digit color
 */
@Composable
fun FlipClockDisplay(
    now: LocalDateTime,
    showSeconds: Boolean,
    is24Hour: Boolean,
    fontScale: Float,
    face: Color,
    digitColor: Color,
    modifier: Modifier = Modifier
) {
    val cells = FlipTime.cells(now, is24Hour, showSeconds)
    val digitCount = cells.digits.count { it.isDigit() }
    val separatorCount = cells.digits.count { !it.isDigit() }
    val amPm = cells.amPm
    val density = LocalDensity.current

    BoxWithConstraints(modifier) {
        val margin = 24.dp
        val amPmFactor = if (amPm != null) 1.2f else 0f
        val totalFactor = digitCount * 0.62f + separatorCount * 0.30f + amPmFactor
        val widthBudget = (maxWidth - margin * 2) / totalFactor.coerceAtLeast(1f)
        val heightBudget = (maxHeight - margin * 2) * 0.55f * fontScale
        val cellHeight = minOf(widthBudget, heightBudget).coerceAtLeast(32.dp)
        val cellWidth = cellHeight * 0.62f
        val borderColor = if (face.isLight()) Color(0x33000000) else Color(0x59FFFFFF)

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            cells.digits.forEach { ch ->
                if (ch.isDigit()) {
                    Box(
                        modifier = Modifier
                            .width(cellWidth)
                            .height(cellHeight)
                            .clip(RoundedCornerShape(cellHeight * 0.14f))
                            .background(face)
                            .border(
                                1.dp,
                                borderColor,
                                RoundedCornerShape(cellHeight * 0.14f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ch.toString(),
                            fontSize = with(density) { (cellHeight * 0.72f).toSp() },
                            fontWeight = FontWeight.Bold,
                            color = digitColor
                        )
                    }
                } else {
                    Spacer(Modifier.width(cellWidth * 0.18f))
                    Text(
                        text = ":",
                        fontSize = with(density) { (cellHeight * 0.30f).toSp() },
                        fontWeight = FontWeight.Bold,
                        color = digitColor
                    )
                    Spacer(Modifier.width(cellWidth * 0.18f))
                }
                Spacer(Modifier.width(cellWidth * 0.10f))
            }
            if (amPm != null) {
                Spacer(Modifier.width(cellWidth * 0.40f))
                Text(
                    text = amPm,
                    fontSize = with(density) { (cellHeight * 0.32f).toSp() },
                    fontWeight = FontWeight.Bold,
                    color = digitColor
                )
            }
        }
    }
}
