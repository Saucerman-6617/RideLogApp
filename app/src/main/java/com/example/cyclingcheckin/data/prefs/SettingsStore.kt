package com.example.cyclingcheckin.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.cyclingcheckin.domain.model.AppSettings
import com.example.cyclingcheckin.domain.model.CalendarMarkStyle
import com.example.cyclingcheckin.domain.model.FrequentAddress
import com.example.cyclingcheckin.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ride_log_settings")

class SettingsStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    private val json = Json { ignoreUnknownKeys = true }

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[KEY_THEME] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            calendarMarkStyle = runCatching {
                CalendarMarkStyle.valueOf(prefs[KEY_MARK_STYLE] ?: CalendarMarkStyle.DOT.name)
            }.getOrDefault(CalendarMarkStyle.DOT),
            autoBackupAfterRecord = prefs[KEY_AUTO_BACKUP_AFTER_RECORD] ?: true,
            lastBackupAt = prefs[KEY_LAST_BACKUP_AT],
            webDavServer = prefs[KEY_WEBDAV_SERVER] ?: AppSettings.DEFAULT_WEBDAV_SERVER,
            webDavAccount = prefs[KEY_WEBDAV_ACCOUNT].orEmpty(),
            webDavRemoteDir = prefs[KEY_WEBDAV_DIR] ?: AppSettings.DEFAULT_WEBDAV_DIR,
            frequentAddresses = decodeFrequentAddresses(prefs[KEY_FREQUENT_ADDRESSES])
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setCalendarMarkStyle(style: CalendarMarkStyle) {
        dataStore.edit { it[KEY_MARK_STYLE] = style.name }
    }

    suspend fun setAutoBackupAfterRecord(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_BACKUP_AFTER_RECORD] = enabled }
    }

    suspend fun setLastBackupAt(timestamp: Long) {
        dataStore.edit { it[KEY_LAST_BACKUP_AT] = timestamp }
    }

    suspend fun setWebDavServer(value: String) {
        dataStore.edit { it[KEY_WEBDAV_SERVER] = value.trim() }
    }

    suspend fun setWebDavAccount(value: String) {
        dataStore.edit { it[KEY_WEBDAV_ACCOUNT] = value.trim() }
    }

    suspend fun setWebDavRemoteDir(value: String) {
        dataStore.edit { it[KEY_WEBDAV_DIR] = normalizeDir(value) }
    }

    suspend fun saveFrequentAddresses(addresses: List<FrequentAddress>) {
        dataStore.edit {
            it[KEY_FREQUENT_ADDRESSES] = json.encodeToString(
                ListSerializer(FrequentAddress.serializer()),
                addresses
            )
        }
    }

    private fun decodeFrequentAddresses(raw: String?): List<FrequentAddress> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(FrequentAddress.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun normalizeDir(value: String): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return "/"
        return "/" + trimmed.trim('/') + "/"
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_MARK_STYLE = stringPreferencesKey("calendar_mark_style")
        val KEY_AUTO_BACKUP_AFTER_RECORD = booleanPreferencesKey("auto_backup_after_record")
        val KEY_LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val KEY_WEBDAV_SERVER = stringPreferencesKey("webdav_server")
        val KEY_WEBDAV_ACCOUNT = stringPreferencesKey("webdav_account")
        val KEY_WEBDAV_DIR = stringPreferencesKey("webdav_dir")
        val KEY_FREQUENT_ADDRESSES = stringPreferencesKey("frequent_addresses")
    }
}
