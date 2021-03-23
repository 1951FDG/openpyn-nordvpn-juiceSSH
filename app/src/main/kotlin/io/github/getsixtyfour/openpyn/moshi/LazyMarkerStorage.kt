package io.github.getsixtyfour.openpyn.moshi

import com.google.android.gms.maps.model.LatLng
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import io.github.getsixtyfour.openpyn.model.LazyMarker

class LazyMarkerStorage(key: String) : AbstractStorage<LazyMarker>(key) {

    override val jsonAdapter: JsonAdapter<List<LazyMarker>> by lazy(::adapter)

    private fun adapter(): JsonAdapter<List<LazyMarker>> {
        val moshi = Moshi.Builder().add(object {
            @Suppress("unused")
            @ToJson
            fun toJson(value: LatLng): Map<String, Double> = mapOf(LAT to value.latitude, LONG to value.longitude)

            @Suppress("unused")
            @FromJson
            fun fromJson(value: Map<String, Double>): LatLng = LatLng(value.getValue(LAT), value.getValue(LONG))
        }).build()
        val listType = Types.newParameterizedType(List::class.java, LazyMarker::class.java)
        return moshi.adapter(listType)
    }

    companion object {

        const val LAT: String = "lat"
        const val LONG: String = "long"
    }
}
