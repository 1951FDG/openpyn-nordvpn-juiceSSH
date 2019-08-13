package io.github.getsixtyfour.openpyn.utilities

import android.util.Xml
import androidx.annotation.WorkerThread
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import de.jupf.staticlog.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException
import java.io.StringWriter
import java.util.Locale

operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

const val CATEGORIES: String = "categories"
const val NAME: String = "name"
const val DEDICATED: String = "Dedicated IP"
const val DOUBLE: String = "Double VPN"
const val OBFUSCATED: String = "Obfuscated Servers"
const val ONION: String = "Onion Over VPN"
const val P2P: String = "P2P"
const val STANDARD: String = "Standard VPN servers"

const val COUNTRY: String = "country"

const val FLAG: String = "flag"

const val LOCATION: String = "location"
const val LAT: String = "lat"
const val LONG: String = "long"

const val SERVER: String = "https://api.nordvpn.com/server"

// extended

const val CITY: String = "city"
const val IP: String = "ip"
const val THREAT: String = "threat"

@Suppress("unused")
@WorkerThread
fun generateXML() {
    // An extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
    SERVER.httpGet().responseJson { _, _, result ->
        when (result) {
            is Result.Failure -> {
                logException(result.getException())
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
                    println(writer.toString())
                } catch (e: FileNotFoundException) {
                    logException(e)
                } catch (e: IOException) {
                    logException(e)
                } catch (e: JSONException) {
                    logException(e)
                }
            }
        }
    }
}

@Suppress("MagicNumber")
fun createJson2(type: Int, content: JSONObject): JSONObject? {
    var flag = ""
    var country = ""
    var city = ""
    var lat = 0.0
    var lon = 0.0
    var ip = ""
    var threat: JSONObject? = null

    when (type) {
        0 -> {
            flag = content.optString("countryCode")
            country = content.optString("country")
            city = content.optString("city")
            lat = content.optDouble("lat", 0.0)
            lon = content.optDouble("lon", 0.0)
            ip = content.optString("query")
        }
        1 -> {
            flag = content.optString("country_code")
            country = content.optString("country_name")
            city = content.optString("city")
            lat = content.optDouble("latitude", 0.0)
            lon = content.optDouble("longitude", 0.0)
            ip = content.optString("ip")

            threat = content.optJSONObject("threat")
        }
        2 -> {
            flag = content.optString("country")
            city = content.optString("city")
            lat = java.lang.Double.valueOf(content.optString("loc").split(",")[0])
            lon = java.lang.Double.valueOf(content.optString("loc").split(",")[1])
            ip = content.optString("ip")
        }
        3 -> {
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
                    logException(Exception(name))
                    Log.error(name)
                }
            }
        }
    }

    val timeout = 1000
    val timeoutRead = 1000
    // An extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
    val (_, _, result) = SERVER.httpGet().timeout(timeout).timeoutRead(timeoutRead).responseJson()
    when (result) {
        is Result.Failure -> {
            logException(result.getException())
        }
        is Result.Success -> {
            val jsonObj = JSONObject()
            val content = result.get().array() //JSONArray
            for (res in content) {
                val location = res.getJSONObject(LOCATION)
                var json: JSONObject? = jsonObj.optJSONObject(location.toString())

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

                    jsonObj.put(location.toString(), json)
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
                logException(e)
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

    array.sortWith(compareBy(
        {it.getString(COUNTRY)},
        {it.getJSONObject(LOCATION).getDouble(LAT)},
        {it.getJSONObject(LOCATION).getDouble(LONG)}
    ))

    val result = JSONArray()
    for (res in array) {
        result.put(res)
    }
    return result
}

fun stringifyJsonArray(jsonArray: JSONArray): String? {
    val jsonArr = sortJsonArray(jsonArray)

    return when {
        jsonArr != null -> jsonArr.toString(2)
        else -> null
    }
}
