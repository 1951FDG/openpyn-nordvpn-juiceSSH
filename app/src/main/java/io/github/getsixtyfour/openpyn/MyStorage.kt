package io.github.sdsstudios.nvidiagpumonitor

import android.content.Context
import androidx.preference.PreferenceManager
import com.squareup.moshi.JsonAdapter

abstract class MyStorage(val key: String) {
    private val jsonAdapter by lazy { jsonAdapter() }

    @Suppress("WeakerAccess")
    fun storeFavorites(context: Context, arrayList: ArrayList<Any>) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val string = jsonAdapter.toJson(arrayList)
        preferences.edit().apply {
            putString(key, string)
            apply()
        }
    }

    @Suppress("WeakerAccess")
    fun loadFavorites(context: Context): ArrayList<Any> {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val string = preferences.getString(key, null)
        if (string != null) {
            val items = jsonAdapter.fromJson(string)
            if (items != null) {
                return ArrayList(items)
            }
        }

        return ArrayList()
    }

    abstract fun jsonAdapter(): JsonAdapter<List<Any>>

    fun addFavorite(context: Context, value: Any) {
        val arrayList = loadFavorites(context)
        val index = arrayList.indexOf(value)
        if (index == -1) {
            arrayList.add(value)
        } else {
            //arrayList[index] = value
        }
        storeFavorites(context, arrayList)
    }

    fun removeFavorite(context: Context, value: Any) {
        val arrayList = loadFavorites(context)
        if (arrayList.isNotEmpty()) {
            arrayList.remove(value)
            storeFavorites(context, arrayList)
        }
    }
}
