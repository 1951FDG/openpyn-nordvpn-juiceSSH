package io.github.getsixtyfour.functions

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.location.Location
import android.os.Build
import android.util.Xml
import androidx.annotation.AnyRes
import androidx.annotation.RawRes
import androidx.annotation.WorkerThread
import androidx.preference.PreferenceManager
import com.abdeveloper.library.MultiSelectModelExtra
import com.abdeveloper.library.MultiSelectable
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.github.getsixtyfour.openpyn.BuildConfig
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.model.LazyMarker
import io.github.getsixtyfour.openpyn.model.LazyMarker.OnLevelChangeCallback
import io.github.getsixtyfour.openpyn.security.SecurityCypher
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.DefaultRequest
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import mu.KotlinLogging
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
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

@Suppress("unused")
@WorkerThread
suspend fun generateXML() {
    HttpClient(Android) {
        install(DefaultRequest) {
            headers.append("Accept", "application/json")
        }
    }.use { client: HttpClient ->
        val response = client.get<HttpResponse>(SERVER)
        val json = response.readText()
        val jsonArray = JSONArray(json)
        val mutableMap = mutableMapOf<String, String>()
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
            serializer.startTag("", "string-array")
            serializer.attribute("", "name", "pref_country_entries")
            sortedMap.forEach { (key, _) ->
                serializer.startTag("", "item")
                serializer.text(key)
                serializer.endTag("", "item")
            }
            serializer.endTag("", "string-array")
            serializer.startTag("", "string-array")
            serializer.attribute("", "name", "pref_country_values")
            serializer.attribute("", "translatable", "false")
            sortedMap.forEach { (_, value) ->
                serializer.startTag("", "item")
                serializer.text(value)
                serializer.endTag("", "item")
            }
            serializer.endTag("", "string-array")
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

@Suppress("ComplexCondition")
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
suspend fun createJson(): JSONArray? {
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

    HttpClient(Android) {
        install(DefaultRequest) {
            headers.append("Accept", "application/json")
        }
    }.use { client: HttpClient ->
        val response = client.get<HttpResponse>(SERVER)
        val json = response.readText()
        val values = JSONArray(json)
        val jsonObject = JSONObject()
        val jsonArray = JSONArray()

        for (value in values) {
            val location = value.getJSONObject(LOCATION)
            var jsonObj = jsonObject.optJSONObject("$location")

            if (jsonObj == null) {
                val features = JSONObject().apply {
                    put(DEDICATED, false)
                    put(DOUBLE, false)
                    put(OBFUSCATED, false)
                    put(ONION, false)
                    put(P2P, false)
                    put(STANDARD, false)
                }
                populateFeatures(value, features)

                jsonObj = JSONObject().apply {
                    put(FLAG, value.getString(FLAG).toLowerCase(Locale.ROOT))
                    put(COUNTRY, value.getString(COUNTRY))
                    put(LOCATION, value.getJSONObject(LOCATION))
                    put(CATEGORIES, features)
                }

                jsonObject.put("$location", jsonObj)
            } else {
                val features = jsonObj.getJSONObject(CATEGORIES)
                populateFeatures(value, features)
            }
        }

        jsonObject.keys().forEach { key ->
            val jsonObj = jsonObject.getJSONObject(key)
            val jsonArr = JSONArray()
            val features = jsonObj.getJSONObject(CATEGORIES)

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

            JSONObject().apply {
                put(FLAG, jsonObj.getString(FLAG))
                put(COUNTRY, jsonObj.getString(COUNTRY))
                put(LOCATION, jsonObj.getJSONObject(LOCATION))
                put(CATEGORIES, jsonArr)
            }.also {
                jsonArray.put(it)
            }
        }

        if (jsonArray.length() > 0) {
            return@createJson jsonArray
        }
    }

    return null
}

fun sortJsonArray(jsonArray: JSONArray): JSONArray {
    val list = ArrayList<JSONObject>()
    for (res in jsonArray) {
        list.add(res)
    }

    list.sortWith(
        compareBy({ it.getString(COUNTRY) }, { it.getJSONObject(LOCATION).getDouble(LAT) }, { it.getJSONObject(LOCATION).getDouble(LONG) })
    )
    val result = JSONArray()
    for (res in list) {
        result.put(res)
    }
    return result
}

fun stringifyJsonArray(jsonArray: JSONArray): String? = when {
    BuildConfig.DEBUG -> sortJsonArray(jsonArray).toString(2)
    else -> "$jsonArray"
}

fun writeJsonArray(context: Context, @AnyRes resId: Int, text: String): File {
    val child = context.resources.getResourceEntryName(resId) + ".json"
    val file = File(context.getExternalFilesDir(null), child)
    file.writeText("$text\n")
    return file
}

@Suppress("UNUSED_VARIABLE")
fun showThreats(jsonObj: JSONObject) {
    val threats: JSONObject? = jsonObj.optJSONObject(THREAT)
    logger.info(threats::toString)

    if (threats != null) {
        val tor = threats.getBoolean("is_tor")
        val proxy = threats.getBoolean("is_proxy")
        val anonymous = threats.getBoolean("is_anonymous")
        val attacker = threats.getBoolean("is_known_attacker")
        val abuser = threats.getBoolean("is_known_abuser")
        val threat = threats.getBoolean("is_threat")
        val bogon = threats.getBoolean("is_bogon")
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
    fun parseToUnicode(countries: List<MultiSelectable>, input: CharSequence): CharSequence = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
            (countries.find { (it as? MultiSelectModelExtra)?.tag == input } as? MultiSelectModelExtra)?.unicode ?: input
        }
        else -> ""
    }

    val size = jsonArray.length()
    val flags = HashSet<CharSequence>(size)
    val markers = HashMap<LatLng, LazyMarker>(size)
    for (res in jsonArray) {
        val flag = res.getString(FLAG)
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
        }

        flags.add(flag)
        val marker = LazyMarker(map, options, flag)
        if (favorites == null) {
            marker.setLevel(0, callback)
        } else {
            val index = favorites.indexOf(marker)
            if (index >= 0) {
                marker.setLevel(favorites[index].level, callback)
            } else {
                marker.setLevel(0, callback)
            }
        }
        markers[latLng] = marker
    }

    return Pair(flags, markers)
}

fun createUserMessage(context: Context, jsonObj: JSONObject): CharSequence {
    /*val country = jsonObj.getString(COUNTRY)
    val lat = jsonObj.getDouble(LAT)
    val lon = jsonObj.getDouble(LONG)*/
    val city = jsonObj.getString(CITY)
    val flag = jsonObj.getString(FLAG).toUpperCase(Locale.ROOT)
    val ip = jsonObj.getString(IP)
    return context.getString(R.string.vpn_msg_connected, city, flag, ip)
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

@WorkerThread
suspend fun createGeoJson(context: Context): JSONObject? {
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
                /*server = "https://api.ipdata.co?api-key=$token&$fields,threat"*/
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
        HttpClient(Android) {
            install(DefaultRequest) {
                headers.append("Accept", "application/json")
            }
        }.use { client: HttpClient ->
            val response = client.get<HttpResponse>(server)
            val json = response.readText()
            val jsonObject = JSONObject(json).also { logger.info { "$api: $it" } }
            return@createGeoJson createJson2(api, jsonObject)
        }
    }

    return null
}

fun getCurrentPosition(flags: HashSet<CharSequence>, jsonObj: JSONObject?, jsonArr: JSONArray? = null): LatLng {
    @Suppress("MagicNumber")
    fun getDefaultLatLng(): LatLng = LatLng(51.514125, -0.093689)

    fun getLatLng(flag: CharSequence, latLng: LatLng, jsonArr: JSONArray): LatLng {
        val latLngList = ArrayList<LatLng>()
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
                val index = results.indexOfFirst { it == result }
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

internal fun copyToExternalFilesDir(context: Context, list: List<Pair<Int, String>>) {
    list.forEach { (id, ext) ->
        try {
            val file = File(context.getExternalFilesDir(null), context.resources.getResourceEntryName(id) + ext)
            if (!file.exists()) {
                copyRawResourceToFile(context, id, file)
            }
        } catch (e: NotFoundException) {
            logger.error(e) { "" }
        } catch (e: FileNotFoundException) {
            logger.error(e) { "" }
        } catch (e: IOException) {
            logger.error(e) { "" }
        }
    }
}

internal fun copyRawResourceToFile(context: Context, @RawRes id: Int, file: File) {
    context.resources.openRawResource(id).use { input ->
        file.outputStream().buffered().use { input.copyTo(it) }
    }
}

internal fun createJsonArray(context: Context, @RawRes id: Int, ext: String): JSONArray {
    try {
        val file = File(context.getExternalFilesDir(null), context.resources.getResourceEntryName(id) + ext)
        if (!file.exists()) {
            copyRawResourceToFile(context, id, file)
        }
        val json = file.bufferedReader().use(BufferedReader::readText)
        return JSONArray(json)
    } catch (e: NotFoundException) {
        logger.error(e) { "" }
    } catch (e: FileNotFoundException) {
        logger.error(e) { "" }
    } catch (e: IOException) {
        logger.error(e) { "" }
    } catch (e: JSONException) {
        logger.error(e) { "" }
    }
    return JSONArray()
}
