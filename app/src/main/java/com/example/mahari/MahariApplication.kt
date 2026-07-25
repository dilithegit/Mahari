package com.example.mahari

import android.app.Application
import com.example.mahari.di.AppContainer

class MahariApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
