package com.example.cyclingcheckin.data.local

import android.content.Context
import com.example.cyclingcheckin.data.remote.WeatherEntry
import com.example.cyclingcheckin.data.remote.WeatherForecast
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate

/**
 * 最近一次成功获取的天气预报缓存。
 * 启动或断网时先用它展示天气，避免出现长时间占位；日期不匹配则视为过期。
 */
class WeatherCache(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    /** 仅当缓存覆盖今天与明天时返回，否则返回 null。 */
    fun loadValid(today: LocalDate = LocalDate.now()): WeatherForecast? {
        val raw = prefs.getString(KEY_FORECAST, null) ?: return null
        val dto = runCatching { json.decodeFromString(ForecastDto.serializer(), raw) }.getOrNull()
            ?: return null
        if (dto.today.date != today.toString()) return null
        if (dto.tomorrow.date != today.plusDays(1).toString()) return null
        return dto.toForecast()
    }

    fun save(forecast: WeatherForecast) {
        val dto = ForecastDto(
            today = DayDto.from(forecast.today),
            tomorrow = DayDto.from(forecast.tomorrow)
        )
        prefs.edit()
            .putString(KEY_FORECAST, json.encodeToString(ForecastDto.serializer(), dto))
            .apply()
    }

    @Serializable
    private data class ForecastDto(val today: DayDto, val tomorrow: DayDto) {
        fun toForecast() = WeatherForecast(today = today.toEntry(), tomorrow = tomorrow.toEntry())
    }

    @Serializable
    private data class DayDto(
        val date: String,
        val weather: String,
        val tempMax: Double,
        val tempMin: Double
    ) {
        fun toEntry() = WeatherEntry(
            date = date,
            weather = weather,
            tempMax = tempMax,
            tempMin = tempMin
        )

        companion object {
            fun from(entry: WeatherEntry) = DayDto(
                date = entry.date,
                weather = entry.weather,
                tempMax = entry.tempMax,
                tempMin = entry.tempMin
            )
        }
    }

    private companion object {
        const val PREFS_NAME = "weather_cache"
        const val KEY_FORECAST = "last_forecast"
    }
}
