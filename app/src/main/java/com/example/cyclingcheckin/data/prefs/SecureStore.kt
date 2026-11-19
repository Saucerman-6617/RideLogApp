package com.example.cyclingcheckin.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * WebDAV 应用密码等敏感信息使用 EncryptedSharedPreferences 存储，绝不明文落盘。
 */
class SecureStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var webDavPassword: String
        get() = runCatching { prefs.getString(KEY_WEBDAV_PASSWORD, "").orEmpty() }.getOrDefault("")
        set(value) {
            runCatching { prefs.edit().putString(KEY_WEBDAV_PASSWORD, value).apply() }
        }

    private companion object {
        const val PREFS_NAME = "ride_log_secure"
        const val KEY_WEBDAV_PASSWORD = "webdav_password"
    }
}
