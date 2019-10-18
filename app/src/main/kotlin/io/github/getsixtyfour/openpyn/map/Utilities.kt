package io.github.getsixtyfour.openpyn.map

import android.R.color
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.Resources.NotFoundException
import android.location.Location
import android.text.SpannableString
import android.view.animation.AccelerateInterpolator
import androidx.annotation.RawRes
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.abdeveloper.library.MultiSelectModelExtra
import com.abdeveloper.library.MultiSelectable
import com.androidmapsextensions.lazy.LazyMarker
import com.androidmapsextensions.lazy.LazyMarker.OnLevelChangeCallback
import com.androidmapsextensions.lazy.LazyMarker.OnMarkerCreateListener
import com.antoniocarlon.map.CameraUpdateAnimator.Animation
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider
import com.getsixtyfour.openvpnmgmt.android.security.SecurityManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition.Builder
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.jayrave.moshi.pristineModels.PristineModelsJsonAdapterFactory
import com.mayurrokade.minibar.UserMessage
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import de.jupf.staticlog.Log
import de.westnordost.countryboundaries.CountryBoundaries
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.R.array
import io.github.getsixtyfour.openpyn.logException
import io.github.getsixtyfour.openpyn.utils.CITY
import io.github.getsixtyfour.openpyn.utils.COUNTRY
import io.github.getsixtyfour.openpyn.utils.FLAG
import io.github.getsixtyfour.openpyn.utils.IP
import io.github.getsixtyfour.openpyn.utils.LAT
import io.github.getsixtyfour.openpyn.utils.LOCATION
import io.github.getsixtyfour.openpyn.utils.LONG
import io.github.getsixtyfour.openpyn.utils.MultiSelectMapper
import io.github.getsixtyfour.openpyn.utils.NetworkInfo
import io.github.getsixtyfour.openpyn.utils.PrintArray
import io.github.getsixtyfour.openpyn.utils.THREAT
import io.github.getsixtyfour.openpyn.utils.createJson2
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.features.DefaultRequest
import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
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
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.HashSet
import java.util.Locale
import kotlin.math.pow

// private const val TASK_TIMEOUT: Long = 500
@Suppress("ComplexMethod", "MagicNumber", "unused")
internal fun showThreats(context: Activity, jsonObj: JSONObject) {
    val threats: JSONObject? = jsonObj.optJSONObject(THREAT)
    Log.info(threats.toString())

    if (threats != null) {
        val tor = threats.getBoolean("is_tor")
        val proxy = threats.getBoolean("is_proxy")
        val anonymous = threats.getBoolean("is_anonymous")
        val attacker = threats.getBoolean("is_known_attacker")
        val abuser = threats.getBoolean("is_known_abuser")
        val threat = threats.getBoolean("is_threat")
        val bogon = threats.getBoolean("is_bogon")
        val color1 = ContextCompat.getColor(context, R.color.colorConnect)
        val color2 = ContextCompat.getColor(context, R.color.colorDisconnect)
        val fl = 22f
        val weight = 1.0f
        with(context) {
            alert {
                customView = verticalLayout {
                    linearLayout {
                        textView {
                            text = getString(R.string.is_tor)
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
                            text = getString(R.string.is_proxy)
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
                            text = getString(R.string.is_anonymous)
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
                            text = getString(R.string.is_known_attacker)
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
                            text = getString(R.string.is_known_abuser)
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
                            text = getString(R.string.is_threat)
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
                            text = getString(R.string.is_bogon)
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
            }.show()
        }
    }
}

fun showPrintArray(context: Context, countries: List<MultiSelectable>, hashSet: HashSet<CharSequence>): HashSet<CharSequence> {
    val length = hashSet.size
    val defaultSelectedIdsList = ArrayList<Int>(length)
    countries.forEach {
        (it as? MultiSelectModelExtra)?.let { selectable ->
            if (hashSet.contains(selectable.tag)) {
                defaultSelectedIdsList.add(selectable.id)
            }
        }
    }
    val defValue = defaultSelectedIdsList.joinToString(separator = PrintArray.delimiter)
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val prefSelectedIdsList = PrintArray.getListInt("pref_country_values", defValue, preferences)
    val currentFlags = HashSet<CharSequence>(length)
    val currentIds = ArrayList<Int>(length)
    val currentCountries = ArrayList<MultiSelectable>(countries.size)
    countries.forEach {
        (it as? MultiSelectModelExtra)?.let { selectable ->
            val id = selectable.id
            val tag = selectable.tag

            if (hashSet.contains(tag)) {
                currentCountries.add(it)

                if (prefSelectedIdsList.contains(id)) {
                    currentFlags.add(tag)
                    currentIds.add(id)
                }
            }
        }
    }

    PrintArray.apply {
        setHint(R.string.multi_select_dialog_hint)
        setTitle(R.string.empty)
        setItems(currentCountries)
        setCheckedItems(currentIds)
    }

    return currentFlags
}

@Suppress("unused", "SpellCheckingInspection")
fun fileBackedTileProvider(): MapBoxOfflineTileProvider {
    // Use a file backed SQLite database
    val tileProvider = MapBoxOfflineTileProvider("file:world.mbtiles?vfs=ndk-asset&immutable=1&mode=ro")
    Log.debug(tileProvider.toString())
    return tileProvider
}

@Suppress("unused", "SpellCheckingInspection")
fun memoryBackedTileProvider(): MapBoxOfflineTileProvider {
    // Use a memory backed SQLite database
    val tileProvider = MapBoxOfflineTileProvider(null, "file:world.mbtiles?vfs=ndk-asset&immutable=1&mode=ro")
    Log.debug(tileProvider.toString())
    return tileProvider
}

fun getCountryBoundaries(context: Context): CountryBoundaries? {
    try {
        return CountryBoundaries.load(context.assets.open("boundaries.ser"))
    } catch (e: FileNotFoundException) {
        logException(e)
    } catch (e: IOException) {
        logException(e)
    }

    return null
}

@Suppress("ComplexMethod")
fun getCurrentPosition(
    context: Context,
    countryBoundaries: CountryBoundaries?,
    lastLocation: Location?,
    flags: HashSet<CharSequence>,
    jsonObj: JSONObject?,
    jsonArr: JSONArray? = null
): LatLng {
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
                val element = LatLng(
                    location.getDouble(LAT), location.getDouble(LONG)
                )

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

    fun getToastString(ids: List<String>?): String = when {
        ids.isNullOrEmpty() -> "is nowhere"
        else -> "is in " + ids.joinToString()
    }

    fun getFlag(list: List<String>?): String = when {
        list != null && list.isNotEmpty() -> list[0].toLowerCase(Locale.ROOT)
        else -> ""
    }

    fun getFLag(countryBoundaries: CountryBoundaries?, lon: Double, lat: Double): String {
        var t = System.nanoTime()
        val ids = countryBoundaries?.getIds(lon, lat)
        t = System.nanoTime() - t
        @Suppress("MagicNumber") val i = 1000
        Log.debug(getToastString(ids) + " (in " + "%.3f".format(t / i / i.toFloat()) + "ms)")
        return getFlag(ids)
    }

    var latLng = getDefaultLatLng()

    when {
        jsonObj != null -> {
            val lat = jsonObj.getDouble(LAT)
            val lon = jsonObj.getDouble(LONG)
            val flag = jsonObj.getString(FLAG)
            Log.debug("is in $flag")
            latLng = latLng(jsonArr, flags, flag, lat, lon)
        }
        jsonArr != null -> lastLocation?.let {
            val lat = it.latitude
            val lon = it.longitude
            val flag = getFLag(countryBoundaries, lon, lat)
            latLng = latLng(jsonArr, flags, flag, lat, lon)
        }
/*
        ContextCompat.checkSelfPermission(
            context, permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED -> {
            val task = FusedLocationProviderClient(context).lastLocation
            try {
                // Block on the task for a maximum of 500 milliseconds, otherwise time out.
                Tasks.await(task, TASK_TIMEOUT, TimeUnit.MILLISECONDS)?.let {
                    val lat = it.latitude
                    val lon = it.longitude
                    val flag = getFLag(countryBoundaries, lon, lat)
                    latLng = latLng(jsonArr, flags, flag, lat, lon)
                }
            } catch (e: ExecutionException) {
                Log.error(e.toString())
            } catch (e: InterruptedException) {
                logException(e)
            } catch (e: TimeoutException) {
                Log.error(e.toString())
            }
        }
*/
    }
    Log.debug(latLng.toString())
    return latLng
}

operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

fun createCameraUpdates(): ArrayList<Animation> {
    // Load all map tiles
    @Suppress("MagicNumber") val z = 3
    //val z = tileProvider!!.minimumZoom.toInt()
    val rows = 2.0.pow(z.toDouble()).toInt() - 1
    val cameraUpdates = ArrayList<Animation>(rows)
    // Traverse through all rows
    for (y in 0..rows) {
        for (x in 0..rows) {
            val bounds = MapBoxOfflineTileProvider.calculateTileBounds(x, y, z)
            val cameraPosition = Builder().target(bounds.northeast).build()
            val animation = Animation(CameraUpdateFactory.newCameraPosition(cameraPosition))
            // Add animations
            cameraUpdates.add(animation)
        }
    }
    return cameraUpdates
}

fun createMarkers(
    context: Context,
    jsonArray: JSONArray,
    countries: List<MultiSelectable>,
    listener: OnMarkerCreateListener,
    favorites: ArrayList<LazyMarker>?,
    callback: OnLevelChangeCallback
): Pair<HashSet<CharSequence>, HashMap<LatLng, LazyMarker>> {
    fun lazyMarker(
        listener: OnMarkerCreateListener,
        favorites: ArrayList<LazyMarker>?,
        options: MarkerOptions,
        flag: CharSequence?,
        callback: OnLevelChangeCallback
    ): LazyMarker {
        val marker = LazyMarker(options, flag, listener)
        favorites?.let {
            val index = it.indexOf(marker)
            if (index >= 0) {
                marker.setLevel(it[index].level, callback)
            }
        }

        return marker
    }

    fun parseToUnicode(countries: List<MultiSelectable>, input: CharSequence): CharSequence {
        // Replace the aliases by their unicode
        var result = input
        val emoji = countries.find { (it as? MultiSelectModelExtra)?.tag == input } as? MultiSelectModelExtra
        if (emoji != null) {
            result = emoji.unicode
        }

        return result
    }

    fun netflix(flag: CharSequence?): Boolean = when (flag) {
        "us" -> true
        "ca" -> true
        "nl" -> true
        "jp" -> true
        "gb" -> true
        "gr" -> true
        "mx" -> true
        else -> false
    }
    // HashSet<E> : MutableSet<E> {
    //     constructor()
    //     constructor(initialCapacity: Int)
    val length = jsonArray.length()
    val flags = HashSet<CharSequence>(length)
    val markers = HashMap<LatLng, LazyMarker>(length)
    val iconDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.map1)
    // val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    // val netflix = preferences.getBoolean("pref_netflix", false)
    // val dedicated = preferences.getBoolean("pref_dedicated", false)
    // val double = preferences.getBoolean("pref_double", false)
    // val obfuscated = preferences.getBoolean("pref_anti_ddos", false)
    // val onion = preferences.getBoolean("pref_tor", false)
    // val p2p = preferences.getBoolean("pref_p2p", false)
    for (res in jsonArray) {
        val flag = res.getString(FLAG)
        // var pass = when {
        //     netflix -> netflix(flag)
        //     dedicated -> false
        //     double -> false
        //     obfuscated -> false
        //     onion -> false
        //     p2p -> false
        //     else -> true
        // }
        // if (!pass && !netflix) {
        //     val categories = res.getJSONArray(CATEGORIES)
        //
        //     loop@ for (category in categories) {
        //         val name = category.getString(NAME)
        //         pass = when {
        //             dedicated and (name == DEDICATED) -> true
        //             double and (name == DOUBLE) -> true
        //             obfuscated and (name == OBFUSCATED) -> true
        //             onion and (name == ONION) -> true
        //             p2p and (name == P2P) -> true
        //             else -> false
        //         }
        //
        //         if (pass) {
        //             break@loop
        //         }
        //     }
        // }
        //
        // if (!pass) {
        //     continue
        // }
        val country = res.getString(COUNTRY)
        val emoji = parseToUnicode(countries, flag)
        val location = res.getJSONObject(LOCATION)
        val latLng = LatLng(location.getDouble(LAT), location.getDouble(LONG))
        val options = MarkerOptions().apply {
            flat(true)
            position(latLng)
            title("$emoji $country")
            visible(false)
            icon(iconDescriptor)
        }

        flags.add(flag)
        markers[latLng] = lazyMarker(listener, favorites, options, flag, callback)
    }

    return Pair(flags, markers)
}

internal fun createUserMessage(context: Context, jsonObj: JSONObject): UserMessage.Builder {
    // val country = it.getString(COUNTRY)
    // val lat = it.getDouble(LAT)
    // val lon = it.getDouble(LONG)
    val city = jsonObj.getString(CITY)
    val flag = jsonObj.getString(FLAG).toUpperCase(Locale.ROOT)
    val ip = jsonObj.getString(IP)
    return UserMessage.Builder().apply {
        with(context.applicationContext)
        setBackgroundColor(R.color.accent_material_indigo_200).setTextColor(color.white)
        setMessage("Connected to $city, $flag ($ip)").setDuration(7000).setShowInterpolator(AccelerateInterpolator())
        setDismissInterpolator(AccelerateInterpolator())
    }
}

internal fun getCurrentFlags(countries: List<MultiSelectable>, selectedIds: ArrayList<Int>): HashSet<CharSequence> {
    val currentFlags = HashSet<CharSequence>(selectedIds.size)
    countries.forEach {
        (it as? MultiSelectModelExtra)?.let { selectable ->
            if (selectedIds.contains(selectable.id)) {
                currentFlags.add(selectable.tag)
            }
        }
    }
    return currentFlags
}

@Suppress("MagicNumber")
fun countryList(context: Context, @RawRes id: Int): List<MultiSelectable> {
    val json = context.resources.openRawResource(id).bufferedReader().use { it.readText() }
    val factory = PristineModelsJsonAdapterFactory.Builder().also { it.add(MultiSelectModelExtra::class.java, MultiSelectMapper()) }
    val moshi = Moshi.Builder().add(factory.build()).add(object {
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

fun jsonArray(context: Context, id: Int, ext: String): JSONArray {
    fun logDifference(set: Set<CharSequence>, string: CharSequence) {
        set.forEach {
            val message = "$string $it"
            logException(Exception(message))
            Log.error(message)
        }
    }

    val jsonArray = createJsonArray(context, id, ext)
    val set1 = context.resources.getTextArray(array.pref_country_values).toHashSet()
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
            if (token != null && token.isNotEmpty()) token = SecurityManager.getInstance(
                context
            ).decryptString(token)
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
