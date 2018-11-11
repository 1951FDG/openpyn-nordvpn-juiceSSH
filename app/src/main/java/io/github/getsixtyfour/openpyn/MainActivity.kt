package io.github.getsixtyfour.openpyn

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
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import androidx.preference.PreferenceManager
import com.abdeveloper.library.MultiSelectModel
import com.adityaanand.morphdialog.MorphDialog
import com.afollestad.materialdialogs.MaterialDialog
import com.androidmapsextensions.lazy.LazyMarker
import com.androidmapsextensions.lazy.LazyMarker.OnLevelChangeCallback
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
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.vdurmont.emoji.EmojiFlagManager
import de.westnordost.countryboundaries.CountryBoundaries
import io.fabric.sdk.android.Fabric
import io.github.getsixtyfour.openpyn.security.SecurityManager
import io.github.getsixtyfour.openpyn.utilities.MyStorage
import io.github.getsixtyfour.openpyn.utilities.PrintArray
import io.github.getsixtyfour.openpyn.utilities.SubmitCallbackListener
import io.github.getsixtyfour.openpyn.utilities.createJson
import io.github.getsixtyfour.openpyn.utilities.createJson2
import io.github.sdsstudios.nvidiagpumonitor.ConnectionListAdapter
import io.github.sdsstudios.nvidiagpumonitor.ConnectionListLoader
import io.github.sdsstudios.nvidiagpumonitor.ConnectionListLoaderFinishedCallback
import io.github.sdsstudios.nvidiagpumonitor.ConnectionManager
import io.github.sdsstudios.nvidiagpumonitor.ConnectionManager.Companion.JUICESSH_REQUEST_CODE
import io.github.sdsstudios.nvidiagpumonitor.OnCommandExecuteListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_maps.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.indefiniteSnackbar
import org.jetbrains.anko.sdk27.listeners.onClick
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

class LazyMarkerStorage(key: String) : MyStorage(key) {
    override fun jsonAdapter(): JsonAdapter<List<Any>> {
        val moshi = Moshi.Builder()
                .add(object {
                    @ToJson
                    @Suppress("unused")
                    fun toJson(value: LatLng): Map<String, Double> {
                        return mapOf("lat" to value.latitude, "long" to value.longitude)
                    }

                    @FromJson
                    @Suppress("unused")
                    fun fromJson(value: Map<String, Double>): LatLng {
                        return LatLng(value["lat"]!!, value["long"]!!)
                    }
                })
                .build()
        val type = Types.newParameterizedType(List::class.java, LazyMarker::class.java)
        return moshi.adapter(type)
    }
}

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
        OnLevelChangeCallback,
        SubmitCallbackListener,
        OnSessionExecuteListener,
        OnCommandExecuteListener {
    companion object {
        private const val READ_CONNECTIONS = "com.sonelli.juicessh.api.v1.permission.READ_CONNECTIONS"
        private const val OPEN_SESSIONS = "com.sonelli.juicessh.api.v1.permission.OPEN_SESSIONS"
        private const val PERMISSION_REQUEST_CODE = 23
        private const val JUICE_SSH_PACKAGE_NAME = "com.sonelli.juicessh"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 0
        private const val REQUEST_GOOGLE_PLAY_SERVICES = 1972
        private const val favorites = "pref_favorites"
    }

    private val mConnectionListAdapter by lazy {
        ConnectionListAdapter(if (supportActionBar == null) this else supportActionBar!!.themedContext)
    }
    private var mConnectionManager: ConnectionManager? = null
    private var mReadConnectionsPerm = false
    private var mOpenSessionsPerm = false
    private val mPermissionsGranted
        get() = mReadConnectionsPerm && mOpenSessionsPerm
    val items: HashMap<LatLng, LazyMarker> by lazy { HashMap<LatLng, LazyMarker>() }
    private val storage by lazy { LazyMarkerStorage(favorites) }
    private val countryList by lazy { ArrayList<String>() }
    private var cameraUpdateAnimator: CameraUpdateAnimator? = null
    private var countryBoundaries: CountryBoundaries? = null
    private var mMap: GoogleMap? = null
    private var networkInfo: NetworkInfo? = null
    private var tileProvider: MapBoxOfflineTileProvider? = null
    private var lastLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        val core = CrashlyticsCore.Builder().disabled(false).build()
        Fabric.with(this, Crashlytics.Builder().core(core).build())

        setContentView(R.layout.activity_main)

        toolbar.hideProgress()
        toolbar.isIndeterminate = true

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        map?.onCreate(savedInstanceState)

        PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false)

        networkInfo = NetworkInfo.getInstance(application)
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            lastLocation = location
                        }
                    }
                    .addOnFailureListener { e: Exception ->
                        error(e)
                    }
        }

        if (isJuiceSSHInstalled()) {
            mConnectionManager = ConnectionManager(this, this, this, this, this)
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

            fab0?.onClick {
                if (mConnectionListAdapter.count == 0 && it is FloatingActionButton) {
                    MorphDialog.Builder(this, it)
                            .title("Error")
                            .content(R.string.error_must_have_atleast_one_server)
                            .positiveText(android.R.string.ok)
                            .show()
                    return@onClick
                }

                if (mPermissionsGranted) {
                    val uuid = mConnectionListAdapter.getConnectionId(spinnerConnectionList.selectedItemPosition)
                    mConnectionManager?.toggleConnection(uuid!!, this)
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
            findViewById<View>(android.R.id.content).indefiniteSnackbar(getString(R.string.error_must_install_juicessh), getString(android.R.string.ok)) {
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

        mConnectionManager?.onDestroy()
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
                findViewById<View>(android.R.id.content).indefiniteSnackbar(getString(R.string.error_must_enable_permissions), getString(android.R.string.ok)) {
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

                    if (networkInfo!!.isOnline()) {
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
                        toolbar.hideProgress(true)
                        //drawable?.stop()
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
            R.id.action_github -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/1951FDG/openpyn-nordvpn-juiceSSH")))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Suppress("MagicNumber")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == JUICESSH_REQUEST_CODE) {
            mConnectionManager?.gotActivityResult(requestCode, resultCode, data)
        }

        if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                map?.getMapAsync(this)
            }
        }
    }

    override fun onSessionStarted(sessionId: Int, sessionKey: String) {
        fab0.isClickable = true

        fab0.setImageResource(R.drawable.ic_flash_off_white_24dp)

        fab1.hide()
        fab2.hide()
        items.forEach { (_, value) ->
            if (value.zIndex == 1.0f) fab3.hide()
        }

        spinnerConnectionList.isEnabled = false

        mMap!!.setOnMapClickListener(null)
        mMap!!.setOnMarkerClickListener { true }
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
        items.forEach { (_, value) ->
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

    @Suppress("MagicNumber")
    override fun onOutputLine(line: String) {
        longToast(line)
        if (line.startsWith("CONNECTING TO SERVER", true)) {
            Handler().postDelayed({
                updateMasterMarker(true)
            }, 10000)
        }
    }

    override fun onError(error: Int, reason: String) {
        longToast(reason)
    }

    override fun onCompleted(exitCode: Int) {
        longToast(exitCode.toString())
        when (exitCode) {
            0 -> {
                error("Success")
            }
        }
    }

    @MainThread
    private fun onPermissionsGranted() {
        mConnectionManager?.startClient(onClientStartedListener = this)

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
        mMap = googleMap
        cameraUpdateAnimator = CameraUpdateAnimator(googleMap, this)
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val arrayList = storage.loadFavorites(this)
        val iconDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.map1)
        val securityManager = SecurityManager.getInstance(this)

        toolbar.showProgress(true)

        doAsync {
            tileProvider = MapBoxOfflineTileProvider(null, "file:world.mbtiles?vfs=ndk-asset&immutable=1&mode=ro")
            //tileProvider = MapBoxOfflineTileProvider("file:world.mbtiles?vfs=ndk-asset&immutable=1&mode=ro")
            info(tileProvider!!.toString())
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
                countryBoundaries = CountryBoundaries.load(assets.open("boundaries.ser"))
            } catch (e: FileNotFoundException) {
                Crashlytics.logException(e)
            } catch (e: IOException) {
                Crashlytics.logException(e)
            }
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
            val strings = resources.getStringArray(R.array.pref_country_values)
            selectedCountries.forEach { index ->
                countryList.add(strings[index])
            }

            PrintArray.apply {
                setHint(R.string.empty)
                setTitle(R.string.empty)
                setItems(listOfCountries)
                setCheckedItems(selectedCountries)
            }
            val p2p = preferences.getBoolean("pref_p2p", false)
            val dedicated = preferences.getBoolean("pref_dedicated", false)
            val double = preferences.getBoolean("pref_double", false)
            val onion = preferences.getBoolean("pref_tor", false)
            val obfuscated = preferences.getBoolean("pref_anti_ddos", false)
            val netflix = preferences.getBoolean("pref_netflix", false)
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

                    if (flag == "uk") {
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
                        pass = when (flag) {
                            "us" -> true
                            "ca" -> true
                            "fr" -> true
                            "nl" -> true
                            "jp" -> true
                            else -> false
                        }
                    }

                    if (!pass) {
                        val categories = res.getJSONArray("categories")

                        for (category in categories) {
                            val name = category.getString("name")

                            if (p2p and (name == "P2P")) {
                                pass = true
                                break
                            } else if (dedicated and (name == "Dedicated IP")) {
                                pass = true
                                break
                            } else if (double and (name == "Double VPN")) {
                                pass = true
                                break
                            } else if (onion and (name == "Onion Over VPN")) {
                                pass = true
                                break
                            } else if (obfuscated and (name == "Obfuscated Servers")) {
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
                    val marker = LazyMarker(googleMap, var1, flag, null)
                    val index = arrayList.indexOf(marker)
                    if (index >= 0) {
                        val any = arrayList[index]
                        if (any is LazyMarker) {
                            val level = any.level
                            marker.setLevel(level, null)
                            onLevelChange(marker, level)
                        }
                    }

                    items[latLng] = marker
                }
            }

            items.forEach { (_, value) ->
                val element = value.tag
                if (element is String && !strings.contains(element)) {
                    Crashlytics.logException(Exception(element))
                    error(element)
                }
            }
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
            val json1 = createGeoJson(networkInfo!!, preferences, securityManager)
            addAnimation(json1, jsonArr, cameraUpdateAnimator, true)

            uiThread {
                toolbar.hideProgress(true)
                val params = fab1.layoutParams as ConstraintLayout.LayoutParams

                googleMap.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider).fadeIn(false))
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(it, R.raw.style_json))
                googleMap.mapType = MAP_TYPE_NORMAL
                googleMap.setMaxZoomPreference(tileProvider!!.maximumZoom)
                googleMap.setMinZoomPreference(tileProvider!!.minimumZoom)
                googleMap.setOnMapClickListener(it)
                googleMap.setOnMapLoadedCallback(it)
                googleMap.setOnMarkerClickListener(it)
                googleMap.setPadding(0, 0, 0, params.height + params.bottomMargin)
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

        fab1?.onClick {
            updateMasterMarker()
        }

        fab2?.onClick {
            val preferences = PreferenceManager.getDefaultSharedPreferences(this)

            PrintArray.show("pref_country_values", this, preferences)
        }

        fab3?.onClick {
            if (mMap != null && items.size != 0) {
                items.forEach { (_, value) ->
                    if (value.zIndex == 1.0f) {
                        val level = value.level
                        when (level) {
                            0 -> {
                                value.setLevel(1, null)
                                storage.addFavorite(this, value)
                            }
                            1 -> {
                                value.setLevel(0, null)
                                storage.removeFavorite(this, value)
                            }
                        }
                    }
                }
            }
        }

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
                val pass = flag == res.getString("flag").toLowerCase()

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
        val ids = countryBoundaries?.getIds(longitude, latitude)
        if (ids != null && !ids.isEmpty()) {
            return ids[0]
        }
        return ""
    }

    private fun addAnimation(json1: JSONObject?, jsonArr: JSONArray?, animator: CameraUpdateAnimator?, closest: Boolean) {
        when {
            json1 != null -> {
                val lat = json1.getDouble("latitude")
                val lon = json1.getDouble("longitude")
                val flag = json1.getString("flag").toLowerCase()
                val latLng = if (closest && countryList.contains(flag)) {
                    getLatLng(flag, LatLng(lat, lon), jsonArr)
                } else {
                    LatLng(lat, lon)
                }

                animateCamera(latLng, animator, closest, false)
            }
            lastLocation != null -> {
                val lat = lastLocation!!.latitude
                val lon = lastLocation!!.longitude
                val flag = getFlag(lon, lat).toLowerCase()
                val latLng = if (closest && countryList.contains(flag)) {
                    getLatLng(flag, LatLng(lat, lon), jsonArr)
                } else {
                    LatLng(lat, lon)
                }

                animateCamera(latLng, animator, closest, false)
            }
            else -> {
                animateCamera(getDefaultLatLng(), animator, false, false)
            }
        }
    }

    @MainThread
    private fun executeAnimation(it: Context, json1: JSONObject?, jsonArr: JSONArray?, animator: CameraUpdateAnimator?, closest: Boolean) {
        when {
            json1 != null -> {
                val lat = json1.getDouble("latitude")
                val lon = json1.getDouble("longitude")
                val flag = json1.getString("flag").toLowerCase()
                val latLng = if (closest && countryList.contains(flag)) {
                    getLatLng(flag, LatLng(lat, lon), jsonArr)
                } else {
                    LatLng(lat, lon)
                }

                animateCamera(latLng, animator, closest, true)
            }
            ActivityCompat.checkSelfPermission(it, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(it)
                fusedLocationClient.lastLocation
                        .addOnSuccessListener { location: Location? ->
                            var latLng = getDefaultLatLng()
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

                                var t = System.nanoTime()
                                t = System.nanoTime() - t
                                info(getToastString(ids) + "\n(in " + "%.3f".format(t / 1000 / 1000.toFloat()) + "ms)")
                                */
                                val lat = location.latitude
                                val lon = location.longitude
                                val flag = getFlag(lon, lat).toLowerCase()

                                latLng = if (closest && countryList.contains(flag)) {
                                    getLatLng(flag, LatLng(lat, lon), jsonArr)
                                } else {
                                    LatLng(lat, lon)
                                }
                            }

                            animateCamera(latLng, animator, closest, true)
                        }
                        .addOnFailureListener { e: Exception ->
                            error(e)
                            animateCamera(getDefaultLatLng(), animator, closest, true)
                        }
            }
            else -> {
                animateCamera(getDefaultLatLng(), animator, closest, true)
            }
        }
    }

    private fun animateCamera(latLng: LatLng, animator: CameraUpdateAnimator?, closest: Boolean, animate: Boolean) {
        info(latLng.toString())

        fun onStart() {
            fab0.isClickable = false
            fab1.isClickable = false
            fab2.isClickable = false
        }

        fun onEnd() {
            fab1.show()
            fab2.show()

            fab0.isClickable = true
            fab1.isClickable = true
            fab2.isClickable = true
        }

        onStart()

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

                onEnd()
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

                onEnd()
            }
        })
        // Execute the animation and set the final OnCameraIdleListener
        if (animate) animator?.execute()
    }

    override fun onCameraIdle() {
        val bounds = mMap!!.projection.visibleRegion.latLngBounds

        items.forEach { (key, value) ->
            if (bounds.contains(key) && countryList.contains(value.tag)) {
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
        items.forEach { (_, value) ->
            if (value.zIndex == 1.0f) {
                value.setLevel(value.level, this)

                fab3.hide()
            }
        }
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        if (p0 != null && p0.zIndex != 1.0f) {
            //info(p0.tag)
            items.forEach { (_, value) ->
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
                marker.zIndex = level / 10.toFloat()
                marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.map2))
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
        countryList.clear()
        val strings: Array<String> = resources.getStringArray(R.array.pref_country_values)
        selectedIds.forEach { index ->
            countryList.add(strings[index])
        }

        onCameraIdle()
    }

    override fun onCancel() {
    }

    @MainThread
    override fun positionAndFlagForSelectedMarker(): Pair<LatLng?, String?> {
        if (mMap != null && items.size != 0) {
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
