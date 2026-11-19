package com.example.cyclingcheckin.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cyclingcheckin.AppContainer
import com.example.cyclingcheckin.data.prefs.SettingsStore
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
    settingsStore: SettingsStore
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val visibleMonth = MutableStateFlow(YearMonth.now())

    private val events = Channel<HomeEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

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
                    .onSuccess { events.send(HomeEvent.Added(it)) }
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

    fun deleteRecord(record: Record) {
        viewModelScope.launch {
            repository.delete(record)
                .onSuccess { events.send(HomeEvent.Deleted(record)) }
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
                HomeViewModel(container.recordRepository, container.settingsStore)
            }
        }
    }
}
