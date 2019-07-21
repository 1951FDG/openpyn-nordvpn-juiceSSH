package io.github.getsixtyfour.openpyn.map

import android.content.Context
import android.view.View
import android.view.View.OnClickListener
import androidx.annotation.MainThread
import com.abdeveloper.library.MultiSelectable
import com.androidmapsextensions.lazy.LazyMarker
import com.androidmapsextensions.lazy.LazyMarker.OnLevelChangeCallback
import com.antoniocarlon.map.CameraUpdateAnimator
import com.antoniocarlon.map.CameraUpdateAnimator.Animation
import com.antoniocarlon.map.CameraUpdateAnimator.AnimatorListener
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.TileOverlayOptions
import com.naver.android.svc.annotation.ControlTower
import com.naver.android.svc.annotation.RequireScreen
import com.naver.android.svc.annotation.RequireViews
import com.vdurmont.emoji.EmojiFlagManager
import de.westnordost.countryboundaries.CountryBoundaries
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.utilities.LazyMarkerStorage
import io.github.getsixtyfour.openpyn.utilities.PrintArray
import io.github.getsixtyfour.openpyn.utilities.SubmitCallbackListener
import io.github.getsixtyfour.openpyn.utilities.countryList
import io.github.getsixtyfour.openpyn.utilities.createGeoJson
import io.github.getsixtyfour.openpyn.utilities.jsonArray
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnCommandExecuteListener
import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.coroutines.experimental.asReference
import org.jetbrains.anko.debug
import org.jetbrains.anko.info
import org.json.JSONArray
import org.json.JSONObject
import java.util.HashSet

/**
 * @author 1951FDG
 */
@ControlTower
@RequireViews(MapViews::class)
@RequireScreen(MapFragment::class)
class MapControlTower : SVC_MapControlTower(), AnkoLogger, OnMapReadyCallback, GoogleMap.OnMapLoadedCallback,
    GoogleMap.OnCameraIdleListener, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener,
    OnLevelChangeCallback, SubmitCallbackListener, MapViewsAction, AnimatorListener, OnCommandExecuteListener,
    CoroutineScope by MainScope() {

    private lateinit var markers: HashMap<LatLng, LazyMarker>
    private lateinit var flags: HashSet<CharSequence>
    private lateinit var mMap: GoogleMap
    private var mCameraUpdateAnimator: CameraUpdateAnimator? = null

    override fun onConnect() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private lateinit var iconDescriptor0: BitmapDescriptor
    private lateinit var iconDescriptor1: BitmapDescriptor
    private lateinit var iconDescriptor10: BitmapDescriptor

    override fun onDisconnect() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreated() {
        super.onCreated()
        loadData()
    }

    private val lazyMarkerStorage by lazy { LazyMarkerStorage(FAVORITE_KEY) }
    //set by async
    private lateinit var mCountries: ArrayList<MultiSelectable>
    private var mCountryBoundaries: CountryBoundaries? = null
    private var mEmojiFlagManager: EmojiFlagManager? = null
    private lateinit var mFavorites: ArrayList<LazyMarker>
    private lateinit var mJsonArray: JSONArray
    private lateinit var mRawResourceStyle: MapStyleOptions
    private lateinit var mTileProvider: MapBoxOfflineTileProvider

    override fun onDestroy() {
        super.onDestroy()
        mCameraUpdateAnimator?.onDestroy()
        cancel()
        // mTileProvider?.close()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
    }

    override fun onMapLoaded() {
        mCameraUpdateAnimator?.execute()

        debug(mMap.minZoomLevel)
        debug(mMap.maxZoomLevel)
    }

    override fun onCameraIdle() {
        val bounds = mMap.projection.visibleRegion.latLngBounds

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

    override fun onMapClick(point: LatLng?) {
        markers.entries.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            it.setLevel(it.level, this)

            views.hideFavoriteFab()
        }
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        marker?.let {
            if (marker.zIndex == 1.0f) {
                views.callConnectFabOnClick()
            } else {
                markers.entries.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
                    it.setLevel(it.level, this)
                }

                markers[marker.position]?.let {
                    it.zIndex = 1.0f
                    it.setIcon(iconDescriptor10)

                    views.toggleFavoriteFab(it.level == 1)
                }
            }
        }

        return false
    }

    override fun onInfoWindowClick(marker: Marker?) {
        marker?.let {
            if (marker.zIndex == 1.0f) {
                views.callConnectFabOnClick()
            }
        }
    }

    @Suppress("MagicNumber")
    override fun onLevelChange(marker: LazyMarker, level: Int) {
        when (level) {
            0 -> {
                marker.zIndex = 0f
                marker.setIcon(iconDescriptor0)
            }
            1 -> {
                marker.zIndex = level / 10.toFloat()
                marker.setIcon(iconDescriptor1)
            }
        }
    }

    override fun onCancel() {
    }

    override fun onSelected(selectedIds: ArrayList<Int>, selectedNames: ArrayList<String>, dataString: String) {
        mCountries.let {
            flags = getCurrentFlags(it, selectedIds)
        }

        onCameraIdle()
    }

    override fun showCountryFilterDialog() {
        PrintArray.show("pref_country_values", screen.requireActivity(), this)
    }

    override fun toggleCommand(v: View?) {
        (screen.requireActivity() as? OnClickListener)?.onClick(v)
    }

    override fun toggleFavoriteMarker() {
        markers.entries.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            when (it.level) {
                0 -> {
                    it.setLevel(1, null)
                    lazyMarkerStorage.addFavorite(screen.requireContext(), it)
                }
                1 -> {
                    it.setLevel(0, null)
                    lazyMarkerStorage.removeFavorite(screen.requireContext(), it)
                }
            }
            views.toggleFavoriteFab((it.level == 1))
        }
    }

    @MainThread
    override fun updateMasterMarker(show: Boolean) {
        views.setClickableLocationFab(false)
        loadData2()
    }

    override fun onAnimationStart() {
        views.setClickableFabs(false)
    }

    override fun onAnimationEnd() {
        views.setClickableFabs(true)
    }

    override fun onAnimationFinish(animation: Animation) {
        views.fakeLayoutAllFabs()
        markers[animation.target]?.let {
            if (flags.contains(it.tag)) {
                it.zIndex = 1.0f
                it.setIcon(iconDescriptor10)

                if (!it.isVisible) it.isVisible = true
                if (!it.isInfoWindowShown) it.showInfoWindow()

                views.toggleFavoriteFab(it.level == 1)
            }
        }

        views.showAllFabs()
    }

    override fun onAnimationCancel(animation: Animation) {
        markers[animation.target]?.let {
            info("Animation to $it canceled")
        }
    }

    fun onSessionFinished() {
        info("onSessionFinished")
        views.setClickableConnectFab(true)
        views.toggleConnectFab(false)

        views.showListAndLocationFab()
        markers.entries.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            if (!it.isInfoWindowShown) it.showInfoWindow()
            views.showFavoriteFab()
        }

        mMap.let {
            it.setOnInfoWindowClickListener(this)
            it.setOnMapClickListener(this)
            it.setOnMarkerClickListener(this)
            it.uiSettings?.isScrollGesturesEnabled = true
            it.uiSettings?.isZoomGesturesEnabled = true
        }
    }

    fun onSessionStarted() {
        info("onSessionStarted")
        views.setClickableConnectFab(true)
        views.toggleConnectFab(true)

        views.hideListAndLocationFab()
        markers.entries.firstOrNull { it.value.zIndex == 1.0f }?.value?.let {
            if (it.isInfoWindowShown) it.hideInfoWindow()
            views.hideFavoriteFab()
        }
        mMap.let {
            it.setOnInfoWindowClickListener(null)
            it.setOnMapClickListener(null)
            it.setOnMarkerClickListener { true }
            it.uiSettings?.isScrollGesturesEnabled = false
            it.uiSettings?.isZoomGesturesEnabled = false
        }
    }

    fun onSessionCancelled() {
        info("onSessionCancelled")
        views.setClickableConnectFab(true)
        views.toggleConnectFab(false)
    }

    @MainThread
    override fun positionAndFlagForSelectedMarker(): Pair<Coordinate?, String> {
        var pair: Pair<Coordinate?, String> = Pair(null, "")

        markers.entries.firstOrNull { it.value.zIndex == 1.0f }?.let {
            val latLng = it.key
            val tag = it.value.tag

            pair = when {
                markers.count { entry -> entry.value.tag == tag } == 1 -> Pair(null, tag.toString())
                else -> Pair(Coordinate(latLng.latitude, latLng.longitude), tag.toString())
            }
        }

        return pair
    }

    private fun loadData() = launch {
        // ui thread
        screen.toolBar?.showProgress(true)
        val ref = screen.requireContext().applicationContext.asReference()
        // background thread
        val countries = async(IO) { countryList(ref(), R.array.pref_country_entries) }
        val countryBoundaries = async(IO) { getCountryBoundaries(ref()) }
        val emojiManager = async(IO) { EmojiFlagManager().load(PATH) }
        val favorites = async(IO) { LazyMarkerStorage(FAVORITE_KEY).loadFavorites(ref()) }
        val jsonArray = async(IO) { jsonArray(ref(), R.raw.nordvpn, ".json") }
        val jsonObj = async(IO) { createGeoJson(ref()) }
        val rawResourceStyle = async(IO) { MapStyleOptions.loadRawResourceStyle(ref(), R.raw.style_json) }
        val tileProvider = async(IO) { memoryBackedTileProvider() }
        // ui thread
        mCountries = countries.await()
        mTileProvider = tileProvider.await()
        mCountryBoundaries = countryBoundaries.await()
        mEmojiFlagManager = emojiManager.await()
        mFavorites = favorites.await()
        mJsonArray = jsonArray.await()
        mRawResourceStyle = rawResourceStyle.await()
        val result = jsonObj.await()
        screen.toolBar?.hideProgress(true)
        showData(ref(), result)
    }

    private fun loadData2() = launch {
        // ui thread
        screen.toolBar?.showProgress(true)
        val ref = screen.requireContext().asReference()
        // background thread
        val result = async(IO) { createGeoJson(ref()) }.await()
        // ui thread
        screen.toolBar?.hideProgress(true)
        showData2(ref(), result)
    }

    private fun showData(context: Context, jsonObj: JSONObject?) {
        mMap.let {
            iconDescriptor0 = BitmapDescriptorFactory.fromResource(R.drawable.map1)
            iconDescriptor1 = BitmapDescriptorFactory.fromResource(R.drawable.map2)
            iconDescriptor10 = BitmapDescriptorFactory.fromResource(R.drawable.map0)
            val location = screen.lastLocation
            val (hashSet, hashMap) = createMarkers(context, mJsonArray, mEmojiFlagManager!!, it, mFavorites, this)
            flags = showPrintArray(context, mCountries, hashSet)
            markers = hashMap

            mCameraUpdateAnimator = CameraUpdateAnimator(it, createCameraUpdates(), this, this)
            val latLng = getCurrentPosition(context, mCountryBoundaries, location, flags, jsonObj, mJsonArray)
            val animation = Animation(CameraUpdateFactory.newLatLng(latLng)).apply {
                isAnimate = true
                isClosest = true
                target = latLng
            }
            mCameraUpdateAnimator?.add(animation)

            it.addTileOverlay(TileOverlayOptions().tileProvider(mTileProvider).fadeIn(false))

            it.setMapStyle(mRawResourceStyle)
            it.mapType = GoogleMap.MAP_TYPE_NORMAL
            it.setMaxZoomPreference(mTileProvider.maximumZoom)
            it.setMinZoomPreference(mTileProvider.minimumZoom)
            it.setOnInfoWindowClickListener(this)
            it.setOnMapClickListener(this)
            it.setOnMarkerClickListener(this)
            it.setOnMapLoadedCallback(this)
            // todo
            //val params = fab1.layoutParams as ConstraintLayout.LayoutParams
            //it.setPadding(0, 0, 0, params.height + params.bottomMargin)
            it.uiSettings?.isScrollGesturesEnabled = true
            it.uiSettings?.isZoomGesturesEnabled = true
            // Load map
            views.showMap()
        }
    }

    private fun showData2(context: Context, jsonObj: JSONObject?) {
        val latLng = getCurrentPosition(context, mCountryBoundaries, screen.lastLocation, flags, jsonObj)
        val animation = Animation(CameraUpdateFactory.newLatLng(latLng)).apply {
            isAnimate = true
            target = latLng
        }
        mCameraUpdateAnimator?.add(animation)
        mCameraUpdateAnimator?.execute()
        jsonObj?.let {
            // showThreats(context, it)
            views.showMiniBar(createUserMessage(context, it).build())
        }
    }

    companion object {
        private const val FAVORITE_KEY = "pref_favorites"
        private const val PATH = "/assets/emojis.json"
    }
}
