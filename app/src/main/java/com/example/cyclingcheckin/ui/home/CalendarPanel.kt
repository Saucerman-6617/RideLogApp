package com.example.cyclingcheckin.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cyclingcheckin.R
import com.example.cyclingcheckin.domain.model.CalendarMarkStyle
import com.example.cyclingcheckin.domain.model.DayStat
import com.example.cyclingcheckin.ui.components.RideLogCard
import com.example.cyclingcheckin.ui.theme.RideLogTheme
import com.example.cyclingcheckin.util.TimeUtils
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarPanel(
    month: YearMonth,
    selectedDate: LocalDate,
    monthStats: Map<LocalDate, DayStat>,
    markStyle: CalendarMarkStyle,
    onSelectDate: (LocalDate) -> Unit,
    onChangeMonth: (Long) -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonth = YearMonth.from(LocalDate.now())
    val grid = TimeUtils.monthGrid(month)
    RideLogCard(
        modifier = modifier.pointerInput(month) {
            var drag = 0f
            detectHorizontalDragGestures(
                onDragStart = { drag = 0f },
                onHorizontalDrag = { change, amount ->
                    change.consume()
                    drag += amount
                },
                onDragEnd = {
                    if (drag > 90f) onChangeMonth(-1) else if (drag < -90f) onChangeMonth(1)
                }
            )
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onChangeMonth(-1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.calendar_month_previous),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = TimeUtils.monthTitle(month),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onToday)
            )
            if (month != currentMonth) {
                TextButton(onClick = onToday) {
                    Text(text = stringResource(R.string.action_today))
                }
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = { onChangeMonth(1) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.calendar_month_next),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { index, label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (index >= 5) {
                        RideLogTheme.colors.danger.copy(alpha = 0.75f)
                    } else {
                        RideLogTheme.colors.secondaryText
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val date = grid.getOrNull(row * 7 + column)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                selected = date == selectedDate,
                                isToday = date == LocalDate.now(),
                                stat = monthStats[date],
                                markStyle = markStyle,
                                onClick = { onSelectDate(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    selected: Boolean,
    isToday: Boolean,
    stat: DayStat?,
    markStyle: CalendarMarkStyle,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val weekend = date.dayOfWeek.value >= 6
    val description = if (stat != null && stat.count > 0) {
        stringResource(
            R.string.calendar_cd,
            TimeUtils.fullDateLabel(date),
            stat.count,
            TimeUtils.formatDistance(stat.distance)
        )
    } else {
        stringResource(R.string.calendar_cd_empty, TimeUtils.fullDateLabel(date))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(vertical = 2.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (selected) primary else Color.Transparent,
            border = if (!selected && isToday) BorderStroke(1.dp, primary) else null,
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = when {
                        selected -> MaterialTheme.colorScheme.onPrimary
                        weekend -> RideLogTheme.colors.danger
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier.height(10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (stat != null && stat.count > 0) {
                if (markStyle == CalendarMarkStyle.DISTANCE) {
                    Text(
                        text = TimeUtils.formatDistance(stat.distance),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                        color = if (selected) primary else RideLogTheme.colors.accent,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = stringResource(R.string.calendar_count, stat.count),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                        color = if (selected) primary else RideLogTheme.colors.accent,
                        maxLines = 1
                    )
                }
            }
        }
    }
}


