package com.example.cyclingcheckin.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration

private const val TAG = "WeatherService"

/**
 * Open-Meteo 免费天气 API 客户端（无需 API Key / 签名）。
 * 文档：https://open-meteo.com/en/docs
 *
 * 请求 /v1/forecast，取未来两天的 WMO 天气代码与最高 / 最低温度。
 */
class WeatherService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Duration.ofSeconds(10))
        .readTimeout(Duration.ofSeconds(10))
        .build()
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val endpoint = "https://api.open-meteo.com/v1/forecast"

    /**
     * 获取指定经纬度的今明两天天气预报。
     * 返回 null 表示网络失败 / 数据不全。
     */
    suspend fun fetchForecast(latitude: Double, longitude: Double): WeatherForecast? {
        val url = "$endpoint?latitude=$latitude&longitude=$longitude" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
            "&timezone=auto&forecast_days=2"
        Log.d(TAG, "URL=$url")

        // OkHttp execute() 是同步阻塞调用，必须在 IO 线程上
        val body = withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            val response = runCatching { client.newCall(request).execute() }
                .onFailure { Log.e(TAG, "network error: ${it.message}") }
                .getOrNull() ?: return@withContext null
            Log.d(TAG, "HTTP ${response.code}")
            if (!response.isSuccessful) return@withContext null
            response.body?.string()
        }
        if (body == null) { Log.e(TAG, "empty body or failed"); return null }
        Log.d(TAG, "body=$body")

        val parsed = runCatching { json.decodeFromString(OpenMeteoResponse.serializer(), body) }
            .onFailure { Log.e(TAG, "parse error: ${it.message}") }
            .getOrNull() ?: return null

        val daily = parsed.daily ?: run { Log.e(TAG, "no daily data"); return null }
        if (daily.time.size < 2) {
            Log.e(TAG, "daily size=${daily.time.size}")
            return null
        }

        return runCatching {
            WeatherForecast(
                today = daily.toEntry(0),
                tomorrow = daily.toEntry(1)
            )
        }.onFailure { Log.e(TAG, "build forecast error: ${it.message}") }.getOrNull()
    }
}

// ---------------------------------------------------------------------------
// Open-Meteo API JSON 结构
// ---------------------------------------------------------------------------

@Serializable
data class OpenMeteoResponse(
    val daily: OpenMeteoDaily? = null
)

@Serializable
data class OpenMeteoDaily(
    val time: List<String> = emptyList(),                                   // ["2026-11-23", "2026-11-24"]
    @SerialName("weather_code") val weatherCode: List<Int?> = emptyList(),  // WMO 代码 [0, 2]
    @SerialName("temperature_2m_max") val tempMax: List<Double?> = emptyList(),
    @SerialName("temperature_2m_min") val tempMin: List<Double?> = emptyList()
)

/** 取第 index 天的数据，转换成 UI 层纯数据。 */
private fun OpenMeteoDaily.toEntry(index: Int) = WeatherEntry(
    date = time[index],
    weather = wmoDescription(weatherCode.getOrNull(index)),
    tempMax = tempMax.getOrNull(index) ?: 0.0,
    tempMin = tempMin.getOrNull(index) ?: tempMax.getOrNull(index) ?: 0.0
)

/** WMO 天气代码 → 中文描述。参考 https://open-meteo.com/en/docs */
private fun wmoDescription(code: Int?): String = when (code) {
    0 -> "晴"
    1 -> "晴间多云"
    2 -> "多云"
    3 -> "阴"
    45, 48 -> "雾"
    51, 53, 55 -> "毛毛雨"
    56, 57 -> "冻雨"
    61, 63, 65 -> "雨"
    66, 67 -> "冻雨"
    71, 73, 75, 77 -> "雪"
    80, 81, 82 -> "阵雨"
    85, 86 -> "阵雪"
    95, 96, 99 -> "雷阵雨"
    else -> ""
}

/** UI 层消费的简化天气条目（Open-Meteo 版，weather 是中文描述）。 */
data class WeatherEntry(
    val date: String,
    val weather: String,       // "晴" / "多云" / "雨"
    val tempMax: Double,
    val tempMin: Double
)

/** 今天 + 明天两天预报。 */
data class WeatherForecast(
    val today: WeatherEntry,
    val tomorrow: WeatherEntry
)

/** 中文天气描述 → emoji + 简短标签的映射。 */
enum class WeatherIcon(val label: String, val emoji: String) {
    SUNNY("晴", "☀"),
    PARTLY_CLOUDY("多云", "⛅"),
    CLOUDY("阴", "☁"),
    FOG("雾", "🌫"),
    DRIZZLE("毛毛雨", "🌦"),
    RAIN("雨", "🌧"),
    SNOW("雪", "❄"),
    SHOWER("阵雨", "🌦"),
    THUNDERSTORM("雷阵雨", "⛈"),
    UNKNOWN("未知", "❓");

    companion object {
        /** 按中文天气描述匹配。 */
        fun fromDesc(desc: String): WeatherIcon = when {
            desc.contains("雷阵雨") || desc.contains("雷雨") -> THUNDERSTORM
            desc.contains("阵雨") || desc.contains("阵雪") -> SHOWER
            desc.contains("雪") -> SNOW
            desc.contains("雾") || desc.contains("霾") -> FOG
            desc.contains("毛毛雨") -> DRIZZLE
            desc.contains("小雨") || desc.contains("中雨") || desc.contains("大雨") ||
                desc.contains("暴雨") || desc.contains("雨") -> RAIN
            desc.contains("多云") -> PARTLY_CLOUDY
            desc.contains("阴") -> CLOUDY
            desc.contains("晴") -> SUNNY
            else -> UNKNOWN
        }
    }
}
