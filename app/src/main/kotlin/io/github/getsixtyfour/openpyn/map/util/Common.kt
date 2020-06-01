package io.github.getsixtyfour.openpyn.map.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Location
import android.util.Xml
import android.view.animation.AccelerateInterpolator
import androidx.annotation.RawRes
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.abdeveloper.library.MultiSelectModelExtra
import com.abdeveloper.library.MultiSelectable
import com.androidmapsextensions.lazy.LazyMarker
import com.androidmapsextensions.lazy.LazyMarker.OnLevelChangeCallback
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.mayurrokade.minibar.UserMessage
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.map.createJsonArray
import io.github.getsixtyfour.openpyn.utils.NetworkInfo
import io.github.getsixtyfour.openpyn.security.SecurityCypher
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.DefaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.readText
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import org.jetbrains.anko.alert
import org.jetbrains.anko.dip
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.padding
import org.jetbrains.anko.textColor
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException
import java.io.StringWriter
import java.util.HashSet
import java.util.Locale

private val logger = KotlinLogging.logger {}

operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

// categories
const val CATEGORIES: String = "categories"
const val NAME: String = "name"
const val DEDICATED: String = "Dedicated IP"
const val DOUBLE: String = "Double VPN"
const val OBFUSCATED: String = "Obfuscated Servers"
const val ONION: String = "Onion Over VPN"
const val P2P: String = "P2P"
const val STANDARD: String = "Standard VPN servers"
// country
const val COUNTRY: String = "country"
// flag
const val FLAG: String = "flag"
// location
const val LOCATION: String = "location"
const val LAT: String = "lat"
const val LONG: String = "long"
// server
const val SERVER: String = "https://api.nordvpn.com/server"
// extended
const val CITY: String = "city"
const val IP: String = "ip"
const val THREAT: String = "threat"
// time
const val TIME_MILLIS: Long = 600
const val DURATION: Long = 7000

@Suppress("unused")
@WorkerThread
fun generateXML() {
    // An extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
    SERVER.httpGet().responseJson { _, _, result ->
        when (result) {
            is Result.Failure -> {
                val e = result.getException()
                logger.error(e) { "" }
            }
            is Result.Success -> {
                val mutableMap = mutableMapOf<String, String>()
                val jsonArray = result.get().array()
                for (res in jsonArray) {
                    if (res.getString(COUNTRY) !in mutableMap) {
                        mutableMap[res.getString(COUNTRY)] = res.getString(FLAG).toLowerCase(Locale.ROOT)
                    }
                }
                val sortedMap = mutableMap.toSortedMap(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                val serializer = Xml.newSerializer()
                val writer = StringWriter()
                try {
                    serializer.setOutput(writer)
                    serializer.startDocument("UTF-8", true)
                    serializer.startTag("", "head")
                    serializer.startTag("", "string-array")
                    serializer.attribute("", "name", "pref_country_entries")
                    for ((key, _) in sortedMap) {
                        serializer.startTag("", "item")
                        serializer.text(key)
                        serializer.endTag("", "item")
                    }
                    serializer.endTag("", "string-array")
                    serializer.startTag("", "string-array")
                    serializer.attribute("", "name", "pref_country_values")
                    for ((_, value) in sortedMap) {
                        serializer.startTag("", "item")
                        serializer.text(value)
                        serializer.endTag("", "item")
                    }
                    serializer.endTag("", "string-array")
                    serializer.endTag("", "head")
                    serializer.endDocument()
                    println("$writer")
                } catch (e: FileNotFoundException) {
                    logger.error(e) { "" }
                } catch (e: IOException) {
                    logger.error(e) { "" }
                } catch (e: JSONException) {
                    logger.error(e) { "" }
                }
            }
        }
    }
}

@Suppress("MagicNumber")
fun createJson2(type: String, content: JSONObject): JSONObject? {
    var flag = ""
    var country = ""
    var city = ""
    var lat = 0.0
    var lon = 0.0
    var ip = ""
    var threat: JSONObject? = null

    when (type) {
        "ipapi" -> {
            flag = content.optString("countryCode")
            country = content.optString("country")
            city = content.optString("city")
            lat = content.optDouble("lat", 0.0)
            lon = content.optDouble("lon", 0.0)
            ip = content.optString("query")
        }
        "ipdata" -> {
            flag = content.optString("country_code")
            country = content.optString("country_name")
            city = content.optString("city")
            lat = content.optDouble("latitude", 0.0)
            lon = content.optDouble("longitude", 0.0)
            ip = content.optString("ip")

            threat = content.optJSONObject("threat")
        }
        "ipinfo" -> {
            flag = content.optString("country")
            city = content.optString("city")
            lat = java.lang.Double.valueOf(content.optString("loc").split(",")[0])
            lon = java.lang.Double.valueOf(content.optString("loc").split(",")[1])
            ip = content.optString("ip")
        }
        "ipstack" -> {
            flag = content.optString("country_code")
            country = content.optString("country_name")
            city = content.optString("city")
            lat = content.optDouble("latitude", 0.0)
            lon = content.optDouble("longitude", 0.0)
            ip = content.optString("ip")
        }
    }

    if (flag.isNotEmpty() && city.isNotEmpty() && lat != 0.0 && lon != 0.0 && ip.isNotEmpty()) {
        return JSONObject().apply {
            put(FLAG, flag.toLowerCase(Locale.ROOT))
            put(COUNTRY, country)
            put(CITY, city)
            put(LAT, lat)
            put(LONG, lon)
            put(IP, ip)

            putOpt(THREAT, threat)
        }
    }

    return null
}

@WorkerThread
@Suppress("MagicNumber")
fun createJson(): JSONArray? {
    fun populateFeatures(res: JSONObject, features: JSONObject) {
        val categories = res.getJSONArray(CATEGORIES)

        for (category in categories) {
            val name = category.getString(NAME)

            when {
                name.equals(DEDICATED, true) -> features.put(DEDICATED, true)
                name.equals(DOUBLE, true) -> features.put(DOUBLE, true)
                name.equals(OBFUSCATED, true) -> features.put(OBFUSCATED, true)
                name.equals(ONION, true) -> features.put(ONION, true)
                name.equals(P2P, true) -> features.put(P2P, true)
                name.equals(STANDARD, true) -> features.put(STANDARD, true)
                else -> {
                    logger.error(Exception()) { name }
                }
            }
        }
    }

    val timeout = 10000
    val timeoutRead = 10000
    // An extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
    val (_, _, result) = SERVER.httpGet().timeout(timeout).timeoutRead(timeoutRead).responseJson()
    when (result) {
        is Result.Failure -> {
            val e = result.getException()
            logger.error(e) { "" }
        }
        is Result.Success -> {
            val jsonObj = JSONObject()
            val content = result.get().array() //JSONArray
            for (res in content) {
                val location = res.getJSONObject(LOCATION)
                var json: JSONObject? = jsonObj.optJSONObject("$location")

                if (json == null) {
                    json = JSONObject().apply {
                        put(FLAG, res.getString(FLAG).toLowerCase(Locale.ROOT))
                        put(COUNTRY, res.getString(COUNTRY))
                        put(LOCATION, res.getJSONObject(LOCATION))
                    }
                    val features = JSONObject().apply {
                        put(DEDICATED, false)
                        put(DOUBLE, false)
                        put(OBFUSCATED, false)
                        put(ONION, false)
                        put(P2P, false)
                        put(STANDARD, false)
                    }
                    populateFeatures(res, features)

                    json.put(CATEGORIES, features)

                    jsonObj.put("$location", json)
                } else {
                    val features = json.getJSONObject(CATEGORIES)
                    populateFeatures(res, features)
                }
            }

            try {
                val jsonArray = JSONArray()
                val keys = jsonObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = jsonObj.getJSONObject(key)
                    val jsonArr = JSONArray()
                    val features = value.getJSONObject(CATEGORIES)

                    if (features.getBoolean(DEDICATED)) {
                        jsonArr.put(JSONObject().put(NAME, DEDICATED))
                    }

                    if (features.getBoolean(DOUBLE)) {
                        jsonArr.put(JSONObject().put(NAME, DOUBLE))
                    }

                    if (features.getBoolean(OBFUSCATED)) {
                        jsonArr.put(JSONObject().put(NAME, OBFUSCATED))
                    }

                    if (features.getBoolean(ONION)) {
                        jsonArr.put(JSONObject().put(NAME, ONION))
                    }

                    if (features.getBoolean(P2P)) {
                        jsonArr.put(JSONObject().put(NAME, P2P))
                    }

                    if (features.getBoolean(STANDARD)) {
                        jsonArr.put(JSONObject().put(NAME, STANDARD))
                    }
                    val json = JSONObject().apply {
                        put(FLAG, value.getString(FLAG))
                        put(COUNTRY, value.getString(COUNTRY))
                        put(LOCATION, value.getJSONObject(LOCATION))
                        put(CATEGORIES, jsonArr)
                    }

                    jsonArray.put(json)
                }

                if (jsonArray.length() > 0) {
                    return jsonArray
                }
            } catch (e: JSONException) {
                logger.error(e) { "" }
            }
        }
    }

    return null
}

fun sortJsonArray(jsonArray: JSONArray): JSONArray? {
    val array = ArrayList<JSONObject>()
    for (res in jsonArray) {
        array.add(res)
    }

    array.sortWith(
        compareBy({ it.getString(COUNTRY) }, { it.getJSONObject(LOCATION).getDouble(LAT) }, { it.getJSONObject(LOCATION).getDouble(LONG) })
    )
    val result = JSONArray()
    for (res in array) {
        result.put(res)
    }
    return result
}

fun stringifyJsonArray(jsonArray: JSONArray): String? = sortJsonArray(jsonArray)?.run { toString(2) }

@Suppress("ComplexMethod", "MagicNumber")
fun showThreats(activity: Activity, jsonObj: JSONObject) {
    val threats: JSONObject? = jsonObj.optJSONObject(THREAT)
    logger.info { threats.toString() }

    if (threats != null) {
        val tor = threats.getBoolean("is_tor")
        val proxy = threats.getBoolean("is_proxy")
        val anonymous = threats.getBoolean("is_anonymous")
        val attacker = threats.getBoolean("is_known_attacker")
        val abuser = threats.getBoolean("is_known_abuser")
        val threat = threats.getBoolean("is_threat")
        val bogon = threats.getBoolean("is_bogon")
        val color1 = ContextCompat.getColor(activity, R.color.colorConnect)
        val color2 = ContextCompat.getColor(activity, R.color.colorDisconnect)
        val fl = 22f
        val weight = 1.0f
        val alert = activity.alert {
            customView = ctx.verticalLayout {
                linearLayout {
                    textView {
                        text = ctx.getString(R.string.is_tor)
                        textSize = fl
                        gravity = android.view.Gravity.START
                    }.lparams(
                        width = org.jetbrains.anko.wrapContent, height = org.jetbrains.anko.wrapContent, weight = weight
                    ) {}
                    textView {
                        text = if (tor) "YES" else "NO"
                        textColor = if (tor) color2 else color1
                        textSize = fl
                        gravity = android.view.Gravity.END
                    }.lparams(
                        width = org.jetbrains.anko.wrapContent, height = org.jetbrains.anko.wrapContent, weight = weight
                    ) {}
                }
                linearLayout {
                    textView {
                        text = ctx.getString(R.string.is_proxy)
                        textSize = fl
                        gravity = android.view.Gravity.START
                    }.lparams(
                        width = org.jetbrains.anko.wrapContent, height = org.jetbrains.anko.wrapContent, weight = weight
                    ) {}
                    textView {
                        text = if (proxy) "YES" else "NO"
                        textColor = if (proxy) color2 else color1
                        textSize = fl
                        gravity = android.view.Gravity.END
                    }.lparams(
                        width = org.jetbrains.anko.wrapContent, height = org.jetbrains.anko.wrapContent, weight = weight
                    ) {}
                }
                linearLayout {
                    textView {
                        text = ctx.getString(R.string.is_anonymous)
                        textSize = fl
                        gravity = android.view.Gravity.START
                    }.lparams(
                        width = org.jetbrains.anko.wrapContent, height = org.jetbrains.anko.wrapContent, weight = weight
                    ) {}
                    textView {
                        text = if (anonymous) "YES" else "NO"
                        textColor = if (anonymous) color2 else color1
                        textSize = fl
                        gravity = android.view.Gravity.END
                    }.lparams(
                        width = org.jetbrains.anko.wrapContent, height = org.jetbrains.anko.wrapContent, weight = weight
                    ) {}
                }
                linearLayout {
                    textView {
                        text = ctx.getString(R.string.is_known_attacker)
                        textSize = fl
                        gravity = android.view.Gravity.START
                    }.lparams(
                        width = org.jetbrains.anko.wrapContent, height = org.jetbrains.anko.wrapContent, weight = weight
                    ) {}
                    textView {
                        text = if (attacker) "YES" else "NO"
                        textColor = if (attacker) color2 else color1
                        textSize = fl
                        gravity = android.view.Gravity.END
                    }.lparams(
                        width = org.jetbrains.anko.wrapContent, height = org.jetbrains.anko.wrapContent, weight = weight
                    ) {}
                }
                linearLayout {
                    textView {
                        text = ctx.getString(R.string.is_known_abuser)
                        textSize = fl
                        gravity = android.view.Gravity.START
                    }.lparams(
                        width = org.jetbrains.anko.wrapContent, height = org.jetbrains.anko.wrapContent, weight = weight
                    ) {}
                    textView {
                        text = if (abuser) "YES" else "NO"
                        textColor = if (abuser) color2 else color1
                        textSize = fl
                        gravity = android.view.Gravity.END
                    }.lparams(
                        width = org.jetbrains.anko.wrapContent, height = org.jetbrains.anko.wrapContent, weight = weight
                    ) {}
                }
                linearLayout {
                    textView {
                        text = ctx.getString(R.string.is_threat)
                        textSize = fl
                        gravity = android.view.Gravity.START
                    }.lparams(
                        width = org.jetbrains.anko.wrapContent, height = org.jetbrains.anko.wrapContent, weight = weight
                    ) {}
                    textView {
                        text = if (threat) "YES" else "NO"
                        textColor = if (threat) color2 else color1
                        textSize = fl
                        gravity = android.view.Gravity.END
                    }.lparams(
                        width = org.jetbrains.anko.wrapContent, height = org.jetbrains.anko.wrapContent, weight = weight
                    ) {}
                }
                linearLayout {
                    textView {
                        text = ctx.getString(R.string.is_bogon)
                        textSize = fl
                        gravity = android.view.Gravity.START
                    }.lparams(
                        width = org.jetbrains.anko.wrapContent, height = org.jetbrains.anko.wrapContent, weight = weight
                    ) {}
                    textView {
                        text = if (bogon) "YES" else "NO"
                        textColor = if (bogon) color2 else color1
                        textSize = fl
                        gravity = android.view.Gravity.END
                    }.lparams(
                        width = org.jetbrains.anko.wrapContent, height = org.jetbrains.anko.wrapContent, weight = weight
                    ) {}
                }
                gravity = android.view.Gravity.CENTER
                padding = dip(40)
            }
        }
        alert.show()
    }
}

fun createMarkers(
    context: Context,
    jsonArray: JSONArray,
    countries: List<MultiSelectable>,
    map: GoogleMap,
    favorites: ArrayList<LazyMarker>?,
    callback: OnLevelChangeCallback
): Pair<HashSet<CharSequence>, HashMap<LatLng, LazyMarker>> {
    fun parseToUnicode(countries: List<MultiSelectable>, input: CharSequence): CharSequence {
        // Replace the aliases by their unicode
        return (countries.find { (it as? MultiSelectModelExtra)?.tag == input } as? MultiSelectModelExtra)?.unicode ?: input
    }
    /*fun netflix(flag: CharSequence?): Boolean = when (flag) {
        "us" -> true
        "ca" -> true
        "nl" -> true
        "jp" -> true
        "gb" -> true
        "gr" -> true
        "mx" -> true
        else -> false
    }
    val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val netflix = preferences.getBoolean("pref_netflix", false)
    val dedicated = preferences.getBoolean("pref_dedicated", false)
    val double = preferences.getBoolean("pref_double", false)
    val obfuscated = preferences.getBoolean("pref_anti_ddos", false)
    val onion = preferences.getBoolean("pref_tor", false)
    val p2p = preferences.getBoolean("pref_p2p", false)*/
    val length = jsonArray.length()
    val flags = HashSet<CharSequence>(length)
    val markers = HashMap<LatLng, LazyMarker>(length)
    val iconDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.map1)

    for (res in jsonArray) {
        val flag = res.getString(FLAG)
        /*var pass = when {
            netflix -> netflix(flag)
            dedicated -> false
            double -> false
            obfuscated -> false
            onion -> false
            p2p -> false
            else -> true
        }
        if (!pass && !netflix) {
            val categories = res.getJSONArray(CATEGORIES)

            loop@ for (category in categories) {
                val name = category.getString(NAME)
                pass = when {
                    dedicated and (name == DEDICATED) -> true
                    double and (name == DOUBLE) -> true
                    obfuscated and (name == OBFUSCATED) -> true
                    onion and (name == ONION) -> true
                    p2p and (name == P2P) -> true
                    else -> false
                }

                if (pass) {
                    break@loop
                }
            }
        }

        if (!pass) {
            continue
        }*/
        val country = res.getString(COUNTRY)
        val emoji = parseToUnicode(countries, flag)
        val location = res.getJSONObject(LOCATION)
        val latLng = LatLng(location.getDouble(LAT), location.getDouble(LONG))
        val title = context.getString(R.string.title_marker, emoji, country)
        val options = MarkerOptions().apply {
            flat(true)
            position(latLng)
            title(title)
            visible(false)
            icon(iconDescriptor)
        }

        flags.add(flag)
        val marker = LazyMarker(map, options, flag)
        favorites?.let {
            val index = it.indexOf(marker)
            if (index >= 0) {
                marker.setLevel(it[index].level, callback)
            }
        }
        markers[latLng] = marker
    }

    return Pair(flags, markers)
}

fun createUserMessage(context: Context, jsonObj: JSONObject): UserMessage.Builder {
    /*val country = jsonObj.getString(COUNTRY)
    val lat = jsonObj.getDouble(LAT)
    val lon = jsonObj.getDouble(LONG)*/
    val city = jsonObj.getString(CITY)
    val flag = jsonObj.getString(FLAG).toUpperCase(Locale.ROOT)
    val ip = jsonObj.getString(IP)
    val message = context.getString(R.string.vpn_msg_connected, city, flag, ip)
    return UserMessage.Builder().apply {
        with(context.applicationContext)
        setBackgroundColor(R.color.accent_material_indigo_200).setTextColor(android.R.color.white)
        setMessage(message).setDuration(DURATION).setShowInterpolator(
            AccelerateInterpolator()
        )
        setDismissInterpolator(AccelerateInterpolator())
    }
}

fun jsonArray(context: Context, @RawRes id: Int, ext: String): JSONArray {
    fun logDifference(set: Set<CharSequence>, string: CharSequence) {
        set.forEach {
            logger.error(Exception()) { "$string $it" }
        }
    }

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

@Suppress("TooGenericExceptionCaught", "ReplaceNotNullAssertionWithElvisReturn")
@SuppressLint("WrongThread")
@WorkerThread
suspend fun createGeoJson(context: Context): JSONObject? {
    if (NetworkInfo.getInstance().isOnline()) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val geo = preferences.getBoolean("pref_geo", true)
        val api = preferences.getString("pref_geo_client", "ipapi")!!
        val ipdata = preferences.getString("pref_api_ipdata", "")!!
        val ipinfo = preferences.getString("pref_api_ipinfo", "")!!
        val ipstack = preferences.getString("pref_api_ipstack", "")!!
        val fields = "fields=country_name,country_code,city,latitude,longitude,ip"
        var server = "http://ip-api.com/json/?fields=8403" // http://ip-api.com/json/?fields=country,countryCode,city,lat,lon,query
        val token: String

        if (geo) {
            when (api) {
                "ipdata" -> {
                    token = SecurityCypher.getInstance(context).decryptString(ipdata).toString()
                    server = "https://api.ipdata.co?api-key=$token&$fields"
                    // server = "https://api.ipdata.co?api-key=$token&$fields,threat"
                }
                "ipinfo" -> {
                    token = SecurityCypher.getInstance(context).decryptString(ipinfo).toString()
                    server = when {
                        token.isNotEmpty() -> "https://ipinfo.io/geo?token=$token"
                        else -> "https://ipinfo.io/geo"
                    }
                }
                "ipstack" -> {
                    token = SecurityCypher.getInstance(context).decryptString(ipstack).toString()
                    server = "http://api.ipstack.com/check?access_key=$token&$fields"
                }
            }
            var jsonObject: JSONObject? = null
            val client = HttpClient(Android) {
                install(DefaultRequest) {
                    headers.append("Accept", "application/json")
                }
            }

            try {
                withTimeout(TIME_MILLIS) {
                    val response = client.get<HttpStatement>(server).execute()
                    val json = response.readText()
                    jsonObject = JSONObject(json)
                }
            } catch (e: TimeoutCancellationException) {
                logger.warn(e) { "" }
            } catch (e: Throwable) {
                logger.error(e) { "" }
            } finally {
                logger.info { "$api: ${jsonObject.toString()}" }
            }

            client.close()
            return jsonObject?.run { createJson2(api, this) }
        }
    }

    return null
}

@Suppress("ComplexMethod")
fun getCurrentPosition(
    context: Context,
    flags: HashSet<CharSequence>,
    jsonObj: JSONObject?,
    jsonArr: JSONArray? = null
): LatLng {
    @Suppress("MagicNumber")
    fun getDefaultLatLng(): LatLng = LatLng(51.514125, -0.093689)

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

    fun latLng(jsonArr: JSONArray?, flags: HashSet<CharSequence>, flag: CharSequence, lat: Double, lon: Double): LatLng = when {
        jsonArr != null && flags.contains(flag) -> getLatLng(flag, LatLng(lat, lon), jsonArr)
        else -> LatLng(lat, lon)
    }

    var latLng = getDefaultLatLng()

    when {
        jsonObj != null -> {
            val lat = jsonObj.getDouble(LAT)
            val lon = jsonObj.getDouble(LONG)
            val flag = jsonObj.getString(FLAG)
            logger.info { "is in: $flag" }
            latLng = latLng(jsonArr, flags, flag, lat, lon)
        }
    }
    logger.info { "$latLng" }
    return latLng
}
