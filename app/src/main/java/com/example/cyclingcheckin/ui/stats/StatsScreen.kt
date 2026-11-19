package com.example.cyclingcheckin.ui.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cyclingcheckin.AppContainer
import com.example.cyclingcheckin.R
import com.example.cyclingcheckin.data.repo.StatsSnapshot
import com.example.cyclingcheckin.domain.model.Record
import com.example.cyclingcheckin.domain.model.StatsMode
import com.example.cyclingcheckin.ui.components.BikeIcon
import com.example.cyclingcheckin.ui.components.EmptyState
import com.example.cyclingcheckin.ui.components.RideLogCard
import com.example.cyclingcheckin.ui.components.SectionTitle
import com.example.cyclingcheckin.ui.theme.RideLogTheme
import com.example.cyclingcheckin.util.TimeUtils
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.abs

private data class Quad(val a: Int, val b: Int, val c: Int, val d: Int)

@Composable
fun StatsScreen(
    container: AppContainer,
    onGoCheckIn: () -> Unit
) {
    val viewModel: StatsViewModel = viewModel(factory = StatsViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snapshot = uiState.snapshot

    if (snapshot == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val groupedRecords = remember(snapshot.records) {
        snapshot.records.groupBy { it.date }
            .entries
            .sortedByDescending { it.key }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatsHeader(streak = snapshot.streak)
        }
        item {
            ModeSelector(mode = uiState.mode, onModeChange = viewModel::setMode)
        }
        item {
            PeriodNavigator(
                label = snapshot.label,
                showBackToCurrent = !isCurrentPeriod(uiState.mode, uiState.anchor),
                backLabel = when (uiState.mode) {
                    StatsMode.DAY -> stringResource(R.string.stats_back_today)
                    StatsMode.WEEK -> stringResource(R.string.stats_back_week)
                    StatsMode.MONTH -> stringResource(R.string.stats_back_month)
                },
                onPrevious = viewModel::previousPeriod,
                onNext = viewModel::nextPeriod,
                onBackToCurrent = viewModel::goToCurrent
            )
        }
        item {
            SummaryGrid(snapshot = snapshot, mode = uiState.mode)
        }
        if (snapshot.summary.count == 0) {
            item {
                RideLogCard {
                    EmptyState(
                        title = stringResource(R.string.stats_empty),
                        hint = stringResource(R.string.stats_empty_hint),
                        action = {
                            Button(onClick = onGoCheckIn) {
                                Text(stringResource(R.string.action_go_check_in))
                            }
                        }
                    )
                }
            }
        } else {
            item {
                ChartCard(
                    snapshot = snapshot,
                    mode = uiState.mode,
                    selectedDate = uiState.effectiveSelectedDate,
                    onSelectDate = viewModel::selectDay
                )
            }
            item {
                SectionTitle(
                    text = stringResource(R.string.stats_detail),
                    trailing = {
                        if (uiState.mode == StatsMode.WEEK) {
                            Text(
                                text = stringResource(
                                    R.string.stats_detail_selected,
                                    TimeUtils.dayLabel(uiState.effectiveSelectedDate)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = RideLogTheme.colors.secondaryText
                            )
                        }
                    }
                )
            }
            when (uiState.mode) {
                StatsMode.DAY -> {
                    items(items = snapshot.records, key = { it.id }) { record ->
                        StatsDetailRow(record = record)
                    }
                }
                StatsMode.WEEK -> {
                    val selected = uiState.selectedRecords
                    if (selected.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.home_empty_date),
                                style = MaterialTheme.typography.bodyMedium,
                                color = RideLogTheme.colors.secondaryText,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        items(items = selected, key = { it.id }) { record ->
                            StatsDetailRow(record = record)
                        }
                    }
                }
                StatsMode.MONTH -> {
                    groupedRecords.forEach { (date, records) ->
                        item(key = "header-$date") {
                            GroupHeader(date = date, records = records)
                        }
                        items(items = records, key = { it.id }) { record ->
                            StatsDetailRow(record = record)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun StatsHeader(streak: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.stats_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        if (streak > 0) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_flame),
                        contentDescription = null,
                        tint = RideLogTheme.colors.accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.stats_mark, streak),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(mode: StatsMode, onModeChange: (StatsMode) -> Unit) {
    val items = listOf(
        StatsMode.DAY to R.string.stats_day,
        StatsMode.WEEK to R.string.stats_week,
        StatsMode.MONTH to R.string.stats_month
    )
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEach { (itemMode, labelRes) ->
                val selected = itemMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { onModeChange(itemMode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodNavigator(
    label: String,
    showBackToCurrent: Boolean,
    backLabel: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBackToCurrent: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.stats_previous),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.stats_next),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showBackToCurrent) {
            TextButton(
                onClick = onBackToCurrent,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(text = backLabel)
            }
        }
    }
}

@Composable
private fun SummaryGrid(snapshot: StatsSnapshot, mode: StatsMode) {
    val daysInPeriod = when (mode) {
        StatsMode.DAY -> 1
        StatsMode.WEEK -> 7
        StatsMode.MONTH -> YearMonth.from(snapshot.anchor).lengthOfMonth()
    }
    val percent = snapshot.periodOverPeriodPercent
    val (noneStr, upStr, downStr, flatStr) = when (mode) {
        StatsMode.DAY -> Quad(
            R.string.stats_prior_day_none, R.string.stats_prior_day_up,
            R.string.stats_prior_day_down, R.string.stats_prior_day_flat
        )
        StatsMode.WEEK -> Quad(
            R.string.stats_prior_week_none, R.string.stats_prior_week_up,
            R.string.stats_prior_week_down, R.string.stats_prior_week_flat
        )
        StatsMode.MONTH -> Quad(
            R.string.stats_prior_month_none, R.string.stats_prior_month_up,
            R.string.stats_prior_month_down, R.string.stats_prior_month_flat
        )
    }
    val compText = when {
        percent == null -> stringResource(noneStr)
        kotlin.math.abs(percent) < 0.05 -> stringResource(flatStr)
        percent > 0 -> stringResource(upStr, String.format(Locale.US, "%.1f", percent))
        else -> stringResource(downStr, String.format(Locale.US, "%.1f", kotlin.math.abs(percent)))
    }
    val compColor = when {
        percent == null -> RideLogTheme.colors.secondaryText
        percent >= 0 -> RideLogTheme.colors.success
        else -> RideLogTheme.colors.accent
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                icon = { BikeIcon(modifier = Modifier.size(16.dp)) },
                label = stringResource(R.string.stats_count),
                value = snapshot.summary.count.toString(),
                unit = stringResource(R.string.stats_unit_count),
                footer = stringResource(R.string.stats_count_total, snapshot.summary.count),
                footerColor = RideLogTheme.colors.secondaryText
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = stringResource(R.string.stats_total),
                value = TimeUtils.formatDistance(snapshot.summary.distance),
                unit = stringResource(R.string.stats_unit_km),
                footer = compText,
                footerColor = compColor
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryCard(
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = stringResource(R.string.stats_avg),
                value = TimeUtils.formatDistance(snapshot.summary.avgPerDay),
                unit = stringResource(R.string.stats_unit_km),
                footer = stringResource(R.string.stats_avg_days, daysInPeriod),
                footerColor = RideLogTheme.colors.secondaryText
            )
            SummaryCard(
                modifier = Modifier.weight(1f),
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_flame),
                        contentDescription = null,
                        tint = RideLogTheme.colors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = stringResource(R.string.stats_max),
                value = TimeUtils.formatDistance(
                    when (mode) {
                        StatsMode.DAY -> snapshot.summary.maxRecord?.distance ?: 0.0
                        else -> snapshot.summary.maxDay?.distance ?: 0.0
                    }
                ),
                unit = stringResource(R.string.stats_unit_km),
                footer = when (mode) {
                    StatsMode.DAY -> snapshot.summary.maxRecord?.let {
                        stringResource(R.string.stats_max_day, TimeUtils.formatTime(it.time))
                    } ?: "—"
                    else -> snapshot.summary.maxDay?.let {
                        stringResource(R.string.stats_max_day, TimeUtils.dayLabel(it.date))
                    } ?: "—"
                },
                footerColor = RideLogTheme.colors.secondaryText
            )
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    unit: String,
    footer: String,
    footerColor: androidx.compose.ui.graphics.Color
) {
    RideLogCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = RideLogTheme.colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = " $unit",
                style = MaterialTheme.typography.bodySmall,
                color = RideLogTheme.colors.secondaryText,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = footer,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = footerColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChartCard(
    snapshot: StatsSnapshot,
    mode: StatsMode,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    RideLogCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    when (mode) {
                        StatsMode.DAY -> R.string.stats_chart_day
                        StatsMode.WEEK -> R.string.stats_chart_week
                        StatsMode.MONTH -> R.string.stats_chart_month
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.stats_chart_legend),
                style = MaterialTheme.typography.bodySmall,
                color = RideLogTheme.colors.secondaryText
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        when (mode) {
            StatsMode.DAY -> DayTimeline(records = snapshot.records)
            StatsMode.WEEK -> WeekBarChart(
                days = snapshot.days,
                selectedDate = selectedDate,
                onSelect = onSelectDate
            )
            StatsMode.MONTH -> MonthHeatmap(
                month = YearMonth.from(snapshot.rangeStart),
                days = snapshot.days,
                selectedDate = selectedDate,
                onSelect = onSelectDate
            )
        }
        val selectedStat = snapshot.days.firstOrNull { it.date == selectedDate }
        if (mode != StatsMode.DAY && selectedStat != null && selectedStat.count > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${TimeUtils.dayLabel(selectedDate)} · " + stringResource(
                    R.string.stats_group_summary,
                    selectedStat.count,
                    TimeUtils.formatDistance(selectedStat.distance)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun GroupHeader(date: LocalDate, records: List<Record>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${TimeUtils.dayLabel(date)} ${TimeUtils.weekdayLong(date)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(
                R.string.stats_group_summary,
                records.size,
                TimeUtils.formatDistance(records.sumOf { it.distance })
            ),
            style = MaterialTheme.typography.bodySmall,
            color = RideLogTheme.colors.secondaryText
        )
    }
}

@Composable
private fun StatsDetailRow(record: Record) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = TimeUtils.formatTime(record.time),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = record.note.ifBlank { stringResource(R.string.home_note_empty) },
                style = MaterialTheme.typography.bodySmall,
                color = RideLogTheme.colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${TimeUtils.formatDistance(record.distance)} km",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = RideLogTheme.colors.accent
            )
        }
    }
}

private fun isCurrentPeriod(mode: StatsMode, anchor: LocalDate): Boolean {
    val today = LocalDate.now()
    return when (mode) {
        StatsMode.DAY -> anchor == today
        StatsMode.WEEK -> TimeUtils.startOfWeek(anchor) == TimeUtils.startOfWeek(today)
        StatsMode.MONTH -> TimeUtils.startOfMonth(anchor) == TimeUtils.startOfMonth(today)
    }
}
