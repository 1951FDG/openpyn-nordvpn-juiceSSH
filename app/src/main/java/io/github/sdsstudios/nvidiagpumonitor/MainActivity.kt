package io.github.sdsstudios.nvidiagpumonitor

import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.constraint.ConstraintLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
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
import com.google.android.gms.maps.model.*
import com.sonelli.juicessh.pluginlibrary.listeners.OnClientStartedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import io.github.sdsstudios.nvidiagpumonitor.ConnectionManager.Companion.JUICESSH_REQUEST_CODE
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_maps.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.indefiniteSnackbar
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
        GoogleMap.OnInfoWindowClickListener {
    companion object {
        private const val READ_CONNECTIONS = "com.sonelli.juicessh.api.v1.permission.READ_CONNECTIONS"
        private const val OPEN_SESSIONS = "com.sonelli.juicessh.api.v1.permission.OPEN_SESSIONS"
        private const val PERMISSION_REQUEST_CODE = 23
        private const val JUICE_SSH_PACKAGE_NAME = "com.sonelli.juicessh"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 0
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

    var mMap: GoogleMap? = null

    var mMarker: Marker? = null

    //private lateinit var offlineTileProvider: ExpandedMBTilesTileProvider
    private lateinit var offlineTileProvider: MapBoxOfflineTileProvider

    private val REQUEST_GOOGLE_PLAY_SERVICES = 1972

    private val items by lazy { ArrayList<Marker>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val mDBHelper = DatabaseHelper(this)

        try {
            val file = File(this.getExternalFilesDir(null),"nordvpn.json")
            val mInput = this.resources.openRawResource(R.raw.nordvpn)
            val mOutput = FileOutputStream(file)
            mInput.copyTo(mOutput, 1024)
            mOutput.flush()
            mOutput.close()
            mInput.close()
        } catch (mIOException: IOException) {
            throw Error("UnableToUpdateDatabase")
        }

        // Get a File reference to the MBTiles file.
        val myMBTiles = File(mDBHelper.DB_PATH + mDBHelper.DB_NAME)
        //Log.e(TAG, myMBTiles.path)
        //Log.e(TAG, mDBHelper.checkDataBase().toString())

        //offlineTileProvider = ExpandedMBTilesTileProvider(myMBTiles, 256, 256)

        offlineTileProvider = MapBoxOfflineTileProvider(myMBTiles)
        Log.e(TAG, offlineTileProvider.minimumZoom.toString())
        Log.e(TAG, offlineTileProvider.maximumZoom.toString())

        map.onCreate(savedInstanceState)

        val api = GoogleApiAvailability.getInstance()
        val errorCode = api.isGooglePlayServicesAvailable(this)

        when {
            errorCode == ConnectionResult.SUCCESS -> onActivityResult(REQUEST_GOOGLE_PLAY_SERVICES, AppCompatActivity.RESULT_OK, null)
            api.isUserResolvableError(errorCode) -> api.showErrorDialogFragment(this, errorCode, REQUEST_GOOGLE_PLAY_SERVICES)
            else -> this.longToast(api.getErrorString(errorCode))
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

            buttonConnect.setOnClickListener {
                if (mConnectionListAdapter.count == 0) {
                    toast(R.string.error_must_have_atleast_one_server)

                    return@setOnClickListener
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

    override fun onResume() {
        super.onResume()
        map.onResume()

        if (!isJuiceSSHInstalled()) {
            indefiniteSnackbar(findViewById<View>(android.R.id.content), getString(R.string.error_must_install_juicessh), "OK") { juiceSSHInstall() }
        }
    }

    override fun onPause() {
        super.onPause()
        map.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        map.onDestroy()
        offlineTileProvider.close()

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
        if (id == R.id.action_settings) {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            return true
        } else {
            return super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == JUICESSH_REQUEST_CODE) {
            mConnectionManager.gotActivityResult(requestCode, resultCode, data)
        }

        if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                map.visibility = View.VISIBLE

                //val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                //val watermark = mapFragment.view?.findViewWithTag<ImageView>("GoogleWatermark")
                val watermark = map.findViewWithTag<ImageView>("GoogleWatermark")

                if (watermark != null) {
                    val params = watermark.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                    params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_START, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_END, 0)
                }

                //val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                //mapFragment.getMapAsync(this)
                map.getMapAsync(this)
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
            val map = mMap
            if (map != null) {
                updateMasterMarker(map)
            }
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

    private fun onPermissionsGranted() {
        mConnectionManager.startClient(onClientStartedListener = this)

        buttonConnect.applyConnectStyle()

        spinnerConnectionList.adapter = mConnectionListAdapter

        supportLoaderManager.initLoader(0, null, ConnectionListLoader(
                mCtx = this,
                mLoaderFinishCallback = this
        ))
    }

//    private fun AppCompatTextView.setData(value: Int?, suffix: String) {
//        if (value == null) {
//            setText(R.string.no_data)
//        } else {
//            val data = "$value $suffix"
//            text = data
//        }
//    }

    private fun requestPermissions() {
        mReadConnectionsPerm = hasPermission(READ_CONNECTIONS)
        mOpenSessionsPerm = hasPermission(OPEN_SESSIONS)

        if (!mReadConnectionsPerm || !mOpenSessionsPerm) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(READ_CONNECTIONS, OPEN_SESSIONS),
                    PERMISSION_REQUEST_CODE)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun isJuiceSSHInstalled(): Boolean {
        try {
            packageManager.getPackageInfo(JUICE_SSH_PACKAGE_NAME, 0)
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            return false
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

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        val params = buttonConnect.layoutParams as ConstraintLayout.LayoutParams

        //googleMap.addTileOverlay(TileOverlayOptions().tileProvider(offlineTileProvider).fadeIn(false))
        //googleMap.setMaxZoomPreference(6.0f)

        googleMap.addTileOverlay(TileOverlayOptions().tileProvider(offlineTileProvider).fadeIn(false))
        googleMap.setMaxZoomPreference(offlineTileProvider.maximumZoom.toFloat())

        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }

        googleMap.setOnCameraIdleListener(this)
        googleMap.setOnInfoWindowClickListener(this)
        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnMapLoadedCallback(this)
        googleMap.setPadding(0,0,0,params.height + params.bottomMargin)
        googleMap.mapType = MAP_TYPE_NORMAL
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true
        //googleMap.isMyLocationEnabled = true

        mMap = googleMap
    }

    override fun onMapLoaded() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        //val server = preferences.getString("pref_server", "")
        //val country_code = preferences.getString("pref_country", "")
        //val country = args.country
        //val area = args.area
        //val tcp = preferences.getBoolean("pref_tcp", false)
        ////val max_load = preferences.getString("pref_max_load", "") *
        ////val top_servers = preferences.getString("pref_top_servers", "") *
        //val pings = preferences.getString("pref_pings", "")
        //val force_fw_rules = preferences.getBoolean("pref_force_fw", false)
        val p2p = preferences.getBoolean("pref_p2p", false)
        val dedicated = preferences.getBoolean("pref_dedicated", false)
        val double_vpn = preferences.getBoolean("pref_double", false)
        val tor_over_vpn = preferences.getBoolean("pref_tor", false)
        val anti_ddos = preferences.getBoolean("pref_anti_ddos", false)
        ////val netflix = preferences.getBoolean("pref_netflix", false)
        //val test = preferences.getBoolean("pref_test", false)
        //val internally_allowed = args.internally_allowed
        //val skip_dns_patch = preferences.getBoolean("pref_skip_dns_patch", false)
        //val silent = preferences.getBoolean("pref_silent", false)
        //val nvram = preferences.getBoolean("pref_nvram", false)

        val file = File(this.getExternalFilesDir(null),"nordvpn.json")
        Log.d(TAG, file.toString())

        lateinit var json: String

        try {
            val fis = FileInputStream(file)
            val bytes = fis.readBytes()
            fis.close()
            json = String(bytes, Charsets.UTF_8)
            Log.d(TAG, json)
        } catch (e: IOException) {
            RuntimeException(e)
        }

        lateinit var jsonArr: JSONArray

        try {
            jsonArr = JSONArray(json)
        } catch (e: JSONException) {
            RuntimeException(e)
        }


        operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

        for (res in jsonArr) {
            /*
            val country = res.getString("flag").toLowerCase()

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

            val country = res.getString("flag").toLowerCase()
            //Log.d(TAG, country)

            val location = res.getJSONObject("location")
            //Log.d(TAG, location.toString())

            val var1 = MarkerOptions()
            var1.position(LatLng(location.getDouble("lat"), location.getDouble("long")))
            var1.visible(false)
            var1.flat(true)

            val marker = mMap!!.addMarker(var1)
            marker.tag = country

            items.add(marker)
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                mMap!!.addMarker(MarkerOptions().position(LatLng(location.latitude, location.longitude)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)).zIndex(0.5f))
                //mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 5.0f))
            }
        }

        val list: ArrayList<String> = ArrayList()
        list.add("https://api.ipdata.co")
        list.add("http://ip-api.com/json")

        Thread({
            val json1 = JSONObject()

            for (name in list) {
                val (_, _, result) = name.httpGet().responseJson() // result is Result<Json, FuelError>
                val (data, error) = result
                if (data != null) {
                    Log.e(TAG, "Success")
                    val content = data.obj()
                    Log.e(TAG, content.toString())

                    var country = content.optString("country_name")
                    var city = content.optString("city")
                    var lat = content.optDouble("latitude", 0.0)
                    var lon = content.optDouble("longitude", 0.0)
                    var emoji = content.optString("emoji_flag")
                    var ip = content.optString("ip")
                    var threat = content.optJSONObject("threat")

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
                    Log.e(TAG, "Failure")
                    Log.e(TAG, error.toString())
                }
            }

            runOnUiThread({
                val country = json1.getString("country")
                val city = json1.getString("city")
                val lat = json1.getDouble("latitude")
                val lon = json1.getDouble("longitude")
                val emoji = json1.getString("emoji_flag")
                val ip = json1.getString("ip")
                //val threat = json1.optJSONObject("threat")

                val var1 = MarkerOptions()
                var1.position(LatLng(lat, lon))
                var1.title("$emoji $city".trimStart())
                var1.snippet("$ip${Typography.ellipsis}")
                var1.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                var1.zIndex(1.0f)
                var1.flat(true)

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
            })
        }).start()
    }


    fun updateMasterMarker(googleMap: GoogleMap) {
        val list: ArrayList<String> = ArrayList()
        list.add("https://api.ipdata.co")
        list.add("http://ip-api.com/json")

        Thread({
            val json1 = JSONObject()

            for (name in list) {
                val (_, _, result) = name.httpGet().responseJson() // result is Result<Json, FuelError>
                val (data, error) = result
                if (data != null) {
                    Log.e(TAG, "Success")
                    val content = data.obj()
                    Log.e(TAG, content.toString())

                    var country = content.optString("country_name")
                    var city = content.optString("city")
                    var lat = content.optDouble("latitude", 0.0)
                    var lon = content.optDouble("longitude", 0.0)
                    var emoji = content.optString("emoji_flag")
                    var ip = content.optString("ip")
                    var threat = content.optJSONObject("threat")

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
                    Log.e(TAG, "Failure")
                    Log.e(TAG, error.toString())
                }
            }

            runOnUiThread({
                val country = json1.getString("country")
                val city = json1.getString("city")
                val lat = json1.getDouble("latitude")
                val lon = json1.getDouble("longitude")
                val emoji = json1.getString("emoji_flag")
                val ip = json1.getString("ip")
                //val threat = json1.optJSONObject("threat")

                if (mMarker!!.isInfoWindowShown) {
                    mMarker!!.hideInfoWindow()
                }

                mMarker!!.tag = json1
                mMarker!!.position = LatLng(lat, lon)
                mMarker!!.title = "$emoji $city".trimStart()
                mMarker!!.snippet = "$ip${Typography.ellipsis}"

                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mMarker!!.position, googleMap.cameraPosition.zoom), 3000,
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
            })
        }).start()
    }
/*
    private fun createJson0() {
        //an extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
        "https://api.nordvpn.com/server".httpGet().responseJson { request, response, result ->
            when (result) {
                is Result.Failure -> {
                    Log.e(TAG, "Failure")
                    val ex = result.getException()
                }
                is Result.Success -> {
                    Log.e(TAG, "Success")
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
                            //Log.e(TAG, location.toString())
                            marker.tag = value

                            items.add(marker)
                        }
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

//                    val file = File(this.getExternalFilesDir(null),"output.json")
//                    file.writeText(jsonObj.toString())
//                    Log.d(TAG, file.toString())
                }
            }
        }
    }
*/
    private fun createJson() {
        //an extension over string (support GET, PUT, POST, DELETE with httpGet(), httpPut(), httpPost(), httpDelete())
        "https://api.nordvpn.com/server".httpGet().responseJson { _, _, result ->
            when (result) {
                is Result.Failure -> {
                    Log.e(TAG, "Failure")
                    result.getException().printStackTrace()
                }
                is Result.Success -> {
                    Log.e(TAG, "Success")
                    operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()
                    val jsonObj = JSONObject()
                    val content = result.get().array() //JSONArray
                    for (res in content) {
                        val location = res.getJSONObject("location")

                        var json1 = jsonObj.optJSONObject(location.toString())
                        if (json1 == null) {
                            json1 = JSONObject()
                            json1.put("flag", res.getString("flag"))
                            json1.put("country", res.getString("country"))
                            json1.put("location", res.getJSONObject("location"))

                            val features = JSONObject()
                            features.put("p2p", false)
                            features.put("dedicated", false)
                            features.put("double_vpn", false)
                            features.put("tor_over_vpn", false)
                            features.put("anti_ddos", false)
                            features.put("standard", false)

                            val categories = res.getJSONArray("categories")

                            for (category in categories) {
                                val name = category.getString("name")

                                if (name.equals("P2P", true)) {
                                    features.put("p2p", true)
                                }
                                else if (name.equals("Dedicated IP servers", true)) {
                                    features.put("dedicated", true)
                                }
                                else if (name.equals("Double VPN", true)) {
                                    features.put("double_vpn", true)
                                }
                                else if (name.equals("Obfuscated Servers", true)) {
                                    features.put("tor_over_vpn", true)
                                }
                                else if (name.equals("Anti DDoS", true)) {
                                    features.put("anti_ddos", true)
                                }
                                else if (name.equals("Standard VPN servers", true)) {
                                    features.put("standard", true)
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

                                if (name.equals("P2P", true)) {
                                    features.put("p2p", true)
                                }
                                else if (name.equals("Dedicated IP servers", true)) {
                                    features.put("dedicated", true)
                                }
                                else if (name.equals("Double VPN", true)) {
                                    features.put("double_vpn", true)
                                }
                                else if (name.equals("Obfuscated Servers", true)) {
                                    features.put("tor_over_vpn", true)
                                }
                                else if (name.equals("Anti DDoS", true)) {
                                    features.put("anti_ddos", true)
                                }
                                else if (name.equals("Standard VPN servers", true)) {
                                    features.put("standard", true)
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

                            val newjsonObjLast = JSONObject()

                            newjsonObjLast.put("flag", value.getString("flag"))
                            newjsonObjLast.put("country", value.getString("country"))
                            newjsonObjLast.put("location", value.getJSONObject("location"))

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

                            newjsonObjLast.put("categories", jsonArr)

                            jsonObjLast.put(newjsonObjLast)
                        }
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                    val file = File(this.getExternalFilesDir(null),"nordvpn.json")
                    file.writeText(jsonObjLast.toString())
                    Log.d(TAG, file.toString())
                }
            }
        }
    }

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
            Log.d(TAG, p0.tag.toString())
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
        if(items.count() != 0) {
            val bounds = mMap!!.projection.visibleRegion.latLngBounds
            for (item in items) {
                if (bounds.contains(item.position)) {
                    if (!item.isVisible) {
                        item.isVisible = true
                    }
                }
                else {
                    if (item.isVisible) {
                        item.isVisible = false
                    }
                }
            }
        }
    }

    override fun onInfoWindowClick(p0: Marker?) {
        val jsonObj = p0?.tag as JSONObject
        Log.d(TAG, jsonObj.toString())

        val threat = jsonObj.optJSONObject("threat")

        if (threat != null) {
            val is_tor = threat.getBoolean("is_tor")
            val is_proxy = threat.getBoolean("is_proxy")
            val is_anonymous = threat.getBoolean("is_anonymous")
            val is_known_attacker = threat.getBoolean("is_known_attacker")
            val is_known_abuser = threat.getBoolean("is_known_abuser")
            val is_threat = threat.getBoolean("is_threat")
            val is_bogon = threat.getBoolean("is_bogon")

            val color1 = ContextCompat.getColor(this, R.color.colorConnect)
            val color2 = ContextCompat.getColor(this, R.color.colorDisconnect)

            alert {
                customView {
                    verticalLayout {
                        linearLayout {
                            textView {
                                text = "is_tor"
                                textSize = 22f
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                            textView {
                                text = if (is_tor) "YES" else "NO"
                                textColor = if (is_tor) color2 else color1
                                textSize = 22f
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = "is_proxy"
                                textSize = 22f
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                            textView {
                                text = if (is_proxy) "YES" else "NO"
                                textColor = if (is_proxy) color2 else color1
                                textSize = 22f
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = "is_anonymous"
                                textSize = 22f
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                            textView {
                                text = if (is_anonymous) "YES" else "NO"
                                textColor = if (is_anonymous) color2 else color1
                                textSize = 22f
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = "is_known_attacker"
                                textSize = 22f
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                            textView {
                                text = if (is_known_attacker) "YES" else "NO"
                                textColor = if (is_known_attacker) color2 else color1
                                textSize = 22f
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = "is_known_abuser"
                                textSize = 22f
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                            textView {
                                text = if (is_known_abuser) "YES" else "NO"
                                textColor = if (is_known_abuser) color2 else color1
                                textSize = 22f
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = "is_threat"
                                textSize = 22f
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                            textView {
                                text = if (is_threat) "YES" else "NO"
                                textColor = if (is_threat) color2 else color1
                                textSize = 22f
                                gravity = Gravity.END
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                        }
                        linearLayout {
                            textView {
                                text = "is_bogon"
                                textSize = 22f
                                gravity = Gravity.START
                            }.lparams(width = wrapContent, height = wrapContent, weight = 1.0f) {
                            }
                            textView {
                                text = if (is_bogon) "YES" else "NO"
                                textColor = if (is_bogon) color2 else color1
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
