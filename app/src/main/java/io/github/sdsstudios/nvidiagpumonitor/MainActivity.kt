package io.github.sdsstudios.nvidiagpumonitor

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.Cursor
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceActivity.EXTRA_NO_HEADERS
import android.preference.PreferenceActivity.EXTRA_SHOW_FRAGMENT
import android.preference.PreferenceManager
import android.support.annotation.MainThread
import android.support.constraint.ConstraintLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.sonelli.juicessh.pluginlibrary.listeners.OnClientStartedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
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
import java.util.*
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity(),
        OnSessionStartedListener,
        OnSessionFinishedListener,
        OnClientStartedListener,
        ConnectionListLoaderFinishedCallback,
        OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraIdleListener,
        GoogleMap.OnInfoWindowClickListener,
        AnkoLogger,
        NetworkInfo.NetworkInfoListener {
    companion object {
        private const val READ_CONNECTIONS = "com.sonelli.juicessh.api.v1.permission.READ_CONNECTIONS"
        private const val OPEN_SESSIONS = "com.sonelli.juicessh.api.v1.permission.OPEN_SESSIONS"
        private const val PERMISSION_REQUEST_CODE = 23
        private const val JUICE_SSH_PACKAGE_NAME = "com.sonelli.juicessh"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 0
        private const val REQUEST_GOOGLE_PLAY_SERVICES = 1972
    }

    private var mReadConnectionsPerm = false
    private var mOpenSessionsPerm = false

    private val mConnectionManager by lazy {
        ConnectionManager(
                ctx = this,
                mActivitySessionStartedListener = this,
                mActivitySessionFinishedListener = this
        )
    }

    private val mConnectionListAdapter by lazy { ConnectionListAdapter(supportActionBar!!.themedContext) }

    private val mPermissionsGranted
        get() = mReadConnectionsPerm && mOpenSessionsPerm

    private var mMap: GoogleMap? = null

    var mMarker: Marker? = null

    private var offlineTileProvider: MapBoxOfflineTileProvider? = null

    private val items by lazy { arrayListOf<Marker>() }

    private var networkInfo: NetworkInfo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        map.onCreate(savedInstanceState)

        val api = GoogleApiAvailability.getInstance()
        val errorCode = api.isGooglePlayServicesAvailable(this)

        when {
            errorCode == ConnectionResult.SUCCESS -> onActivityResult(REQUEST_GOOGLE_PLAY_SERVICES, AppCompatActivity.RESULT_OK, null)
            api.isUserResolvableError(errorCode) -> api.showErrorDialogFragment(this, errorCode, REQUEST_GOOGLE_PLAY_SERVICES)
            else -> this.longToast(api.getErrorString(errorCode))
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
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

            buttonConnect.onClick {
                if (mConnectionListAdapter.count == 0) {
                    toast(R.string.error_must_have_atleast_one_server)

                    return@onClick
                }

                if (mPermissionsGranted) {
                    if (buttonConnect.text.toString().equals(getString(R.string.btn_connect), true)) {
                        buttonConnect.applyConnectingStyle()
                    } else {
                        buttonConnect.applyDisconnectingStyle()
                    }

                    val uuid = mConnectionListAdapter.getConnectionId(spinnerConnectionList.selectedItemPosition)
                    mConnectionManager.toggleConnection(uuid = uuid!!, activity = this)

                } else {
                    requestPermissions()
                }
            }
        }

//        val jsonObjLast = createJson()
//
//        if (jsonObjLast != null) {
//            val text = jsonObjLast.toString()
//            debug(text)
//
//            try {
//                val file = File(this.getExternalFilesDir(null), resources.getResourceEntryName(R.raw.nordvpn) + ".json")
//                debug(file)
//
//                file.writeText(text)
//            } catch (e: Resources.NotFoundException) {
//                error(e)
//            } catch (e: FileNotFoundException) {
//                error(e)
//            } catch (e: IOException) {
//                error(e)
//            }
//        }

//        if (NetworkInfo.getConnectivity(applicationContext).status == NetworkInfo.NetworkStatus.INTERNET) generateXML()
    }

    override fun onStart() {
        super.onStart()
        map.onStart()
    }

    override fun onResume() {
        super.onResume()
        map.onResume()

        if (!isJuiceSSHInstalled()) {
            indefiniteSnackbar(findViewById<View>(android.R.id.content), getString(R.string.error_must_install_juicessh), "OK") { juiceSSHInstall() }
        }
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onStop() {
        super.onStop()
        map.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        map.onDestroy()

        offlineTileProvider?.close()

        mConnectionManager.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map.onLowMemory()
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
                indefiniteSnackbar(findViewById<View>(android.R.id.content), getString(R.string.error_must_enable_permissions), "OK") { requestPermissions() }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return when (id) {
            R.id.action_settings -> {
                startActivity<SettingsActivity>(EXTRA_SHOW_FRAGMENT to SettingsActivity.SettingsSyncPreferenceFragment::class.java.name, EXTRA_NO_HEADERS to true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == JUICESSH_REQUEST_CODE) {
            mConnectionManager.gotActivityResult(requestCode, resultCode, data)
        }

        if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                val watermark = map.findViewWithTag<ImageView>("GoogleWatermark")

                if (watermark != null) {
                    watermark.imageAlpha = 204

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
                            error(e)
                        } catch (e: FileNotFoundException) {
                            error(e)
                        } catch (e: IOException) {
                            error(e)
                        }
                    }

                    uiThread {
                        networkInfo = NetworkInfo.getInstance(it)
                        networkInfo!!.addListener(it)

                        offlineTileProvider = MapBoxOfflineTileProvider("file:world.mbtiles?vfs=ndk-asset&immutable=1&mode=ro")
                        info(offlineTileProvider!!.minimumZoom)
                        info(offlineTileProvider!!.maximumZoom)

                        map.getMapAsync(it)
                    }
                }
            }
        }
    }

    override fun onSessionStarted(sessionId: Int, sessionKey: String?) {
        buttonConnect.applyDisconnectStyle()
        //cardViewLayout.visibility = View.VISIBLE
        spinnerConnectionList.isEnabled = false
    }

    override fun onSessionCancelled() {
        buttonConnect.applyConnectStyle()
    }

    override fun onSessionFinished() {
        buttonConnect.applyConnectStyle()
        //cardViewLayout.visibility = View.GONE
        spinnerConnectionList.isEnabled = true

        Timer().schedule(5000){
            updateMasterMarker()
        }
    }

    override fun onClientStarted() {
        buttonConnect.isEnabled = true
    }

    override fun onClientStopped() {
        buttonConnect.isEnabled = false
    }

    override fun onLoaderFinished(newCursor: Cursor?) {
        mConnectionListAdapter.swapCursor(newCursor)
    }

    @MainThread
    private fun onPermissionsGranted() {
        mConnectionManager.startClient(onClientStartedListener = this)

        buttonConnect.applyConnectStyle()

        spinnerConnectionList.adapter = mConnectionListAdapter

        supportLoaderManager.initLoader(0, null, ConnectionListLoader(
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
        val launchIntent = packageManager.getLaunchIntentForPackage("com.android.vending")
        if (launchIntent != null) {
            launchIntent.component = ComponentName("com.android.vending", "com.google.android.finsky.activities.LaunchUrlHandlerActivity")
            launchIntent.data = Uri.parse("market://details?id=$JUICE_SSH_PACKAGE_NAME")
            startActivity(launchIntent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val params = buttonConnect.layoutParams as ConstraintLayout.LayoutParams

        try {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))

        } catch (e: Resources.NotFoundException) {
            error(e)
        }

        googleMap.addTileOverlay(TileOverlayOptions().tileProvider(offlineTileProvider).fadeIn(false))
        googleMap.setMapType(MAP_TYPE_NORMAL)
        googleMap.setMaxZoomPreference(offlineTileProvider!!.maximumZoom)
        googleMap.setMinZoomPreference(offlineTileProvider!!.minimumZoom)
        googleMap.setOnInfoWindowClickListener(this)
        googleMap.setOnMapLoadedCallback(this)
        googleMap.setOnMarkerClickListener(this)
        googleMap.setPadding(0,0,0,params.height + params.bottomMargin)
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true

        mMap = googleMap

        map.visibility = View.VISIBLE
    }

    override fun onMapLoaded() {
        info(mMap!!.minZoomLevel)
        info(mMap!!.maxZoomLevel)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

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
        val double_vpn = preferences.getBoolean("pref_double", false)
        val tor_over_vpn = preferences.getBoolean("pref_tor", false)
        val anti_ddos = preferences.getBoolean("pref_anti_ddos", false)
        val netflix = preferences.getBoolean("pref_netflix", false)
        //val test = preferences.getBoolean("pref_test", false)
        //val internally_allowed = args.internally_allowed
        //val skip_dns_patch = preferences.getBoolean("pref_skip_dns_patch", false)
        //val silent = preferences.getBoolean("pref_silent", false)
        //val nvram = preferences.getBoolean("pref_nvram", false)

        var jsonArr: JSONArray? = null

        try {
            val file = File(getExternalFilesDir(null),resources.getResourceEntryName(R.raw.nordvpn) + ".json")
            val json = file.bufferedReader().use {
                it.readText()
            }
            jsonArr = JSONArray(json)
        } catch (e: Resources.NotFoundException) {
            error(e)
        } catch (e: FileNotFoundException) {
            error(e)
        } catch (e: IOException) {
            error(e)
        } catch (e: JSONException) {
            error(e)
        }

        operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

        if (jsonArr != null) {
        for (res in jsonArr) {
            val country = res.getString("flag").toLowerCase()
            /*
            var pass = country.equals(country_code,true)

            if (!pass) {
                continue
            }
            */

            var pass = when {
                p2p -> false
                dedicated -> false
                double_vpn -> false
                tor_over_vpn -> false
                anti_ddos -> false
                netflix -> false
                else -> true
            }

            if (!pass && netflix) {
                pass = when {
                    country.equals("us", true) -> true
                    country.equals("ca", true) -> true
                    country.equals("fr", true) -> true
                    country.equals("nl", true) -> true
                    country.equals("jp", true) -> true
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
                    } else if (dedicated and name.equals("Dedicated IP servers", true)) {
                        pass = true
                        break
                    } else if (double_vpn and name.equals("Double VPN", true)) {
                        pass = true
                        break
                    } else if (tor_over_vpn and name.equals("Obfuscated Servers", true)) {
                        pass = true
                        break
                    } else if (anti_ddos and name.equals("Anti DDoS", true)) {
                        pass = true
                        break
                    }
                }
            }

            if (!pass) {
                continue
            }

            val location = res.getJSONObject("location")

            val var1 = MarkerOptions().apply {
                position(LatLng(location.getDouble("lat"), location.getDouble("long")))
                visible(false)
                flat(true)
            }

            val marker = mMap!!.addMarker(var1)
            marker.tag = country

            items.add(marker)
        }
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener { location : Location? ->
            if (location != null) {
                val var1 = MarkerOptions().apply {
                    flat(true)
                    icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
                    position(LatLng(location.latitude, location.longitude))
                    zIndex(0.5f)
                }

                mMap!!.addMarker(var1)
            }
        }
        }

        doAsync {
            var json1: JSONObject? = null

            if (networkInfo!!.getNetwork().status == NetworkInfo.NetworkStatus.INTERNET)
            {
                json1 = createJson1()
            }

            uiThread {
                // Create a new CameraUpdateAnimator for a given mMap
                // with an OnCameraIdleListener to set when the animation ends
                val animator = CameraUpdateAnimator(mMap!!, it)
                val z = mMap!!.minZoomLevel.toInt()
                val rows = Math.pow(2.0, z.toDouble()).toInt() - 1
                // Traverse through all rows
                for (y in 0..rows) {
                    for (x in 0..rows) {
                        val bounds = offlineTileProvider!!.calculateTileBounds(x, y, z)
                        val cameraPosition = CameraPosition.Builder().target(bounds.northeast).build()
                        // Add animations
                        animator.add(CameraUpdateFactory.newCameraPosition(cameraPosition), false, 0)
                    }
                }

                if (json1 != null) {
                val country = json1.getString("country")
                val city = json1.getString("city")
                val lat = json1.getDouble("latitude")
                val lon = json1.getDouble("longitude")
                val emoji = json1.getString("emoji_flag")
                val ip = json1.getString("ip")
                //val threat = json1.optJSONObject("threat")

                val var1 = MarkerOptions().apply {
                    flat(true)
                    icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    position(LatLng(lat, lon))
                    snippet("$ip${Typography.ellipsis}")
                    title("$emoji $city".trimStart())
                    zIndex(1.0f)
                }

                mMarker = mMap!!.addMarker(var1)
                mMarker!!.tag = json1

                animator.add(CameraUpdateFactory.newLatLng(mMarker!!.position), true, 0,
                        object : CancelableCallback {
                            override fun onFinish() {
                                longToast("Animation to $country complete")
                                mMarker!!.showInfoWindow()
                            }

                            override fun onCancel() {
                                longToast("Animation to $country canceled")
                                mMarker!!.showInfoWindow()
                            }
                        })
                }
                // Execute the animation and set the final OnCameraIdleListener
                animator.execute()
            }
        }
    }

    override fun onCameraIdle() {
        val bounds = mMap!!.projection.visibleRegion.latLngBounds

        if (items.count() != 0) {
            for (item in items) {
                if (bounds.contains(item.position)) {
                    if (!item.isVisible) item.isVisible = true
                } else {
                    if (item.isVisible) item.isVisible = false
                }
            }
        }

        val item = mMarker

        if (item != null) {
            if (bounds.contains(item.position)) {
                if (!item.isVisible) item.isVisible = true
            } else {
                if (item.isVisible) item.isVisible = false
            }
        }
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        if (p0?.zIndex == 0f) {
            debug(p0.tag)
            if (items.count() != 0) {
                for (item in items) {
                    if (item.zIndex == 1.0f) {
                        item.zIndex = 0f
                        item.setIcon(null)
                    }
                }
            }
            p0.zIndex = 1.0f
            p0.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
        }

        return false
    }

    override fun onInfoWindowClick(p0: Marker?) {
        val jsonObj = p0?.tag as JSONObject
        debug(jsonObj)

        val threats: JSONObject? = jsonObj.optJSONObject("threat")

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

            alert {
                customView {
                    verticalLayout {
                        linearLayout {
                            textView {
                                text = getString(R.string.is_tor)
                                textSize = 22f
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                            textView {
                                text = if (tor) "YES" else "NO"
                                textColor = if (tor) color2 else color1
                                textSize = 22f
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = getString(R.string.is_proxy)
                                textSize = 22f
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                            textView {
                                text = if (proxy) "YES" else "NO"
                                textColor = if (proxy) color2 else color1
                                textSize = 22f
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = getString(R.string.is_anonymous)
                                textSize = 22f
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                            textView {
                                text = if (anonymous) "YES" else "NO"
                                textColor = if (anonymous) color2 else color1
                                textSize = 22f
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = getString(R.string.is_known_attacker)
                                textSize = 22f
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                            textView {
                                text = if (attacker) "YES" else "NO"
                                textColor = if (attacker) color2 else color1
                                textSize = 22f
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = getString(R.string.is_known_abuser)
                                textSize = 22f
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                            textView {
                                text = if (abuser) "YES" else "NO"
                                textColor = if (abuser) color2 else color1
                                textSize = 22f
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = getString(R.string.is_threat)
                                textSize = 22f
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                            textView {
                                text = if (threat) "YES" else "NO"
                                textColor = if (threat) color2 else color1
                                textSize = 22f
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = getString(R.string.is_bogon)
                                textSize = 22f
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                            textView {
                                text = if (bogon) "YES" else "NO"
                                textColor = if (bogon) color2 else color1
                                textSize = 22f
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                        }
                        gravity = Gravity.CENTER
                        padding = dip(40)
                    }
                }
            }.show()
        }
    }

    override fun networkStatusChange(network: NetworkInfo.Network) {
        when(network.status){
            NetworkInfo.NetworkStatus.INTERNET -> {
//                longToast("ONLINE: ${network.type}")
                updateMasterMarker()
            }
            NetworkInfo.NetworkStatus.OFFLINE -> {
//                longToast("OFFLINE: ${network.type}")
            }
        }
    }

    @MainThread
    fun positionAndFlagForSelectedMarker(): Pair<LatLng?, String?> {
        if (mMap != null && items.count() != 0) {
            for (item in items) {
                if (item.zIndex == 1.0f) {
                    return Pair(item.position, item.tag.toString())
                }
            }
        }

        return Pair(null, null)
    }

    fun updateMasterMarker() {
        val p0 = mMarker
        val googleMap = mMap
        if (p0 != null && googleMap != null) {
            doAsync {
                var json1: JSONObject? = null

                if (networkInfo!!.getNetwork().status == NetworkInfo.NetworkStatus.INTERNET)
                {
                    json1 = createJson1()
                }

                uiThread {
                    if (json1 != null) {
                        if (p0.isInfoWindowShown) {
                            p0.hideInfoWindow()
                        }

                        val country = json1.getString("country")
                        val city = json1.getString("city")
                        val lat = json1.getDouble("latitude")
                        val lon = json1.getDouble("longitude")
                        val emoji = json1.getString("emoji_flag")
                        val ip = json1.getString("ip")
                        //val threat = json1.optJSONObject("threat")

                        p0.position = LatLng(lat, lon)
                        p0.snippet = "$ip${Typography.ellipsis}"
                        p0.tag = json1
                        p0.title = "$emoji $city".trimStart()

                        googleMap.animateCamera(CameraUpdateFactory.newLatLng(p0.position),
                                object : CancelableCallback {
                                    override fun onFinish() {
                                        longToast("Animation to $country complete")
                                        mMarker!!.showInfoWindow()
                                    }

                                    override fun onCancel() {
                                        longToast("Animation to $country canceled")
                                        mMarker!!.showInfoWindow()
                                    }
                                })
                    }
                }
            }
        }
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
