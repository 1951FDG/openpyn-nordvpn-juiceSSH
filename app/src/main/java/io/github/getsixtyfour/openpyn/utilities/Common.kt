package io.github.getsixtyfour.openpyn.utilities

import android.util.Xml
import androidx.annotation.WorkerThread
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpGet
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

@WorkerThread
fun generateXML() {
    // An extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
    val server = "https://api.nordvpn.com/server"
    server.httpGet().responseJson { _, _, result ->
        when (result) {
            is Result.Failure -> {
                logException(result.getException())
            }
            is Result.Success -> {
                val mutableMap = mutableMapOf<String, String>()
                val jsonArray = result.get().array()
                for (res in jsonArray) {
                    if (res.getString("country") !in mutableMap) {
                        mutableMap[res.getString("country")] = res.getString("flag").toLowerCase(Locale.ROOT)
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

@WorkerThread
@Suppress("MagicNumber")
fun createJson2(value: String?, token: String?): JSONObject? {
    var server = "http://ip-api.com/json"

    when {
        value.equals("ipdata", true) -> {
            if (token != null && token.isNotEmpty()) {
                server = "https://api.ipdata.co?api-key=$token"
            }
        }
        value.equals("ipinfo", true) -> {
            server = when {
                token != null && token.isNotEmpty() -> "https://ipinfo.io/json?token=$token"
                else -> "https://ipinfo.io/json"
            }
        }
        value.equals("ipstack", true) -> {
            if (token != null && token.isNotEmpty()) {
                server = "http://api.ipstack.com/check?access_key=$token"
            }
        }
    }
    val timeout = 500
    val timeoutRead = 500
    // An extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
    val (_, _, result) = server.httpGet().timeout(timeout).timeoutRead(timeoutRead).responseJson()
    when (result) {
        is Result.Failure -> {
            Log.error(result.getException().toString())
        }
        is Result.Success -> {
            val content = result.get().obj()
            //Log.debug(content.toString())
            var flag = ""
            var country = ""
            var city = ""
            var lat = 0.0
            var lon = 0.0
            var ip = ""
            var threat: JSONObject? = null

            when {
                server.startsWith("http://ip-api.com", true) -> {
                    flag = content.optString("countryCode")
                    country = content.optString("country")
                    city = content.optString("city")
                    lat = content.optDouble("lat", 0.0)
                    lon = content.optDouble("lon", 0.0)
                    ip = content.optString("query")
                }
                server.startsWith("https://api.ipdata.co", true) -> {
                    flag = content.optString("country_code")
                    country = content.optString("country_server")
                    city = content.optString("city")
                    lat = content.optDouble("latitude", 0.0)
                    lon = content.optDouble("longitude", 0.0)
                    ip = content.optString("ip")

                    threat = content.optJSONObject("threat")
                }
                server.startsWith("https://ipinfo.io", true) -> {
                    flag = content.optString("country")
                    city = content.optString("city")
                    lat = java.lang.Double.valueOf(content.optString("loc").split(",")[0])
                    lon = java.lang.Double.valueOf(content.optString("loc").split(",")[1])
                    ip = content.optString("ip")
                }
                server.startsWith("http://api.ipstack.com", true) -> {
                    flag = content.optString("country_code")
                    country = content.optString("country_name")
                    city = content.optString("city")
                    lat = content.optDouble("latitude", 0.0)
                    lon = content.optDouble("longitude", 0.0)
                    ip = content.optString("ip")
                }
            }

            if (flag.isNotEmpty() && city.isNotEmpty() && lat != 0.0 && lon != 0.0 && ip.isNotEmpty()) {
                Log.debug("is in $flag")
                return JSONObject().apply {
                    put("flag", flag.toLowerCase(Locale.ROOT))
                    put("country", country)
                    put("city", city)
                    put("latitude", lat)
                    put("longitude", lon)
                    put("ip", ip)

                    putOpt("threat", threat)
                }
            }
        }
    }

    return null
}

@WorkerThread
@Suppress("MagicNumber")
fun createJson(): JSONArray? {
    val server = "https://api.nordvpn.com/server"
    val timeout = 1000
    val timeoutRead = 1000
    // An extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
    val (_, _, result) = server.httpGet().timeout(timeout).timeoutRead(timeoutRead).responseJson()
    when (result) {
        is Result.Failure -> {
            logException(result.getException())
        }
        is Result.Success -> {
            val jsonObj = JSONObject()
            val content = result.get().array() //JSONArray
            for (res in content) {
                val location = res.getJSONObject("location")
                var json1: JSONObject? = jsonObj.optJSONObject(location.toString())
                if (json1 == null) {
                    json1 = JSONObject().apply {
                        put("flag", res.getString("flag").toLowerCase(Locale.ROOT))
                        put("country", res.getString("country"))
                        put("location", res.getJSONObject("location"))
                    }
                    val features = JSONObject().apply {
                        put("p2p", false)
                        put("dedicated", false)
                        put("double_vpn", false)
                        put("tor_over_vpn", false)
                        put("anti_ddos", false)
                        put("standard", false)
                    }
                    val categories = res.getJSONArray("categories")

                    for (category in categories) {
                        val name = category.getString("name")

                        when {
                            name.equals("P2P", true) -> features.put("p2p", true)
                            name.equals("Dedicated IP", true) -> features.put("dedicated", true)
                            name.equals("Double VPN", true) -> features.put("double_vpn", true)
                            name.equals("Onion Over VPN", true) -> features.put("tor_over_vpn", true)
                            name.startsWith("Obfuscated", true) -> features.put("anti_ddos", true)
                            name.startsWith("Standard VPN", true) -> features.put("standard", true)
                            else -> {
                                logException(Exception(name))
                                Log.error(name)
                            }
                        }
                    }

                    json1.put("features", features)

                    jsonObj.put(location.toString(), json1)
                } else {
                    val features = json1.getJSONObject("features")
                    val categories = res.getJSONArray("categories")

                    for (category in categories) {
                        val name = category.getString("name")

                        when {
                            name.equals("P2P", true) -> features.put("p2p", true)
                            name.equals("Dedicated IP", true) -> features.put("dedicated", true)
                            name.equals("Double VPN", true) -> features.put("double_vpn", true)
                            name.equals("Onion Over VPN", true) -> features.put("tor_over_vpn", true)
                            name.startsWith("Obfuscated", true) -> features.put("anti_ddos", true)
                            name.startsWith("Standard VPN", true) -> features.put("standard", true)
                            else -> {
                                logException(Exception(name))
                                Log.error(name)
                            }
                        }
                    }
                }
            }
            val jsonArray = JSONArray()

            try {
                val keys = jsonObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = jsonObj.getJSONObject(key)
                    val jsonArr = JSONArray()
                    val features = value.getJSONObject("features")

                    if (features.getBoolean("anti_ddos")) {
                        jsonArr.put(JSONObject().put("name", "Obfuscated Servers"))
                    }

                    if (features.getBoolean("dedicated")) {
                        jsonArr.put(JSONObject().put("name", "Dedicated IP"))
                    }

                    if (features.getBoolean("double_vpn")) {
                        jsonArr.put(JSONObject().put("name", "Double VPN"))
                    }

                    if (features.getBoolean("tor_over_vpn")) {
                        jsonArr.put(JSONObject().put("name", "Onion Over VPN"))
                    }

                    if (features.getBoolean("p2p")) {
                        jsonArr.put(JSONObject().put("name", "P2P"))
                    }

                    if (features.getBoolean("standard")) {
                        jsonArr.put(JSONObject().put("name", "Standard VPN servers"))
                    }
                    val json1 = JSONObject().apply {
                        put("flag", value.getString("flag"))
                        put("country", value.getString("country"))
                        put("location", value.getJSONObject("location"))
                        put("categories", jsonArr)
                    }

                    jsonArray.put(json1)
                }
            } catch (e: JSONException) {
                logException(e)
            }

            if (jsonArray.length() > 0) {
                return jsonArray
            }
        }
    }

    return null
}
