package com.example.metroinfo

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class MetroApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        WorkManager.initialize(this, workManagerConfiguration)
    }
} 