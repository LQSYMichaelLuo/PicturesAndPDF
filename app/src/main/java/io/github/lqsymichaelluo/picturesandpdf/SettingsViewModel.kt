package io.github.lqsymichaelluo.picturesandpdf

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel


class SettingsViewModel(app: Application) : AndroidViewModel(application = app) {

    val debuggable get() = AppFlags.debuggable
    val context = app
    val uploadedPDFList = AppFlags.uploadedPDFList
    fun toggleDebug() {
        val new = !AppFlags.debuggable.value
        AppFlags.debuggable.value = new
        Prefs.setDebug(new)
    }

    fun print(text: String){
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    fun printLong(text: String){
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }
    fun clearFileCache(context: Context) {
        val cacheDir = context.cacheDir
        val keepNames = uploadedPDFList.toSet()
        cacheDir.listFiles()
            ?.filter { file ->
                file.isFile && file.name.endsWith(".pdf")
            }
            ?.forEach { file ->
                if (file.name !in keepNames) {
                    file.delete()
                    //print(file.name)
                }
            }
    }
}