package com.example.cyclingcheckin.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cyclingcheckin.AppContainer
import com.example.cyclingcheckin.data.repo.StatsRepository
import com.example.cyclingcheckin.data.repo.StatsSnapshot
import com.example.cyclingcheckin.domain.model.Record
import com.example.cyclingcheckin.domain.model.StatsMode
import com.example.cyclingcheckin.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class StatsUiState(
    val mode: StatsMode = StatsMode.WEEK,
    val anchor: LocalDate = LocalDate.now(),
    val snapshot: StatsSnapshot? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val loading: Boolean = true
) {
    val selectedRecords: List<Record>
        get() {
            val data = snapshot ?: return emptyList()
            val inRange = !selectedDate.isBefore(data.rangeStart) && !selectedDate.isAfter(data.rangeEnd)
            if (!inRange) return emptyList()
            return data.records.filter { it.date == selectedDate }
        }

    val effectiveSelectedDate: LocalDate
        get() {
            val data = snapshot ?: return selectedDate
            return if (!selectedDate.isBefore(data.rangeStart) && !selectedDate.isAfter(data.rangeEnd)) {
                selectedDate
            } else {
                data.rangeStart
            }
        }
}

class StatsViewModel(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val mode = MutableStateFlow(StatsMode.WEEK)
    private val anchor = MutableStateFlow(LocalDate.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<StatsUiState> = combine(
        mode,
        anchor,
        statsRepository.observeAll(),
        selectedDate
    ) { currentMode, currentAnchor, records, selected ->
        val snapshot = statsRepository.compute(records, currentMode, currentAnchor)
        StatsUiState(
            mode = currentMode,
            anchor = currentAnchor,
            snapshot = snapshot,
            selectedDate = selected,
            loading = false
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )

    fun setMode(newMode: StatsMode) {
        mode.value = newMode
        selectedDate.value = anchor.value
    }

    fun previousPeriod() {
        val newAnchor = when (mode.value) {
            StatsMode.DAY -> anchor.value.minusDays(1)
            StatsMode.WEEK -> anchor.value.minusWeeks(1)
            StatsMode.MONTH -> anchor.value.minusMonths(1)
        }
        anchor.value = newAnchor
        selectedDate.value = newAnchor
    }

    fun nextPeriod() {
        val newAnchor = when (mode.value) {
            StatsMode.DAY -> anchor.value.plusDays(1)
            StatsMode.WEEK -> anchor.value.plusWeeks(1)
            StatsMode.MONTH -> anchor.value.plusMonths(1)
        }
        anchor.value = newAnchor
        selectedDate.value = newAnchor
    }

    fun goToCurrent() {
        val today = LocalDate.now()
        anchor.value = today
        selectedDate.value = today
    }

    fun selectDay(date: LocalDate) {
        selectedDate.value = date
    }

    fun isCurrentPeriod(): Boolean {
        val today = LocalDate.now()
        return when (mode.value) {
            StatsMode.DAY -> anchor.value == today
            StatsMode.WEEK -> TimeUtils.startOfWeek(anchor.value) == TimeUtils.startOfWeek(today)
            StatsMode.MONTH -> TimeUtils.startOfMonth(anchor.value) == TimeUtils.startOfMonth(today)
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                StatsViewModel(container.statsRepository)
            }
        }
    }
}
