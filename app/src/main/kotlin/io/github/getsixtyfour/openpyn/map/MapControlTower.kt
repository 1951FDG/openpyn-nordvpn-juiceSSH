package io.github.getsixtyfour.openpyn.map

import android.content.Context
import android.view.View
import android.view.View.OnClickListener
import androidx.preference.PreferenceManager
import com.abdeveloper.library.MultiSelectDialog.SubmitCallbackListener
import com.abdeveloper.library.MultiSelectable
import com.androidmapsextensions.lazy.LazyMarker
import com.androidmapsextensions.lazy.LazyMarker.OnLevelChangeCallback
import com.antoniocarlon.map.CameraUpdateAnimator
import com.antoniocarlon.map.CameraUpdateAnimator.Animation
import com.antoniocarlon.map.CameraUpdateAnimator.AnimatorListener
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider
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
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.map.util.LazyMarkerStorage
import io.github.getsixtyfour.openpyn.utils.PrintArray
import io.github.getsixtyfour.openpyn.map.util.createGeoJson
import io.github.getsixtyfour.openpyn.map.util.createMarkers
import io.github.getsixtyfour.openpyn.map.util.createUserMessage
import io.github.getsixtyfour.openpyn.map.util.getCurrentPosition
import io.github.getsixtyfour.openpyn.map.util.jsonArray
import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate
import kotlinx.android.synthetic.main.fragment_map.view.map
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.map
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KLogging
import org.json.JSONArray
import org.json.JSONObject
import java.util.HashSet

@ControlTower
@RequireViews(MapViews::class)
@RequireScreen(MapFragment::class)
class MapControlTower : AbstractMapControlTower(), OnMapReadyCallback, OnMapLoadedCallback, OnCameraIdleListener, OnMapClickListener,
    OnMarkerClickListener, OnInfoWindowClickListener, SubmitCallbackListener, MapViewsAction, AnimatorListener,
    CoroutineScope by MainScope() {

    private val applicationContext: Context
        get() = screen.requireContext().applicationContext
    private val map by lazy { views.rootView.map }
    private val mHandler: CoroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
        // TODO: add dialog
        screen.toolBar?.hideProgress(true)
        logger.error(e) { "" }
    }
    private var mGoogleMap: GoogleMap? = null
    private var mCameraUpdateAnimator: CameraUpdateAnimator? = null
    private val mMarkerStorage by lazy { LazyMarkerStorage(FAVORITE_KEY) }

    //set by async
    private lateinit var mCountries: List<MultiSelectable>
    private lateinit var mJsonArray: JSONArray
    private lateinit var mTileProvider: MapBoxOfflineTileProvider
    private lateinit var mMarkers: HashMap<LatLng, LazyMarker>
    private lateinit var mFlags: HashSet<CharSequence>

    @OptIn(ExperimentalCoroutinesApi::class, ObsoleteCoroutinesApi::class)
    private val mSendChannel = actor<Context>(coroutineContext, Channel.RENDEZVOUS) {
        channel.map(IO) { createGeoJson(it) }.consumeEach { animateCamera(it) }
    }

    override fun onStarted() {
        super.onStarted()

        mGoogleMap?.let { map.onStart() }
    }

    override fun onResumed() {
        super.onResumed()

        mGoogleMap?.let { map.onResume() }
    }

    override fun onPause() {
        super.onPause()

        mGoogleMap?.let { map.onPause() }
    }

    override fun onStop() {
        super.onStop()

        mGoogleMap?.let { map.onStop() }
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()

        mCameraUpdateAnimator?.onDestroy()

        mGoogleMap?.let { map.onDestroy() }

        mSendChannel.close()

        if (::mTileProvider.isInitialized) {
            mTileProvider.close()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        mGoogleMap?.let { loadData() }
    }

    override fun onMapLoaded() {
        mCameraUpdateAnimator?.execute()
    }

    override fun onCameraIdle() {
        val bounds = (mGoogleMap ?: return).projection.visibleRegion.latLngBounds

        mMarkers.forEach { (key, value) ->
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
        mMarkers.entries.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            it.setLevel(it.level, onLevelChangeCallback)

            views.hideFavoriteButton()
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        if (marker.zIndex == 1.0f) {
            views.callConnectFabOnClick()
        } else {
            mMarkers.entries.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
                it.setLevel(it.level, onLevelChangeCallback)
            }

            mMarkers[marker.position]?.let {
                onLevelChangeCallback.onLevelChange(it, 10)

                views.toggleFavoriteButton(it.level == 1)
            }
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

        mCountries.let { mFlags = getCurrentFlags(it, selectedIds) }

        onCameraIdle()
    }

    override fun showCountryFilterDialog() {
        mCameraUpdateAnimator?.let {
            if (!it.isAnimating) {
                PrintArray.show("pref_country_values", screen.requireActivity())
            }
        }
    }

    override fun toggleCommand(v: View?) {
        (screen.requireActivity() as? OnClickListener)?.onClick(v)
    }

    override fun toggleFavoriteMarker() {
        mMarkers.entries.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
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

    override fun updateMasterMarkerWithDelay(timeMillis: Long) {
        launch {
            delay(timeMillis)
            mSendChannel.offer(applicationContext)
        }
    }

    override fun onAnimationStart() {
        views.setClickableButtons(false)
    }

    override fun onAnimationEnd() {
        views.setClickableButtons(true)
    }

    override fun onAnimationFinish(animation: Animation) {
        if (animation.isClosest) {
            views.fakeLayoutButtons()
            mMarkers[animation.target]?.let {
                if (mFlags.contains(it.tag)) {
                    onLevelChangeCallback.onLevelChange(it, 10)

                    if (!it.isVisible) it.isVisible = true
                    if (!it.isInfoWindowShown) it.showInfoWindow()

                    views.toggleFavoriteButton(it.level == 1)
                }
            }

            views.showAllButtons()
        } else {
            (animation.tag as? JSONObject)?.let {
                views.showMiniBar(createUserMessage(screen.requireActivity(), it).build())
                /*showThreats(screen.requireActivity(), it)*/
            }
        }
    }

    override fun onAnimationCancel(animation: Animation) {
        mMarkers[animation.target]?.let { logger.info { "Animation to $it canceled" } }
    }

    fun positionAndFlagForSelectedMarker(): Pair<Coordinate?, String> {
        var pair: Pair<Coordinate?, String> = Pair(null, "")

        mMarkers.entries.firstOrNull { it.value.zIndex == 1.0f }?.let {
            val latLng = it.key
            val tag = it.value.tag

            pair = when {
                mMarkers.count { entry -> entry.value.tag == tag } == 1 -> Pair(null, tag.toString())
                else -> Pair(Coordinate(latLng.latitude, latLng.longitude), tag.toString())
            }
        }

        return pair
    }

    fun onConnect() {
        updateMasterMarkerWithDelay(DELAY_MILLIS)
    }

    fun onDisconnect() {
        updateMasterMarkerWithDelay(DELAY_MILLIS)
    }

    fun onSessionCancelled() {
        views.toggleConnectButton(false)
    }

    fun onSessionStarted() {
        views.toggleConnectButton(true)

        views.hideListAndLocationButton()
        mMarkers.entries.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            if (it.isInfoWindowShown) it.hideInfoWindow()
            views.hideFavoriteButton()
        }
        mGoogleMap?.let {
            it.setOnInfoWindowClickListener(null)
            it.setOnMapClickListener(null)
            it.setOnMarkerClickListener { true }
            it.uiSettings?.isScrollGesturesEnabled = false
            it.uiSettings?.isZoomGesturesEnabled = false
        }
    }

    fun onSessionFinished() {
        views.toggleConnectButton(false)

        views.showListAndLocationButton()
        mMarkers.entries.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            if (!it.isInfoWindowShown) it.showInfoWindow()
            views.showFavoriteButton()
        }

        mGoogleMap?.let {
            it.setOnInfoWindowClickListener(this)
            it.setOnMapClickListener(this)
            it.setOnMarkerClickListener(this)
            it.uiSettings?.isScrollGesturesEnabled = true
            it.uiSettings?.isZoomGesturesEnabled = true
        }
    }

    private fun CoroutineScope.loadData() = launch(mHandler) {
        screen.toolBar?.showProgress(true)

        val animations: ArrayList<Animation> = withContext(IO) {
            val countries = async { countryList(applicationContext, R.raw.emojis) }
            val jsonArray = async { jsonArray(applicationContext, R.raw.nordvpn, ".json") }
            val tileProvider = async { fileBackedTileProvider() }
            val favorites = async { LazyMarkerStorage(FAVORITE_KEY).loadFavorites(applicationContext) }
            val jsonObj = async { createGeoJson(applicationContext) }

            mCountries = countries.await()
            mJsonArray = jsonArray.await()
            mTileProvider = tileProvider.await()
            val list = favorites.await()
            val json = jsonObj.await()

            val (hashSet, hashMap) = createMarkers(applicationContext, mJsonArray, mCountries, mGoogleMap!!, list, onLevelChangeCallback)
            mFlags = setUpPrintArray(applicationContext, mCountries, hashSet)
            mMarkers = hashMap
            val animations = getCameraUpdates()
            val latLng = getCurrentPosition(applicationContext, mFlags, json, mJsonArray)
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

    private fun loadMap(animations: ArrayList<Animation>) {
        mGoogleMap?.let {
            it.addTileOverlay(TileOverlayOptions().tileProvider(mTileProvider).fadeIn(false))
            it.setMaxZoomPreference(mTileProvider.maximumZoom)
            it.setMinZoomPreference(mTileProvider.minimumZoom)
            it.setOnInfoWindowClickListener(this)
            it.setOnMapClickListener(this)
            it.setOnMarkerClickListener(this)
            it.setOnMapLoadedCallback(this)

            /*val params = fab1.layoutParams as ConstraintLayout.LayoutParams
            it.setPadding(0, 0, 0, params.height + params.bottomMargin)*/

            it.uiSettings?.isScrollGesturesEnabled = true
            it.uiSettings?.isZoomGesturesEnabled = true

            mCameraUpdateAnimator = CameraUpdateAnimator(it, this, animations)
            mCameraUpdateAnimator?.animatorListener = this

            // Load map
            views.showMap()
            map.onResume()
        }
    }

    private fun animateCamera(json: JSONObject?, closest: Boolean = false, execute: Boolean = true) {
        // Check if not already animating
        mCameraUpdateAnimator?.let {
            if (!it.isAnimating) {
                val latLng = getCurrentPosition(applicationContext, mFlags, json, mJsonArray)
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
        }
    }

    companion object : KLogging() {
        private const val DELAY_MILLIS: Long = 10000
        private const val FAVORITE_KEY = "pref_favorites"
        val onLevelChangeCallback: OnLevelChangeCallback = object : OnLevelChangeCallback {
            val mDescriptor0: BitmapDescriptor by lazy { BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker_0) }
            val mDescriptor1: BitmapDescriptor by lazy { BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker_1) }
            val mDescriptor10: BitmapDescriptor by lazy { BitmapDescriptorFactory.fromResource(R.drawable.ic_map_marker_10) }

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
