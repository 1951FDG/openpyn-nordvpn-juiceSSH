package io.github.getsixtyfour.openpyn.map.util

import com.androidmapsextensions.lazy.LazyMarker
import com.google.android.gms.maps.model.LatLng
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types

class LazyMarkerStorage(key: String) : MyStorage<LazyMarker>(key) {
    override val jsonAdapter: JsonAdapter<List<LazyMarker>> by lazy(::adapter)

    private fun adapter(): JsonAdapter<List<LazyMarker>> {
        val moshi = Moshi.Builder().add(object {
            @ToJson
            @Suppress("unused")
            fun toJson(value: LatLng): Map<String, Double> = mapOf(LAT to value.latitude, LONG to value.longitude)

            @FromJson
            @Suppress("unused")
            fun fromJson(value: Map<String, Double>): LatLng = LatLng(value.getValue(LAT), value.getValue(LONG))
        }).build()
        val listType = Types.newParameterizedType(List::class.java, LazyMarker::class.java)
        return moshi.adapter(listType)
    }
}
