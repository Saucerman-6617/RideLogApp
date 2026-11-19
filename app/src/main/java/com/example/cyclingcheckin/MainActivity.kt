package com.example.cyclingcheckin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cyclingcheckin.domain.model.AppSettings
import com.example.cyclingcheckin.navigation.RideLogApp
import com.example.cyclingcheckin.ui.theme.RideLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CyclingCheckInApp).container
        setContent {
            val settings by container.settingsStore.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )
            RideLogTheme(themeMode = settings.themeMode) {
                RideLogApp(container = container)
            }
        }
    }
}
