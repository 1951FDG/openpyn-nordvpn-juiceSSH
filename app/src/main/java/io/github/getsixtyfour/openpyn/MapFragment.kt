package io.github.getsixtyfour.openpyn

import android.Manifest
import android.Manifest.permission
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources.NotFoundException
import android.location.Location
import android.os.Bundle
import android.text.SpannableString
import android.view.Gravity
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.abdeveloper.library.MultiSelectModel
import com.abdeveloper.library.MultiSelectable
import com.androidmapsextensions.lazy.LazyMarker
import com.androidmapsextensions.lazy.LazyMarker.OnLevelChangeCallback
import com.antoniocarlon.map.CameraUpdateAnimator
import com.ariascode.networkutility.NetworkInfo
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider
import com.crashlytics.android.Crashlytics
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.mayurrokade.minibar.UserMessage
import com.naver.android.svc.annotation.RequireControlTower
import com.naver.android.svc.annotation.RequireViews
import com.naver.android.svc.annotation.SvcFragment
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import com.vdurmont.emoji.EmojiFlagManager
import de.westnordost.countryboundaries.CountryBoundaries
import io.github.getsixtyfour.openpyn.R.drawable
import io.github.getsixtyfour.openpyn.R.string
import io.github.getsixtyfour.openpyn.security.SecurityManager
import io.github.getsixtyfour.openpyn.utilities.PrintArray
import io.github.getsixtyfour.openpyn.utilities.createJson2
import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.fragment_map.fab0
import kotlinx.android.synthetic.main.fragment_map.fab1
import kotlinx.android.synthetic.main.fragment_map.fab2
import kotlinx.android.synthetic.main.fragment_map.fab3
import kotlinx.android.synthetic.main.fragment_map.map
import kotlinx.android.synthetic.main.fragment_map.maplayout
import kotlinx.android.synthetic.main.fragment_map.minibarView
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.error
import org.jetbrains.anko.info
import org.jetbrains.anko.uiThread
import org.jetbrains.anko.wrapContent
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Locale

/**
 * @author 1951FDG
 */
@SvcFragment
@RequireViews(MapViews::class)
@RequireControlTower(MapControlTower::class)
class MapFragment : SVC_MapFragment(), OnMapReadyCallback,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMapLoadedCallback,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnCameraIdleListener,
    AnkoLogger,
    OnLevelChangeCallback,
    OnSessionStartedListener,
    OnSessionFinishedListener, OnClickListener {

    private val markers: HashMap<LatLng, LazyMarker> by lazy { HashMap<LatLng, LazyMarker>() }
    private val storage by lazy { LazyMarkerStorage(FAVORITE_KEY) }
    private val flags by lazy { ArrayList<String>() }
    private var cameraUpdateAnimator: CameraUpdateAnimator? = null
    private var countryBoundaries: CountryBoundaries? = null
    private var mMap: GoogleMap? = null
    private var tileProvider: MapBoxOfflineTileProvider? = null

    private var snackProgressBarManager: SnackProgressBarManager? = null

    private var lastLocation: Location? = null

    private var networkInfo: NetworkInfo? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 23
        private const val FAVORITE_KEY = "pref_favorites"
        @Suppress("MagicNumber")
        private fun countryList(array: Array<CharSequence>): ArrayList<MultiSelectable> {
            return arrayListOf(
                MultiSelectModel(0, SpannableString(array[0]), drawable.ic_albania_40dp),
                MultiSelectModel(1, SpannableString(array[1]), drawable.ic_argentina_40dp),
                MultiSelectModel(2, SpannableString(array[2]), drawable.ic_australia_40dp),
                MultiSelectModel(3, SpannableString(array[3]), drawable.ic_austria_40dp),
                MultiSelectModel(4, SpannableString(array[4]), drawable.ic_azerbaijan_40dp),
                MultiSelectModel(5, SpannableString(array[5]), drawable.ic_belgium_40dp),
                MultiSelectModel(6, SpannableString(array[6]), drawable.ic_bosnia_and_herzegovina_40dp),
                MultiSelectModel(7, SpannableString(array[7]), drawable.ic_brazil_40dp),
                MultiSelectModel(8, SpannableString(array[8]), drawable.ic_bulgaria_40dp),
                MultiSelectModel(9, SpannableString(array[9]), drawable.ic_canada_40dp),
                MultiSelectModel(10, SpannableString(array[10]), drawable.ic_chile_40dp),
                MultiSelectModel(11, SpannableString(array[11]), drawable.ic_costa_rica_40dp),
                MultiSelectModel(12, SpannableString(array[12]), drawable.ic_croatia_40dp),
                MultiSelectModel(13, SpannableString(array[13]), drawable.ic_cyprus_40dp),
                MultiSelectModel(14, SpannableString(array[14]), drawable.ic_czech_republic_40dp),
                MultiSelectModel(15, SpannableString(array[15]), drawable.ic_denmark_40dp),
                MultiSelectModel(16, SpannableString(array[16]), drawable.ic_egypt_40dp),
                MultiSelectModel(17, SpannableString(array[17]), drawable.ic_estonia_40dp),
                MultiSelectModel(18, SpannableString(array[18]), drawable.ic_finland_40dp),
                MultiSelectModel(19, SpannableString(array[19]), drawable.ic_france_40dp),
                MultiSelectModel(20, SpannableString(array[20]), drawable.ic_georgia_40dp),
                MultiSelectModel(21, SpannableString(array[21]), drawable.ic_germany_40dp),
                MultiSelectModel(22, SpannableString(array[22]), drawable.ic_greece_40dp),
                MultiSelectModel(23, SpannableString(array[23]), drawable.ic_hong_kong_40dp),
                MultiSelectModel(24, SpannableString(array[24]), drawable.ic_hungary_40dp),
                MultiSelectModel(25, SpannableString(array[25]), drawable.ic_iceland_40dp),
                MultiSelectModel(26, SpannableString(array[26]), drawable.ic_india_40dp),
                MultiSelectModel(27, SpannableString(array[27]), drawable.ic_indonesia_40dp),
                MultiSelectModel(28, SpannableString(array[28]), drawable.ic_ireland_40dp),
                MultiSelectModel(29, SpannableString(array[29]), drawable.ic_israel_40dp),
                MultiSelectModel(30, SpannableString(array[30]), drawable.ic_italy_40dp),
                MultiSelectModel(31, SpannableString(array[31]), drawable.ic_japan_40dp),
                MultiSelectModel(32, SpannableString(array[32]), drawable.ic_latvia_40dp),
                MultiSelectModel(33, SpannableString(array[33]), drawable.ic_luxembourg_40dp),
                MultiSelectModel(34, SpannableString(array[34]), drawable.ic_republic_of_macedonia_40dp),
                MultiSelectModel(35, SpannableString(array[35]), drawable.ic_malaysia_40dp),
                MultiSelectModel(36, SpannableString(array[36]), drawable.ic_mexico_40dp),
                MultiSelectModel(37, SpannableString(array[37]), drawable.ic_moldova_40dp),
                MultiSelectModel(38, SpannableString(array[38]), drawable.ic_netherlands_40dp),
                MultiSelectModel(39, SpannableString(array[39]), drawable.ic_new_zealand_40dp),
                MultiSelectModel(40, SpannableString(array[40]), drawable.ic_norway_40dp),
                MultiSelectModel(41, SpannableString(array[41]), drawable.ic_poland_40dp),
                MultiSelectModel(42, SpannableString(array[42]), drawable.ic_portugal_40dp),
                MultiSelectModel(43, SpannableString(array[43]), drawable.ic_romania_40dp),
                MultiSelectModel(44, SpannableString(array[44]), drawable.ic_russia_40dp),
                MultiSelectModel(45, SpannableString(array[45]), drawable.ic_serbia_40dp),
                MultiSelectModel(46, SpannableString(array[46]), drawable.ic_singapore_40dp),
                MultiSelectModel(47, SpannableString(array[47]), drawable.ic_slovakia_40dp),
                MultiSelectModel(48, SpannableString(array[48]), drawable.ic_slovenia_40dp),
                MultiSelectModel(49, SpannableString(array[49]), drawable.ic_south_africa_40dp),
                MultiSelectModel(50, SpannableString(array[50]), drawable.ic_south_korea_40dp),
                MultiSelectModel(51, SpannableString(array[51]), drawable.ic_spain_40dp),
                MultiSelectModel(52, SpannableString(array[52]), drawable.ic_sweden_40dp),
                MultiSelectModel(53, SpannableString(array[53]), drawable.ic_switzerland_40dp),
                MultiSelectModel(54, SpannableString(array[54]), drawable.ic_taiwan_40dp),
                MultiSelectModel(55, SpannableString(array[55]), drawable.ic_thailand_40dp),
                MultiSelectModel(56, SpannableString(array[56]), drawable.ic_turkey_40dp),
                MultiSelectModel(57, SpannableString(array[57]), drawable.ic_ukraine_40dp),
                MultiSelectModel(58, SpannableString(array[58]), drawable.ic_united_arab_emirates_40dp),
                MultiSelectModel(59, SpannableString(array[59]), drawable.ic_united_kingdom_40dp),
                MultiSelectModel(60, SpannableString(array[60]), drawable.ic_united_states_of_america_40dp),
                MultiSelectModel(61, SpannableString(array[61]), drawable.ic_vietnam_40dp)
            )
        }

        private fun jsonArray(context: Context, id: Int, ext: String): JSONArray? {
            try {
                val file = File(context.getExternalFilesDir(null), context.resources.getResourceEntryName(id) + ext)
                if (!file.exists()) {
                    context.resources.openRawResource(id).use { input ->
                        file.outputStream().buffered().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                val json = file.bufferedReader().use {
                    it.readText()
                }
                return JSONArray(json)
            } catch (e: NotFoundException) {
                Crashlytics.logException(e)
            } catch (e: FileNotFoundException) {
                Crashlytics.logException(e)
            } catch (e: IOException) {
                Crashlytics.logException(e)
            } catch (e: JSONException) {
                Crashlytics.logException(e)
            }
            return null
        }

        @WorkerThread
        fun createGeoJson(value: NetworkInfo, preferences: SharedPreferences, securityManager: SecurityManager): JSONObject? {
            if (value.isOnline()) {
                val geo = preferences.getBoolean("pref_geo", false)
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        map?.onCreate(savedInstanceState)

        networkInfo = NetworkInfo.getInstance(requireActivity().application)

        snackProgressBarManager = SnackProgressBarManager(maplayout).setViewsToMove(arrayOf(fab0, fab1))

        fab0?.setOnClickListener(this)

        map?.getMapAsync(this)

        val permissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        requestPermissions(permissions,
            PERMISSION_REQUEST_CODE
        )


        super.onCreate(savedInstanceState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        fun isGranted(index: Int): Boolean {
            return (index >= 0 && index <= grantResults.lastIndex) && (grantResults[index] == PackageManager.PERMISSION_GRANTED)
        }

        fun hasPermission(permission: String): Boolean {
            return ActivityCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
        }

        fun getLastLocation() {
            FusedLocationProviderClient(requireActivity()).lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        lastLocation = location
                    }
                }
                .addOnFailureListener { e: Exception ->
                    error(e)
                }
        }

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (isGranted(0)) {
                    getLastLocation()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        map?.onStart()
    }

    override fun onResume() {
        super.onResume()
        map?.onResume()
    }

    override fun onPause() {
        super.onPause()
        map?.onPause()
    }

    override fun onStop() {
        super.onStop()
        map?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        map?.onDestroy()

        tileProvider?.close()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map?.onLowMemory()
    }

    override fun onSessionStarted(sessionId: Int, sessionKey: String) {
        fab0.isClickable = true
        fab0.setImageResource(R.drawable.ic_flash_off_white_24dp)

        fab1.hide()
        fab2.hide()
        markers.forEach { (_, value) ->
            if (value.zIndex == 1.0f) fab3.hide()
        }

        mMap?.setOnMapClickListener(null)
        mMap?.setOnMarkerClickListener { true }
        mMap?.uiSettings?.isScrollGesturesEnabled = false
        mMap?.uiSettings?.isZoomGesturesEnabled = false
    }

    override fun onSessionCancelled() {
        fab0.isClickable = true
    }

    @Suppress("MagicNumber")
    override fun onSessionFinished() {
        toolbar.hideProgress(true)

        fab0.isClickable = true
        fab0.setImageResource(R.drawable.ic_flash_on_white_24dp)

        mMap?.let { fab1.show() }
        mMap?.let { fab2.show() }
        markers.forEach { (_, value) ->
            if (value.zIndex == 1.0f) fab3.show()
        }

        mMap?.setOnMapClickListener(this)
        mMap?.setOnMarkerClickListener(this)
        mMap?.uiSettings?.isScrollGesturesEnabled = true
        mMap?.uiSettings?.isZoomGesturesEnabled = true
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        cameraUpdateAnimator = CameraUpdateAnimator(googleMap, this)
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val favorites = storage.loadFavorites(requireContext())
        val iconDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.map1)
        val securityManager = SecurityManager.getInstance(requireContext())

        toolbar.showProgress(true)

        fun selectedCountries(list: ArrayList<MultiSelectable>): ArrayList<Int> {
            val preSelectedIdsList = ArrayList<Int>()
            for (i in list.indices) {
                preSelectedIdsList.add(i)
            }
            val defValue = preSelectedIdsList.joinToString(separator = PrintArray.delimiter)
            return PrintArray.getListInt("pref_country_values", defValue, preferences)
        }

        fun printArray(items: ArrayList<MultiSelectable>, checkedItems: ArrayList<Int>) {
            PrintArray.apply {
                setHint(string.multi_select_dialog_hint)
                setTitle(string.empty)
                setItems(items)
                setCheckedItems(checkedItems)
            }
        }

        fun countryBoundaries(): CountryBoundaries? {
            try {
                return CountryBoundaries.load(requireContext().assets.open("boundaries.ser"))
            } catch (e: FileNotFoundException) {
                Crashlytics.logException(e)
            } catch (e: IOException) {
                Crashlytics.logException(e)
            }

            return null
        }

        fun tileProvider(): MapBoxOfflineTileProvider {
            // Use a memory backed SQLite database
            val tileProvider = MapBoxOfflineTileProvider(null, "file:world.mbtiles?vfs=ndk-asset&immutable=1&mode=ro")
            //val tileProvider = MapBoxOfflineTileProvider("file:world.mbtiles?vfs=ndk-asset&immutable=1&mode=ro")
            info(tileProvider.toString())
            return tileProvider
        }

        doAsync {
            val jsonArray = Companion.jsonArray(requireContext(), R.raw.nordvpn, ".json")
            val stringArray = resources.getStringArray(R.array.pref_country_values)
            val textArray = resources.getTextArray(R.array.pref_country_entries)
            val countries = Companion.countryList(textArray)
            val selectedCountries = selectedCountries(countries)
            val p2p = preferences.getBoolean("pref_p2p", false)
            val dedicated = preferences.getBoolean("pref_dedicated", false)
            val double = preferences.getBoolean("pref_double", false)
            val onion = preferences.getBoolean("pref_tor", false)
            val obfuscated = preferences.getBoolean("pref_anti_ddos", false)
            val netflix = preferences.getBoolean("pref_netflix", false)

            tileProvider = tileProvider()

            countryBoundaries = countryBoundaries()

            printArray(countries, selectedCountries)

            selectedCountries.forEach { index ->
                flags.add(stringArray[index])
            }

            if (jsonArray != null) {
                fun netflix(flag: String?): Boolean = when (flag) {
                    "us" -> true
                    "ca" -> true
                    "nl" -> true
                    "jp" -> true
                    "gb" -> true
                    "gr" -> true
                    "mx" -> true
                    else -> false
                }

                fun parseToUnicode(input: String): String {
                    // Replace the aliases by their unicode
                    var result = input
                    val emoji = EmojiFlagManager.getForAlias(input)
                    if (emoji != null) {
                        result = emoji.unicode
                    }

                    return result
                }

                fun lazyMarker(options: MarkerOptions, flag: String?): LazyMarker {
                    val marker = LazyMarker(googleMap, options, flag, null)
                    val index = favorites.indexOf(marker)
                    if (index >= 0) {
                        val any = favorites[index]
                        if (any is LazyMarker) {
                            val level = any.level
                            marker.setLevel(level, null)
                            onLevelChange(marker, level)
                        }
                    }
                    return marker
                }

                for (res in jsonArray) {
                    val flag = res.getString("flag")
                    var pass = when {
                        netflix -> netflix(flag)
                        p2p -> false
                        dedicated -> false
                        double -> false
                        onion -> false
                        obfuscated -> false
                        else -> true
                    }

                    if (!pass && !netflix) {
                        val categories = res.getJSONArray("categories")

                        loop@ for (category in categories) {
                            val name = category.getString("name")

                            pass = when {
                                p2p and (name == "P2P") -> true
                                dedicated and (name == "Dedicated IP") -> true
                                double and (name == "Double VPN") -> true
                                onion and (name == "Onion Over VPN") -> true
                                obfuscated and (name == "Obfuscated Servers") -> true
                                else -> false
                            }

                            if (pass) {
                                break@loop
                            }
                        }
                    }

                    if (!pass) {
                        continue
                    }
                    val country = res.getString("country")
                    val emoji = parseToUnicode(flag)
                    val location = res.getJSONObject("location")
                    val latLng = LatLng(location.getDouble("lat"), location.getDouble("long"))
                    val options = MarkerOptions().apply {
                        flat(true)
                        position(latLng)
                        title("$emoji $country")
                        visible(false)
                        icon(iconDescriptor)
                    }

                    markers[latLng] = lazyMarker(options, flag)
                }
            }
            // Log new countries, if any
            markers.forEach { (_, value) ->
                val element = value.tag
                if (element is String && !stringArray.contains(element)) {
                    Crashlytics.logException(Exception(element))
                    error(element)
                }
            }
            // Load all map tiles
            val z = 3
            //val z = tileProvider!!.minimumZoom.toInt()
            val rows = Math.pow(2.0, z.toDouble()).toInt() - 1
            // Traverse through all rows
            for (y in 0..rows) {
                for (x in 0..rows) {
                    val bounds = MapBoxOfflineTileProvider.calculateTileBounds(x, y, z)
                    val cameraPosition = CameraPosition.Builder().target(bounds.northeast).build()
                    // Add animations
                    cameraUpdateAnimator?.add(CameraUpdateFactory.newCameraPosition(cameraPosition), false, 0)
                }
            }
            val jsonObj = Companion.createGeoJson(networkInfo!!, preferences, securityManager)
            addAnimation(jsonObj, jsonArray, true)

            uiThread {
                toolbar.hideProgress(true)

                googleMap.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider).fadeIn(false))

                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.style_json))
                googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                googleMap.setMaxZoomPreference(tileProvider!!.maximumZoom)
                googleMap.setMinZoomPreference(tileProvider!!.minimumZoom)
                googleMap.setOnMapClickListener(it)
                googleMap.setOnMapLoadedCallback(it)
                googleMap.setOnMarkerClickListener(it)
                //val params = fab1.layoutParams as ConstraintLayout.LayoutParams
                //googleMap.setPadding(0, 0, 0, params.height + params.bottomMargin)
                googleMap.uiSettings.isScrollGesturesEnabled = true
                googleMap.uiSettings.isZoomGesturesEnabled = true
                val watermark = map?.findViewWithTag<ImageView>("GoogleWatermark")

                if (watermark != null) {
                    watermark.visibility = View.INVISIBLE
                    /*
                    val params = watermark.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_START, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_END, 0)
                    */
                }

                map.visibility = View.VISIBLE
            }
        }
    }

    override fun onMapLoaded() {
        // Execute the animation and set the final OnCameraIdleListener
        cameraUpdateAnimator?.execute()

        fab1.setOnClickListener(this)

        fab2?.setOnClickListener(this)

        fab3?.setOnClickListener(this)

        debug(mMap!!.minZoomLevel)
        debug(mMap!!.maxZoomLevel)
    }

    @Suppress("MagicNumber")
    private fun getDefaultLatLng(): LatLng {
        return LatLng(51.514125, -0.093689)
    }

    private fun getLatLng(flag: String, latLng: LatLng, jsonArr: JSONArray?): LatLng {
        info(latLng.toString())

        if (jsonArr != null) {
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

            if (!latLngList.isEmpty() && !match) {
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
        }

        return latLng
    }

    private fun getFlag(longitude: Double, latitude: Double): String {
        fun getToastString(ids: List<String>?): String {
            return when {
                ids == null || ids.isEmpty() -> "is nowhere"
                else -> "is in " + ids.joinToString()
            }
        }

        var t = System.nanoTime()
        val ids = countryBoundaries?.getIds(longitude, latitude)
        t = System.nanoTime() - t
        info(getToastString(ids) + " (in " + "%.3f".format(t / 1000 / 1000.toFloat()) + "ms)")
        if (ids != null && !ids.isEmpty()) {
            return ids[0].toLowerCase(Locale.ROOT)
        }
        return ""
    }

    private fun addAnimation(jsonObj: JSONObject?, jsonArr: JSONArray?, closest: Boolean) = when {
        jsonObj != null -> {
            val lat = jsonObj.getDouble("latitude")
            val lon = jsonObj.getDouble("longitude")
            val flag = jsonObj.getString("flag")
            val latLng = if (closest && flags.contains(flag)) {
                getLatLng(flag, LatLng(lat, lon), jsonArr)
            } else {
                LatLng(lat, lon)
            }

            animateCamera(latLng, closest, false)
        }
        lastLocation != null -> {
            val lat = lastLocation!!.latitude
            val lon = lastLocation!!.longitude
            val flag = getFlag(lon, lat)
            val latLng = if (closest && flags.contains(flag)) {
                getLatLng(flag, LatLng(lat, lon), jsonArr)
            } else {
                LatLng(lat, lon)
            }

            animateCamera(latLng, closest, false)
        }
        else -> {
            val latLng = getDefaultLatLng()
            animateCamera(latLng, closest, false)
        }
    }

    @MainThread
    private fun executeAnimation(jsonObj: JSONObject?, jsonArr: JSONArray?, closest: Boolean) = when {
        jsonObj != null -> {
            val lat = jsonObj.getDouble("latitude")
            val lon = jsonObj.getDouble("longitude")
            val flag = jsonObj.getString("flag")
            val latLng = if (closest && flags.contains(flag)) {
                getLatLng(flag, LatLng(lat, lon), jsonArr)
            } else {
                LatLng(lat, lon)
            }

            animateCamera(latLng, closest, true)
        }
        ActivityCompat.checkSelfPermission(requireContext(), permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
            val task = FusedLocationProviderClient(requireContext()).lastLocation
                .addOnSuccessListener { location: Location? ->
                    var latLng = getDefaultLatLng()
                    if (location != null) {
                        val lat = location.latitude
                        val lon = location.longitude
                        val flag = getFlag(lon, lat)

                        latLng = if (closest && flags.contains(flag)) {
                            getLatLng(flag, LatLng(lat, lon), jsonArr)
                        } else {
                            LatLng(lat, lon)
                        }
                    }

                    animateCamera(latLng, closest, true)
                }
                .addOnFailureListener { e: Exception ->
                    error(e)
                    animateCamera(getDefaultLatLng(), closest, true)
                }
        }
        else -> {
            animateCamera(getDefaultLatLng(), closest, true)
        }
    }

    private fun animateCamera(latLng: LatLng, closest: Boolean, animate: Boolean) {
        info(latLng.toString())

        fun onStart() {
            fab0.isClickable = false
            fab1.isClickable = false
            fab2.isClickable = false
        }

        fun onEnd() {
            fab0.isClickable = true
            fab1.isClickable = true
            fab2.isClickable = true
        }

        fun onFinish() {
            markers.forEach { (key, value) ->
                if (key == latLng) {
                    if (!value.isVisible) value.isVisible = true
                    if (!value.isInfoWindowShown) value.showInfoWindow()

                    value.zIndex = 1.0f
                    value.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.map0))

                    fab3.isChecked = (value.level == 1)
                    fab3.refreshDrawableState()

                    fab3.show()
                } else {
                    if (value.zIndex == 1.0f) {
                        //if (value.isInfoWindowShown) value.hideInfoWindow()
                        value.setLevel(value.level, null)
                        onLevelChange(value, value.level)
                    }
                }
            }

            fab1.show()
            fab2.show()
        }

        fun onCancel() {
            markers.forEach { (key, value) ->
                if (key == latLng) {
                    info("Animation to $value canceled")
                    return@onCancel
                }
            }
        }

        onStart()

        cameraUpdateAnimator?.add(CameraUpdateFactory.newLatLng(latLng), true, 0, object : CancelableCallback {
            override fun onFinish() {
                if (closest) {
                    onFinish()
                }

                onEnd()
            }

            override fun onCancel() {
                if (closest) {
                    onCancel()
                }

                onEnd()
            }
        })
        // Execute the animation and set the final OnCameraIdleListener
        if (animate) cameraUpdateAnimator?.execute()
    }

    override fun onCameraIdle() {
        val bounds = mMap!!.projection.visibleRegion.latLngBounds

        markers.forEach { (key, value) ->
            if (bounds.contains(key) && flags.contains(value.tag)) {
                if (!value.isVisible) value.isVisible = true
            } else {
                if (value.isVisible) value.isVisible = false

                if (value.zIndex == 1.0f) {
                    value.setLevel(value.level, this)

                    fab3.hide()
                }
            }
        }
    }

    override fun onMapClick(p0: LatLng?) {
        markers.forEach { (_, value) ->
            if (value.zIndex == 1.0f) {
                value.setLevel(value.level, this)

                fab3.hide()
            }
        }
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        if (p0 != null && p0.zIndex != 1.0f) {
            //info(p0.tag)
            markers.forEach { (_, value) ->
                if (value.zIndex == 1.0f) {
                    value.setLevel(value.level, this)
                }
            }
            p0.zIndex = 1.0f
            p0.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.map0))

            fab3.isChecked = (markers[p0.position]?.level == 1)
            fab3.refreshDrawableState()
        }

        fab3.show()

        return false
    }

    @Suppress("MagicNumber")
    override fun onLevelChange(marker: LazyMarker, level: Int) {
        when (level) {
            0 -> {
                marker.zIndex = 0f
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.map1))
            }
            1 -> {
                marker.zIndex = level / 10.toFloat()
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.map2))
            }
        }
    }

    override fun onClick(v: View?) {
        val id = checkNotNull(v).id

        if (id == R.id.fab0) {
            val listener = requireActivity() as? OnClickListener

            if (listener != null) {
                return listener.onClick(v)
            }
        } else if (id == R.id.fab1) {
            updateMasterMarker()
        } else if (id == R.id.fab2) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            //todo
            // PrintArray.show("pref_country_values", requireContext(), preferences)
        } else if (id == R.id.fab3) {
            if (mMap != null && markers.size != 0) {
                markers.forEach { (_, value) ->
                    if (value.zIndex == 1.0f) {
                        val level = value.level
                        when (level) {
                            0 -> {
                                value.setLevel(1, null)
                                storage.addFavorite(requireContext(), value)
                            }
                            1 -> {
                                value.setLevel(0, null)
                                storage.removeFavorite(requireContext(), value)
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("MagicNumber", "unused")
    private fun showThreats(jsonObj: JSONObject) {
        val threats: JSONObject? = jsonObj.optJSONObject("threat")
        info(threats)

        if (threats != null) {
            val tor = threats.getBoolean("is_tor")
            val proxy = threats.getBoolean("is_proxy")
            val anonymous = threats.getBoolean("is_anonymous")
            val attacker = threats.getBoolean("is_known_attacker")
            val abuser = threats.getBoolean("is_known_abuser")
            val threat = threats.getBoolean("is_threat")
            val bogon = threats.getBoolean("is_bogon")
            val color1 = ActivityCompat.getColor(requireContext(), R.color.colorConnect)
            val color2 = ActivityCompat.getColor(requireContext(), R.color.colorDisconnect)
            val fl = 22f
            val weight = 1.0f
            alert {
                customView {
                    verticalLayout {
                        linearLayout {
                            textView {
                                text = getString(R.string.is_tor)
                                textSize = fl
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = weight) {
                            }
                            textView {
                                text = if (tor) "YES" else "NO"
                                textColor = if (tor) color2 else color1
                                textSize = fl
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = weight) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = getString(R.string.is_proxy)
                                textSize = fl
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = weight) {
                            }
                            textView {
                                text = if (proxy) "YES" else "NO"
                                textColor = if (proxy) color2 else color1
                                textSize = fl
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = weight) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = getString(R.string.is_anonymous)
                                textSize = fl
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = weight) {
                            }
                            textView {
                                text = if (anonymous) "YES" else "NO"
                                textColor = if (anonymous) color2 else color1
                                textSize = fl
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = weight) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = getString(R.string.is_known_attacker)
                                textSize = fl
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = weight) {
                            }
                            textView {
                                text = if (attacker) "YES" else "NO"
                                textColor = if (attacker) color2 else color1
                                textSize = fl
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = weight) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = getString(R.string.is_known_abuser)
                                textSize = fl
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = weight) {
                            }
                            textView {
                                text = if (abuser) "YES" else "NO"
                                textColor = if (abuser) color2 else color1
                                textSize = fl
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = weight) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = getString(R.string.is_threat)
                                textSize = fl
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = weight) {
                            }
                            textView {
                                text = if (threat) "YES" else "NO"
                                textColor = if (threat) color2 else color1
                                textSize = fl
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = weight) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = getString(R.string.is_bogon)
                                textSize = fl
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = weight) {
                            }
                            textView {
                                text = if (bogon) "YES" else "NO"
                                textColor = if (bogon) color2 else color1
                                textSize = fl
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = weight) {
                            }
                        }
                        gravity = Gravity.CENTER
                        padding = dip(40)
                    }
                }
            }.show()
        }
    }

    override fun onSelected(selectedIds: ArrayList<Int>, selectedNames: ArrayList<String>, dataString: String) {
        flags.clear()
        val strings = resources.getStringArray(R.array.pref_country_values)
        selectedIds.forEach { index ->
            flags.add(strings[index])
        }

        onCameraIdle()
    }

    override fun onCancel() {
    }

    @MainThread
    fun positionAndFlagForSelectedMarker(): Pair<Coordinate?, String?> {
        if (mMap != null && markers.size != 0) {
            markers.forEach { (key, value) ->
                if (value.zIndex == 1.0f) {
                    return Pair(Coordinate(key.latitude, key.longitude), value.tag.toString())
                }
            }
        }

        return Pair(null, null)
    }

    @MainThread
    @Suppress("MagicNumber")
    fun updateMasterMarker(show: Boolean = false) {
        fab1.isClickable = false
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val securityManager = SecurityManager.getInstance(requireContext())

        toolbar.showProgress(true)

        doAsync {
            val jsonObj = Companion.createGeoJson(networkInfo!!, preferences, securityManager)
            val jsonArr = Companion.jsonArray(requireContext(), R.raw.nordvpn, ".json")

            uiThread {
                toolbar.hideProgress(true)
                mMap?.let { executeAnimation(jsonObj, jsonArr, false) }

                if (show && jsonObj != null) {
                    val flag = jsonObj.getString("flag").toUpperCase(Locale.ROOT)
                    //val country = jsonObj.getString("country")
                    val city = jsonObj.getString("city")
                    //val lat = jsonObj.getDouble("latitude")
                    //val lon = jsonObj.getDouble("longitude")
                    val ip = jsonObj.getString("ip")
                    val userMessage = UserMessage.Builder()
                        .with(requireContext())
                        .setBackgroundColor(R.color.accent_material_indigo_200)
                        .setTextColor(android.R.color.white)
                        .setMessage("Connected to $city, $flag ($ip)")
                        .setDuration(5000)
                        .setShowInterpolator(AccelerateInterpolator())
                        .setDismissInterpolator(AccelerateInterpolator())
                        .build()

                    minibarView.translationZ = 0.0f
                    minibarView.show(userMessage)
                    //jsonObj?.let { jsonObject -> showThreats(jsonObject) }
                }
            }
        }
    }
}