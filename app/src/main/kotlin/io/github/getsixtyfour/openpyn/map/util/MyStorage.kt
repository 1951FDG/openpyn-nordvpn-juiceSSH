package io.github.getsixtyfour.openpyn.map.util

import android.content.Context
import androidx.preference.PreferenceManager
import com.squareup.moshi.JsonAdapter

abstract class MyStorage<T>(val key: String) {
    abstract val jsonAdapter: JsonAdapter<List<T>>

    @Suppress("WeakerAccess")
    fun storeFavorites(context: Context, arrayList: ArrayList<T>) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val string = jsonAdapter.toJson(arrayList)
        preferences.edit().putString(key, string).apply()
    }

    @Suppress("WeakerAccess")
    fun loadFavorites(context: Context): ArrayList<T> {
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

    fun addFavorite(context: Context, value: T) {
        val arrayList = loadFavorites(context)
        val index = arrayList.indexOf(value)
        if (index == -1) {
            arrayList.add(value)
        }
        storeFavorites(context, arrayList)
    }

    fun removeFavorite(context: Context, value: T) {
        val arrayList = loadFavorites(context)
        if (arrayList.isNotEmpty()) {
            arrayList.remove(value)
            storeFavorites(context, arrayList)
        }
    }
}
