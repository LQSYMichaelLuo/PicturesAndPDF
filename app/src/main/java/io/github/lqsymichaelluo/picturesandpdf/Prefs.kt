package io.github.lqsymichaelluo.picturesandpdf

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object Prefs {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        AppFlags.debuggable.value = sp.getBoolean("debug", false)
    }

    fun setDebug(value: Boolean) {
        sp.edit { putBoolean("debug", value) }
    }

}