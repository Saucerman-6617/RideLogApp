package com.example.cyclingcheckin.data.remote

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

private const val TAG = "LocationService"

/**
 * 位置获取：优先从系统缓存读；缓存为空时主动请求一次短时位置更新。
 * 这样即使刚装完 App、第一次授权也能拿到一个粗略位置。
 * - 只需要 COARSE_LOCATION（网络位置），精度足以查天气。
 * - 超时 10 秒内拿不到位置 → 返回默认坐标（北京天安门），天气至少能显示。
 */
class LocationService(private val context: Context) {

    private val lm: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    /** 超时兜底坐标：北京天安门。保证天气至少能展示。 */
    companion object {
        private val FALLBACK = Pair(39.905023, 116.724502)
        private const val REQUEST_TIMEOUT_MS = 10_000L
    }

    /** 定位结果：坐标 + 是否使用了兜底坐标。 */
    data class LocationSnapshot(
        val latitude: Double,
        val longitude: Double,
        val isFallback: Boolean
    )

    suspend fun getLastKnown(): LocationSnapshot {
        val fallback = LocationSnapshot(FALLBACK.first, FALLBACK.second, isFallback = true)
        val manager = lm ?: run { Log.e(TAG, "LocationManager null"); return fallback }
        if (!hasPermission()) { Log.e(TAG, "no permission"); return fallback }

        val cached = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (cached != null && cached.latitude != 0.0) {
            Log.d(TAG, "cache hit: ${cached.latitude},${cached.longitude}")
            return LocationSnapshot(cached.latitude, cached.longitude, isFallback = false)
        }
        Log.d(TAG, "cache miss, requesting fresh location...")

        val fresh = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
            requestSingleLocationUpdate(manager)
        }
        if (fresh != null) {
            Log.d(TAG, "fresh: ${fresh.latitude},${fresh.longitude}")
            return LocationSnapshot(fresh.latitude, fresh.longitude, isFallback = false)
        }
        Log.w(TAG, "fresh timeout, using fallback")
        return fallback
    }

    /** 请求一次位置更新，拿到后立刻移除监听器。 */
    private suspend fun requestSingleLocationUpdate(manager: LocationManager): Location? =
        suspendCancellableCoroutine { cont ->
            val provider = when {
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                    LocationManager.NETWORK_PROVIDER
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                    LocationManager.GPS_PROVIDER
                else -> {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }
            }

            val listener = object : LocationListener {
                override fun onLocationChanged(loc: Location) {
                    if (cont.isActive) {
                        runCatching { manager.removeUpdates(this) }
                        cont.resume(loc)
                    }
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }

            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    manager.requestLocationUpdates(
                        provider, 0L, 0f, listener, Looper.getMainLooper()
                    )
                } else {
                    @Suppress("DEPRECATION")
                    manager.requestLocationUpdates(provider, 0L, 0f, listener)
                }
            }.onFailure {
                if (cont.isActive) cont.resume(null)
            }

            cont.invokeOnCancellation {
                runCatching { manager.removeUpdates(listener) }
            }
        }

    private fun hasPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 逆地理编码：坐标 → 位置名称（优先区县，其次城市、省份）。
     * 系统 Geocoder 不可用或查询失败时返回 null。
     */
    suspend fun getPlaceName(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) {
                Log.w(TAG, "geocoder not present")
                return@withContext null
            }
            runCatching {
                @Suppress("DEPRECATION")
                val address = Geocoder(context, Locale.getDefault())
                    .getFromLocation(latitude, longitude, 1)
                    ?.firstOrNull() ?: return@withContext null
                sequenceOf(address.subLocality, address.locality, address.subAdminArea, address.adminArea)
                    .firstOrNull { !it.isNullOrBlank() }
            }.onFailure { Log.e(TAG, "geocode error: ${it.message}") }
                .getOrNull()
        }
}
