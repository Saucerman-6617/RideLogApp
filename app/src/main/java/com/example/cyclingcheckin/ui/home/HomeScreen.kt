package com.example.cyclingcheckin.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cyclingcheckin.AppContainer
import com.example.cyclingcheckin.R
import com.example.cyclingcheckin.domain.model.Record
import com.example.cyclingcheckin.ui.components.BikeIcon
import com.example.cyclingcheckin.ui.components.EmptyState
import com.example.cyclingcheckin.ui.components.SectionTitle
import com.example.cyclingcheckin.ui.theme.RideLogTheme
import com.example.cyclingcheckin.util.TimeUtils
import java.time.LocalDate

@Composable
fun HomeScreen(
    container: AppContainer,
    snackbarHostState: SnackbarHostState
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showSheet by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<Record?>(null) }
    var pendingDelete by remember { mutableStateOf<Record?>(null) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is HomeEvent.Added -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(
                            R.string.snackbar_recorded,
                            TimeUtils.formatDistance(event.record.distance)
                        ),
                        actionLabel = context.getString(R.string.action_undo),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoAdd(event.record)
                }
                is HomeEvent.Deleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.snackbar_deleted),
                        actionLabel = context.getString(R.string.action_undo),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.restoreRecord(event.record)
                }
                is HomeEvent.Updated -> snackbarHostState.showSnackbar(
                    context.getString(R.string.snackbar_updated)
                )
                is HomeEvent.Restored -> snackbarHostState.showSnackbar(
                    context.getString(R.string.snackbar_restored)
                )
                is HomeEvent.Failed -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HomeHeader()
        }
        item {
            CalendarPanel(
                month = uiState.visibleMonth,
                selectedDate = uiState.selectedDate,
                monthStats = uiState.monthStats,
                markStyle = uiState.markStyle,
                onSelectDate = viewModel::selectDate,
                onChangeMonth = viewModel::changeMonth,
                onToday = viewModel::goToToday
            )
        }
        item {
            SummaryBanner(
                selectedDate = uiState.selectedDate,
                count = uiState.selectedCount,
                distance = uiState.selectedDistance
            )
        }
        item {
            val addDescription = stringResource(R.string.cd_add_record)
            Button(
                onClick = {
                    editingRecord = null
                    showSheet = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .semantics { contentDescription = addDescription },
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = stringResource(R.string.home_add),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        item {
            val isToday = uiState.selectedDate == LocalDate.now()
            SectionTitle(
                text = if (isToday) {
                    stringResource(R.string.home_records_today)
                } else {
                    stringResource(R.string.home_records_date, TimeUtils.dayLabel(uiState.selectedDate))
                },
                trailing = {
                    if (uiState.records.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.home_records_count, uiState.records.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = RideLogTheme.colors.secondaryText
                        )
                    }
                }
            )
        }
        if (uiState.records.isEmpty()) {
            item {
                val isToday = uiState.selectedDate == LocalDate.now()
                EmptyState(
                    title = if (isToday) {
                        stringResource(R.string.home_summary_empty)
                    } else {
                        stringResource(R.string.home_empty_date)
                    },
                    hint = if (isToday) {
                        stringResource(R.string.home_empty_today)
                    } else {
                        stringResource(R.string.home_empty_title)
                    }
                )
            }
        } else {
            items(items = uiState.records, key = { it.id }) { record ->
                RecordRow(
                    record = record,
                    onEdit = {
                        editingRecord = record
                        showSheet = true
                    },
                    onRequestDelete = { pendingDelete = record }
                )
            }
        }
    }

    if (showSheet) {
        RecordEditSheet(
            existing = editingRecord,
            preselectedDate = uiState.selectedDate,
            onDismiss = { showSheet = false },
            onSave = { date, time, distance, note ->
                viewModel.saveRecord(editingRecord, date, time, distance, note)
                showSheet = false
            }
        )
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.delete_message,
                        TimeUtils.dayLabel(record.date),
                        TimeUtils.formatTime(record.time),
                        TimeUtils.formatDistance(record.distance)
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecord(record)
                        pendingDelete = null
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = RideLogTheme.colors.danger
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun HomeHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            BikeIcon(modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.padding(horizontal = 5.dp))
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${TimeUtils.fullDateLabel(LocalDate.now())} ${TimeUtils.weekdayLong(LocalDate.now())}",
                style = MaterialTheme.typography.bodySmall,
                color = RideLogTheme.colors.secondaryText
            )
        }
    }
}

@Composable
private fun SummaryBanner(
    selectedDate: LocalDate,
    count: Int,
    distance: Double
) {
    val today = LocalDate.now()
    val gradient = Brush.linearGradient(
        colors = listOf(RideLogTheme.colors.heroStart, RideLogTheme.colors.heroEnd)
    )
    val dateText = if (selectedDate == today) {
        stringResource(R.string.home_summary_today, TimeUtils.dayLabel(selectedDate))
    } else {
        "${TimeUtils.dayLabel(selectedDate)} ${TimeUtils.weekdayLong(selectedDate)}"
    }
    val summaryText = if (count > 0) {
        stringResource(R.string.home_summary, count, TimeUtils.formatDistance(distance))
    } else {
        stringResource(R.string.home_summary_empty)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_bike),
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
        Column {
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = summaryText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = androidx.compose.ui.graphics.Color.White
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        BikeIcon(
            modifier = Modifier.size(38.dp),
            tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.22f)
        )
    }
}
