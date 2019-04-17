package io.github.getsixtyfour.openpyn.map

import android.Manifest.permission
import android.content.pm.PackageManager
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.AccelerateInterpolator
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.abdeveloper.library.MultiSelectable
import com.androidmapsextensions.lazy.LazyMarker
import com.androidmapsextensions.lazy.LazyMarker.OnLevelChangeCallback
import com.antoniocarlon.map.CameraUpdateAnimator
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider
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
import com.google.android.gms.tasks.Tasks
import com.mayurrokade.minibar.UserMessage
import com.naver.android.svc.annotation.ControlTower
import com.naver.android.svc.annotation.RequireScreen
import com.naver.android.svc.annotation.RequireViews
import com.vdurmont.emoji.EmojiFlagManager
import de.westnordost.countryboundaries.CountryBoundaries
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.security.SecurityManager
import io.github.getsixtyfour.openpyn.utilities.LazyMarkerStorage
import io.github.getsixtyfour.openpyn.utilities.PrintArray
import io.github.getsixtyfour.openpyn.utilities.SubmitCallbackListener
import io.github.getsixtyfour.openpyn.utilities.countryList
import io.github.getsixtyfour.openpyn.utilities.createGeoJson
import io.github.getsixtyfour.openpyn.utilities.getDefaultLatLng
import io.github.getsixtyfour.openpyn.utilities.getLatLng
import io.github.getsixtyfour.openpyn.utilities.jsonArray
import io.github.getsixtyfour.openpyn.utilities.logException
import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.alert
import org.jetbrains.anko.debug
import org.jetbrains.anko.dip
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.error
import org.jetbrains.anko.info
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.padding
import org.jetbrains.anko.textColor
import org.jetbrains.anko.textView
import org.jetbrains.anko.uiThread
import org.jetbrains.anko.verticalLayout
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * @author 1951FDG
 */
@ControlTower
@RequireViews(MapViews::class)
@RequireScreen(MapFragment::class)
@Suppress("LargeClass", "TooManyFunctions")
class MapControlTower : SVC_MapControlTower(),
    AnkoLogger,
    OnMapReadyCallback,
    GoogleMap.OnMapLoadedCallback,
    GoogleMap.OnCameraIdleListener,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMarkerClickListener,
    OnLevelChangeCallback,
    SubmitCallbackListener,
    MapViewsAction {

    private var cameraUpdateAnimator: CameraUpdateAnimator? = null
    private var countryBoundaries: CountryBoundaries? = null
    private val flags by lazy { ArrayList<String>() }
    private var mMap: GoogleMap? = null
    private val markers: HashMap<LatLng, LazyMarker> by lazy { HashMap<LatLng, LazyMarker>() }
    private val storage by lazy { LazyMarkerStorage(FAVORITE_KEY) }
    private var tileProvider: MapBoxOfflineTileProvider? = null
    override fun onCreated() {
    }

    override fun onDestroy() {
        super.onDestroy()

        tileProvider?.close()
    }

    @Suppress("ComplexMethod")
    override fun onMapReady(googleMap: GoogleMap) {
        screen.toolBar?.showProgress(true)

        mMap = googleMap
        cameraUpdateAnimator = CameraUpdateAnimator(googleMap, this)
        val preferences = PreferenceManager.getDefaultSharedPreferences(screen.requireContext())
        val favorites = storage.loadFavorites(screen.requireContext())
        val iconDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.map1)
        val securityManager = SecurityManager.getInstance(screen.requireContext())

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
                setHint(R.string.multi_select_dialog_hint)
                setTitle(R.string.empty)
                setItems(items)
                setCheckedItems(checkedItems)
            }
        }

        fun countryBoundaries(): CountryBoundaries? {
            try {
                return CountryBoundaries.load(screen.requireContext().assets.open("boundaries.ser"))
            } catch (e: FileNotFoundException) {
                logException(e)
            } catch (e: IOException) {
                logException(e)
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
            // todo benchmarking, coroutines?
            val jsonArray = jsonArray(screen.requireContext(), R.raw.nordvpn, ".json")
            val stringArray = screen.resources.getStringArray(R.array.pref_country_values)
            val textArray = screen.resources.getTextArray(R.array.pref_country_entries)
            val countries = countryList(textArray)
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

                operator fun JSONArray.iterator(): Iterator<JSONObject> =
                    (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

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
                            //todo check python code again
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
                    logException(Exception(element))
                    error(element)
                }
            }
            // Load all map tiles
            @Suppress("MagicNumber")
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
            val jsonObj = createGeoJson(preferences, securityManager)
            val latLng = getCurrentPosition(jsonObj, jsonArray)

            animateCamera(latLng, closest = true, animate = false)

            uiThread {
                screen.toolBar?.hideProgress(true)

                googleMap.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider).fadeIn(false))

                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(screen.requireContext(), R.raw.style_json))
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
                // Load map
                views.showMap()
            }
        }
    }

    override fun onMapLoaded() {
        // Execute the animation and set the final OnCameraIdleListener
        cameraUpdateAnimator?.execute()

        debug(mMap!!.minZoomLevel)
        debug(mMap!!.maxZoomLevel)
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

                    views.hideFavoriteFab()
                }
            }
        }
    }

    override fun onMapClick(p0: LatLng?) {
        markers.forEach { (_, value) ->
            if (value.zIndex == 1.0f) {
                value.setLevel(value.level, this)

                views.hideFavoriteFab()
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

            views.toggleFavoriteFab((markers[p0.position]?.level == 1))
        }

        views.showFavoriteFab()

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

    override fun onCancel() {
    }

    override fun onSelected(selectedIds: ArrayList<Int>, selectedNames: ArrayList<String>, dataString: String) {
        flags.clear()
        val strings = screen.resources.getStringArray(R.array.pref_country_values)
        selectedIds.forEach { index ->
            flags.add(strings[index])
        }

        onCameraIdle()
    }

    override fun showCountryFilterDialog() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(screen.requireContext())
        PrintArray.show("pref_country_values", (screen.requireActivity() as AppCompatActivity), preferences, this)
    }

    override fun toggleCommand(v: View?) {
        val listener = screen.requireActivity() as? OnClickListener

        listener?.onClick(v)
    }

    override fun toggleFavoriteMarker() {
        fun toggleLevel(value: LazyMarker) {
            when (value.level) {
                0 -> {
                    value.setLevel(1, null)
                    storage.addFavorite(screen.requireContext(), value)
                }
                1 -> {
                    value.setLevel(0, null)
                    storage.removeFavorite(screen.requireContext(), value)
                }
            }
        }

        if (mMap != null && markers.size != 0) {
            markers.forEach { (_, value) ->
                if (value.zIndex == 1.0f) {
                    toggleLevel(value)
                }
            }
        }
    }

    @MainThread
    @Suppress("MagicNumber")
    override fun updateMasterMarker(show: Boolean) {
        //todo check all clickable status
        views.setClickableLocationFab(false)
        val preferences = PreferenceManager.getDefaultSharedPreferences(screen.requireContext())
        val securityManager = SecurityManager.getInstance(screen.requireContext())

        screen.toolBar?.showProgress(true)

        doAsync {
            val jsonObj = createGeoJson(preferences, securityManager)
            val latLng = getCurrentPosition(jsonObj)

            uiThread {
                screen.toolBar?.hideProgress(true)

                animateCamera(latLng, closest = false)

                if (show && jsonObj != null) {
                    val flag = jsonObj.getString("flag").toUpperCase(Locale.ROOT)
                    //val country = jsonObj.getString("country")
                    val city = jsonObj.getString("city")
                    //val lat = jsonObj.getDouble("latitude")
                    //val lon = jsonObj.getDouble("longitude")
                    val ip = jsonObj.getString("ip")
                    val userMessage = UserMessage.Builder()
                        .with(screen.requireContext())
                        .setBackgroundColor(R.color.accent_material_indigo_200)
                        .setTextColor(android.R.color.white)
                        .setMessage("Connected to $city, $flag ($ip)")
                        .setDuration(5000)
                        .setShowInterpolator(AccelerateInterpolator())
                        .setDismissInterpolator(AccelerateInterpolator())
                        .build()

                    views.showMiniBar(userMessage)
                    //jsonObj?.let { jsonObject -> showThreats(jsonObject) }
                }
            }
        }
    }

    fun onSessionFinished() {
        views.setClickableConnectFab(true)
        views.setAppearanceConnectFab(false)

        views.showListAndLocationFab()
        markers.forEach { (_, value) ->
            if (value.zIndex == 1.0f) views.showFavoriteFab()
        }

        mMap?.setOnMapClickListener(this)
        mMap?.setOnMarkerClickListener(this)
        mMap?.uiSettings?.isScrollGesturesEnabled = true
        mMap?.uiSettings?.isZoomGesturesEnabled = true
    }

    fun onSessionStarted() {
        views.setClickableConnectFab(true)
        views.setAppearanceConnectFab(true)

        views.hideListAndLocationFab()
        markers.forEach { (_, value) ->
            if (value.zIndex == 1.0f) views.hideFavoriteFab()
        }

        mMap?.setOnMapClickListener(null)
        mMap?.setOnMarkerClickListener { true }
        mMap?.uiSettings?.isScrollGesturesEnabled = false
        mMap?.uiSettings?.isZoomGesturesEnabled = false
    }

    fun onSessionCancelled() {
        views.setClickableConnectFab(true)
    }

    @MainThread
    fun positionAndFlagForSelectedMarker(): Pair<Coordinate?, String> {
        if (mMap != null && markers.size != 0) {
            markers.forEach { (key, value) ->
                if (value.zIndex == 1.0f) {
                    return Pair(Coordinate(key.latitude, key.longitude), value.tag.toString())
                }
            }
        }

        return Pair(null, "")
    }

    @Suppress("ComplexMethod")
    private fun animateCamera(latLng: LatLng, closest: Boolean, animate: Boolean = true) {
        info(latLng.toString())

        fun onStart() {
            views.setClickableFabs(false)
        }

        fun onEnd() {
            views.setClickableFabs(true)
        }

        fun onFinish() {
            markers.forEach { (key, value) ->
                val level = value.level
                if (key == latLng) {
                    if (!value.isVisible) value.isVisible = true
                    if (!value.isInfoWindowShown) value.showInfoWindow()

                    value.zIndex = 1.0f
                    value.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.map0))

                    views.toggleFavoriteFab((level == 1))

                    views.showFavoriteFab()
                } else {
                    if (value.zIndex == 1.0f) {
                        //if (value.isInfoWindowShown) value.hideInfoWindow()
                        value.setLevel(level, null)
                        onLevelChange(value, level)
                    }
                }
            }

            views.showAllFabs()
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

    @MainThread
    @Suppress("ComplexMethod")
    private fun getCurrentPosition(jsonObj: JSONObject?, jsonArr: JSONArray? = null): LatLng {
        fun latLng(flag: String, lat: Double, lon: Double): LatLng = when {
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

        fun getFLag(lon: Double, lat: Double): String {
            var t = System.nanoTime()
            val ids = countryBoundaries?.getIds(lon, lat)
            t = System.nanoTime() - t
            @Suppress("MagicNumber")
            val i = 1000
            debug(getToastString(ids) + " (in " + "%.3f".format(t / i / i.toFloat()) + "ms)")
            return getFlag(ids)
        }

        var latLng = getDefaultLatLng()

        when {
            jsonObj != null -> {
                val lat = jsonObj.getDouble("latitude")
                val lon = jsonObj.getDouble("longitude")
                val flag = jsonObj.getString("flag")
                latLng = latLng(flag, lat, lon)
            }
            jsonArr != null -> screen.lastLocation?.let {
                val lat = it.latitude
                val lon = it.longitude
                val flag = getFLag(lon, lat)
                latLng = latLng(flag, lat, lon)
            }
            ActivityCompat.checkSelfPermission(
                screen.requireContext(),
                permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            -> {
                val task = FusedLocationProviderClient(screen.requireContext()).lastLocation
                try {
                    // Block on the task for a maximum of 500 milliseconds, otherwise time out.
                    val location = Tasks.await(task, TASK_TIMEOUT, TimeUnit.MILLISECONDS)
                    val lat = location.latitude
                    val lon = location.longitude
                    val flag = getFLag(lon, lat)
                    latLng = latLng(flag, lat, lon)
                } catch (e: ExecutionException) {
                    error(e)
                } catch (e: InterruptedException) {
                    logException(e)
                } catch (e: TimeoutException) {
                    error(e)
                }
            }
        }

        return latLng
    }

    @Suppress("ComplexMethod", "MagicNumber", "unused")
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
            val color1 = ActivityCompat.getColor(screen.requireContext(), R.color.colorConnect)
            val color2 = ActivityCompat.getColor(screen.requireContext(), R.color.colorDisconnect)
            val fl = 22f
            val weight = 1.0f
            with(screen.requireContext()) {
                alert {
                    customView =
                        verticalLayout {
                            linearLayout {
                                textView {
                                    text = getString(R.string.is_tor)
                                    textSize = fl
                                    gravity = android.view.Gravity.START
                                }.lparams(
                                    width = org.jetbrains.anko.wrapContent,
                                    height = org.jetbrains.anko.wrapContent,
                                    weight = weight
                                ) {
                                }
                                textView {
                                    text = if (tor) "YES" else "NO"
                                    textColor = if (tor) color2 else color1
                                    textSize = fl
                                    gravity = android.view.Gravity.END
                                }.lparams(
                                    width = org.jetbrains.anko.wrapContent,
                                    height = org.jetbrains.anko.wrapContent,
                                    weight = weight
                                ) {
                                }
                            }
                            linearLayout {
                                textView {
                                    text = getString(R.string.is_proxy)
                                    textSize = fl
                                    gravity = android.view.Gravity.START
                                }.lparams(
                                    width = org.jetbrains.anko.wrapContent,
                                    height = org.jetbrains.anko.wrapContent,
                                    weight = weight
                                ) {
                                }
                                textView {
                                    text = if (proxy) "YES" else "NO"
                                    textColor = if (proxy) color2 else color1
                                    textSize = fl
                                    gravity = android.view.Gravity.END
                                }.lparams(
                                    width = org.jetbrains.anko.wrapContent,
                                    height = org.jetbrains.anko.wrapContent,
                                    weight = weight
                                ) {
                                }
                            }
                            linearLayout {
                                textView {
                                    text = getString(R.string.is_anonymous)
                                    textSize = fl
                                    gravity = android.view.Gravity.START
                                }.lparams(
                                    width = org.jetbrains.anko.wrapContent,
                                    height = org.jetbrains.anko.wrapContent,
                                    weight = weight
                                ) {
                                }
                                textView {
                                    text = if (anonymous) "YES" else "NO"
                                    textColor = if (anonymous) color2 else color1
                                    textSize = fl
                                    gravity = android.view.Gravity.END
                                }.lparams(
                                    width = org.jetbrains.anko.wrapContent,
                                    height = org.jetbrains.anko.wrapContent,
                                    weight = weight
                                ) {
                                }
                            }
                            linearLayout {
                                textView {
                                    text = getString(R.string.is_known_attacker)
                                    textSize = fl
                                    gravity = android.view.Gravity.START
                                }.lparams(
                                    width = org.jetbrains.anko.wrapContent,
                                    height = org.jetbrains.anko.wrapContent,
                                    weight = weight
                                ) {
                                }
                                textView {
                                    text = if (attacker) "YES" else "NO"
                                    textColor = if (attacker) color2 else color1
                                    textSize = fl
                                    gravity = android.view.Gravity.END
                                }.lparams(
                                    width = org.jetbrains.anko.wrapContent,
                                    height = org.jetbrains.anko.wrapContent,
                                    weight = weight
                                ) {
                                }
                            }
                            linearLayout {
                                textView {
                                    text = getString(R.string.is_known_abuser)
                                    textSize = fl
                                    gravity = android.view.Gravity.START
                                }.lparams(
                                    width = org.jetbrains.anko.wrapContent,
                                    height = org.jetbrains.anko.wrapContent,
                                    weight = weight
                                ) {
                                }
                                textView {
                                    text = if (abuser) "YES" else "NO"
                                    textColor = if (abuser) color2 else color1
                                    textSize = fl
                                    gravity = android.view.Gravity.END
                                }.lparams(
                                    width = org.jetbrains.anko.wrapContent,
                                    height = org.jetbrains.anko.wrapContent,
                                    weight = weight
                                ) {
                                }
                            }
                            linearLayout {
                                textView {
                                    text = getString(R.string.is_threat)
                                    textSize = fl
                                    gravity = android.view.Gravity.START
                                }.lparams(
                                    width = org.jetbrains.anko.wrapContent,
                                    height = org.jetbrains.anko.wrapContent,
                                    weight = weight
                                ) {
                                }
                                textView {
                                    text = if (threat) "YES" else "NO"
                                    textColor = if (threat) color2 else color1
                                    textSize = fl
                                    gravity = android.view.Gravity.END
                                }.lparams(
                                    width = org.jetbrains.anko.wrapContent,
                                    height = org.jetbrains.anko.wrapContent,
                                    weight = weight
                                ) {
                                }
                            }
                            linearLayout {
                                textView {
                                    text = getString(R.string.is_bogon)
                                    textSize = fl
                                    gravity = android.view.Gravity.START
                                }.lparams(
                                    width = org.jetbrains.anko.wrapContent,
                                    height = org.jetbrains.anko.wrapContent,
                                    weight = weight
                                ) {
                                }
                                textView {
                                    text = if (bogon) "YES" else "NO"
                                    textColor = if (bogon) color2 else color1
                                    textSize = fl
                                    gravity = android.view.Gravity.END
                                }.lparams(
                                    width = org.jetbrains.anko.wrapContent,
                                    height = org.jetbrains.anko.wrapContent,
                                    weight = weight
                                ) {
                                }
                            }
                            gravity = android.view.Gravity.CENTER
                            padding = dip(40)
                        }
                }.show()
            }
        }
    }

    companion object {
        private const val FAVORITE_KEY = "pref_favorites"
        private const val TASK_TIMEOUT: Long = 500
    }
}
