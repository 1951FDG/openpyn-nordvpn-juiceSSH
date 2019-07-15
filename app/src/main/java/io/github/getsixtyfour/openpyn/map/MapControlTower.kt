package io.github.getsixtyfour.openpyn.map

import android.Manifest.permission
import android.content.pm.PackageManager
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.AccelerateInterpolator
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.abdeveloper.library.MultiSelectable
import com.androidmapsextensions.lazy.LazyMarker
import com.androidmapsextensions.lazy.LazyMarker.OnLevelChangeCallback
import com.antoniocarlon.map.CameraUpdateAnimator
import com.antoniocarlon.map.CameraUpdateAnimator.Animation
import com.antoniocarlon.map.CameraUpdateAnimator.AnimatorListener
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
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
import io.github.getsixtyfour.openpyn.utilities.CATEGORIES
import io.github.getsixtyfour.openpyn.utilities.COUNTRY
import io.github.getsixtyfour.openpyn.utilities.DEDICATED
import io.github.getsixtyfour.openpyn.utilities.DOUBLE
import io.github.getsixtyfour.openpyn.utilities.FLAG
import io.github.getsixtyfour.openpyn.utilities.LAT
import io.github.getsixtyfour.openpyn.utilities.LOCATION
import io.github.getsixtyfour.openpyn.utilities.LONG
import io.github.getsixtyfour.openpyn.utilities.LazyMarkerStorage
import io.github.getsixtyfour.openpyn.utilities.MultiSelectModelExtra
import io.github.getsixtyfour.openpyn.utilities.NAME
import io.github.getsixtyfour.openpyn.utilities.OBFUSCATED
import io.github.getsixtyfour.openpyn.utilities.ONION
import io.github.getsixtyfour.openpyn.utilities.P2P
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
import kotlin.math.pow

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
    GoogleMap.OnInfoWindowClickListener,
    OnLevelChangeCallback,
    SubmitCallbackListener,
    MapViewsAction,
    AnimatorListener {

    private var cameraUpdateAnimator: CameraUpdateAnimator? = null
    private var countryBoundaries: CountryBoundaries? = null
    private val flags by lazy { ArrayList<CharSequence>() }
    private var mMap: GoogleMap? = null
    private var markers: HashMap<LatLng, LazyMarker>? = HashMap()
    private val storage by lazy { LazyMarkerStorage(FAVORITE_KEY) }
    private var tileProvider: MapBoxOfflineTileProvider? = null
    private var countries: java.util.ArrayList<MultiSelectable>? = null

    override fun onDestroy() {
        super.onDestroy()
        mMap?.clear()
        cameraUpdateAnimator?.onDestroy()
        cameraUpdateAnimator = null
        countryBoundaries = null
        countries = null
        markers = null
        tileProvider?.close()
        tileProvider = null
        mMap = null

        PrintArray.onDestroy()
    }

    @Suppress("ComplexMethod")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        cameraUpdateAnimator = CameraUpdateAnimator(googleMap, this)
        cameraUpdateAnimator?.animatorListener = this
        val preferences = PreferenceManager.getDefaultSharedPreferences(screen.requireContext())
        val favorites = storage.loadFavorites(screen.requireContext())
        val iconDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.map1)
        val securityManager = SecurityManager.getInstance(screen.requireContext())

        fun selectedCountries(list: ArrayList<MultiSelectable>): ArrayList<Int> {
            val preSelectedIdsList = ArrayList<Int>()
            list.forEach {
                preSelectedIdsList.add(it.id)
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

        screen.toolBar?.showProgress(true)

        doAsync {
            // todo benchmarking, coroutines?
            val jsonArray = jsonArray(screen.requireContext(), R.raw.nordvpn, ".json")
            val stringArray = screen.resources.getStringArray(R.array.pref_country_values)
            val textArray = screen.resources.getTextArray(R.array.pref_country_entries)
            
            val netflix = preferences.getBoolean("pref_netflix", false)
            val dedicated = preferences.getBoolean("pref_dedicated", false)
            val double = preferences.getBoolean("pref_double", false)
            val obfuscated = preferences.getBoolean("pref_anti_ddos", false)
            val onion = preferences.getBoolean("pref_tor", false)
            val p2p = preferences.getBoolean("pref_p2p", false)

            tileProvider = tileProvider()

            countryBoundaries = countryBoundaries()

            val emojiManager = EmojiFlagManager()
            emojiManager.load("/assets/emojis.json")

            countries = countryList(textArray)
            val selectedCountries = selectedCountries(countries!!)

            printArray(countries!!, selectedCountries)

            countries!!.forEach { selectable: MultiSelectable ->
                (selectable as? MultiSelectModelExtra)?.let {
                    if (selectedCountries.contains(it.id)) {
                        flags.add(it.tag)
                    }
                }
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
                    val emoji = emojiManager.getForAlias(input)
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

                fun logDifference(set: Set<String>, string: String) {
                    set.forEach {
                        val message = "$string $it"
                        logException(Exception(message))
                        error(message)
                    }
                }

                operator fun JSONArray.iterator(): Iterator<JSONObject> =
                    (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

                val set1 = stringArray.toHashSet()
                val set2 = hashSetOf<String>()

                for (res in jsonArray) {
                    val flag = res.getString(FLAG)
                    set2.add(flag)

                    var pass = when {
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
                            //todo check python code again
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
                    }
                    val country = res.getString(COUNTRY)
                    val emoji = parseToUnicode(flag)
                    val location = res.getJSONObject(LOCATION)
                    val latLng = LatLng(location.getDouble(LAT), location.getDouble(LONG))
                    val options = MarkerOptions().apply {
                        flat(true)
                        position(latLng)
                        title("$emoji $country")
                        visible(false)
                        icon(iconDescriptor)
                    }

                    markers?.set(latLng, lazyMarker(options, flag))
                }

                // Log old countries, if any
                logDifference(set1.subtract(set2), "old")
                // Log new countries, if any
                logDifference(set2.subtract(set1), "new")
            }
            // Load all map tiles
            @Suppress("MagicNumber") val z = 3
            //val z = tileProvider!!.minimumZoom.toInt()
            val rows = 2.0.pow(z.toDouble()).toInt() - 1
            // Traverse through all rows
            for (y in 0..rows) {
                for (x in 0..rows) {
                    val bounds = MapBoxOfflineTileProvider.calculateTileBounds(x, y, z)
                    val cameraPosition = CameraPosition.Builder().target(bounds.northeast).build()
                    // Add animations
                    val animation = Animation(CameraUpdateFactory.newCameraPosition(cameraPosition))
                    cameraUpdateAnimator?.add(animation)
                }
            }
            val jsonObj = createGeoJson(preferences, securityManager)
            val latLng = getCurrentPosition(jsonObj, jsonArray)

            uiThread {
                screen.toolBar?.hideProgress(true)

                animateCamera(latLng, closest = true, execute = false)

                googleMap.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider).fadeIn(false))

                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(screen.requireContext(), R.raw.style_json))
                googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                googleMap.setMaxZoomPreference(tileProvider!!.maximumZoom)
                googleMap.setMinZoomPreference(tileProvider!!.minimumZoom)

                googleMap.setOnInfoWindowClickListener(it)
                googleMap.setOnMapClickListener(it)
                googleMap.setOnMarkerClickListener(it)

                googleMap.setOnMapLoadedCallback(it)

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

        markers?.forEach { (key, value) ->
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
        markers?.entries?.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            it.setLevel(it.level, this)

            views.hideFavoriteFab()
        }
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        if (p0 != null) {
            if (p0.zIndex == 1.0f) {
                views.callConnectFabOnClick()
            } else {
                markers?.entries?.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
                    it.setLevel(it.level, this)
                }

                markers?.get(p0.position)?.let {
                    it.zIndex = 1.0f
                    it.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.map0))

                    views.toggleFavoriteFab(it.level == 1)
                }
            }
        }

        return false
    }

    override fun onInfoWindowClick(p0: Marker?) {
        if (p0 != null) {
            if (p0.zIndex == 1.0f) {
                views.callConnectFabOnClick()
            }
        }
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
        countries?.forEach { selectable: MultiSelectable ->
            (selectable as? MultiSelectModelExtra)?.let {
                if (selectedIds.contains(it.id)) {
                    flags.add(it.tag)
                }
            }
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

        markers?.entries?.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            toggleLevel(it)
            views.toggleFavoriteFab((it.level == 1))
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
                        .with(screen.requireContext().applicationContext)
                        .setBackgroundColor(R.color.accent_material_indigo_200)
                        .setTextColor(android.R.color.white)
                        .setMessage("Connected to $city, $flag ($ip)")
                        .setDuration(7000)
                        .setShowInterpolator(AccelerateInterpolator())
                        .setDismissInterpolator(AccelerateInterpolator())
                        .build()

                    views.showMiniBar(userMessage)
                    //jsonObj?.let { jsonObject -> showThreats(jsonObject) }
                }
            }
        }
    }

    override fun onAnimationStart() {
        views.setClickableFabs(false)
    }

    override fun onAnimationEnd() {
        views.setClickableFabs(true)
    }

    override fun onAnimationFinish(animation: Animation) {
        views.fakeLayoutAllFabs()

        markers?.get(animation.target)?.let {
            if (flags.contains(it.tag)) {
                it.zIndex = 1.0f
                it.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.map0))

                if (!it.isVisible) it.isVisible = true
                if (!it.isInfoWindowShown) it.showInfoWindow()

                views.toggleFavoriteFab(it.level == 1)
            }
        }

        views.showAllFabs()
    }

    override fun onAnimationCancel(animation: Animation) {
        markers?.get(animation.target)?.let {
            info("Animation to $it canceled")
        }
    }

    fun onSessionFinished() {
        info("onSessionFinished")
        views.setClickableConnectFab(true)
        views.toggleConnectFab(false)

        views.showListAndLocationFab()
        markers?.entries?.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            if (!it.isInfoWindowShown) it.showInfoWindow()
            views.showFavoriteFab()
        }

        mMap?.setOnInfoWindowClickListener(this)
        mMap?.setOnMapClickListener(this)
        mMap?.setOnMarkerClickListener(this)
        mMap?.uiSettings?.isScrollGesturesEnabled = true
        mMap?.uiSettings?.isZoomGesturesEnabled = true
    }

    fun onSessionStarted() {
        info("onSessionStarted")
        views.setClickableConnectFab(true)
        views.toggleConnectFab(true)

        views.hideListAndLocationFab()
        markers?.entries?.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            if (it.isInfoWindowShown) it.hideInfoWindow()
            views.hideFavoriteFab()
        }

        mMap?.setOnInfoWindowClickListener(null)
        mMap?.setOnMapClickListener(null)
        mMap?.setOnMarkerClickListener { true }
        mMap?.uiSettings?.isScrollGesturesEnabled = false
        mMap?.uiSettings?.isZoomGesturesEnabled = false
    }

    fun onSessionCancelled() {
        info("onSessionCancelled")
        views.setClickableConnectFab(true)
        views.toggleConnectFab(false)
    }

    @MainThread
    fun positionAndFlagForSelectedMarker(): Pair<Coordinate?, String> {
        var pair: Pair<Coordinate?, String> = Pair(null, "")

        markers?.entries?.firstOrNull { it.value.zIndex == 1.0f }?.let {
            val latLng = it.key
            val tag = it.value.tag

            pair = when {
                markers?.count { entry -> entry.value.tag == tag } == 1 -> Pair(null, tag.toString())
                else -> Pair(Coordinate(latLng.latitude, latLng.longitude), tag.toString())
            }
        }

        return pair
    }

    @Suppress("ComplexMethod")
    private fun animateCamera(latLng: LatLng, closest: Boolean, execute: Boolean = true) {
        val animation = Animation(CameraUpdateFactory.newLatLng(latLng)).apply {
            isAnimate = true
            isClosest = closest
            target = latLng
        }
        cameraUpdateAnimator?.add(animation)
        // Execute the animation and set the final OnCameraIdleListener
        if (execute) cameraUpdateAnimator?.execute()
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
            @Suppress("MagicNumber") val i = 1000
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
            ContextCompat.checkSelfPermission(
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
            val color1 = ContextCompat.getColor(screen.requireContext(), R.color.colorConnect)
            val color2 = ContextCompat.getColor(screen.requireContext(), R.color.colorDisconnect)
            val fl = 22f
            val weight = 1.0f
            with(screen.requireContext()) {
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

    companion object {
        private const val FAVORITE_KEY = "pref_favorites"
        private const val TASK_TIMEOUT: Long = 500
    }
}
