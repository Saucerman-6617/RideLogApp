package com.example.cyclingcheckin.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.util.Log
import com.example.cyclingcheckin.AppContainer
import com.example.cyclingcheckin.data.prefs.SettingsStore
import com.example.cyclingcheckin.data.remote.BackupService
import com.example.cyclingcheckin.data.remote.LocationService
import com.example.cyclingcheckin.data.remote.WeatherForecast
import com.example.cyclingcheckin.data.remote.WeatherService
import com.example.cyclingcheckin.data.repo.RecordRepository
import com.example.cyclingcheckin.domain.model.CalendarMarkStyle
import com.example.cyclingcheckin.domain.model.DayStat
import com.example.cyclingcheckin.domain.model.Record
import com.example.cyclingcheckin.domain.model.Validators
import com.example.cyclingcheckin.util.TimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val visibleMonth: YearMonth = YearMonth.now(),
    val records: List<Record> = emptyList(),
    val monthStats: Map<LocalDate, DayStat> = emptyMap(),
    val markStyle: CalendarMarkStyle = CalendarMarkStyle.DOT,
    val loading: Boolean = true
) {
    val selectedCount: Int get() = records.size
    val selectedDistance: Double
        get() = Validators.roundDistance(records.sumOf { it.distance })
}

sealed interface HomeEvent {
    data class Added(val record: Record) : HomeEvent
    data class Updated(val record: Record) : HomeEvent
    data class Deleted(val record: Record) : HomeEvent
    data class Restored(val record: Record) : HomeEvent
    data class Failed(val message: String) : HomeEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: RecordRepository,
    private val settingsStore: SettingsStore,
    private val backupService: BackupService,
    private val weatherService: WeatherService,
    private val locationService: LocationService
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val visibleMonth = MutableStateFlow(YearMonth.now())

    private val events = Channel<HomeEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    /** 天气数据：null 表示未加载 / 无权限 / 网络失败，UI 静默降级。 */
    private val _weather = MutableStateFlow<WeatherForecast?>(null)
    val weather: StateFlow<WeatherForecast?> = _weather

    /** 当前定位的位置名称（区县 / 城市），null 表示未能解析。 */
    private val _locationName = MutableStateFlow<String?>(null)
    val locationName: StateFlow<String?> = _locationName

    init {
        refreshWeather()
    }

    /** 尝试获取位置并拉取今明两天天气。永远展示（兜底坐标）。 */
    fun refreshWeather() {
        viewModelScope.launch {
            val (lat, lng) = locationService.getLastKnown()
            Log.d("HomeVM", "got location: $lat,$lng")
            val forecast = runCatching { weatherService.fetchForecast(lat, lng) }
                .onFailure { Log.e("HomeVM", "weather fetch exception", it) }
                .getOrNull()
            Log.d("HomeVM", "forecast=$forecast")
            _weather.value = forecast
            // 位置名称在天气之后解析，避免拖慢首屏
            _locationName.value = locationService.getPlaceName(lat, lng)
            Log.d("HomeVM", "locationName=${_locationName.value}")
        }
    }

    private val selectedRecords = selectedDate.flatMapLatest { date ->
        repository.observeByDate(date)
    }

    private val monthStats = visibleMonth.flatMapLatest { month ->
        repository.observeRange(month.atDay(1), month.atEndOfMonth()).map { stats ->
            stats.associateBy { it.date }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        selectedDate,
        visibleMonth,
        selectedRecords,
        monthStats,
        settingsStore.settings
    ) { date, month, records, stats, settings ->
        HomeUiState(
            selectedDate = date,
            visibleMonth = month,
            records = records,
            monthStats = stats,
            markStyle = settings.calendarMarkStyle,
            loading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
        visibleMonth.value = YearMonth.from(date)
    }

    fun changeMonth(offset: Long) {
        val target = visibleMonth.value.plusMonths(offset)
        visibleMonth.value = target
        val today = LocalDate.now()
        selectedDate.value = if (YearMonth.from(today) == target) {
            today
        } else {
            target.atDay(1)
        }
    }

    fun goToToday() {
        val today = LocalDate.now()
        visibleMonth.value = YearMonth.from(today)
        selectedDate.value = today
    }

    fun saveRecord(
        existing: Record?,
        date: LocalDate,
        time: LocalTime,
        distance: Double,
        note: String
    ) {
        viewModelScope.launch {
            if (existing == null) {
                repository.create(date, time, distance, note)
                    .onSuccess {
                        events.send(HomeEvent.Added(it))
                        triggerAutoBackup()
                    }
                    .onFailure { events.send(HomeEvent.Failed("保存失败，请重试")) }
            } else {
                val updated = existing.copy(
                    date = date,
                    time = time,
                    distance = Validators.roundDistance(distance),
                    note = note
                )
                repository.update(updated)
                    .onSuccess { events.send(HomeEvent.Updated(updated)) }
                    .onFailure { events.send(HomeEvent.Failed("保存失败，请重试")) }
            }
        }
    }

    /** 自动备份：新增记录后，如果开启了自动备份且已配置 WebDAV，则后台触发一次全量备份。 */
    private fun triggerAutoBackup() {
        viewModelScope.launch {
            try {
                val settings = settingsStore.settings.first()
                if (!settings.autoBackupAfterRecord) return@launch
                if (!settings.webDavConfigured) return@launch
                backupService.backup()
            } catch (_: Exception) {
                // 自动备份静默失败，不打扰用户
            }
        }
    }

    fun deleteRecord(record: Record) {
        viewModelScope.launch {
            repository.delete(record)
                .onSuccess {
                    events.send(HomeEvent.Deleted(record))
                    triggerAutoBackup()
                }
                .onFailure { events.send(HomeEvent.Failed("删除失败，请重试")) }
        }
    }

    fun restoreRecord(record: Record) {
        viewModelScope.launch {
            repository.restore(record)
                .onSuccess { events.send(HomeEvent.Restored(record)) }
                .onFailure { events.send(HomeEvent.Failed("恢复失败，请重试")) }
        }
    }

    fun undoAdd(record: Record) {
        viewModelScope.launch {
            repository.deleteById(record.id)
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                HomeViewModel(
                    container.recordRepository,
                    container.settingsStore,
                    container.backupService,
                    container.weatherService,
                    container.locationService
                )
            }
        }
    }
}
