package io.github.sdsstudios.nvidiagpumonitor.Controllers
import android.content.ContentValues.TAG
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.util.Log
import android.preference.PreferenceManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.result.Result
import org.json.JSONArray
import org.json.JSONObject
import android.util.Xml
import java.io.StringWriter

import android.content.SharedPreferences
import org.jetbrains.anko.toast

class OpenpynController(
        ctx: Context,
        liveData: MutableLiveData<Int>
) : BaseController(ctx, liveData) {

    override val regex = Regex("""\d+""")

    // the file /etc/profile is only loaded for a login shell, this is a non-interactive shell
    override val command = "[ -f /opt/etc/profile ] && . /opt/etc/profile ; echo \$PATH ; echo \$-"

    val openpyn: String

    init {
        val server = "pref_server"
        val preferences = PreferenceManager.getDefaultSharedPreferences(ctx)
        var name = preferences.getString(server, "")

        openpyn = name;

        Log.v(TAG, name)

        //an extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
        "https://api.nordvpn.com/server".httpGet().responseJson { request, response, result ->
            when (result) {
                is Result.Failure -> {
                    Log.e(TAG, "Failure")
                    val ex = result.getException()
                }
                is Result.Success -> {
                    Log.e(TAG, "Success")
                    operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()
                    val countries_mapping = mutableMapOf<String, String>()
                    val json_response = result.get().array() //JSONArray
                    for (res in json_response) {
                        if (res.getString("country") !in countries_mapping) {
                            countries_mapping.put(res.getString("country"), res.getString("domain").take(2))
                        }
                    }
                    val sorted_countries_mapping = countries_mapping.toSortedMap(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
                    val serializer = Xml.newSerializer()
                    val writer = StringWriter()
                    try {
                        serializer.setOutput(writer)
                        serializer.startDocument("UTF-8", true)
                        serializer.startTag("", "head");
                        serializer.startTag("", "string-array")
                        serializer.attribute("", "name", "pref_country_entries")
                        for ((key, value) in sorted_countries_mapping) {
                            serializer.startTag("", "item")
                            serializer.text(key)
                            serializer.endTag("", "item")
                        }
                        serializer.endTag("", "string-array")
                        serializer.startTag("", "string-array")
                        serializer.attribute("", "name", "pref_country_values")
                        for ((key, value) in sorted_countries_mapping) {
                            serializer.startTag("", "item")
                            serializer.text(value)
                            serializer.endTag("", "item")
                        }
                        serializer.endTag("", "string-array")
                        serializer.endTag("", "head");
                        serializer.endDocument()
                        println(writer.toString())
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }
                }
            }
        }
    }

    override fun convertDataToInt(data: String): Int {
        return data.toInt()
    }

    override fun onOutputLine(line: String) {
        Log.e(TAG, line)

    }
}