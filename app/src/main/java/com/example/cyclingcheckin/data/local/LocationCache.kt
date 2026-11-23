package com.example.cyclingcheckin.data.local

import android.content.Context

/**
 * 最近一次成功定位的本地缓存。
 * 下次获取不到定位时，用它查询天气，避免退回固定的默认地址。
 */
class LocationCache(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class CachedLocation(
        val latitude: Double,
        val longitude: Double,
        val name: String?
    )

    fun load(): CachedLocation? {
        val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull() ?: return null
        val lng = prefs.getString(KEY_LNG, null)?.toDoubleOrNull() ?: return null
        return CachedLocation(lat, lng, prefs.getString(KEY_NAME, null))
    }

    fun save(latitude: Double, longitude: Double, name: String?) {
        prefs.edit()
            .putString(KEY_LAT, latitude.toString())
            .putString(KEY_LNG, longitude.toString())
            .putString(KEY_NAME, name)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "location_cache"
        const val KEY_LAT = "last_lat"
        const val KEY_LNG = "last_lng"
        const val KEY_NAME = "last_name"
    }
}
