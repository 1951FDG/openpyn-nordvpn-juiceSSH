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
import android.support.annotation.WorkerThread
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
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
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
import java.io.FileInputStream
import java.io.FileOutputStream
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
        AnkoLogger {
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

        //this.createJson()
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
                        val file = File(getExternalFilesDir(null), resources.getResourceEntryName(id) + ext)

                        if (!file.exists()) {
                            try {
                                val mInput = resources.openRawResource(id)
                                val mOutput = FileOutputStream(file)
                                mInput.copyTo(mOutput, 1024)
                                mOutput.flush()
                                mOutput.close()
                                mInput.close()
                            } catch (e: IOException) {
                                error(e)
                            }
                        }
                    }

                    uiThread {
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

        // Create a new CameraUpdateAnimator for a given mMap
        // with an OnCameraIdleListener to set when the animation ends
        val animator = CameraUpdateAnimator(mMap, this)
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
        // Execute the animation and set the final OnCameraIdleListener
        animator.execute()

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

        val file = File(this.getExternalFilesDir(null),resources.getResourceEntryName(R.raw.nordvpn) + ".json")

        var jsonArr: JSONArray? = null

        try {
            val fis = FileInputStream(file)
            val bytes = fis.readBytes()
            fis.close()
            val json = String(bytes, Charsets.UTF_8)
            jsonArr = JSONArray(json)
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
                mMap!!.addMarker(MarkerOptions().position(LatLng(location.latitude, location.longitude)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)).zIndex(0.5f))
                //mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 5.0f))
            }
        }
        }

        doAsync {
            val json1 = JSONObject()

            for (name in listOf("https://api.ipdata.co", "http://ip-api.com/json")) {
                val timeout = 1000 // 1000 milliseconds = 1 second
                val (_, _, result) = name.httpGet().timeout(timeout).responseJson()
                val (data, error) = result
                if (data != null) {
                    val content = data.obj()
                    debug(content)

                    var country = content.optString("country_name")
                    val city = content.optString("city")
                    var lat = content.optDouble("latitude", 0.0)
                    var lon = content.optDouble("longitude", 0.0)
                    val emoji = content.optString("emoji_flag")
                    var ip = content.optString("ip")
                    val threat = content.optJSONObject("threat")

                    if (country.isEmpty()) country = content.optString("country")
                    //if (city.isEmpty()) city = content.optString("city")
                    if (lat == 0.0) lat = content.optDouble("lat", 0.0)
                    if (lon == 0.0) lon = content.optDouble("lon", 0.0)
                    //if (emoji.isEmpty()) emoji = content.optString("emoji_flag")
                    if (ip.isEmpty()) ip = content.optString("query")
                    //if (threat == null) threat = content.optJSONObject("threat")

                    if (json1.optString("country").isEmpty()) json1.put("country", country)
                    if (json1.optString("city").isEmpty()) json1.put("city", city)
                    if (json1.optDouble("latitude", 0.0) == 0.0) json1.put("latitude", lat)
                    if (json1.optDouble("longitude", 0.0) == 0.0) json1.put("longitude", lon)
                    if (json1.optString("emoji_flag").isEmpty()) json1.put("emoji_flag", emoji)
                    if (json1.optString("ip").isEmpty()) json1.put("ip", ip)
                    if (json1.optJSONObject("threat") == null) json1.putOpt("threat", threat)

                    //break
                }
                else {
                    error(error)
                }
            }

            uiThread {
                if (json1.keys().hasNext()) {
                val country = json1.getString("country")
                val city = json1.getString("city")
                val lat = json1.getDouble("latitude")
                val lon = json1.getDouble("longitude")
                val emoji = json1.getString("emoji_flag")
                val ip = json1.getString("ip")
                //val threat = json1.optJSONObject("threat")

                val var1 = MarkerOptions().apply {
                    position(LatLng(lat, lon))
                    title("$emoji $city".trimStart())
                    snippet("$ip${Typography.ellipsis}")
                    icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    zIndex(1.0f)
                    flat(true)
                }

                val marker = mMap!!.addMarker(var1)
                marker.tag = json1

                mMarker = marker

                mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(var1.position, mMap!!.cameraPosition.zoom), 3000,
                        object : CancelableCallback {
                            override fun onFinish() {
                                baseContext.longToast("Animation to $country complete")
                                mMarker!!.showInfoWindow()
                            }

                            override fun onCancel() {
                                baseContext.longToast("Animation to $country canceled")
                                mMarker!!.showInfoWindow()
                            }
                        })
                }
            }
        }
    }


    fun updateMasterMarker() {
        val p0 = mMarker
        val googleMap = mMap
        if (p0 != null && googleMap != null) {
        doAsync {
            val json1 = JSONObject()

            for (name in listOf("https://api.ipdata.co", "http://ip-api.com/json")) {
                val (_, _, result) = name.httpGet().responseJson()
                val (data, error) = result
                if (data != null) {
                    val content = data.obj()
                    debug(content)

                    var country = content.optString("country_name")
                    val city = content.optString("city")
                    var lat = content.optDouble("latitude", 0.0)
                    var lon = content.optDouble("longitude", 0.0)
                    val emoji = content.optString("emoji_flag")
                    var ip = content.optString("ip")
                    val threat = content.optJSONObject("threat")

                    if (country.isEmpty()) country = content.optString("country")
                    //if (city.isEmpty()) city = content.optString("city")
                    if (lat == 0.0) lat = content.optDouble("lat", 0.0)
                    if (lon == 0.0) lon = content.optDouble("lon", 0.0)
                    //if (emoji.isEmpty()) emoji = content.optString("emoji_flag")
                    if (ip.isEmpty()) ip = content.optString("query")
                    //if (threat == null) threat = content.optJSONObject("threat")

                    if (json1.optString("country").isEmpty()) json1.put("country", country)
                    if (json1.optString("city").isEmpty()) json1.put("city", city)
                    if (json1.optDouble("latitude", 0.0) == 0.0) json1.put("latitude", lat)
                    if (json1.optDouble("longitude", 0.0) == 0.0) json1.put("longitude", lon)
                    if (json1.optString("emoji_flag").isEmpty()) json1.put("emoji_flag", emoji)
                    if (json1.optString("ip").isEmpty()) json1.put("ip", ip)
                    if (json1.optJSONObject("threat") == null) json1.putOpt("threat", threat)

                    //break
                } else {
                    error(error)
                }
            }

            uiThread {
                val country = json1.getString("country")
                val city = json1.getString("city")
                val lat = json1.getDouble("latitude")
                val lon = json1.getDouble("longitude")
                val emoji = json1.getString("emoji_flag")
                val ip = json1.getString("ip")
                //val threat = json1.optJSONObject("threat")

                if (p0.isInfoWindowShown) {
                    p0.hideInfoWindow()
                }

                p0.tag = json1
                p0.position = LatLng(lat, lon)
                p0.title = "$emoji $city".trimStart()
                p0.snippet = "$ip${Typography.ellipsis}"

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(p0.position, googleMap.cameraPosition.zoom), 3000,
                        object : CancelableCallback {
                            override fun onFinish() {
                                baseContext.longToast("Animation to $country complete")
                                p0.showInfoWindow()
                            }

                            override fun onCancel() {
                                baseContext.longToast("Animation to $country canceled")
                                p0.showInfoWindow()
                            }
                        })
            }
        }
        }
    }
/*
    private fun createJson0() {
        //an extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
        "https://api.nordvpn.com/server".httpGet().responseJson { request, response, result ->
            when (result) {
                is Result.Failure -> {
                    val ex = result.getException()
                }
                is Result.Success -> {
                    operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()
                    val jsonObj = JSONObject()
                    val content = result.get().array() //JSONArray
                    for (res in content) {
                        val country = res.getString("domain").take(2)

                        var pass = country.equals(country_code,true)

                        if (!pass) {
                            //continue
                        }

                        pass = when {
                            p2p -> false
                            dedicated -> false
                            double_vpn -> false
                            tor_over_vpn -> false
                            anti_ddos -> false
                            else -> true
                        }

                        if (!pass) {
                            val categories = res.getJSONArray("categories")

                            for (category in categories) {
                                val name = category.getString("name")

                                if (p2p and name.equals("P2P", true)) {
                                    pass = true
                                    break
                                }
                                else if (dedicated and name.equals("Dedicated IP servers", true)) {
                                    pass = true
                                    break
                                }
                                else if (double_vpn and name.equals("Double VPN", true)) {
                                    pass = true
                                    break
                                }
                                else if (tor_over_vpn and name.equals("Obfuscated Servers", true)) {
                                    pass = true
                                    break
                                }
                                else if (anti_ddos and name.equals("Anti DDoS", true)) {
                                    pass = true
                                    break
                                }
                            }
                        }

                        if (!pass) {
                            continue
                        }

                        val location = res.getJSONObject("location")

                        var jsonArr = jsonObj.optJSONArray(location.toString())
                        if (jsonArr == null) {
                            jsonArr = JSONArray()
                            jsonArr.put(res)
                            jsonObj.put(location.toString(), jsonArr)
                        }
                        else {
                            jsonArr.put(res)
                        }
                    }

                    try {
                        val keys = jsonObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = jsonObj.getJSONArray(key)
                            val location = value.getJSONObject(0).getJSONObject("location")
                            val marker = mMap.addMarker(MarkerOptions().position(LatLng(location.getDouble("lat"), location.getDouble("long"))).visible(false))
                            marker.tag = value

                            items.add(marker)
                        }
                    } catch (e: JSONException) {
                        error(e)
                    }

//                    val file = File(this.getExternalFilesDir(null),"output.json")
//                    file.writeText(jsonObj.toString())
//                    debug(file)
                }
            }
        }
    }
*/

    @WorkerThread
    private fun createJson() {
        //an extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
        "https://api.nordvpn.com/server".httpGet().responseJson { _, _, result ->
            when (result) {
                is Result.Failure -> {
                    error(result.getException())
                }
                is Result.Success -> {
                    operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()
                    val jsonObj = JSONObject()
                    val content = result.get().array() //JSONArray
                    for (res in content) {
                        val location = res.getJSONObject("location")

                        var json1: JSONObject? = jsonObj.optJSONObject(location.toString())
                        if (json1 == null) {
                            json1 = JSONObject().apply {
                                put("flag", res.getString("flag"))
                                put("country", res.getString("country"))
                                put("location", res.getJSONObject("location"))
                            }

                            val features = JSONObject().apply {
                                put("p2p", false)
                                put("dedicated", false)
                                put("double_vpn", false)
                                put("tor_over_vpn", false)
                                put("anti_ddos", false)
                                put("standard", false)
                            }

                            val categories = res.getJSONArray("categories")

                            for (category in categories) {
                                val name = category.getString("name")

                                when {
                                    name.equals("P2P", true) -> features.put("p2p", true)
                                    name.equals("Dedicated IP servers", true) -> features.put("dedicated", true)
                                    name.equals("Double VPN", true) -> features.put("double_vpn", true)
                                    name.equals("Obfuscated Servers", true) -> features.put("tor_over_vpn", true)
                                    name.equals("Anti DDoS", true) -> features.put("anti_ddos", true)
                                    name.equals("Standard VPN servers", true) -> features.put("standard", true)
                                }
                            }

                            json1.put("features", features)

                            jsonObj.put(location.toString(), json1)
                        }
                        else {

                            val features = json1.getJSONObject("features")

                            val categories = res.getJSONArray("categories")

                            for (category in categories) {
                                val name = category.getString("name")

                                when {
                                    name.equals("P2P", true) -> features.put("p2p", true)
                                    name.equals("Dedicated IP servers", true) -> features.put("dedicated", true)
                                    name.equals("Double VPN", true) -> features.put("double_vpn", true)
                                    name.equals("Obfuscated Servers", true) -> features.put("tor_over_vpn", true)
                                    name.equals("Anti DDoS", true) -> features.put("anti_ddos", true)
                                    name.equals("Standard VPN servers", true) -> features.put("standard", true)
                                }
                            }
                        }
                    }

                    val jsonObjLast = JSONArray()

                    try {
                        val keys = jsonObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = jsonObj.getJSONObject(key)

                            val jsonArr = JSONArray()
                            val features = value.getJSONObject("features")

                            if (features.getBoolean("anti_ddos")) {
                                jsonArr.put(JSONObject().put("name", "Anti DDoS"))
                            }

                            if (features.getBoolean("dedicated")) {
                                jsonArr.put(JSONObject().put("name", "Dedicated IP servers"))
                            }

                            if (features.getBoolean("double_vpn")) {
                                jsonArr.put(JSONObject().put("name", "Double VPN"))
                            }

                            if (features.getBoolean("tor_over_vpn")) {
                                jsonArr.put(JSONObject().put("name", "Obfuscated Servers"))
                            }

                            if (features.getBoolean("p2p")) {
                                jsonArr.put(JSONObject().put("name", "P2P"))
                            }

                            if (features.getBoolean("standard")) {
                                jsonArr.put(JSONObject().put("name", "Standard VPN servers"))
                            }

                            val objLast = JSONObject().apply {
                                put("flag", value.getString("flag"))
                                put("country", value.getString("country"))
                                put("location", value.getJSONObject("location"))
                                put("categories", jsonArr)
                            }

                            jsonObjLast.put(objLast)
                        }
                    } catch (e: JSONException) {
                        error(e)
                    }

                    val text = jsonObjLast.toString()
                    debug(text)

                    val file = File(this.getExternalFilesDir(null),resources.getResourceEntryName(R.raw.nordvpn) + ".json")
                    debug(file)

                    file.writeText(text)
                }
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
