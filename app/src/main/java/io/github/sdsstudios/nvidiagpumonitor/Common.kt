package io.github.sdsstudios.nvidiagpumonitor

import androidx.annotation.WorkerThread
import android.util.Xml
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import de.jupf.staticlog.Log
import java.io.StringWriter

@WorkerThread
fun generateXML() {
    // An extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
    "https://api.nordvpn.com/server".httpGet().responseJson { _, _, result ->
        when (result) {
            is Result.Failure -> {
                Log.error(result.getException().toString())
            }
            is Result.Success -> {
                operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()
                val countries_mapping = mutableMapOf<String, String>()
                val json_response = result.get().array()
                for (res in json_response) {
                    if (res.getString("country") !in countries_mapping) {
                        countries_mapping[res.getString("country")] = res.getString("domain").take(2)
                    }
                }
                val sorted_countries_mapping = countries_mapping.toSortedMap(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                val serializer = Xml.newSerializer()
                val writer = StringWriter()
                try {
                    serializer.setOutput(writer)
                    serializer.startDocument("UTF-8", true)
                    serializer.startTag("", "head")
                    serializer.startTag("", "string-array")
                    serializer.attribute("", "name", "pref_country_entries")
                    for ((key, _) in sorted_countries_mapping) {
                        serializer.startTag("", "item")
                        serializer.text(key)
                        serializer.endTag("", "item")
                    }
                    serializer.endTag("", "string-array")
                    serializer.startTag("", "string-array")
                    serializer.attribute("", "name", "pref_country_values")
                    for ((_, value) in sorted_countries_mapping) {
                        serializer.startTag("", "item")
                        serializer.text(value)
                        serializer.endTag("", "item")
                    }
                    serializer.endTag("", "string-array")
                    serializer.endTag("", "head")
                    serializer.endDocument()
                    println(writer.toString())
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
        }
    }
}

@WorkerThread
fun createJson1(): JSONObject? {
    val json1 = JSONObject()

    for (name in listOf("https://api.ipdata.co", "http://ip-api.com/json")) {
        val timeout = 500
        val timeoutRead = 500
        // An extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
        val (_, _, result) = name.httpGet().timeout(timeout).timeoutRead(timeoutRead).responseJson()
        when (result) {
            is Result.Failure -> {
                Log.error(result.getException().toString())
            }
            is Result.Success -> {
            val content = result.get().obj()
            Log.debug(content.toString())

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
fun createJson(): JSONArray? {
    val jsonObjLast = JSONArray()
    val timeout = 1000
    val timeoutRead = 1000
    // An extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
    val (_, _, result) = "https://api.nordvpn.com/server".httpGet().timeout(timeout).timeoutRead(timeoutRead).responseJson()
        when (result) {
            is Result.Failure -> {
                Log.error(result.getException().toString())
            }
            is Result.Success -> {
                operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()
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
                                name.equals("Dedicated IP servers", true) -> features.put("dedicated", true)
                                name.equals("Double VPN", true) -> features.put("double_vpn", true)
                                name.equals("Onion Over VPN", true) -> features.put("tor_over_vpn", true)
                                name.equals("Obfuscated Servers", true) -> features.put("anti_ddos", true)
                                name.equals("Standard VPN servers", true) -> features.put("standard", true)
                                else -> Log.error(name)
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
                                name.equals("Dedicated IP servers", true) -> features.put("dedicated", true)
                                name.equals("Double VPN", true) -> features.put("double_vpn", true)
                                name.equals("Onion Over VPN", true) -> features.put("tor_over_vpn", true)
                                name.equals("Obfuscated Servers", true) -> features.put("anti_ddos", true)
                                name.equals("Standard VPN servers", true) -> features.put("standard", true)
                                else -> Log.error(name)
                            }
                        }
                    }
                }

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
                            jsonArr.put(JSONObject().put("name", "Dedicated IP servers"))
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

                        val objLast = JSONObject().apply {
                            put("flag", value.getString("flag"))
                            put("country", value.getString("country"))
                            put("location", value.getJSONObject("location"))
                            put("categories", jsonArr)
                        }

                        jsonObjLast.put(objLast)
                    }
                } catch (e: JSONException) {
                    Log.error(e.toString())
                }
            }
        }

    return if (jsonObjLast.length() > 0) jsonObjLast else null
}