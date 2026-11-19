package com.example.cyclingcheckin.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cyclingcheckin.R
import com.example.cyclingcheckin.domain.model.DayStat
import com.example.cyclingcheckin.ui.theme.RideLogTheme
import com.example.cyclingcheckin.util.TimeUtils
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthHeatmap(
    month: YearMonth,
    days: List<DayStat>,
    selectedDate: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = days.associateBy { it.date }
    val maxDistance = (days.maxOfOrNull { it.distance } ?: 0.0).coerceAtLeast(1.0)
    val grid = TimeUtils.monthGrid(month)
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val secondary = RideLogTheme.colors.secondaryText

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val date = grid.getOrNull(row * 7 + column)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .height(36.dp)
                    ) {
                        if (date != null) {
                            val stat = stats[date]
                            val ratio = ((stat?.distance ?: 0.0) / maxDistance).toFloat()
                            val hasData = (stat?.count ?: 0) > 0
                            val cellColor = if (hasData) {
                                primary.copy(alpha = 0.14f + 0.78f * ratio.coerceIn(0f, 1f))
                            } else {
                                surfaceVariant.copy(alpha = 0.55f)
                            }
                            val textColor = if (hasData && ratio > 0.42f) {
                                androidx.compose.ui.graphics.Color.White
                            } else if (hasData) {
                                primary
                            } else {
                                secondary
                            }
                            val description = if (hasData) {
                                stringResource(
                                    R.string.calendar_cd,
                                    TimeUtils.fullDateLabel(date),
                                    stat!!.count,
                                    TimeUtils.formatDistance(stat.distance)
                                )
                            } else {
                                stringResource(R.string.calendar_cd_empty, TimeUtils.fullDateLabel(date))
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cellColor)
                                    .then(
                                        if (date == selectedDate) {
                                            Modifier.border(
                                                BorderStroke(2.dp, primary),
                                                RoundedCornerShape(10.dp)
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { onSelect(date) }
                                    .semantics { contentDescription = description },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (hasData) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "少",
                style = MaterialTheme.typography.bodySmall,
                color = secondary,
                modifier = Modifier.padding(end = 4.dp)
            )
            listOf(0.15f, 0.35f, 0.55f, 0.75f, 0.95f).forEach { alpha ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 1.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(primary.copy(alpha = alpha))
                ) {
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                }
            }
            Text(
                text = "多",
                style = MaterialTheme.typography.bodySmall,
                color = secondary,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
