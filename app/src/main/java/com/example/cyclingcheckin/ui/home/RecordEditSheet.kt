package com.example.cyclingcheckin.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.cyclingcheckin.R
import com.example.cyclingcheckin.domain.model.Record
import com.example.cyclingcheckin.domain.model.RecordFieldError
import com.example.cyclingcheckin.domain.model.Validators
import com.example.cyclingcheckin.ui.theme.RideLogTheme
import com.example.cyclingcheckin.util.TimeUtils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditSheet(
    existing: Record?,
    onDismiss: () -> Unit,
    onSave: (date: LocalDate, time: LocalTime, distance: Double, note: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var dateEpochDay by rememberSaveable(existing?.id) {
        mutableLongStateOf((existing?.date ?: LocalDate.now()).toEpochDay())
    }
    var hour by rememberSaveable(existing?.id) {
        mutableIntStateOf(existing?.time?.hour ?: LocalTime.now().hour)
    }
    var minute by rememberSaveable(existing?.id) {
        mutableIntStateOf(existing?.time?.minute ?: LocalTime.now().minute)
    }
    var distanceText by rememberSaveable(existing?.id) {
        mutableStateOf(
            existing?.let { TimeUtils.formatDistance(it.distance) } ?: "5.0"
        )
    }
    var note by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.note.orEmpty())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val date = LocalDate.ofEpochDay(dateEpochDay)
    val time = LocalTime.of(hour, minute)
    val dateTime = LocalDateTime.of(date, time)
    val validation = Validators.validate(distanceText, dateTime, note)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
                Text(
                    text = stringResource(
                        if (existing == null) R.string.record_title_add else R.string.record_title_edit
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = {
                        val distance = validation.distance ?: return@TextButton
                        onSave(date, time, distance, note)
                    },
                    enabled = validation.isValid
                ) {
                    Text(
                        text = stringResource(
                            if (existing == null) R.string.action_add_record else R.string.action_save
                        ),
                        color = if (validation.isValid) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.record_label_date_time),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FieldShell(
                            modifier = Modifier.weight(1f),
                            onClick = { showDatePicker = true }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = TimeUtils.formatDate(date),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        FieldShell(
                            modifier = Modifier.weight(1f),
                            onClick = { showTimePicker = true }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_clock),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = String.format("%02d:%02d", hour, minute),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val timeError = validation.error == RecordFieldError.TIME_IN_FUTURE
                    Text(
                        text = if (timeError) {
                            stringResource(R.string.error_time_future)
                        } else {
                            stringResource(R.string.record_label_date_time_hint)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (timeError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            RideLogTheme.colors.secondaryText
                        }
                    )
                }

                Column {
                    Text(
                        text = stringResource(R.string.record_label_distance),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = distanceText,
                        onValueChange = { distanceText = Validators.sanitizeDistanceInput(it) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        isError = validation.error != null && validation.error != RecordFieldError.TIME_IN_FUTURE,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_bike),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.unit_km),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = RideLogTheme.colors.secondaryText
                                )
                                if (distanceText.isNotEmpty()) {
                                    IconButton(onClick = { distanceText = "" }) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.record_clear_distance),
                                            tint = RideLogTheme.colors.secondaryText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = {
                            Text(
                                text = distanceSupportText(validation.error),
                                color = if (validation.error != null &&
                                    validation.error != RecordFieldError.TIME_IN_FUTURE
                                ) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    RideLogTheme.colors.secondaryText
                                }
                            )
                        }
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.record_label_note),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(
                                R.string.record_note_counter,
                                note.length,
                                Validators.MAX_NOTE_LENGTH
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = RideLogTheme.colors.secondaryText
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = {
                            if (it.length <= Validators.MAX_NOTE_LENGTH) note = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.record_note_placeholder),
                                color = RideLogTheme.colors.secondaryText
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.toEpochDay() * MILLIS_PER_DAY
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            dateEpochDay = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toEpochDay()
                        }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = true
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.record_label_date_time),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TimePicker(state = pickerState)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        TextButton(
                            onClick = {
                                hour = pickerState.hour
                                minute = pickerState.minute
                                showTimePicker = false
                            }
                        ) {
                            Text(stringResource(R.string.action_confirm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldShell(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
private fun distanceSupportText(error: RecordFieldError?): String = when (error) {
    RecordFieldError.DISTANCE_EMPTY -> stringResource(R.string.error_distance_empty)
    RecordFieldError.DISTANCE_INVALID -> stringResource(R.string.error_distance_invalid)
    RecordFieldError.DISTANCE_OUT_OF_RANGE -> stringResource(R.string.error_distance_range)
    RecordFieldError.TIME_IN_FUTURE -> stringResource(R.string.record_distance_hint)
    RecordFieldError.NOTE_TOO_LONG -> stringResource(R.string.error_note_too_long)
    null -> stringResource(R.string.record_distance_hint)
}

private const val MILLIS_PER_DAY = 86_400_000L
