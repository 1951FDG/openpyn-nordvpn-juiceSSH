package io.github.getsixtyfour.openpyn.utilities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources.NotFoundException
import android.location.Location
import android.net.Uri
import android.text.SpannableString
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import com.abdeveloper.library.MultiSelectModel
import com.abdeveloper.library.MultiSelectable
import com.ariascode.networkutility.NetworkInfo
import com.crashlytics.android.Crashlytics
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.maps.model.LatLng
import de.jupf.staticlog.Log
import io.fabric.sdk.android.Fabric
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.security.SecurityManager
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
        val uriBuilder = Uri.parse("https://play.google.com/store/apps/details")
            .buildUpon()
            .appendQueryParameter("id", JUICE_SSH_PACKAGE_NAME)
            .appendQueryParameter("launch", "true")
        try {
            openURI(uriBuilder.build(), GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE)
        } catch (e: ActivityNotFoundException) {
            openURI(uriBuilder.build())
        }
    } catch (e : NameNotFoundException) {
        val s = "juicessh-2-1-4"
        val uriString = "https://www.apkmirror.com/apk/sonelli-ltd/juicessh-ssh-client/$s-release/$s-android-apk-download/download/"
        openURI(Uri.parse(uriString))
    }
}

@Suppress("MagicNumber")
fun countryList(array: Array<CharSequence>): ArrayList<MultiSelectable> {
    // TODO change preferences to use tag instead of id, dynamic creation? with valid indexes
    var i = 0
    return arrayListOf(
        MultiSelectModelExtra(0, SpannableString(array[i++]), R.drawable.ic_albania_40dp, "al"),
        MultiSelectModelExtra(1, SpannableString(array[i++]), R.drawable.ic_argentina_40dp, "ar"),
        MultiSelectModelExtra(2, SpannableString(array[i++]), R.drawable.ic_australia_40dp, "au"),
        MultiSelectModelExtra(3, SpannableString(array[i++]), R.drawable.ic_austria_40dp, "at"),
        MultiSelectModelExtra(5, SpannableString(array[i++]), R.drawable.ic_belgium_40dp, "be"),
        MultiSelectModelExtra(6, SpannableString(array[i++]), R.drawable.ic_bosnia_and_herzegovina_40dp, "ba"),
        MultiSelectModelExtra(7, SpannableString(array[i++]), R.drawable.ic_brazil_40dp, "br"),
        MultiSelectModelExtra(8, SpannableString(array[i++]), R.drawable.ic_bulgaria_40dp, "bg"),
        MultiSelectModelExtra(9, SpannableString(array[i++]), R.drawable.ic_canada_40dp, "ca"),
        MultiSelectModelExtra(10, SpannableString(array[i++]), R.drawable.ic_chile_40dp, "cl"),
        MultiSelectModelExtra(11, SpannableString(array[i++]), R.drawable.ic_costa_rica_40dp, "cr"),
        MultiSelectModelExtra(12, SpannableString(array[i++]), R.drawable.ic_croatia_40dp, "hr"),
        MultiSelectModelExtra(13, SpannableString(array[i++]), R.drawable.ic_cyprus_40dp, "cy"),
        MultiSelectModelExtra(14, SpannableString(array[i++]), R.drawable.ic_czech_republic_40dp, "cz"),
        MultiSelectModelExtra(15, SpannableString(array[i++]), R.drawable.ic_denmark_40dp, "dk"),
        MultiSelectModelExtra(16, SpannableString(array[i++]), R.drawable.ic_egypt_40dp, "eg"),
        MultiSelectModelExtra(17, SpannableString(array[i++]), R.drawable.ic_estonia_40dp, "ee"),
        MultiSelectModelExtra(18, SpannableString(array[i++]), R.drawable.ic_finland_40dp, "fi"),
        MultiSelectModelExtra(19, SpannableString(array[i++]), R.drawable.ic_france_40dp, "fr"),
        MultiSelectModelExtra(20, SpannableString(array[i++]), R.drawable.ic_georgia_40dp, "ge"),
        MultiSelectModelExtra(21, SpannableString(array[i++]), R.drawable.ic_germany_40dp, "de"),
        MultiSelectModelExtra(22, SpannableString(array[i++]), R.drawable.ic_greece_40dp, "gr"),
        MultiSelectModelExtra(23, SpannableString(array[i++]), R.drawable.ic_hong_kong_40dp, "hk"),
        MultiSelectModelExtra(24, SpannableString(array[i++]), R.drawable.ic_hungary_40dp, "hu"),
        MultiSelectModelExtra(25, SpannableString(array[i++]), R.drawable.ic_iceland_40dp, "is"),
        MultiSelectModelExtra(26, SpannableString(array[i++]), R.drawable.ic_india_40dp, "in"),
        MultiSelectModelExtra(27, SpannableString(array[i++]), R.drawable.ic_indonesia_40dp, "id"),
        MultiSelectModelExtra(28, SpannableString(array[i++]), R.drawable.ic_ireland_40dp, "ie"),
        MultiSelectModelExtra(29, SpannableString(array[i++]), R.drawable.ic_israel_40dp, "il"),
        MultiSelectModelExtra(30, SpannableString(array[i++]), R.drawable.ic_italy_40dp, "it"),
        MultiSelectModelExtra(31, SpannableString(array[i++]), R.drawable.ic_japan_40dp, "jp"),
        MultiSelectModelExtra(32, SpannableString(array[i++]), R.drawable.ic_latvia_40dp, "lv"),
        MultiSelectModelExtra(33, SpannableString(array[i++]), R.drawable.ic_luxembourg_40dp, "lu"),
        MultiSelectModelExtra(35, SpannableString(array[i++]), R.drawable.ic_malaysia_40dp, "my"),
        MultiSelectModelExtra(36, SpannableString(array[i++]), R.drawable.ic_mexico_40dp, "mx"),
        MultiSelectModelExtra(37, SpannableString(array[i++]), R.drawable.ic_moldova_40dp, "md"),
        MultiSelectModelExtra(38, SpannableString(array[i++]), R.drawable.ic_netherlands_40dp, "nl"),
        MultiSelectModelExtra(39, SpannableString(array[i++]), R.drawable.ic_new_zealand_40dp, "nz"),
        MultiSelectModelExtra(34, SpannableString(array[i++]), R.drawable.ic_republic_of_macedonia_40dp, "mk"),
        MultiSelectModelExtra(40, SpannableString(array[i++]), R.drawable.ic_norway_40dp, "no"),
        MultiSelectModelExtra(41, SpannableString(array[i++]), R.drawable.ic_poland_40dp, "pl"),
        MultiSelectModelExtra(42, SpannableString(array[i++]), R.drawable.ic_portugal_40dp, "pt"),
        MultiSelectModelExtra(43, SpannableString(array[i++]), R.drawable.ic_romania_40dp, "ro"),
        MultiSelectModelExtra(45, SpannableString(array[i++]), R.drawable.ic_serbia_40dp, "rs"),
        MultiSelectModelExtra(46, SpannableString(array[i++]), R.drawable.ic_singapore_40dp, "sg"),
        MultiSelectModelExtra(47, SpannableString(array[i++]), R.drawable.ic_slovakia_40dp, "sk"),
        MultiSelectModelExtra(48, SpannableString(array[i++]), R.drawable.ic_slovenia_40dp, "si"),
        MultiSelectModelExtra(49, SpannableString(array[i++]), R.drawable.ic_south_africa_40dp, "za"),
        MultiSelectModelExtra(50, SpannableString(array[i++]), R.drawable.ic_south_korea_40dp, "kr"),
        MultiSelectModelExtra(51, SpannableString(array[i++]), R.drawable.ic_spain_40dp, "es"),
        MultiSelectModelExtra(52, SpannableString(array[i++]), R.drawable.ic_sweden_40dp, "se"),
        MultiSelectModelExtra(53, SpannableString(array[i++]), R.drawable.ic_switzerland_40dp, "ch"),
        MultiSelectModelExtra(54, SpannableString(array[i++]), R.drawable.ic_taiwan_40dp, "tw"),
        MultiSelectModelExtra(55, SpannableString(array[i++]), R.drawable.ic_thailand_40dp, "th"),
        MultiSelectModelExtra(56, SpannableString(array[i++]), R.drawable.ic_turkey_40dp, "tr"),
        MultiSelectModelExtra(57, SpannableString(array[i++]), R.drawable.ic_ukraine_40dp, "ua"),
        MultiSelectModelExtra(58, SpannableString(array[i++]), R.drawable.ic_united_arab_emirates_40dp, "ae"),
        MultiSelectModelExtra(59, SpannableString(array[i++]), R.drawable.ic_united_kingdom_40dp, "gb"),
        MultiSelectModelExtra(60, SpannableString(array[i++]), R.drawable.ic_united_states_of_america_40dp, "us"),
        MultiSelectModelExtra(61, SpannableString(array[i++]), R.drawable.ic_vietnam_40dp, "vn")
    )
}

fun jsonArray(context: Context, id: Int, ext: String): JSONArray? {
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
    return null
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
fun createGeoJson(preferences: SharedPreferences, securityManager: SecurityManager): JSONObject? {
    if (NetworkInfo.getInstance().isOnline()) {
        val geo = preferences.getBoolean("pref_geo", true)
        val api = preferences.getString("pref_geo_client", "")
        val ipdata = preferences.getString("pref_api_ipdata", "")
        val ipinfo = preferences.getString("pref_api_ipinfo", "")
        val ipstack = preferences.getString("pref_api_ipstack", "")

        if (geo) {
            var key: String? = null
            when (api) {
                "ipdata" -> {
                    key = ipdata
                }
                "ipinfo" -> {
                    key = ipinfo
                }
                "ipstack" -> {
                    key = ipstack
                }
            }

            if (key != null && key.isNotEmpty()) key = securityManager.decryptString(key)

            return createJson2(api, key)
        }
    }

    return null
}

@Suppress("MagicNumber")
fun getDefaultLatLng(): LatLng {
    return LatLng(51.514125, -0.093689)
}

fun getLatLng(flag: String, latLng: LatLng, jsonArr: JSONArray): LatLng {
    Log.info(latLng.toString())
    val latLngList = arrayListOf<LatLng>()
    var match = false

    loop@ for (res in jsonArr) {
        val pass = flag == res.getString("flag")

        if (pass) {
            val location = res.getJSONObject("location")
            val element = LatLng(location.getDouble("lat"), location.getDouble("long"))

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
