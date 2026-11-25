package com.example.cyclingcheckin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cyclingcheckin.navigation.RideLogApp
import com.example.cyclingcheckin.ui.theme.RideLogTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CyclingCheckInApp).container
        // 首帧前读取已保存设置，避免先按系统主题渲染造成闪烁。
        val initialSettings = runBlocking { container.settingsStore.settings.first() }
        setContent {
            val settings by container.settingsStore.settings.collectAsStateWithLifecycle(
                initialValue = initialSettings
            )
            RideLogTheme(themeMode = settings.themeMode) {
                RideLogApp(container = container)
            }
        }
    }
}
