package com.example.cyclingcheckin

import android.app.Application

class CyclingCheckInApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
