package io.github.sdsstudios.nvidiagpumonitor

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.Cursor
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceActivity.EXTRA_NO_HEADERS
import android.preference.PreferenceActivity.EXTRA_SHOW_FRAGMENT
import androidx.preference.PreferenceManager
import androidx.annotation.MainThread
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.annotation.WorkerThread
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import com.abdeveloper.library.MultiSelectModel
import com.adityaanand.morphdialog.MorphDialog
import com.afollestad.materialdialogs.MaterialDialog
import com.androidmapsextensions.lazy.LazyMarker
import com.androidmapsextensions.lazy.OnLevelChangeCallback
import com.antoniocarlon.map.CameraUpdateAnimator
import com.ariascode.networkutility.NetworkInfo
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mayurrokade.minibar.UserMessage
import com.sonelli.juicessh.pluginlibrary.listeners.OnClientStartedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import com.vdurmont.emoji.EmojiFlagManager
import de.westnordost.countryboundaries.CountryBoundaries
import io.fabric.sdk.android.Fabric
import io.github.sdsstudios.nvidiagpumonitor.ConnectionManager.Companion.JUICESSH_REQUEST_CODE
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_maps.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.sdk25.listeners.onClick
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Exception

class MainActivity : AppCompatActivity(),
        OnSessionStartedListener,
        OnSessionFinishedListener,
        OnClientStartedListener,
        ConnectionListLoaderFinishedCallback,
        OnMapReadyCallback,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraIdleListener,
        AnkoLogger,
        NetworkInfo.NetworkInfoListener,
        OnLevelChangeCallback,
        SubmitCallbackListener {
    companion object {
        private const val READ_CONNECTIONS = "com.sonelli.juicessh.api.v1.permission.READ_CONNECTIONS"
        private const val OPEN_SESSIONS = "com.sonelli.juicessh.api.v1.permission.OPEN_SESSIONS"
        private const val PERMISSION_REQUEST_CODE = 23
        private const val JUICE_SSH_PACKAGE_NAME = "com.sonelli.juicessh"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 0
        private const val REQUEST_GOOGLE_PLAY_SERVICES = 1972
    }

    private val mConnectionListAdapter by lazy {
        ConnectionListAdapter(if (supportActionBar == null) this else supportActionBar!!.themedContext)
    }

    private val mConnectionManager by lazy {
        ConnectionManager(
                ctx = this,
                mActivitySessionStartedListener = this,
                mActivitySessionFinishedListener = this
        )
    }

    private var mReadConnectionsPerm = false
    private var mOpenSessionsPerm = false

    private val mPermissionsGranted
        get() = mReadConnectionsPerm && mOpenSessionsPerm

    private val items by lazy { hashMapOf<LatLng, LazyMarker>() }
    private var storage: MyStorage? = null
    private var favorites = "pref_favorites"
    private var countryList: ArrayList<String>? = null

    private var cameraUpdateAnimator: CameraUpdateAnimator? = null
    private var countryBoundaries: CountryBoundaries? = null
    private var mMap: GoogleMap? = null
    private var networkInfo: NetworkInfo? = null
    private var tileProvider: MapBoxOfflineTileProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val core = CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()
        Fabric.with(this, Crashlytics.Builder().core(core).build())

        setContentView(R.layout.activity_main)

        toolbar.hideProgress()
        toolbar.isIndeterminate = true

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        map?.onCreate(savedInstanceState)

        PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false)

        val api = GoogleApiAvailability.getInstance()
        val errorCode = api.isGooglePlayServicesAvailable(this)

        when {
            errorCode == ConnectionResult.SUCCESS -> onActivityResult(REQUEST_GOOGLE_PLAY_SERVICES, AppCompatActivity.RESULT_OK, null)
            api.isUserResolvableError(errorCode) -> api.showErrorDialogFragment(this, errorCode, REQUEST_GOOGLE_PLAY_SERVICES)
            else -> longToast(api.getErrorString(errorCode))
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        if (isJuiceSSHInstalled()) {
//            mConnectionManager.powerUsage.observe(this, Observer {
//                textViewPower.setData(it, "W")
//            })
//
//            mConnectionManager.temperature.observe(this, Observer {
//                textViewTemp.setData(it, "C")
//            })
//
//            mConnectionManager.fanSpeed.observe(this, Observer {
//                textViewFanSpeed.setData(it, "%")
//            })
//
//            mConnectionManager.freeMemory.observe(this, Observer {
//                textViewFreeMemory.setData(it, "MB")
//            })
//
//            mConnectionManager.usedMemory.observe(this, Observer {
//                textViewUsedMemory.setData(it, "MB")
//            })
//
//            mConnectionManager.graphicsClock.observe(this, Observer {
//                textViewClockGraphics.setData(it, "MHz")
//            })
//
//            mConnectionManager.videoClock.observe(this, Observer {
//                textViewClockVideo.setData(it, "MHz")
//            })
//
//            mConnectionManager.memoryClock.observe(this, Observer {
//                textViewClockMemory.setData(it, "MHz")
//            })

            requestPermissions()

            if (mPermissionsGranted) {
                onPermissionsGranted()
            }

            fab0.onClick {
                if (mConnectionListAdapter.count == 0) {
                    MorphDialog.Builder(this, it as FloatingActionButton)
                            .title("Error")
                            .content(R.string.error_must_have_atleast_one_server)
                            .positiveText(android.R.string.ok)
                            .show()
                    return@onClick
                }

                if (mPermissionsGranted) {
                    val uuid = mConnectionListAdapter.getConnectionId(spinnerConnectionList.selectedItemPosition)
                    mConnectionManager.toggleConnection(uuid = uuid!!, activity = this)
                    it?.isClickable = false
                } else {
                    requestPermissions()
                }
            }
        }

        //if (NetworkInfo.getConnectivity(applicationContext).status == NetworkInfo.NetworkStatus.INTERNET) generateXML()
    }

    override fun onStart() {
        super.onStart()
        map?.onStart()
    }

    override fun onResume() {
        super.onResume()
        map?.onResume()

        if (!isJuiceSSHInstalled()) {
            indefiniteSnackbar(findViewById<View>(android.R.id.content), getString(R.string.error_must_install_juicessh), "OK") {
                juiceSSHInstall()
            }
        }
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

        mConnectionManager.onDestroy()
        tileProvider?.close()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map?.onLowMemory()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {

        fun isGranted(resultIndex: Int): Boolean {
            return grantResults.isNotEmpty() && grantResults[resultIndex] ==
                    PackageManager.PERMISSION_GRANTED
        }

        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                mReadConnectionsPerm = isGranted(0)
                mOpenSessionsPerm = isGranted(1)
            }
        }

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (mPermissionsGranted) {
                onPermissionsGranted()
            } else {
                indefiniteSnackbar(findViewById<View>(android.R.id.content), getString(R.string.error_must_enable_permissions), "OK") {
                    requestPermissions()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return when (id) {
            R.id.action_settings -> {
                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this)
                val intent = Intent(this, SettingsActivity::class.java).apply {
                    putExtra(EXTRA_SHOW_FRAGMENT, SettingsActivity.SettingsSyncPreferenceFragment::class.java.name)
                    putExtra(EXTRA_NO_HEADERS, true)
                }
                ActivityCompat.startActivity(this, intent, options.toBundle())
                /*
                startActivity<SettingsActivity>(
                        EXTRA_SHOW_FRAGMENT to SettingsActivity.SettingsSyncPreferenceFragment::class.java.name,
                        EXTRA_NO_HEADERS to true
                )
                */
                true
            }
            R.id.action_refresh -> {
                //val drawable = item.icon as? Animatable
                //drawable?.start()

                toolbar.showProgress(true)

                doAsync {
                    var json1: JSONArray? = null

                    if (networkInfo!!.getNetwork().status == NetworkInfo.NetworkStatus.INTERNET) {
                        json1 = createJson()
                    }
                    var thrown = true

                    if (json1 != null) {
                        val text = json1.toString()

                        try {
                            val file = File(getExternalFilesDir(null), resources.getResourceEntryName(R.raw.nordvpn) + ".json")
                            file.writeText(text)
                            thrown = false
                        } catch (e: Resources.NotFoundException) {
                            Crashlytics.logException(e)
                        } catch (e: FileNotFoundException) {
                            Crashlytics.logException(e)
                        } catch (e: IOException) {
                            Crashlytics.logException(e)
                        }
                    }

                    uiThread {
                        //drawable?.stop()

                        toolbar.hideProgress(true)

                        if (!thrown) {
                            MaterialDialog.Builder(it)
                                    .title("Warning")
                                    .content(R.string.warning_must_restart_app)
                                    .positiveText(android.R.string.ok)
                                    .show()
                        }
                    }

                    onComplete {

                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Suppress("MagicNumber")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == JUICESSH_REQUEST_CODE) {
            mConnectionManager.gotActivityResult(requestCode, resultCode, data)
        }

        if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                val watermark = map?.findViewWithTag<ImageView>("GoogleWatermark")

                if (watermark != null) {
                    //watermark.imageAlpha = 204
                    watermark.visibility = View.INVISIBLE

                    val params = watermark.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_START, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_END, 0)
                }

                doAsync {
                    val list = listOf(
                            Pair(R.raw.nordvpn, ".json")
                    )

                    for ((id, ext) in list) {
                        try {
                            val file = File(getExternalFilesDir(null), resources.getResourceEntryName(id) + ext)
                            if (!file.exists()) {
                                resources.openRawResource(id).use { input ->
                                    file.outputStream().buffered().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        } catch (e: Resources.NotFoundException) {
                            Crashlytics.logException(e)
                        } catch (e: FileNotFoundException) {
                            Crashlytics.logException(e)
                        } catch (e: IOException) {
                            Crashlytics.logException(e)
                        }
                    }

                    try {
                        //var t = System.currentTimeMillis()
                        countryBoundaries = CountryBoundaries.load(assets.open("boundaries.ser"))
                        //t = System.currentTimeMillis() - t
                        //info("Loading took " + t + "ms")
                    } catch (e: FileNotFoundException) {
                        Crashlytics.logException(e)
                    } catch (e: IOException) {
                        Crashlytics.logException(e)
                    }

                    uiThread {
                        networkInfo = NetworkInfo.getInstance(it)
                        networkInfo!!.addListener(it)

                        tileProvider = MapBoxOfflineTileProvider(null, "file:world.mbtiles?vfs=ndk-asset&immutable=1&mode=ro")
                        //tileProvider = MapBoxOfflineTileProvider("file:world.mbtiles?vfs=ndk-asset&immutable=1&mode=ro")
                        info(tileProvider!!.toString())

                        storage = MyStorage(favorites)

                        val preferences = PreferenceManager.getDefaultSharedPreferences(it)
                        /*
                        val array = resources.getTextArray(R.array.pref_country_entries)

                        if (preferences.getString("pref_country_values", "") == "") {
                            val list = Array(size = array.size) { false }.toCollection(ArrayList())
                            val editor = PrintArray.putListBoolean("pref_country_values", list, preferences)
                            editor.commit()
                        }

                        val checkedItems = PrintArray.getListBoolean("pref_country_values", preferences).toBooleanArray()

                        PrintArray.show( "pref_country_values", array, checkedItems, it, preferences)
                        */

                        // List of Countries with Name and ID
                        val listOfCountries = arrayListOf(
                                MultiSelectModel(0, "Albania", R.drawable.flag_al),
                                MultiSelectModel(1, "Argentina", R.drawable.flag_ar),
                                MultiSelectModel(2, "Australia", R.drawable.flag_au),
                                MultiSelectModel(3, "Austria", R.drawable.flag_at),
                                MultiSelectModel(4, "Azerbaijan", R.drawable.flag_az),
                                MultiSelectModel(5, "Belgium", R.drawable.flag_be),
                                MultiSelectModel(6, "Bosnia and Herzegovina", R.drawable.flag_ba),
                                MultiSelectModel(7, "Brazil", R.drawable.flag_br),
                                MultiSelectModel(8, "Bulgaria", R.drawable.flag_bg),
                                MultiSelectModel(9, "Canada", R.drawable.flag_ca),
                                MultiSelectModel(10, "Chile", R.drawable.flag_cl),
                                MultiSelectModel(11, "Costa Rica", R.drawable.flag_cr),
                                MultiSelectModel(12, "Croatia", R.drawable.flag_hr),
                                MultiSelectModel(13, "Cyprus", R.drawable.flag_cy),
                                MultiSelectModel(14, "Czech Republic", R.drawable.flag_cz),
                                MultiSelectModel(15, "Denmark", R.drawable.flag_dk),
                                MultiSelectModel(16, "Egypt", R.drawable.flag_eg),
                                MultiSelectModel(17, "Estonia", R.drawable.flag_ee),
                                MultiSelectModel(18, "Finland", R.drawable.flag_fi),
                                MultiSelectModel(19, "France", R.drawable.flag_fr),
                                MultiSelectModel(20, "Georgia", R.drawable.flag_ge),
                                MultiSelectModel(21, "Germany", R.drawable.flag_de),
                                MultiSelectModel(22, "Greece", R.drawable.flag_gr),
                                MultiSelectModel(23, "Hong Kong", R.drawable.flag_hk),
                                MultiSelectModel(24, "Hungary", R.drawable.flag_hu),
                                MultiSelectModel(25, "Iceland", R.drawable.flag_is),
                                MultiSelectModel(26, "India", R.drawable.flag_in),
                                MultiSelectModel(27, "Indonesia", R.drawable.flag_id),
                                MultiSelectModel(28, "Ireland", R.drawable.flag_ie),
                                MultiSelectModel(29, "Israel", R.drawable.flag_il),
                                MultiSelectModel(30, "Italy", R.drawable.flag_it),
                                MultiSelectModel(31, "Japan", R.drawable.flag_jp),
                                MultiSelectModel(32, "Latvia", R.drawable.flag_lv),
                                MultiSelectModel(33, "Luxembourg", R.drawable.flag_lu),
                                MultiSelectModel(34, "Macedonia", R.drawable.flag_mk),
                                MultiSelectModel(35, "Malaysia", R.drawable.flag_my),
                                MultiSelectModel(36, "Mexico", R.drawable.flag_mx),
                                MultiSelectModel(37, "Moldova", R.drawable.flag_md),
                                MultiSelectModel(38, "Netherlands", R.drawable.flag_nl),
                                MultiSelectModel(39, "New Zealand", R.drawable.flag_nz),
                                MultiSelectModel(40, "Norway", R.drawable.flag_no),
                                MultiSelectModel(41, "Poland", R.drawable.flag_pl),
                                MultiSelectModel(42, "Portugal", R.drawable.flag_pt),
                                MultiSelectModel(43, "Romania", R.drawable.flag_ro),
                                MultiSelectModel(44, "Russia", R.drawable.flag_ru),
                                MultiSelectModel(45, "Serbia", R.drawable.flag_rs),
                                MultiSelectModel(46, "Singapore", R.drawable.flag_sg),
                                MultiSelectModel(47, "Slovakia", R.drawable.flag_sk),
                                MultiSelectModel(48, "Slovenia", R.drawable.flag_si),
                                MultiSelectModel(49, "South Africa", R.drawable.flag_za),
                                MultiSelectModel(50, "South Korea", R.drawable.flag_kr),
                                MultiSelectModel(51, "Spain", R.drawable.flag_es),
                                MultiSelectModel(52, "Sweden", R.drawable.flag_se),
                                MultiSelectModel(53, "Switzerland", R.drawable.flag_ch),
                                MultiSelectModel(54, "Taiwan", R.drawable.flag_tw),
                                MultiSelectModel(55, "Thailand", R.drawable.flag_th),
                                MultiSelectModel(56, "Turkey", R.drawable.flag_tr),
                                MultiSelectModel(57, "Ukraine", R.drawable.flag_ua),
                                MultiSelectModel(58, "United Arab Emirates", R.drawable.flag_ae),
                                MultiSelectModel(59, "United Kingdom", R.drawable.flag_gb),
                                MultiSelectModel(60, "United States", R.drawable.flag_us),
                                MultiSelectModel(61, "Vietnam", R.drawable.flag_vn)
                        )

                        // Preselected IDs of Country List
                        val array = ArrayList<Int>()
                        for (i in listOfCountries.indices) {
                            array.add(i)
                        }
                        val defValue = array.joinToString(separator = PrintArray.delimiter)
                        val selectedCountries = PrintArray.getListInt("pref_country_values", defValue, preferences)

                        countryList = arrayListOf()
                        val strings: Array<String> = resources.getStringArray(R.array.pref_country_values)
                        selectedCountries.forEach { index ->
                            countryList!!.add(strings[index])
                        }

                        PrintArray.apply {
                            setHint(R.string.empty)
                            setTitle(R.string.empty)
                            setItems(listOfCountries)
                            setCheckedItems(selectedCountries)
                        }

                        map?.getMapAsync(it)
                    }
                }
            }
        }
    }

    override fun onSessionStarted(sessionId: Int, sessionKey: String?) {
        fab0.isClickable = true

        fab0.setImageResource(R.drawable.ic_flash_off_white_24dp)

        fab1.hide()
        fab2.hide()
        items.forEach { (key, value) ->
            if (value.zIndex == 1.0f) fab3.hide()
        }

        spinnerConnectionList.isEnabled = false

        mMap!!.setOnMapClickListener(null)
        mMap!!.setOnMarkerClickListener(null)
        mMap!!.uiSettings.isScrollGesturesEnabled = false
        mMap!!.uiSettings.isZoomGesturesEnabled = false

        //cardViewLayout.visibility = View.VISIBLE
    }

    override fun onSessionCancelled() {
        fab0.isClickable = true
    }

    @Suppress("MagicNumber")
    override fun onSessionFinished() {
        fab0.isClickable = true

        fab0.setImageResource(R.drawable.ic_flash_on_white_24dp)

        fab1.show()
        fab2.show()
        items.forEach { (key, value) ->
            if (value.zIndex == 1.0f) fab3.show()
        }

        spinnerConnectionList.isEnabled = true

        mMap!!.setOnMapClickListener(this)
        mMap!!.setOnMarkerClickListener(this)
        mMap!!.uiSettings.isScrollGesturesEnabled = true
        mMap!!.uiSettings.isZoomGesturesEnabled = true

        //cardViewLayout.visibility = View.GONE

        Handler().postDelayed({
            updateMasterMarker()
        }, 5000)
    }

    override fun onClientStarted() {
    }

    override fun onClientStopped() {
    }

    override fun onLoaderFinished(newCursor: Cursor?) {
        mConnectionListAdapter.swapCursor(newCursor)
    }

    @MainThread
    private fun onPermissionsGranted() {
        mConnectionManager.startClient(onClientStartedListener = this)

        spinnerConnectionList.adapter = mConnectionListAdapter

        LoaderManager.getInstance<FragmentActivity>(this).initLoader(0, null, ConnectionListLoader(
                mCtx = this,
                mLoaderFinishCallback = this
        ))
    }

    private fun requestPermissions() {
        mReadConnectionsPerm = hasPermission(READ_CONNECTIONS)
        mOpenSessionsPerm = hasPermission(OPEN_SESSIONS)

        if (!mReadConnectionsPerm || !mOpenSessionsPerm) {
            ActivityCompat.requestPermissions(this, arrayOf(READ_CONNECTIONS, OPEN_SESSIONS), PERMISSION_REQUEST_CODE)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun isJuiceSSHInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo(JUICE_SSH_PACKAGE_NAME, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun juiceSSHInstall() {
        val pkg = "com.android.vending"
        val cls = "com.google.android.finsky.activities.LaunchUrlHandlerActivity"
        val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
        if (launchIntent != null) {
            launchIntent.component = ComponentName(pkg, cls)
            launchIntent.data = Uri.parse("market://details?id=$JUICE_SSH_PACKAGE_NAME")
            startActivity(launchIntent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val params = fab1.layoutParams as ConstraintLayout.LayoutParams

        googleMap.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider).fadeIn(false))
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))
        googleMap.mapType = MAP_TYPE_NORMAL
        googleMap.setMaxZoomPreference(tileProvider!!.maximumZoom)
        googleMap.setMinZoomPreference(tileProvider!!.minimumZoom)
        googleMap.setOnMapClickListener(this)
        googleMap.setOnMapLoadedCallback(this)
        googleMap.setOnMarkerClickListener(this)
        googleMap.setPadding(0,0,0,params.height + params.bottomMargin)
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true

        mMap = googleMap
        cameraUpdateAnimator = CameraUpdateAnimator(mMap!!, this)

        map.visibility = View.VISIBLE
    }

    override fun onMapLoaded() {
        val arrayList = storage?.loadFavorites(this) as ArrayList<LazyMarker>?
        val iconDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.map1)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val securityManager = SecurityManager.getInstance(this)
        val z = mMap!!.minZoomLevel.toInt()

        fab1?.onClick {
            updateMasterMarker()
        }

        fab2?.onClick {
            PrintArray.show("pref_country_values", this, preferences)
        }

        fab3?.onClick {
            if (mMap != null && items.count() != 0) {
                items.forEach { (key, value) ->
                    if (value.zIndex == 1.0f) {
                        val level = value.level
                        when (level) {
                            0 -> {
                                value.setLevel(1, null)
                                storage?.addFavorite(this, value)
                            }
                            1 -> {
                                value.setLevel(0, null)
                                storage?.removeFavorite(this, value)
                            }
                        }
                    }
                }
            }
        }

        info(mMap!!.minZoomLevel)
        info(mMap!!.maxZoomLevel)

        toolbar.showProgress(true)

        doAsync {
        //val server = preferences.getString("pref_server", "")
        //val country_code = preferences.getString("pref_country", "")
        //val country = args.country
        //val area = args.area
        //val tcp = preferences.getBoolean("pref_tcp", false)
        //val max_load = preferences.getString("pref_max_load", "")
        //val top_servers = preferences.getString("pref_top_servers", "")
        //val pings = preferences.getString("pref_pings", "")
        //val force_fw_rules = preferences.getBoolean("pref_force_fw", false)
        val p2p = preferences.getBoolean("pref_p2p", false)
        val dedicated = preferences.getBoolean("pref_dedicated", false)
        val double = preferences.getBoolean("pref_double", false)
        val onion = preferences.getBoolean("pref_tor", false)
        val obfuscated = preferences.getBoolean("pref_anti_ddos", false)
        val netflix = preferences.getBoolean("pref_netflix", false)
        //val test = preferences.getBoolean("pref_test", false)
        //val internally_allowed = args.internally_allowed
        //val skip_dns_patch = preferences.getBoolean("pref_skip_dns_patch", false)
        //val silent = preferences.getBoolean("pref_silent", false)
        //val nvram = preferences.getBoolean("pref_nvram", false)

        var jsonArr: JSONArray? = null

        try {
            val file = File(getExternalFilesDir(null), resources.getResourceEntryName(R.raw.nordvpn) + ".json")
            val json = file.bufferedReader().use {
                it.readText()
            }
            jsonArr = JSONArray(json)
        } catch (e: Resources.NotFoundException) {
            Crashlytics.logException(e)
        } catch (e: FileNotFoundException) {
            Crashlytics.logException(e)
        } catch (e: IOException) {
            Crashlytics.logException(e)
        } catch (e: JSONException) {
            Crashlytics.logException(e)
        }

        if (jsonArr != null) {
        for (res in jsonArr) {
            var flag = res.getString("flag").toLowerCase()
            /*
            var pass = flag.equals(country_code, true)

            if (!pass) {
                continue
            }
            */

            if (flag == "uk")
            {
                flag = "gb"
                Crashlytics.logException(Exception(flag))
                error(flag)
            }

            var pass = when {
                p2p -> false
                dedicated -> false
                double -> false
                onion -> false
                obfuscated -> false
                netflix -> false
                else -> true
            }

            if (!pass && netflix) {
                pass = when {
                    flag.equals("us", true) -> true
                    flag.equals("ca", true) -> true
                    flag.equals("fr", true) -> true
                    flag.equals("nl", true) -> true
                    flag.equals("jp", true) -> true
                    else -> false
                }
            }

            if (!pass) {
                val categories = res.getJSONArray("categories")

                for (category in categories) {
                    val name = category.getString("name")

                    if (p2p and name.equals("P2P", true)) {
                        pass = true
                        break
                    } else if (dedicated and name.equals("Dedicated IP", true)) {
                        pass = true
                        break
                    } else if (double and name.equals("Double VPN", true)) {
                        pass = true
                        break
                    } else if (onion and name.equals("Onion Over VPN", true)) {
                        pass = true
                        break
                    } else if (obfuscated and name.equals("Obfuscated Servers", true)) {
                        pass = true
                        break
                    }
                }
            }

            if (!pass) {
                continue
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

            val country = res.getString("country")
            val emoji = parseToUnicode(flag)
            val location = res.getJSONObject("location")
            val latLng = LatLng(location.getDouble("lat"), location.getDouble("long"))
            val var1 = MarkerOptions().apply {
                flat(true)
                position(latLng)
                title("$emoji $country")
                visible(false)
                icon(iconDescriptor)
            }
            val marker = LazyMarker(mMap!!, var1, flag)

            if (arrayList != null) {
                val index = arrayList.indexOf(marker)
                if (index >= 0) {
                    val level = arrayList[index].level
                    marker.setLevel(level, null)
                    onLevelChange(marker, level)
                }
            }

            items[latLng] = marker
        }
        }

            val strings = resources.getStringArray(R.array.pref_country_values)
            items.forEach { (key, value) ->
                if (!strings.contains(value.tag)) {
                    Crashlytics.logException(Exception(value.tag.toString()))
                    error(value.tag)
                }
            }

            val json1 = createGeoJson(networkInfo!!, preferences, securityManager)

                val rows = Math.pow(2.0, z.toDouble()).toInt() - 1
                // Traverse through all rows
                for (y in 0..rows) {
                    for (x in 0..rows) {
                        val bounds = tileProvider!!.calculateTileBounds(x, y, z)
                        val cameraPosition = CameraPosition.Builder().target(bounds.northeast).build()
                        // Add animations
                        cameraUpdateAnimator?.add(CameraUpdateFactory.newCameraPosition(cameraPosition), false, 0)
                    }
                }

            uiThread {
                toolbar.hideProgress(true)

                if (json1 != null && countryList!!.contains(json1.getString("flag").toLowerCase())) {
                    executeAnimation(it, json1, jsonArr, cameraUpdateAnimator, true)
                }
                else {
                    executeAnimation(it, json1, jsonArr, cameraUpdateAnimator, false)
                }
            }
        }
    }


    @MainThread
    @Suppress("MagicNumber")
    private fun executeAnimation(it: Context, json1: JSONObject?, jsonArr: JSONArray?, animator: CameraUpdateAnimator?, closest: Boolean) {
        fun getDefaultLatLng(): LatLng
        {
            return LatLng(51.514125, -0.093689)
        }

        fun getLatLng(flag: String, latLng: LatLng, jsonArr: JSONArray?): LatLng {
            info(latLng.toString())

            if (jsonArr != null) {
                val latLngList = arrayListOf<LatLng>()
                var match = false

                loop@ for (res in jsonArr) {
                    val pass = flag.equals(res.getString("flag"), true)

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

        when {
            json1 != null -> {
                val flag = json1.getString("flag").toLowerCase()
                //val country = json1.getString("country")
                //val city = json1.getString("city")
                val lat = json1.getDouble("latitude")
                val lon = json1.getDouble("longitude")
                //val ip = json1.getString("ip")
                //val threat = json1.optJSONObject("threat")

                if (closest) {
                    animateCamera(getLatLng(flag, LatLng(lat, lon), jsonArr), animator, closest)
                }
                else {
                    animateCamera(LatLng(lat, lon), animator, closest)
                }
            }
            ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(it)
                fusedLocationClient.lastLocation
                        .addOnSuccessListener { location: Location? ->
                            if (location != null) {
                                /*
                                try {
                                    val gcd =  Geocoder(it, Locale.getDefault())
                                    val addresses = gcd.getFromLocation(location.latitude, location.longitude, 1)
                                    if (addresses != null && !addresses.isEmpty()) {
                                        country = addresses[0].countryCode
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                fun getToastString(ids: List<String>?): String {
                                    return when {
                                        ids == null || ids.isEmpty() -> "is nowhere"
                                        else -> "is in " + ids.joinToString()
                                    }
                                }
                                */

                                //var t = System.nanoTime()
                                val lat = location.latitude
                                val lon = location.longitude
                                val ids = countryBoundaries?.getIds(lon, lat)
                                //t = System.nanoTime() - t
                                //info(getToastString(ids) + "\n(in " + "%.3f".format(t / 1000 / 1000.toFloat()) + "ms)")

                                if (ids != null && !ids.isEmpty()) {
                                    if (closest) {
                                        animateCamera(getLatLng(ids[0].toLowerCase(), LatLng(lat, lon), jsonArr), animator, closest)
                                    }
                                    else {
                                        animateCamera(LatLng(lat, lon), animator, closest)
                                    }
                                }
                                else {
                                    animateCamera(getDefaultLatLng(), animator, closest)
                                }
                            }
                            else {
                                animateCamera(getDefaultLatLng(), animator, closest)
                            }
                        }
                        .addOnFailureListener{ e: Exception ->
                            error(e)
                            animateCamera(getDefaultLatLng(), animator, closest)
                        }
            }
            else -> {
                animateCamera(getDefaultLatLng(), animator, closest)
            }
        }
    }

    @MainThread
    private fun animateCamera(latLng: LatLng, animator: CameraUpdateAnimator?, closest: Boolean) {
        info(latLng.toString())
        animator?.add(CameraUpdateFactory.newLatLng(latLng), true, 0, object : CancelableCallback {
            override fun onFinish() {
                if (closest) {
                    items.forEach { (key, value) ->
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
                }
            }

            override fun onCancel() {
                if (closest) {
                    items.forEach { (key, value) ->
                        if (key == latLng) {
                            info("Animation to $value canceled")
                            return@onCancel
                        }
                    }
                }
            }
        })

        // Execute the animation and set the final OnCameraIdleListener
        animator?.execute()
    }

    override fun onCameraIdle() {
        val bounds = mMap!!.projection.visibleRegion.latLngBounds

            items.forEach { (key, value) ->
                if (bounds.contains(key) && countryList!!.contains(value.tag)) {
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
        items.forEach { (key, value) ->
            if (value.zIndex == 1.0f) {
                value.setLevel(value.level, this)

                fab3.hide()
            }
        }
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        if (p0 != null && p0.zIndex != 1.0f) {
            //info(p0.tag)
                items.forEach { (key, value) ->
                    if (value.zIndex == 1.0f) {
                        value.setLevel(value.level, this)
                    }
                }
            p0.zIndex = 1.0f
            p0.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.map0))

            fab3.isChecked = (items[p0.position]?.level == 1)
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
                marker.zIndex = level/10.toFloat()
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.map2))
            }
        }
    }

    @Suppress("MagicNumber")
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

            val color1 = ContextCompat.getColor(this, R.color.colorConnect)
            val color2 = ContextCompat.getColor(this, R.color.colorDisconnect)
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
        // Preselected IDs of Country List
        countryList = arrayListOf()
        val strings: Array<String> = resources.getStringArray(R.array.pref_country_values)
        selectedIds.forEach { index ->
            countryList!!.add(strings[index])
        }

        onCameraIdle()
    }

    override fun onCancel() {
    }

    override fun networkStatusChange(network: NetworkInfo.Network) {
        when(network.status){
            NetworkInfo.NetworkStatus.INTERNET -> {
//                longToast("ONLINE: ${network.type}")
            }
            NetworkInfo.NetworkStatus.OFFLINE -> {
//                longToast("OFFLINE: ${network.type}")
            }
        }
    }

    @MainThread
    fun positionAndFlagForSelectedMarker(): Pair<LatLng?, String?> {
        if (mMap != null && items.count() != 0) {
            items.forEach { (key, value) ->
                if (value.zIndex == 1.0f) {
                    return Pair(key, value.tag.toString())
                }
            }
        }

        return Pair(null, null)
    }

    @MainThread
    @Suppress("MagicNumber")
    fun updateMasterMarker(show: Boolean = false) {
        fab1.isClickable = false
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val securityManager = SecurityManager.getInstance(this)

        toolbar.showProgress(true)

        doAsync {
            val var1 = createGeoJson(networkInfo!!, preferences, securityManager)

            var var2: JSONArray? = null

            try {
                val file = File(getExternalFilesDir(null), resources.getResourceEntryName(R.raw.nordvpn) + ".json")
                val json = file.bufferedReader().use {
                    it.readText()
                }
                var2 = JSONArray(json)
            } catch (e: Resources.NotFoundException) {
                Crashlytics.logException(e)
            } catch (e: FileNotFoundException) {
                Crashlytics.logException(e)
            } catch (e: IOException) {
                Crashlytics.logException(e)
            } catch (e: JSONException) {
                Crashlytics.logException(e)
            }

            uiThread {
                toolbar.hideProgress(true)

                //var1?.let { jsonObject -> showThreats(jsonObject) }

                executeAnimation(it, var1, var2, cameraUpdateAnimator, false)

                fab1.isClickable = true

                if (show && var1 != null) {
                    val flag = var1.getString("flag").toUpperCase()
                    //val country = var1.getString("country")
                    val city = var1.getString("city")
                    //val lat = var1.getDouble("latitude")
                    //val lon = var1.getDouble("longitude")
                    val ip = var1.getString("ip")

                    val userMessage = UserMessage.Builder()
                            .with(this@MainActivity)
                            .setBackgroundColor(R.color.accent_material_indigo_200)
                            .setTextColor(android.R.color.white)
                            .setMessage("Connected to $city, $flag ($ip)")
                            .setDuration(5000)
                            .setShowInterpolator(AccelerateInterpolator())
                            .setDismissInterpolator(AccelerateInterpolator())
                            .build()

                    minibarView.translationZ = 0.0f
                    minibarView.show(userMessage)
                }
            }
        }
    }

    @WorkerThread
    fun createGeoJson(value: NetworkInfo, preferences: SharedPreferences, securityManager: SecurityManager): JSONObject? {
        if (value.getNetwork().status == NetworkInfo.NetworkStatus.INTERNET) {
            val geo = preferences.getBoolean("pref_geo", false)
            val api = preferences.getString("pref_geo_client", "")
            val ipdata = preferences.getString("pref_api_ipdata", "")
            val ipinfo = preferences.getString("pref_api_ipinfo", "")
            val ipstack = preferences.getString("pref_api_ipstack", "")

            if (geo) {
                var key: String? = null
                when {
                    api.equals("ipdata", true) -> {
                        key = ipdata
                    }
                    api.equals("ipinfo", true) -> {
                        key = ipinfo
                    }
                    api.equals("ipstack", true) -> {
                        key = ipstack
                    }
                }

                if (key != null && key.isNotEmpty()) key = securityManager.decryptString(key)

                return createJson2(api, key)
            }
        }

        return null
    }

//    class MainActivityUI : AnkoComponent<MainActivity> {
//        override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {
//            verticalLayout {
//                relativeLayout {
//                    textView {
//                        text = "23"
//                        textSize = 24f
//                        //gravity = Gravity.START
//                    }.lparams(width = wrapContent, height = wrapContent) {
//                        alignParentLeft()
//
//                    }
//
//                    textView {
//                        text = "Enter your request"
//                        textSize = 24f
//                        //gravity = Gravity.END
//                    }.lparams(width = wrapContent, height = wrapContent) {
//                        //margin = dip(20)
//                        //gravity = Gravity.END
//                        alignParentRight()
//                    }
//                }
//                gravity = Gravity.CENTER
//                padding = dip(40)
//
//
//
//                textView {
//                    gravity = Gravity.CENTER
//                    text = "Enter your request"
//                    textColor = Color.BLACK
//                    textSize = 24f
//                }.lparams(width = matchParent) {
//                    margin = dip(40)
//                }
//
//                val name = editText {
//                hint = "What is your name?"
//            }
//
//                editText {
//                    hint = "What is your message?"
//                    lines = 3
//                }
//
//                button("Enter") {
//                    }
//                }
//
//            }
//        }
}
