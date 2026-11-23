package com.example.cyclingcheckin.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditSheet(
    existing: Record?,
    preselectedDate: LocalDate? = null,
    onDismiss: () -> Unit,
    onSave: (date: LocalDate, time: LocalTime, distance: Double, note: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var dateEpochDay by rememberSaveable(existing?.id) {
        val initial = existing?.date ?: preselectedDate ?: LocalDate.now()
        mutableLongStateOf(initial.toEpochDay())
    }
    var hour by rememberSaveable(existing?.id) {
        mutableIntStateOf(existing?.time?.hour ?: LocalTime.now().hour)
    }
    var minute by rememberSaveable(existing?.id) {
        mutableIntStateOf(existing?.time?.minute ?: LocalTime.now().minute)
    }
    // 距离以「公里 + 0.1 公里」两列拨轮保存，避免数字键盘输入
    val initialTenths = ((existing?.distance ?: DEFAULT_DISTANCE_KM) * 10)
        .roundToInt()
        .coerceIn(1, MAX_KM * 10 + 9)
    var distanceKm by rememberSaveable(existing?.id) {
        mutableIntStateOf(initialTenths / 10)
    }
    var distanceTenth by rememberSaveable(existing?.id) {
        mutableIntStateOf(initialTenths % 10)
    }
    var note by rememberSaveable(existing?.id) {
        mutableStateOf(existing?.note.orEmpty())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val date = LocalDate.ofEpochDay(dateEpochDay)
    val time = LocalTime.of(hour, minute)
    val dateTime = LocalDateTime.of(date, time)
    val distanceText = "$distanceKm.$distanceTenth"
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
                    DistanceWheelPicker(
                        kilometers = distanceKm,
                        tenth = distanceTenth,
                        onKilometersChange = { distanceKm = it },
                        onTenthChange = { distanceTenth = it },
                        isError = validation.error != null &&
                            validation.error != RecordFieldError.TIME_IN_FUTURE
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = distanceSupportText(validation.error),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (validation.error != null &&
                            validation.error != RecordFieldError.TIME_IN_FUTURE
                        ) {
                            MaterialTheme.colorScheme.error
                        } else {
                            RideLogTheme.colors.secondaryText
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

// ---------------------------------------------------------------------------
// 距离拨轮选择器（类似闹钟的上下滑动选择）
// ---------------------------------------------------------------------------

private const val MAX_KM = 9999
private const val DEFAULT_DISTANCE_KM = 5.0
private val WHEEL_ITEM_HEIGHT = 36.dp
private const val WHEEL_VISIBLE_COUNT = 3

/** 循环拨轮重复的次数（小数位 0-9 循环滚动用）。 */
private const val LOOP_REPEATS = 500

/** 两列拨轮：整数公里 + 一位小数。 */
@Composable
private fun DistanceWheelPicker(
    kilometers: Int,
    tenth: Int,
    onKilometersChange: (Int) -> Unit,
    onTenthChange: (Int) -> Unit,
    isError: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WHEEL_ITEM_HEIGHT * WHEEL_VISIBLE_COUNT)
                .padding(horizontal = 10.dp)
        ) {
            // 中间选中高亮条
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WHEEL_ITEM_HEIGHT)
                    .align(Alignment.Center)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        RoundedCornerShape(10.dp)
                    )
            )
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelColumn(
                    count = MAX_KM + 1,
                    selected = kilometers,
                    onSelected = onKilometersChange,
                    modifier = Modifier.weight(1.4f)
                )
                Box(
                    modifier = Modifier.height(WHEEL_ITEM_HEIGHT),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ".",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                WheelColumn(
                    count = 10,
                    selected = tenth,
                    onSelected = onTenthChange,
                    modifier = Modifier.weight(0.8f),
                    loop = true
                )
                Text(
                    text = stringResource(R.string.unit_km),
                    style = MaterialTheme.typography.bodyMedium,
                    color = RideLogTheme.colors.secondaryText
                )
            }
        }
    }
}

/** 单列拨轮：滚动停止后自动吸附，中间项即选中值。loop 为 true 时循环滚动。 */
@Composable
private fun WheelColumn(
    count: Int,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    loop: Boolean = false
) {
    val totalCount = if (loop) count * LOOP_REPEATS else count
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = remember(count, loop) {
            if (loop) {
                val middle = totalCount / 2
                middle - middle % count + selected.coerceIn(0, count - 1)
            } else {
                selected.coerceIn(0, count - 1)
            }
        }
    )
    val centerIndex by remember(count, loop) {
        derivedStateOf { listState.centerItemIndex() }
    }
    LaunchedEffect(centerIndex) {
        onSelected(if (loop) centerIndex % count else centerIndex)
    }

    // 滚动停止后把最近的一项吸附到正中，保证视觉与取值一致
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.isScrollInProgress to listState.layoutInfo.visibleItemsInfo.size
        }.collect { (scrolling, visibleCount) ->
            if (scrolling || visibleCount == 0) return@collect
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            val closest = info.visibleItemsInfo
                .minByOrNull { abs(it.offset + it.size / 2 - center) } ?: return@collect
            val delta = closest.offset + closest.size / 2 - center
            if (delta != 0) listState.animateScrollBy(delta.toFloat())
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        // 上下留白，让首尾项也能滚到中间
        contentPadding = PaddingValues(
            vertical = WHEEL_ITEM_HEIGHT * (WHEEL_VISIBLE_COUNT / 2)
        ),
        flingBehavior = rememberSnapFlingBehavior(listState)
    ) {
        items(totalCount) { index ->
            val value = if (loop) index % count else index
            val distance = abs(index - centerIndex)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WHEEL_ITEM_HEIGHT),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (distance == 0) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = when (distance) {
                            0 -> 1f
                            1 -> 0.45f
                            else -> 0.22f
                        }
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** 当前位于视口正中的项索引。 */
private fun LazyListState.centerItemIndex(): Int {
    val info = layoutInfo
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo
        .minByOrNull { abs(it.offset + it.size / 2 - center) }
        ?.index ?: firstVisibleItemIndex
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
