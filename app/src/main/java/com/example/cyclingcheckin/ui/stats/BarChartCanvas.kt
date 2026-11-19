package com.example.cyclingcheckin.ui.stats

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cyclingcheckin.domain.model.DayStat
import com.example.cyclingcheckin.domain.model.Record
import com.example.cyclingcheckin.ui.theme.RideLogTheme
import com.example.cyclingcheckin.util.TimeUtils
import java.time.LocalDate

@Composable
fun WeekBarChart(
    days: List<DayStat>,
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val track = RideLogTheme.colors.chartTrack
    val textColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val secondary = RideLogTheme.colors.secondaryText
    val selectedColor = RideLogTheme.colors.chartHighlight

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .pointerInput(days) {
                detectTapGestures { offset ->
                    if (days.isEmpty() || size.width <= 0) return@detectTapGestures
                    val index = (offset.x / (size.width / days.size.toFloat()))
                        .toInt()
                        .coerceIn(0, days.lastIndex)
                    days.getOrNull(index)?.let { onSelect(it.date) }
                }
            }
    ) {
        if (days.isEmpty()) return@Canvas
        val topPadding = 34.dp.toPx()
        val bottomPadding = 30.dp.toPx()
        val chartHeight = size.height - topPadding - bottomPadding
        val slotWidth = size.width / days.size
        val maxDistance = (days.maxOfOrNull { it.distance } ?: 0.0).coerceAtLeast(1.0)

        // 横向网格
        for (i in 0..2) {
            val y = topPadding + chartHeight * i / 2f
            drawLine(
                color = Color(0x14808A9C),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val weekdayPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12.sp.toPx()
            textAlign = Paint.Align.CENTER
            color = secondary.toArgb()
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }
        val valuePaint = Paint().apply {
            isAntiAlias = true
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("monospace", Typeface.BOLD)
        }
        val bubbleTextPaint = Paint(valuePaint).apply {
            color = android.graphics.Color.WHITE
            textSize = 11.sp.toPx()
        }

        days.forEachIndexed { index, day ->
            val centerX = slotWidth * (index + 0.5f)
            val isSelected = day.date == selectedDate
            val barWidth = slotWidth * 0.44f
            val ratio = (day.distance / maxDistance).toFloat().coerceIn(0f, 1f)
            val barHeight = (chartHeight * ratio).coerceAtLeast(3.dp.toPx())
            val barTop = topPadding + chartHeight - barHeight
            val barColor = when {
                day.distance <= 0.0 -> track
                isSelected -> selectedColor
                else -> primary.copy(alpha = 0.35f)
            }
            drawRoundRect(
                color = barColor,
                topLeft = Offset(centerX - barWidth / 2f, barTop),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
            )

            if (day.distance > 0.0) {
                val label = TimeUtils.formatDistance(day.distance)
                if (isSelected) {
                    val textWidth = bubbleTextPaint.measureText(label)
                    val bubbleWidth = textWidth + 16.dp.toPx()
                    val bubbleHeight = 20.dp.toPx()
                    val bubbleTop = (barTop - bubbleHeight - 6.dp.toPx())
                        .coerceAtLeast(0f)
                    drawRoundRect(
                        color = selectedColor,
                        topLeft = Offset(centerX - bubbleWidth / 2f, bubbleTop),
                        size = Size(bubbleWidth, bubbleHeight),
                        cornerRadius = CornerRadius(bubbleHeight / 2f, bubbleHeight / 2f)
                    )
                    drawIntoCanvas {
                        val baseline = bubbleTop + bubbleHeight / 2f -
                            (bubbleTextPaint.descent() + bubbleTextPaint.ascent()) / 2f
                        it.nativeCanvas.drawText(label, centerX, baseline, bubbleTextPaint)
                    }
                } else {
                    valuePaint.color = secondary.toArgb()
                    drawIntoCanvas {
                        it.nativeCanvas.drawText(label, centerX, barTop - 6.dp.toPx(), valuePaint)
                    }
                }
            }

            val weekday = TimeUtils.weekdayShort(day.date)
            weekdayPaint.color = if (isSelected) primary.toArgb() else secondary.toArgb()
            weekdayPaint.typeface = Typeface.create(
                "sans-serif",
                if (isSelected) Typeface.BOLD else Typeface.NORMAL
            )
            drawIntoCanvas {
                it.nativeCanvas.drawText(
                    weekday,
                    centerX,
                    size.height - 8.dp.toPx(),
                    weekdayPaint
                )
            }
        }
        // 右上角最大值刻度
        drawIntoCanvas {
            it.nativeCanvas.drawText(
                TimeUtils.formatDistance(maxDistance) + " km",
                size.width - 2.dp.toPx(),
                topPadding - 14.dp.toPx(),
                Paint(valuePaint).apply {
                    textAlign = Paint.Align.RIGHT
                    color = secondary.toArgb()
                }
            )
        }
    }
}

@Composable
fun DayTimeline(
    records: List<Record>,
    modifier: Modifier = Modifier
) {
    val primary = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val accent = RideLogTheme.colors.accent
    val secondary = RideLogTheme.colors.secondaryText
    val axisColor = RideLogTheme.colors.chartTrack

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val leftPadding = 8.dp.toPx()
        val rightPadding = 8.dp.toPx()
        val axisY = size.height * 0.62f
        val usableWidth = size.width - leftPadding - rightPadding

        drawLine(
            color = axisColor,
            start = Offset(leftPadding, axisY),
            end = Offset(size.width - rightPadding, axisY),
            strokeWidth = 3.dp.toPx()
        )

        val tickPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
            color = secondary.toArgb()
            typeface = Typeface.create("monospace", Typeface.NORMAL)
        }
        listOf(0, 6, 12, 18, 24).forEach { hour ->
            val x = leftPadding + usableWidth * (hour / 24f)
            drawLine(
                color = axisColor,
                start = Offset(x, axisY - 4.dp.toPx()),
                end = Offset(x, axisY + 4.dp.toPx()),
                strokeWidth = 2.dp.toPx()
            )
            drawIntoCanvas {
                it.nativeCanvas.drawText(
                    hour.toString(),
                    x,
                    axisY + 20.dp.toPx(),
                    tickPaint
                )
            }
        }

        val valuePaint = Paint().apply {
            isAntiAlias = true
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
            color = accent.toArgb()
            typeface = Typeface.create("monospace", Typeface.BOLD)
        }

        records.sortedBy { it.time }.forEachIndexed { index, record ->
            val minutes = record.time.hour * 60 + record.time.minute
            val x = leftPadding + usableWidth * (minutes / 1440f)
            val above = index % 2 == 0
            val centerY = if (above) axisY - 18.dp.toPx() else axisY + 30.dp.toPx()
            drawCircle(
                color = primary,
                radius = 6.dp.toPx(),
                center = Offset(x, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = Offset(x, centerY)
            )
            drawLine(
                color = primary.copy(alpha = 0.45f),
                start = Offset(x, centerY),
                end = Offset(x, axisY),
                strokeWidth = 1.dp.toPx()
            )
            drawIntoCanvas {
                it.nativeCanvas.drawText(
                    TimeUtils.formatDistance(record.distance),
                    x,
                    if (above) centerY - 10.dp.toPx() else centerY + 18.dp.toPx(),
                    valuePaint
                )
            }
        }
    }
}
