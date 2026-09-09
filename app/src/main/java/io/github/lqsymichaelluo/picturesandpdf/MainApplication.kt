package io.github.lqsymichaelluo.picturesandpdf

import android.app.Application

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }
}