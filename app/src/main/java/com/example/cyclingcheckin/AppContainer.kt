package com.example.cyclingcheckin

import android.content.Context
import com.example.cyclingcheckin.data.local.AppDatabase
import com.example.cyclingcheckin.data.local.LocationCache
import com.example.cyclingcheckin.data.prefs.SecureStore
import com.example.cyclingcheckin.data.prefs.SettingsStore
import com.example.cyclingcheckin.data.remote.BackupService
import com.example.cyclingcheckin.data.remote.LocationService
import com.example.cyclingcheckin.data.remote.WebDavClient
import com.example.cyclingcheckin.data.remote.WeatherService
import com.example.cyclingcheckin.data.repo.RecordRepository
import com.example.cyclingcheckin.data.repo.RecordRepositoryImpl
import com.example.cyclingcheckin.data.repo.StatsRepository

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase = AppDatabase.create(appContext)
    val recordRepository: RecordRepository = RecordRepositoryImpl(database.recordDao())
    val statsRepository: StatsRepository = StatsRepository(recordRepository)
    val settingsStore: SettingsStore = SettingsStore(appContext)
    val secureStore: SecureStore = SecureStore(appContext)
    val webDavClient: WebDavClient = WebDavClient()
    val weatherService: WeatherService = WeatherService()
    val locationService: LocationService = LocationService(appContext)
    val locationCache: LocationCache = LocationCache(appContext)
    val backupService: BackupService = BackupService(
        repository = recordRepository,
        settingsStore = settingsStore,
        secureStore = secureStore,
        client = webDavClient,
        context = appContext
    )
}
