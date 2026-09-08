package io.github.lqsymichaelluo.picturesandpdf

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        PDFBoxResourceLoader.init(this)
    }
}