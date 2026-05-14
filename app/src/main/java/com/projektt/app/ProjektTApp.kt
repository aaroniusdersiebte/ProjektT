package com.projektt.app

import android.app.Application
import com.projektt.app.widget.WidgetUpdateWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ProjektTApp : Application() {
    override fun onCreate() {
        super.onCreate()
        WidgetUpdateWorker.schedule(this)
    }
}
