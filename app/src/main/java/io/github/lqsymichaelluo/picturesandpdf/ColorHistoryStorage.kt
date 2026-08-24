package io.github.lqsymichaelluo.picturesandpdf

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ColorHistoryStorage {

    private const val NAME = "color_history"
    private const val KEY = "list"

    private fun getSharedPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun load(context: Context): List<ColorHistory> {
        val json = getSharedPreferences(context).getString(KEY, null)
        if (json.isNullOrEmpty()) return emptyList()

        val type = object : TypeToken<List<ColorHistory>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun save(context: Context, list: List<ColorHistory>) {
        val json = Gson().toJson(list)
        getSharedPreferences(context).edit { putString(KEY, json) }
    }
}
