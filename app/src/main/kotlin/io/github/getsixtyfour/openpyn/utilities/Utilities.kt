package io.github.getsixtyfour.openpyn.utilities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources.NotFoundException
import android.location.Location
import android.net.Uri
import android.text.SpannableString
import androidx.annotation.RawRes
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.abdeveloper.library.MultiSelectModelExtra
import com.abdeveloper.library.MultiSelectable
import com.ariascode.networkutility.NetworkInfo
import com.crashlytics.android.Crashlytics
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.maps.model.LatLng
import com.jayrave.moshi.pristineModels.PristineModelsJsonAdapterFactory
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi.Builder
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import de.jupf.staticlog.Log
import io.fabric.sdk.android.Fabric
import io.github.getsixtyfour.openpyn.R
import de.blinkt.openvpn.security.SecurityManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.DefaultRequest
import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

private fun copyToExternalFilesDir(context: Context, list: List<Pair<Int, String>>) {
    for ((id, ext) in list) {
        try {
            val file = File(context.getExternalFilesDir(null), context.resources.getResourceEntryName(id) + ext)
            if (!file.exists()) {
                copyRawResourceToFile(context, id, file)
            }
        } catch (e: NotFoundException) {
            logException(e)
        } catch (e: FileNotFoundException) {
            logException(e)
        } catch (e: IOException) {
            logException(e)
        }
    }
}

private const val JUICE_SSH_PACKAGE_NAME = "com.sonelli.juicessh"

fun isJuiceSSHInstalled(context: Context): Boolean = try {
    context.packageManager.getPackageInfo(JUICE_SSH_PACKAGE_NAME, 0)
    true
} catch (e: NameNotFoundException) {
    false
}

fun juiceSSHInstall(activity: Activity) {
    fun openURI(uri: Uri, packageName: String? = null) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage(packageName)
        ContextCompat.startActivity(activity, intent, null)
    }

    try {
        activity.packageManager.getPackageInfo(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE, 0)
        val uriBuilder =
            Uri.parse("https://play.google.com/store/apps/details").buildUpon().appendQueryParameter("id", JUICE_SSH_PACKAGE_NAME)
                .appendQueryParameter("launch", "true")
        try {
            openURI(uriBuilder.build(), GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE)
        } catch (e: ActivityNotFoundException) {
            openURI(uriBuilder.build())
        }
    } catch (e: NameNotFoundException) {
        val s = "juicessh-2-1-4"
        val uriString = "https://www.apkmirror.com/apk/sonelli-ltd/juicessh-ssh-client/$s-release/$s-android-apk-download/download/"
        openURI(Uri.parse(uriString))
    }
}

@Suppress("MagicNumber")
fun countryList(context: Context, @RawRes id: Int): List<MultiSelectable> {
    val json = context.resources.openRawResource(id).bufferedReader().use { it.readText() }
    val factory = PristineModelsJsonAdapterFactory.Builder().also { it.add(MultiSelectModelExtra::class.java, MultiSelectMapper()) }
    val moshi = Builder().add(factory.build()).add(object {
        @ToJson
        @Suppress("unused")
        fun toJson(value: CharSequence): String {
            return value.toString()
        }

        @FromJson
        @Suppress("unused")
        fun fromJson(value: String): CharSequence {
            return SpannableString(value)
        }
    }).build()
    val listType = Types.newParameterizedType(List::class.java, MultiSelectModelExtra::class.java)
    val adapter: JsonAdapter<List<MultiSelectModelExtra>> = moshi.adapter(listType)
    return adapter.nonNull().fromJson(json).orEmpty()
}

fun logDifference(set: Set<CharSequence>, string: CharSequence) {
    set.forEach {
        val message = "$string $it"
        logException(Exception(message))
        Log.error(message)
    }
}

fun jsonArray(context: Context, id: Int, ext: String): JSONArray {
    val jsonArray = createJsonArray(context, id, ext)
    val set1 = context.resources.getTextArray(R.array.pref_country_values).toHashSet()
    val set2 = hashSetOf<CharSequence>()

    for (res in jsonArray) {
        val flag = res.getString(FLAG)
        set2.add(flag)
    }
    // Log old countries, if any
    logDifference(set1.subtract(set2), "old")
    // Log new countries, if any
    logDifference(set2.subtract(set1), "new")
    return jsonArray
}

fun createJsonArray(context: Context, id: Int, ext: String): JSONArray {
    try {
        val file = File(context.getExternalFilesDir(null), context.resources.getResourceEntryName(id) + ext)
        if (!file.exists()) {
            copyRawResourceToFile(context, id, file)
        }
        val json = file.bufferedReader().use {
            it.readText()
        }
        return JSONArray(json)
    } catch (e: NotFoundException) {
        logException(e)
    } catch (e: FileNotFoundException) {
        logException(e)
    } catch (e: IOException) {
        logException(e)
    } catch (e: JSONException) {
        logException(e)
    }
    return JSONArray()
}

private fun copyRawResourceToFile(context: Context, id: Int, file: File) {
    context.resources.openRawResource(id).use { input ->
        file.outputStream().buffered().use { output ->
            input.copyTo(output)
        }
    }
}

@SuppressLint("WrongThread")
@WorkerThread
suspend fun createGeoJson(context: Context): JSONObject? {
    if (NetworkInfo.getInstance().isOnline()) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val geo = preferences.getBoolean("pref_geo", true)
        val api = preferences.getString("pref_geo_client", "")
        val ipdata = preferences.getString("pref_api_ipdata", "")
        val ipinfo = preferences.getString("pref_api_ipinfo", "")
        val ipstack = preferences.getString("pref_api_ipstack", "")

        if (geo) {
            var server = "http://ip-api.com/json/?fields=8403" // http://ip-api.com/json/?fields=country,countryCode,city,lat,lon,query
            var type = 0
            var token: String? = null
            when (api) {
                "ipdata" -> {
                    type = 1
                    token = ipdata
                }
                "ipinfo" -> {
                    type = 2
                    token = ipinfo
                }
                "ipstack" -> {
                    type = 3
                    token = ipstack
                }
            }
            if (token != null && token.isNotEmpty()) token = SecurityManager.getInstance(context).decryptString(token)
            val fields = "fields=country_name,country_code,city,latitude,longitude,ip"
            when (type) {
                1 -> {
                    if (token != null && token.isNotEmpty()) {
                        server = "https://api.ipdata.co?api-key=$token&$fields"
                    }
                }
                2 -> {
                    server = when {
                        token != null && token.isNotEmpty() -> "https://ipinfo.io/geo?token=$token"
                        else -> "https://ipinfo.io/geo"
                    }
                }
                3 -> {
                    if (token != null && token.isNotEmpty()) {
                        server = "http://api.ipstack.com/check?access_key=$token&$fields"
                    }
                }
            }
            var jsonObject: JSONObject? = null
            val client = HttpClient(Android) {
                install(DefaultRequest) {
                    headers.append("Accept", "application/json")
                }
            }

            try {
                withTimeout(600) {
                    val response = client.get<HttpResponse>(server)
                    val json = response.readText()
                    jsonObject = JSONObject(json)
                }
            } catch (exception: TimeoutCancellationException) {
                exception.message?.let { Log.info(it) }
            } catch (cause: Throwable) {
                cause.message?.let { Log.error(it) }
            } finally {
                Log.debug(jsonObject.toString())
            }

            client.close()

            return jsonObject?.let { createJson2(type, it) } ?: jsonObject
        }
    }

    return null
}

@Suppress("MagicNumber")
fun getDefaultLatLng(): LatLng {
    return LatLng(51.514125, -0.093689)
}

fun getLatLng(flag: CharSequence, latLng: LatLng, jsonArr: JSONArray): LatLng {
    val latLngList = arrayListOf<LatLng>()
    var match = false

    loop@ for (res in jsonArr) {
        val pass = flag == res.getString(FLAG)

        if (pass) {
            val location = res.getJSONObject(LOCATION)
            val element = LatLng(location.getDouble(LAT), location.getDouble(LONG))

            match = element == latLng
            when {
                match -> break@loop
                else -> latLngList.add(element)
            }
        }
    }

    if (latLngList.isNotEmpty() && !match) {
        val results = FloatArray(latLngList.size)

        latLngList.withIndex().forEach { (index, it) ->
            val result = FloatArray(1)
            Location.distanceBetween(latLng.latitude, latLng.longitude, it.latitude, it.longitude, result)
            results[index] = result[0]
        }
        val result = results.min()
        if (result != null) {
            val index = results.indexOf(result)
            return latLngList[index]
        }
    }

    return latLng
}

fun logException(throwable: Throwable) {
    if (Fabric.isInitialized()) {
        Crashlytics.logException(throwable)
    }
}
