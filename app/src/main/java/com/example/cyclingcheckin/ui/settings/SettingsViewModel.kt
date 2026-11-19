package com.example.cyclingcheckin.ui.settings

import android.content.Context
import android.net.Uri
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
import com.example.cyclingcheckin.data.repo.RecordRepository
import com.example.cyclingcheckin.domain.model.AppSettings
import com.example.cyclingcheckin.domain.model.CalendarMarkStyle
import com.example.cyclingcheckin.domain.model.Record
import com.example.cyclingcheckin.domain.model.ThemeMode
import com.example.cyclingcheckin.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val busy: Boolean = false
)

sealed interface SettingsEvent {
    data class Message(val text: String) : SettingsEvent
    data class BackupDone(val count: Int) : SettingsEvent
    data class RemoteBackupsLoaded(val files: List<RemoteBackupFile>) : SettingsEvent
    data class ImportFileLoaded(val records: List<Record>) : SettingsEvent
    data class RestoreDone(val imported: Int, val skipped: Int) : SettingsEvent
    data class WebDavFailure(val kind: WebDavError, val httpCode: Int?) : SettingsEvent
}

class SettingsViewModel(
    private val repository: RecordRepository,
    private val settingsStore: SettingsStore,
    private val backupService: BackupService,
    private val appContext: Context
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val events = Channel<SettingsEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsStore.settings,
        busy
    ) { settings, isBusy ->
        SettingsUiState(settings = settings, busy = isBusy)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setCalendarMarkStyle(style: CalendarMarkStyle) {
        viewModelScope.launch { settingsStore.setCalendarMarkStyle(style) }
    }

    fun exportJson(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val records = repository.getAll()
                    val json = com.example.cyclingcheckin.data.remote.BackupCodec.encode(
                        records,
                        android.os.Build.MODEL
                    )
                    FileUtils.writeText(appContext, uri, json)
                    records.size
                }
            }
            result.onSuccess { events.send(SettingsEvent.Message("已导出 $it 条记录（JSON）")) }
                .onFailure { events.send(SettingsEvent.Message("导出失败：${it.message ?: "未知错误"}")) }
        }
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val records = repository.getAll()
                    FileUtils.writeText(appContext, uri, FileUtils.csvOf(records))
                    records.size
                }
            }
            result.onSuccess { events.send(SettingsEvent.Message("已导出 $it 条记录（CSV）")) }
                .onFailure { events.send(SettingsEvent.Message("导出失败：${it.message ?: "未知错误"}")) }
        }
    }

    fun readImportFile(uri: Uri) {
        viewModelScope.launch {
            busy.value = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = FileUtils.readText(appContext, uri)
                    com.example.cyclingcheckin.data.remote.BackupCodec.decode(text)
                }
            }
            busy.value = false
            result.onSuccess { events.send(SettingsEvent.ImportFileLoaded(it)) }
                .onFailure { events.send(SettingsEvent.Message("文件损坏或版本不兼容")) }
        }
    }

    fun importRecords(records: List<Record>, overwrite: Boolean) {
        viewModelScope.launch {
            busy.value = true
            val result = runCatching { repository.importRecords(records, overwrite) }
            busy.value = false
            result.onSuccess {
                events.send(SettingsEvent.Message("导入完成：新增 ${it.imported} 条，跳过 ${it.skipped} 条"))
            }.onFailure {
                events.send(SettingsEvent.Message("导入失败，请重试"))
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            runCatching { repository.clearAll() }
                .onSuccess { events.send(SettingsEvent.Message("已清空全部数据")) }
                .onFailure { events.send(SettingsEvent.Message("清空失败，请重试")) }
        }
    }

    fun backupNow() {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            backupService.backup()
                .onSuccess { events.send(SettingsEvent.BackupDone(it.count)) }
                .onFailure { events.send(it.toEvent()) }
            busy.value = false
        }
    }

    fun loadRemoteBackups() {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            backupService.listBackups()
                .onSuccess { files ->
                    // 即使列表为空也发事件，让 UI 展示空态对话框
                    events.send(SettingsEvent.RemoteBackupsLoaded(files))
                }
                .onFailure { events.send(it.toEvent()) }
            busy.value = false
        }
    }

    fun restore(file: RemoteBackupFile, policy: ConflictPolicy) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            backupService.restore(file, policy)
                .onSuccess { events.send(SettingsEvent.RestoreDone(it.imported, it.skipped)) }
                .onFailure { events.send(it.toEvent()) }
            busy.value = false
        }
    }

    private fun Throwable.toEvent(): SettingsEvent {
        return if (this is WebDavException) {
            SettingsEvent.WebDavFailure(kind, httpCode)
        } else {
            SettingsEvent.WebDavFailure(WebDavError.NETWORK, null)
        }
    }

    companion object {
        fun factory(container: AppContainer, context: Context) = viewModelFactory {
            initializer {
                SettingsViewModel(
                    repository = container.recordRepository,
                    settingsStore = container.settingsStore,
                    backupService = container.backupService,
                    appContext = context.applicationContext
                )
            }
        }
    }
}
