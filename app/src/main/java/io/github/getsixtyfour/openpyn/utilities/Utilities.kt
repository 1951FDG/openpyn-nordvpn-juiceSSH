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
import androidx.core.app.ActivityCompat
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
        ActivityCompat.startActivity(activity, intent, null)
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
    return arrayListOf(
        MultiSelectModel(0, SpannableString(array[0]), R.drawable.ic_albania_40dp),
        MultiSelectModel(1, SpannableString(array[1]), R.drawable.ic_argentina_40dp),
        MultiSelectModel(2, SpannableString(array[2]), R.drawable.ic_australia_40dp),
        MultiSelectModel(3, SpannableString(array[3]), R.drawable.ic_austria_40dp),
        MultiSelectModel(4, SpannableString(array[4]), R.drawable.ic_azerbaijan_40dp),
        MultiSelectModel(5, SpannableString(array[5]), R.drawable.ic_belgium_40dp),
        MultiSelectModel(6, SpannableString(array[6]), R.drawable.ic_bosnia_and_herzegovina_40dp),
        MultiSelectModel(7, SpannableString(array[7]), R.drawable.ic_brazil_40dp),
        MultiSelectModel(8, SpannableString(array[8]), R.drawable.ic_bulgaria_40dp),
        MultiSelectModel(9, SpannableString(array[9]), R.drawable.ic_canada_40dp),
        MultiSelectModel(10, SpannableString(array[10]), R.drawable.ic_chile_40dp),
        MultiSelectModel(11, SpannableString(array[11]), R.drawable.ic_costa_rica_40dp),
        MultiSelectModel(12, SpannableString(array[12]), R.drawable.ic_croatia_40dp),
        MultiSelectModel(13, SpannableString(array[13]), R.drawable.ic_cyprus_40dp),
        MultiSelectModel(14, SpannableString(array[14]), R.drawable.ic_czech_republic_40dp),
        MultiSelectModel(15, SpannableString(array[15]), R.drawable.ic_denmark_40dp),
        MultiSelectModel(16, SpannableString(array[16]), R.drawable.ic_egypt_40dp),
        MultiSelectModel(17, SpannableString(array[17]), R.drawable.ic_estonia_40dp),
        MultiSelectModel(18, SpannableString(array[18]), R.drawable.ic_finland_40dp),
        MultiSelectModel(19, SpannableString(array[19]), R.drawable.ic_france_40dp),
        MultiSelectModel(20, SpannableString(array[20]), R.drawable.ic_georgia_40dp),
        MultiSelectModel(21, SpannableString(array[21]), R.drawable.ic_germany_40dp),
        MultiSelectModel(22, SpannableString(array[22]), R.drawable.ic_greece_40dp),
        MultiSelectModel(23, SpannableString(array[23]), R.drawable.ic_hong_kong_40dp),
        MultiSelectModel(24, SpannableString(array[24]), R.drawable.ic_hungary_40dp),
        MultiSelectModel(25, SpannableString(array[25]), R.drawable.ic_iceland_40dp),
        MultiSelectModel(26, SpannableString(array[26]), R.drawable.ic_india_40dp),
        MultiSelectModel(27, SpannableString(array[27]), R.drawable.ic_indonesia_40dp),
        MultiSelectModel(28, SpannableString(array[28]), R.drawable.ic_ireland_40dp),
        MultiSelectModel(29, SpannableString(array[29]), R.drawable.ic_israel_40dp),
        MultiSelectModel(30, SpannableString(array[30]), R.drawable.ic_italy_40dp),
        MultiSelectModel(31, SpannableString(array[31]), R.drawable.ic_japan_40dp),
        MultiSelectModel(32, SpannableString(array[32]), R.drawable.ic_latvia_40dp),
        MultiSelectModel(33, SpannableString(array[33]), R.drawable.ic_luxembourg_40dp),
        MultiSelectModel(34, SpannableString(array[34]), R.drawable.ic_republic_of_macedonia_40dp),
        MultiSelectModel(35, SpannableString(array[35]), R.drawable.ic_malaysia_40dp),
        MultiSelectModel(36, SpannableString(array[36]), R.drawable.ic_mexico_40dp),
        MultiSelectModel(37, SpannableString(array[37]), R.drawable.ic_moldova_40dp),
        MultiSelectModel(38, SpannableString(array[38]), R.drawable.ic_netherlands_40dp),
        MultiSelectModel(39, SpannableString(array[39]), R.drawable.ic_new_zealand_40dp),
        MultiSelectModel(40, SpannableString(array[40]), R.drawable.ic_norway_40dp),
        MultiSelectModel(41, SpannableString(array[41]), R.drawable.ic_poland_40dp),
        MultiSelectModel(42, SpannableString(array[42]), R.drawable.ic_portugal_40dp),
        MultiSelectModel(43, SpannableString(array[43]), R.drawable.ic_romania_40dp),
        MultiSelectModel(44, SpannableString(array[44]), R.drawable.ic_russia_40dp),
        MultiSelectModel(45, SpannableString(array[45]), R.drawable.ic_serbia_40dp),
        MultiSelectModel(46, SpannableString(array[46]), R.drawable.ic_singapore_40dp),
        MultiSelectModel(47, SpannableString(array[47]), R.drawable.ic_slovakia_40dp),
        MultiSelectModel(48, SpannableString(array[48]), R.drawable.ic_slovenia_40dp),
        MultiSelectModel(49, SpannableString(array[49]), R.drawable.ic_south_africa_40dp),
        MultiSelectModel(50, SpannableString(array[50]), R.drawable.ic_south_korea_40dp),
        MultiSelectModel(51, SpannableString(array[51]), R.drawable.ic_spain_40dp),
        MultiSelectModel(52, SpannableString(array[52]), R.drawable.ic_sweden_40dp),
        MultiSelectModel(53, SpannableString(array[53]), R.drawable.ic_switzerland_40dp),
        MultiSelectModel(54, SpannableString(array[54]), R.drawable.ic_taiwan_40dp),
        MultiSelectModel(55, SpannableString(array[55]), R.drawable.ic_thailand_40dp),
        MultiSelectModel(56, SpannableString(array[56]), R.drawable.ic_turkey_40dp),
        MultiSelectModel(57, SpannableString(array[57]), R.drawable.ic_ukraine_40dp),
        MultiSelectModel(58, SpannableString(array[58]), R.drawable.ic_united_arab_emirates_40dp),
        MultiSelectModel(59, SpannableString(array[59]), R.drawable.ic_united_kingdom_40dp),
        MultiSelectModel(60, SpannableString(array[60]), R.drawable.ic_united_states_of_america_40dp),
        MultiSelectModel(61, SpannableString(array[61]), R.drawable.ic_vietnam_40dp)
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
