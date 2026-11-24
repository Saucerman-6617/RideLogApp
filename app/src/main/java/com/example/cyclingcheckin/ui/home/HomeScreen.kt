package com.example.cyclingcheckin.ui.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cyclingcheckin.AppContainer
import com.example.cyclingcheckin.R
import com.example.cyclingcheckin.data.remote.WeatherForecast
import com.example.cyclingcheckin.data.remote.WeatherIcon
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
    val weather by viewModel.weather.collectAsStateWithLifecycle()
    val location by viewModel.location.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 启动时随机选一条励志标语，整个会话保持不变
    val quotes = stringArrayResource(R.array.home_quotes)
    val quote by remember { mutableStateOf(quotes.random()) }

    var showSheet by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<Record?>(null) }
    var pendingDelete by remember { mutableStateOf<Record?>(null) }

    // 位置权限：首次启动请求一次；点定位图标时如无权限则再次请求
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> viewModel.refreshWeather() }

    val onRefreshLocation: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.refreshWeather()
        } else {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!hasCoarse && !hasFine) {
            locationLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

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
            HomeHeader(
                weather = weather,
                location = location,
                quote = quote,
                onRefreshLocation = onRefreshLocation
            )
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
                EmptyState(
                    title = stringResource(R.string.home_empty_date),
                    hint = ""
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
            frequentAddresses = uiState.frequentAddresses,
            onSaveFrequentAddress = viewModel::saveFrequentAddress,
            onDeleteFrequentAddress = viewModel::deleteFrequentAddress,
            onDismiss = { showSheet = false },
            onSave = { date, time, distance, note, routeStart, routeEnd ->
                viewModel.saveRecord(editingRecord, date, time, distance, note, routeStart, routeEnd)
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
private fun HomeHeader(
    weather: WeatherForecast?,
    location: HomeLocation,
    quote: String,
    onRefreshLocation: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左半部分：Logo + 标题，日期与标语单独占整行，避免被图标挤窄
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    BikeIcon(modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
            Text(
                text = "${TimeUtils.fullDateLabel(LocalDate.now())}    ${TimeUtils.weekdayLong(LocalDate.now())}",
                style = MaterialTheme.typography.bodySmall,
                color = RideLogTheme.colors.secondaryText,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
            QuoteBar(text = quote)
        }
        Spacer(modifier = Modifier.width(8.dp))
        // 右半部分：宽度由定位 + 天气内容决定，剩余宽度全部给左侧
        Row(verticalAlignment = Alignment.CenterVertically) {
            LocationBadge(
                name = location.name ?: stringResource(
                    if (location.usingDefault) R.string.home_location_default
                    else R.string.home_location_current
                ),
                onClick = onRefreshLocation
            )
            Spacer(modifier = Modifier.width(6.dp))
            WeatherWidget(forecast = weather)
        }
    }
}

/** 标题下方的励志标语，固定最小高度，保证天气加载前后布局稳定。 */
@Composable
private fun QuoteBar(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp)
            .padding(top = 6.dp, bottom = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** 定位标记：上方蓝色定位图标，下方位置名称，点击重新获取。固定宽度避免标题区宽度变化。 */
@Composable
private fun LocationBadge(name: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(LOCATION_BADGE_WIDTH)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = stringResource(R.string.home_location_cd),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(27.dp)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = RideLogTheme.colors.secondaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 今明两天天气。始终使用完整布局：
 * 数据未到时用占位文本（相同行数/字号），保证启动阶段与加载完成后布局完全一致。
 */
@Composable
private fun WeatherWidget(forecast: WeatherForecast?) {
    Box(
        modifier = Modifier
            .width(WEATHER_WIDGET_WIDTH)
            .height(WEATHER_WIDGET_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (forecast != null) {
                WeatherDayCell(
                    label = "今天",
                    mmdd = forecast.today.date.substringAfter("-"),
                    icon = WeatherIcon.fromDesc(forecast.today.weather),
                    tempMax = forecast.today.tempMax,
                    tempMin = forecast.today.tempMin
                )
                WeatherDayCell(
                    label = "明天",
                    mmdd = forecast.tomorrow.date.substringAfter("-"),
                    icon = WeatherIcon.fromDesc(forecast.tomorrow.weather),
                    tempMax = forecast.tomorrow.tempMax,
                    tempMin = forecast.tomorrow.tempMin
                )
            } else {
                val today = LocalDate.now()
                WeatherPlaceholderCell(label = "今天", date = today)
                WeatherPlaceholderCell(label = "明天", date = today.plusDays(1))
            }
        }
    }
}

/** 天气未加载时的占位单元，结构与 [WeatherDayCell] 一致，确保高度相同。 */
@Composable
private fun WeatherPlaceholderCell(label: String, date: LocalDate) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = RideLogTheme.colors.secondaryText
        )
        Text(
            text = "%02d/%02d".format(date.monthValue, date.dayOfMonth),
            style = MaterialTheme.typography.labelSmall,
            color = RideLogTheme.colors.secondaryText
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "⏳",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp)
        )
        Text(
            text = "--",
            style = MaterialTheme.typography.labelSmall,
            color = RideLogTheme.colors.secondaryText.copy(alpha = 0.6f)
        )
        Text(
            text = "--°/--°",
            style = MaterialTheme.typography.labelSmall,
            color = RideLogTheme.colors.secondaryText.copy(alpha = 0.6f)
        )
    }
}

private val LOCATION_BADGE_WIDTH = 60.dp
private val WEATHER_WIDGET_WIDTH = 104.dp
private val WEATHER_WIDGET_HEIGHT = 96.dp

@Composable
private fun WeatherDayCell(
    label: String,
    mmdd: String,
    icon: WeatherIcon,
    tempMax: Double,
    tempMin: Double
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = RideLogTheme.colors.secondaryText
        )
        Text(
            text = mmdd.replace("-", "/"),
            style = MaterialTheme.typography.labelSmall,
            color = RideLogTheme.colors.secondaryText
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = icon.emoji,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp)
        )
        Text(
            text = icon.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${tempMax.toInt()}°/${tempMin.toInt()}°",
            style = MaterialTheme.typography.labelSmall,
            color = RideLogTheme.colors.secondaryText
        )
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
