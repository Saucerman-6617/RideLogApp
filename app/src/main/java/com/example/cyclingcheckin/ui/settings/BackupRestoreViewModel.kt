package com.example.cyclingcheckin.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cyclingcheckin.AppContainer
import com.example.cyclingcheckin.data.prefs.SettingsStore
import com.example.cyclingcheckin.data.remote.BackupService
import com.example.cyclingcheckin.data.remote.ConflictPolicy
import com.example.cyclingcheckin.data.remote.RemoteBackupFile
import com.example.cyclingcheckin.data.remote.WebDavError
import com.example.cyclingcheckin.data.remote.WebDavException
import com.example.cyclingcheckin.util.TimeUtils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BackupRestoreUiState(
    val autoBackupAfterRecord: Boolean = true,
    val webDavConfigured: Boolean = false,
    val lastBackupAt: Long? = null,
    val busy: Boolean = false,
    val files: List<RemoteBackupFile> = emptyList(),
    val loadedOnce: Boolean = false
)

sealed interface BackupRestoreEvent {
    data class Message(val text: String) : BackupRestoreEvent
    data class BackupDone(val count: Int) : BackupRestoreEvent
    data class RestoreDone(val imported: Int, val skipped: Int) : BackupRestoreEvent
    data class WebDavFailure(val kind: WebDavError, val httpCode: Int?) : BackupRestoreEvent
}

class BackupRestoreViewModel(
    private val settingsStore: SettingsStore,
    private val backupService: BackupService
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val files = MutableStateFlow<List<RemoteBackupFile>>(emptyList())
    private val loadedOnce = MutableStateFlow(false)
    private val events = Channel<BackupRestoreEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<BackupRestoreUiState> = combine(
        settingsStore.settings,
        busy,
        files,
        loadedOnce
    ) { settings, isBusy, list, once ->
        BackupRestoreUiState(
            autoBackupAfterRecord = settings.autoBackupAfterRecord,
            webDavConfigured = settings.webDavConfigured,
            lastBackupAt = settings.lastBackupAt,
            busy = isBusy,
            files = list,
            loadedOnce = once
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BackupRestoreUiState()
    )

    fun setAutoBackupAfterRecord(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setAutoBackupAfterRecord(enabled)
        }
    }

    fun refreshFiles() {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            backupService.listBackups()
                .onSuccess {
                    files.value = it
                    loadedOnce.value = true
                }
                .onFailure { events.send(it.toEvent()) }
            busy.value = false
        }
    }

    fun backupNow() {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            backupService.backup()
                .onSuccess { events.send(BackupRestoreEvent.BackupDone(it.count)) }
                .onFailure { events.send(it.toEvent()) }
            busy.value = false
        }
    }

    fun restore(file: RemoteBackupFile) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            backupService.restore(file, ConflictPolicy.MERGE)
                .onSuccess { events.send(BackupRestoreEvent.RestoreDone(it.imported, it.skipped)) }
                .onFailure { events.send(it.toEvent()) }
            busy.value = false
        }
    }

    private fun Throwable.toEvent(): BackupRestoreEvent {
        return if (this is WebDavException) {
            BackupRestoreEvent.WebDavFailure(kind, httpCode)
        } else {
            BackupRestoreEvent.WebDavFailure(WebDavError.NETWORK, null)
        }
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                BackupRestoreViewModel(
                    settingsStore = container.settingsStore,
                    backupService = container.backupService
                )
            }
        }
    }
}
