package com.example.cyclingcheckin.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.cyclingcheckin.AppContainer
import com.example.cyclingcheckin.data.prefs.SecureStore
import com.example.cyclingcheckin.data.prefs.SettingsStore
import com.example.cyclingcheckin.data.remote.BackupService
import com.example.cyclingcheckin.data.remote.WebDavConfig
import com.example.cyclingcheckin.data.remote.WebDavError
import com.example.cyclingcheckin.data.remote.WebDavException
import com.example.cyclingcheckin.domain.model.AppSettings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WebDavUiState(
    val settings: AppSettings = AppSettings(),
    val password: String = "",
    val loaded: Boolean = false,
    val testing: Boolean = false,
    val saving: Boolean = false
)

sealed interface WebDavEvent {
    data object Saved : WebDavEvent
    data object TestSuccess : WebDavEvent
    data class TestFailure(val kind: WebDavError, val httpCode: Int?) : WebDavEvent
}

class WebDavConfigViewModel(
    private val settingsStore: SettingsStore,
    private val secureStore: SecureStore,
    private val backupService: BackupService
) : ViewModel() {

    private val testing = MutableStateFlow(false)
    private val saving = MutableStateFlow(false)
    private val loaded = MutableStateFlow(false)
    private val password = MutableStateFlow("")

    private val events = Channel<WebDavEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    private var initialPassword: String = ""

    val uiState: StateFlow<WebDavUiState> = combine(
        settingsStore.settings,
        password,
        testing,
        saving,
        loaded
    ) { settings, pwd, isTesting, isSaving, isLoaded ->
        WebDavUiState(
            settings = settings,
            password = pwd,
            loaded = isLoaded,
            testing = isTesting,
            saving = isSaving
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WebDavUiState()
    )

    init {
        viewModelScope.launch {
            val settings = settingsStore.settings.first()
            initialPassword = secureStore.webDavPassword
            password.value = initialPassword
            loaded.value = true
            // 触发一次 flow 组合更新
            settingsStore.settings.first()
        }
    }

    fun testConnection(server: String, account: String, appPassword: String, dir: String) {
        if (testing.value) return
        viewModelScope.launch {
            testing.value = true
            val config = WebDavConfig(
                serverUrl = server.trim(),
                username = account.trim(),
                password = appPassword,
                remoteDir = normalizeDir(dir)
            )
            val validation = validate(config)
            if (validation != null) {
                events.send(WebDavEvent.TestFailure(validation, null))
            } else {
                backupService.testConnection(config)
                    .onSuccess { events.send(WebDavEvent.TestSuccess) }
                    .onFailure {
                        if (it is WebDavException) {
                            events.send(WebDavEvent.TestFailure(it.kind, it.httpCode))
                        } else {
                            events.send(WebDavEvent.TestFailure(WebDavError.NETWORK, null))
                        }
                    }
            }
            testing.value = false
        }
    }

    fun save(server: String, account: String, appPassword: String, dir: String) {
        viewModelScope.launch {
            saving.value = true
            settingsStore.setWebDavServer(server)
            settingsStore.setWebDavAccount(account)
            settingsStore.setWebDavRemoteDir(normalizeDir(dir))
            secureStore.webDavPassword = appPassword
            initialPassword = appPassword
            password.value = appPassword
            saving.value = false
            events.send(WebDavEvent.Saved)
        }
    }

    private fun validate(config: WebDavConfig): WebDavError? {
        if (config.serverUrl.isBlank() || config.username.isBlank() || config.password.isBlank()) {
            return WebDavError.NOT_CONFIGURED
        }
        if (!config.serverUrl.startsWith("https://", ignoreCase = true)) {
            return WebDavError.INSECURE
        }
        return null
    }

    private fun normalizeDir(value: String): String {
        val trimmed = value.trim().ifEmpty { "/" }
        return "/" + trimmed.trim('/') + "/"
    }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                WebDavConfigViewModel(
                    settingsStore = container.settingsStore,
                    secureStore = container.secureStore,
                    backupService = container.backupService
                )
            }
        }
    }
}
