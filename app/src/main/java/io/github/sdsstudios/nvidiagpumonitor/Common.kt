package io.github.sdsstudios.nvidiagpumonitor

import androidx.annotation.WorkerThread
import android.util.Xml
import com.crashlytics.android.Crashlytics
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import de.jupf.staticlog.Log
import java.io.FileNotFoundException
import java.io.IOException
import java.io.StringWriter

operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()


@WorkerThread
fun generateXML() {
    // An extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
    val server = "https://api.nordvpn.com/server"
    server.httpGet().responseJson { _, _, result ->
        when (result) {
            is Result.Failure -> {
                Crashlytics.logException(result.getException())
            }
            is Result.Success -> {
                val mutableMap = mutableMapOf<String, String>()
                val jsonArray = result.get().array()
                for (res in jsonArray) {
                    if (res.getString("country") !in mutableMap) {
                        mutableMap[res.getString("country")] = res.getString("domain").take(2)
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
                    Crashlytics.logException(e)
                } catch (e: IOException) {
                    Crashlytics.logException(e)
                } catch (e: JSONException) {
                    Crashlytics.logException(e)
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
            if (token != null && token.isNotEmpty())
            {
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
            if (token != null && token.isNotEmpty())
            {
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
            //Log.info(content.toString())

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
                return JSONObject().apply {
                    put("flag", flag)
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
fun createJson1(): JSONObject? {
    val json1 = JSONObject()

    for (server in listOf("https://api.ipdata.co", "http://ip-api.com/json")) {
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
            //Log.info(content.toString())

            var flag = content.optString("country_code")
            var country = content.optString("country_name")
            val city = content.optString("city")
            var lat = content.optDouble("latitude", 0.0)
            var lon = content.optDouble("longitude", 0.0)
            val emoji = content.optString("emoji_flag")
            var ip = content.optString("ip")
            val threat = content.optJSONObject("threat")

            if (flag.isEmpty()) flag = content.optString("countryCode")
            if (country.isEmpty()) country = content.optString("country")
            //if (city.isEmpty()) city = content.optString("city")
            if (lat == 0.0) lat = content.optDouble("lat", 0.0)
            if (lon == 0.0) lon = content.optDouble("lon", 0.0)
            //if (emoji.isEmpty()) emoji = content.optString("emoji_flag")
            if (ip.isEmpty()) ip = content.optString("query")
            //if (threat == null) threat = content.optJSONObject("threat")

            if (json1.optString("flag").isEmpty()) json1.put("flag", flag)
            if (json1.optString("country").isEmpty()) json1.put("country", country)
            if (json1.optString("city").isEmpty()) json1.put("city", city)
            if (json1.optDouble("latitude", 0.0) == 0.0) json1.put("latitude", lat)
            if (json1.optDouble("longitude", 0.0) == 0.0) json1.put("longitude", lon)
            if (json1.optString("emoji_flag").isEmpty()) json1.put("emoji_flag", emoji)
            if (json1.optString("ip").isEmpty()) json1.put("ip", ip)
            if (json1.optJSONObject("threat") == null) json1.putOpt("threat", threat)

            //break
            }
        }
    }

    return if (json1.length() > 0) json1 else null
}

@WorkerThread
@Suppress("MagicNumber")
fun createJson(): JSONArray? {
    var server = "https://api.nordvpn.com/server"
    val timeout = 1000
    val timeoutRead = 1000
    // An extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
    val (_, _, result) = server.httpGet().timeout(timeout).timeoutRead(timeoutRead).responseJson()
        when (result) {
            is Result.Failure -> {
                Crashlytics.logException(result.getException())
            }
            is Result.Success -> {
                val jsonObj = JSONObject()
                val content = result.get().array() //JSONArray
                for (res in content) {
                    val location = res.getJSONObject("location")

                    var json1: JSONObject? = jsonObj.optJSONObject(location.toString())
                    if (json1 == null) {
                        json1 = JSONObject().apply {
                            put("flag", res.getString("flag"))
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
                                name.equals("Obfuscated Servers", true) -> features.put("anti_ddos", true)
                                name.equals("Standard VPN servers", true) -> features.put("standard", true)
                                else -> {
                                    Crashlytics.logException(Exception(name))
                                    Log.error(name)
                                }
                            }
                        }

                        json1.put("features", features)

                        jsonObj.put(location.toString(), json1)
                    }
                    else {
                        val features = json1.getJSONObject("features")

                        val categories = res.getJSONArray("categories")

                        for (category in categories) {
                            val name = category.getString("name")

                            when {
                                name.equals("P2P", true) -> features.put("p2p", true)
                                name.equals("Dedicated IP", true) -> features.put("dedicated", true)
                                name.equals("Double VPN", true) -> features.put("double_vpn", true)
                                name.equals("Onion Over VPN", true) -> features.put("tor_over_vpn", true)
                                name.equals("Obfuscated Servers", true) -> features.put("anti_ddos", true)
                                name.equals("Standard VPN servers", true) -> features.put("standard", true)
                                else -> {
                                    Crashlytics.logException(Exception(name))
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
                    Crashlytics.logException(e)
                }

                if (jsonArray.length() > 0) {
                    return jsonArray
                }
            }
        }

    return null
}
