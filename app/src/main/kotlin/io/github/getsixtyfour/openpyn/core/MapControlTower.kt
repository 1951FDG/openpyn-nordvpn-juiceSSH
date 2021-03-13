package io.github.getsixtyfour.openpyn.core

import android.content.Context
import android.view.View
import android.widget.Spinner
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.abdeveloper.library.MultiSelectDialog.SubmitCallbackListener
import com.abdeveloper.library.MultiSelectable
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.GoogleMap.OnMapLoadedCallback
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.TileOverlayOptions
import com.naver.android.svc.annotation.ControlTower
import com.naver.android.svc.annotation.RequireScreen
import com.naver.android.svc.annotation.RequireViews
import io.github.getsixtyfour.functions.countryList
import io.github.getsixtyfour.functions.createGeoJson
import io.github.getsixtyfour.functions.createMarkers
import io.github.getsixtyfour.functions.createUserMessage
import io.github.getsixtyfour.functions.dpToPxSize
import io.github.getsixtyfour.functions.fileBackedTileProvider
import io.github.getsixtyfour.functions.getCameraUpdates
import io.github.getsixtyfour.functions.getCurrentFlags
import io.github.getsixtyfour.functions.getCurrentPosition
import io.github.getsixtyfour.functions.jsonArray
import io.github.getsixtyfour.functions.onSettingsItemSelected
import io.github.getsixtyfour.functions.setUpPrintArray
import io.github.getsixtyfour.functions.showSystemUI
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.maps.CameraUpdateAnimator
import io.github.getsixtyfour.openpyn.maps.CameraUpdateAnimator.Animation
import io.github.getsixtyfour.openpyn.maps.CameraUpdateAnimator.AnimatorListener
import io.github.getsixtyfour.openpyn.maps.MapBoxOfflineTileProvider
import io.github.getsixtyfour.openpyn.maps.MaterialInfoWindowAdapter
import io.github.getsixtyfour.openpyn.model.LazyMarker
import io.github.getsixtyfour.openpyn.model.LazyMarker.OnLevelChangeCallback
import io.github.getsixtyfour.openpyn.moshi.LazyMarkerStorage
import io.github.getsixtyfour.openpyn.utils.NetworkInfo
import io.github.getsixtyfour.openpyn.utils.PrintArray
import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import org.json.JSONArray
import org.json.JSONObject
import java.util.HashSet
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@ControlTower
@RequireViews(MapViews::class)
@RequireScreen(MapFragment::class)
class MapControlTower : AbstractMapControlTower(), OnMapReadyCallback, OnMapLoadedCallback, OnCameraIdleListener, OnMapClickListener,
    OnMarkerClickListener, OnInfoWindowClickListener, SubmitCallbackListener, MapViewsAction, AnimatorListener {

    private val logger = KotlinLogging.logger {}

    private val applicationContext: Context
        get() = screen.requireContext().applicationContext

    // set by async
    private lateinit var mCountries: List<MultiSelectable>
    private lateinit var mFlags: HashSet<CharSequence>
    private lateinit var mJsonArray: JSONArray
    private lateinit var mTileProvider: MapBoxOfflineTileProvider
    private var mMarkers: HashMap<LatLng, LazyMarker>? = null

    // TODO: add dialog
    private val mExceptionHandler: CoroutineExceptionHandler by lazy {
        CoroutineExceptionHandler { _, e ->
            screen.toolBar?.hideProgress(true)
            logger.error(e) { "" }
        }
    }
    private val mMarkerStorage by lazy { LazyMarkerStorage(FAVORITE_KEY) }

    @OptIn(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class)
    private val mSendChannel by lazy {
        screen.lifecycleScope.actor<Context>(EmptyCoroutineContext, Channel.RENDEZVOUS) {
            channel.map(IO) { runCatching { withTimeout(GEO_IP_TIMEOUT_MILLIS) { createGeoJson(it) } }.getOrNull() }
                .consumeEach { it?.let { animateCamera(it) } }
        }
    }

    private var mGoogleMap: GoogleMap? = null
    private var mGoogleMapAnimator: CameraUpdateAnimator? = null

    override fun onStarted() {
        super.onStarted()

        mGoogleMap?.let { views.map.onStart() }
    }

    override fun onResumed() {
        super.onResumed()

        mGoogleMap?.let { views.map.onResume() }
    }

    override fun onPause() {
        super.onPause()

        mGoogleMap?.let { views.map.onPause() }
    }

    override fun onStop() {
        super.onStop()

        mGoogleMap?.let { views.map.onStop() }
    }

    override fun onDestroy() {
        super.onDestroy()

        mGoogleMapAnimator?.onDestroy()

        mGoogleMap?.let { views.map.onDestroy() }

        mSendChannel.close()

        if (::mTileProvider.isInitialized) {
            mTileProvider.close()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap.also { screen.lifecycleScope.loadData() }
    }

    override fun onMapLoaded() {
        // Load all map tiles
        mGoogleMapAnimator?.execute()
    }

    override fun onCameraIdle() {
        val bounds = (mGoogleMap ?: return).projection.visibleRegion.latLngBounds

        mMarkers?.forEach { (key, value) ->
            if (bounds.contains(key) && mFlags.contains(value.tag)) {
                if (!value.isVisible) value.isVisible = true
            } else {
                if (value.isVisible) value.isVisible = false

                if (value.zIndex == 1.0f) {
                    value.setLevel(value.level, onLevelChangeCallback)

                    views.hideFavoriteButton()
                }
            }
        }
    }

    override fun onMapClick(point: LatLng) {
        mMarkers?.entries?.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            it.setLevel(it.level, onLevelChangeCallback)

            views.hideFavoriteButton()
        }
    }

    @Suppress("MagicNumber")
    override fun onMarkerClick(marker: Marker): Boolean {
        if (marker.zIndex == 1.0f) {
            return false
        }

        mMarkers?.entries?.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            it.setLevel(it.level, onLevelChangeCallback)
        }

        mMarkers?.get(marker.position)?.let {
            onLevelChangeCallback.onLevelChange(it, 10)

            views.toggleFavoriteButton(it.level == 1)
        }

        return false
    }

    override fun onInfoWindowClick(marker: Marker) {
        if (marker.zIndex == 1.0f) {
            views.callConnectFabOnClick()
        }
    }

    override fun onCancel() {
    }

    override fun onSelected(selectedIds: ArrayList<Int>, selectedNames: ArrayList<String>, dataString: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (PrintArray.putListInt("pref_country_values", selectedIds, prefs).commit()) {
            PrintArray.checkedItemsList = selectedIds
        }

        mFlags = getCurrentFlags(mCountries, selectedIds)

        onCameraIdle()
    }

    override fun showCountryFilterDialog() {
        mGoogleMapAnimator?.let {
            if (!it.isAnimating) {
                PrintArray.show("pref_country_values", screen.requireActivity())
            }
        }
    }

    override fun toggleCommand(v: View) {
        (screen.requireActivity() as? View.OnClickListener)?.onClick(v)
    }

    override fun toggleFavoriteMarker() {
        mMarkers?.entries?.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            when (it.level) {
                0 -> {
                    it.setLevel(1, null)
                    mMarkerStorage.addFavorite(applicationContext, it)
                }
                1 -> {
                    it.setLevel(0, null)
                    mMarkerStorage.removeFavorite(applicationContext, it)
                }
            }
            views.toggleFavoriteButton((it.level == 1))
        }
    }

    override fun toggleJuiceSSH() {
        (screen.requireActivity().findViewById<Spinner>(R.id.spinner))?.performClick()
    }

    override fun toggleSettings() {
        onSettingsItemSelected(screen.requireActivity())
    }

    override fun updateMasterMarkerWithDelay(timeMillis: Long) {
        screen.lifecycleScope.launch {
            delay(timeMillis)
            if (!NetworkInfo.getInstance().isOnline()) return@launch
            mSendChannel.offer(applicationContext)
        }
    }

    override fun onAnimationStart() {
        views.setClickableButtons(false)
    }

    override fun onAnimationEnd() {
        views.setClickableButtons(true)
    }

    @Suppress("MagicNumber")
    override fun onAnimationFinish(animation: Animation) {
        if (animation.isClosest) {
            views.fakeLayoutButtons()

            screen.requireActivity().run {
                window.navigationBarColor = ContextCompat.getColor(this, R.color.navigationBarColor)
            }
            /*views.hideOverlayLayout()*/
            views.crossFadeOverlayLayout()

            mMarkers?.get(animation.target)?.let {
                if (mFlags.contains(it.tag)) {
                    onLevelChangeCallback.onLevelChange(it, 10)

                    if (!it.isVisible) it.isVisible = true
                    if (!it.isInfoWindowShown) it.showInfoWindow()

                    views.toggleFavoriteButton(it.level == 1)
                }
            }

            screen.requireActivity().run {
                showSystemUI(window, views.rootView)
            }

            views.showAllButtons()
        } else {
            (animation.tag as? JSONObject)?.let {
                views.showMiniBar(createUserMessage(screen.requireActivity(), it), SHOW_MINIBAR_DURATION_MILLIS)
                /*showThreats(screen.requireActivity(), it)*/
            }
        }
    }

    override fun onAnimationCancel(animation: Animation) {
        mMarkers?.get(animation.target)?.let { logger.info { "Animation to $it canceled" } }
    }

    fun positionAndFlagForSelectedMarker(): Pair<Coordinate?, String> {
        var pair: Pair<Coordinate?, String> = Pair(null, "")

        mMarkers?.entries?.firstOrNull { it.value.zIndex == 1.0f }?.let {
            val latLng = it.key
            val tag = it.value.tag

            pair = when {
                mMarkers?.count { entry -> entry.value.tag == tag } == 1 -> Pair(null, tag.toString())
                else -> Pair(Coordinate(latLng.latitude, latLng.longitude), tag.toString())
            }
        }

        return pair
    }

    fun onConnect() {
        updateMasterMarkerWithDelay(UPDATE_MARKER_DELAY_MILLIS)
    }

    fun onDisconnect() {
        updateMasterMarkerWithDelay(UPDATE_MARKER_DELAY_MILLIS)
    }

    fun onSessionCancelled() {
        views.toggleConnectButton(false)
    }

    fun onSessionStarted() {
        views.toggleConnectButton(true)

        views.hideAllExceptConnectAndFavoriteButton()
        mMarkers?.entries?.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            if (it.isInfoWindowShown) it.hideInfoWindow()
            views.hideFavoriteButton()
        }
        mGoogleMap?.apply {
            setOnInfoWindowClickListener(null)
            setOnMapClickListener(null)
            setOnMarkerClickListener { true }
            uiSettings.isScrollGesturesEnabled = false
            uiSettings.isZoomGesturesEnabled = false
        }
    }

    fun onSessionFinished() {
        views.toggleConnectButton(false)

        views.showAllExceptConnectAndFavoriteButton()
        mMarkers?.entries?.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            if (!it.isInfoWindowShown) it.showInfoWindow()
            views.showFavoriteButton()
        }

        mGoogleMap?.apply {
            setOnInfoWindowClickListener(this@MapControlTower)
            setOnMapClickListener(this@MapControlTower)
            setOnMarkerClickListener(this@MapControlTower)
            uiSettings.isScrollGesturesEnabled = true
            uiSettings.isZoomGesturesEnabled = true
        }
    }

    private fun CoroutineScope.loadData() = launch(mExceptionHandler) {
        screen.toolBar?.showProgress(true)

        val animations: ArrayList<Animation> = withContext(IO) {
            val countries = async { countryList(applicationContext, R.array.countries) }
            val jsonArray = async { jsonArray(applicationContext, R.raw.nordvpn, ".json") }
            val tileProvider = async { fileBackedTileProvider(applicationContext.getString(R.string.path_to_mbtiles)) }
            val favorites = async { LazyMarkerStorage(FAVORITE_KEY).loadFavorites(applicationContext) }
            val jsonObj = when {
                !NetworkInfo.getInstance().isOnline() -> {
                    null
                }
                else -> {
                    async { runCatching { withTimeout(GEO_IP_TIMEOUT_MILLIS) { createGeoJson(applicationContext) } }.getOrNull() }
                }
            }

            mCountries = countries.await()
            mJsonArray = jsonArray.await()
            mTileProvider = tileProvider.await()
            val list = favorites.await()
            val json = jsonObj?.await()

            val (hashSet, hashMap) = createMarkers(applicationContext, mJsonArray, mCountries, mGoogleMap!!, list, onLevelChangeCallback)
            mFlags = setUpPrintArray(applicationContext, mCountries, hashSet)
            mMarkers = hashMap
            val animations = getCameraUpdates()
            val latLng = getCurrentPosition(mFlags, json, mJsonArray)
            val animation = Animation(CameraUpdateFactory.newLatLng(latLng)).apply {
                isCallback = true
                isAnimate = true
                isClosest = true
                tag = json
                target = latLng
            }
            animations.add(animation)
            animations
        }

        screen.toolBar?.hideProgress(true)

        loadMap(animations)
    }

    @Suppress("MagicNumber")
    private fun loadMap(animations: ArrayList<Animation>) {
        val left = dpToPxSize(8F, applicationContext)
        val top = views.systemWindowInsetTop
        val right = dpToPxSize(8F, applicationContext)
        val bottom = views.systemWindowInsetBottom
        val adapter = MaterialInfoWindowAdapter(screen.requireActivity())
        val options = TileOverlayOptions().tileProvider(mTileProvider).fadeIn(false)
        val maxZoomPreference = mTileProvider.maxZoom
        val minZoomPreference = mTileProvider.minZoom

        mGoogleMap?.apply {
            addTileOverlay(options)
            setInfoWindowAdapter(adapter)
            setMaxZoomPreference(maxZoomPreference)
            setMinZoomPreference(minZoomPreference)
            setPadding(left, top, right, bottom)

            setOnInfoWindowClickListener(this@MapControlTower)
            setOnMapClickListener(this@MapControlTower)
            setOnMapLoadedCallback(this@MapControlTower)
            setOnMarkerClickListener(this@MapControlTower)

            uiSettings.isScrollGesturesEnabled = true
            uiSettings.isZoomGesturesEnabled = false
        }?.also {
            mGoogleMapAnimator = CameraUpdateAnimator(it, this, animations)
            mGoogleMapAnimator?.animatorListener = this
        }

        views.showMap()
    }

    private fun animateCamera(json: JSONObject, closest: Boolean = false, execute: Boolean = true) {
        mGoogleMapAnimator.let {
            if (it != null) {
                // Check if not already animating
                if (!it.isAnimating) {
                    val latLng = getCurrentPosition(mFlags, json, mJsonArray)
                    val animation = Animation(CameraUpdateFactory.newLatLng(latLng)).apply {
                        isCallback = true
                        isAnimate = true
                        isClosest = closest
                        tag = json
                        target = latLng
                    }
                    it.add(animation)
                    // Execute the animation
                    if (execute) it.execute()
                }
            } else {
                views.showMiniBar(createUserMessage(screen.requireActivity(), json), SHOW_MINIBAR_DURATION_MILLIS)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    companion object {

        private const val FAVORITE_KEY = "pref_favorites"
        private const val GEO_IP_TIMEOUT_MILLIS: Long = 600L
        private val SHOW_MINIBAR_DURATION_MILLIS: Long = 10.seconds.toLongMilliseconds()
        private val UPDATE_MARKER_DELAY_MILLIS: Long = 10.seconds.toLongMilliseconds()
        val onLevelChangeCallback: OnLevelChangeCallback = object : OnLevelChangeCallback {
            val mDescriptor0: BitmapDescriptor by lazy { BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker_0) }
            val mDescriptor1: BitmapDescriptor by lazy { BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker_1) }
            val mDescriptor10: BitmapDescriptor by lazy { BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker_10) }

            @Suppress("MagicNumber")
            override fun onLevelChange(marker: LazyMarker, level: Int) {
                when (level) {
                    0 -> {
                        marker.zIndex = 0.0f
                        marker.setIcon(mDescriptor0)
                    }
                    1 -> {
                        marker.zIndex = level.toFloat() / 10
                        marker.setIcon(mDescriptor1)
                    }
                    10 -> {
                        marker.zIndex = 1.0f
                        marker.setIcon(mDescriptor10)
                    }
                }
            }
        }
    }
}
