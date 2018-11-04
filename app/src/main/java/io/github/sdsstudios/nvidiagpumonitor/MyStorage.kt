package io.github.sdsstudios.nvidiagpumonitor

import android.content.Context
import androidx.preference.PreferenceManager
import com.androidmapsextensions.lazy.LazyMarker
import com.google.android.gms.maps.model.LatLng
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types

class MyStorage(val key: String) {
    private val jsonAdapter by lazy { jsonAdapter() }

    fun storeFavorites(context: Context, arrayList: ArrayList<Any>) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val string = jsonAdapter.toJson(arrayList)
        preferences.edit().apply {
            putString(key, string)
            apply()
        }
    }

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

    private fun jsonAdapter(): JsonAdapter<List<Any>> {
        val moshi = Moshi.Builder()
                .add(object {
                    @ToJson
                    @Suppress("unused")
                    fun toJson(value: LatLng): Map<String, Double> {
                        return mapOf("lat" to value.latitude, "long" to value.longitude)
                    }

                    @FromJson
                    @Suppress("unused")
                    fun fromJson(value: Map<String, Double>): LatLng {
                        return LatLng(value["lat"]!!, value["long"]!!)
                    }
                })
                .build()
        val type = Types.newParameterizedType(List::class.java, LazyMarker::class.java)
        return moshi.adapter(type)
    }

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
