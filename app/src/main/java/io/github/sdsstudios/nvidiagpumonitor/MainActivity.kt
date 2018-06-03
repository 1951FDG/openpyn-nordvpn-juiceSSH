package io.github.sdsstudios.nvidiagpumonitor

//import kotlinx.android.synthetic.main.content_main.*
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.Cursor
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.annotation.IdRes
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
//import android.support.v7.widget.AppCompatTextView
import android.util.Log
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.*
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.sonelli.juicessh.pluginlibrary.listeners.OnClientStartedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import io.github.sdsstudios.nvidiagpumonitor.ConnectionManager.Companion.JUICESSH_REQUEST_CODE
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_maps.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity(),
        OnSessionStartedListener,
        OnSessionFinishedListener,
        OnClientStartedListener,
        ConnectionListLoaderFinishedCallback,
        OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnCameraIdleListener {
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

    private val mConnectionListAdapter by lazy { ConnectionListAdapter(this) }

    private val mPermissionsGranted
        get() = mReadConnectionsPerm && mOpenSessionsPerm

    private val mView: MapView by bind(R.id.map)

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    //private lateinit var offlineTileProvider: ExpandedMBTilesTileProvider
    private lateinit var offlineTileProvider: MapBoxOfflineTileProvider

    private val REQUEST_GOOGLE_PLAY_SERVICES = 1972

    private val items =  ArrayList<Marker>()

    private fun <T : View> Activity.bind(@IdRes idRes: Int): Lazy<T> {
        @Suppress("UNCHECKED_CAST")
        return lazy(LazyThreadSafetyMode.NONE){ findViewById<T>(idRes) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mDBHelper = DatabaseHelper(this)

        try {
            mDBHelper.updateDataBase()
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

        mView.onCreate(savedInstanceState)

        setSupportActionBar(toolbar)

        val api = GoogleApiAvailability.getInstance()
        val errorCode = api.isGooglePlayServicesAvailable(this)

        when {
            errorCode == ConnectionResult.SUCCESS -> onActivityResult(REQUEST_GOOGLE_PLAY_SERVICES, Activity.RESULT_OK, null)
            api.isUserResolvableError(errorCode) -> api.showErrorDialogFragment(this, errorCode, REQUEST_GOOGLE_PLAY_SERVICES)
            else -> this.longToast(api.getErrorString(errorCode))
        }

        if (isJuiceSSHInstalled()) {

            textViewErrorMessage.setText(R.string.error_must_enable_permissions)

            requestPermissions()

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

            if (mPermissionsGranted) {
                onPermissionsGranted()
            }

            buttonConnect.setOnClickListener {
                if (mConnectionListAdapter.count == 0) {
                    toast(R.string.error_must_have_atleast_one_server)

                    return@setOnClickListener
                }

                if (mPermissionsGranted) {
                    buttonConnect.applyConnectingStyle()

                    val uuid = mConnectionListAdapter
                            .getConnectionId(spinnerConnectionList.selectedItemPosition)

                    mConnectionManager.toggleConnection(uuid = uuid!!, activity = this)

                } else {
                    requestPermissions()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mView.onResume()

        if (!isJuiceSSHInstalled()) {
            textViewErrorMessage.setText(R.string.error_must_install_juicessh)
        }
    }

    override fun onPause() {
        super.onPause()
        mView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        mView.onDestroy()
        offlineTileProvider.close()

        mConnectionManager.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mView.onLowMemory()
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

        if (mPermissionsGranted) onPermissionsGranted()
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
            if (resultCode === Activity.RESULT_OK) {
                mView.visibility = View.VISIBLE

                //val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                //val watermark = mapFragment.view?.findViewWithTag<ImageView>("GoogleWatermark")
                val watermark = mView.findViewWithTag<ImageView>("GoogleWatermark")

                if (watermark != null) {
                    val params = watermark.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0)
                    //params.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE)
                    //params.addRule(RelativeLayout.ALIGN_PARENT_END, 0)
                }

                //val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
                //mapFragment.getMapAsync(this)
                mView.getMapAsync(this)
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

        textViewErrorMessage.visibility = View.GONE
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
        mMap = googleMap

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
        googleMap.setOnMarkerClickListener(this)
        googleMap.setOnMapLoadedCallback(this)
        googleMap.setPadding(0,0,0,buttonConnect.height + buttonConnect.paddingBottom)
        googleMap.mapType = MAP_TYPE_NORMAL
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isZoomGesturesEnabled = true
        //googleMap.isMyLocationEnabled = true
    }

    override fun onMapLoaded() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        //val server = preferences.getString("pref_server", "")
        val country_code = preferences.getString("pref_country", "")
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
                    val json_response = result.get().array() //JSONArray
                    for (res in json_response) {
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

//                    val gson = GsonBuilder().setPrettyPrinting().create()
//                    val file = File(this.getExternalFilesDir(null),"output.json")
//                    file.writeText(gson.toJson(jsonObj))
//                    Log.d(TAG, file.toString())
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                mMap.addMarker(MarkerOptions().position(LatLng(location.latitude, location.longitude)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).zIndex(1.0f))
//                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 5.0f))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), mMap.cameraPosition.zoom), 3000,
                        object : CancelableCallback {
                            override fun onFinish() {
                                baseContext.longToast("Animation to Sydney complete")
                            }

                            override fun onCancel() {
                                baseContext.longToast("Animation to Sydney canceled")
                            }
                        })
            }
        }

        //this.createJson()
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
                    val json_response = result.get().array() //JSONArray
                    for (res in json_response) {
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
                    val json_response = result.get().array() //JSONArray
                    for (res in json_response) {
                        val location = res.getJSONObject("location")

                        var json1 = jsonObj.optJSONObject(location.toString())
                        if (json1 == null) {
                            json1 = JSONObject()
                            json1.put("flag", res.getString("flag"))
                            json1.put("country", res.getString("country"))
                            json1.put("location", res.getJSONObject("location"))

                            val jsonfeatures = JSONObject()
                            jsonfeatures.put("p2p", false)
                            jsonfeatures.put("dedicated", false)
                            jsonfeatures.put("double_vpn", false)
                            jsonfeatures.put("tor_over_vpn", false)
                            jsonfeatures.put("anti_ddos", false)

                            val categories = res.getJSONArray("categories")

                            for (category in categories) {
                                val name = category.getString("name")

                                if (name.equals("P2P", true)) {
                                    jsonfeatures.put("p2p", true)
                                }
                                else if (name.equals("Dedicated IP servers", true)) {
                                    jsonfeatures.put("dedicated", true)
                                }
                                else if (name.equals("Double VPN", true)) {
                                    jsonfeatures.put("double_vpn", true)
                                }
                                else if (name.equals("Obfuscated Servers", true)) {
                                    jsonfeatures.put("tor_over_vpn", true)
                                }
                                else if (name.equals("Anti DDoS", true)) {
                                    jsonfeatures.put("anti_ddos", true)
                                }
                                else if (name.equals("Standard VPN servers", true)) {
                                    jsonfeatures.put("standard", true)
                                }
                            }

                            json1.put("features", jsonfeatures)

                            jsonObj.put(location.toString(), json1)
                        }
                        else {

                            val jsonfeatures = json1.getJSONObject("features")

                            val categories = res.getJSONArray("categories")

                            for (category in categories) {
                                val name = category.getString("name")

                                if (name.equals("P2P", true)) {
                                    jsonfeatures.put("p2p", true)
                                }
                                else if (name.equals("Dedicated IP servers", true)) {
                                    jsonfeatures.put("dedicated", true)
                                }
                                else if (name.equals("Double VPN", true)) {
                                    jsonfeatures.put("double_vpn", true)
                                }
                                else if (name.equals("Obfuscated Servers", true)) {
                                    jsonfeatures.put("tor_over_vpn", true)
                                }
                                else if (name.equals("Anti DDoS", true)) {
                                    jsonfeatures.put("anti_ddos", true)
                                }
                                else if (name.equals("Standard VPN servers", true)) {
                                    jsonfeatures.put("standard", true)
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

                            val jsonfeatures = value.getJSONObject("features")

                            if (jsonfeatures.optBoolean("tor_over_vpn"))
                            {
                                val newjsonObjLast2 = JSONObject()
                                newjsonObjLast2.put("name", "Obfuscated Servers")

                                jsonArr.put(newjsonObjLast2)
                            }

                            if (jsonfeatures.optBoolean("anti_ddos"))
                            {
                                val newjsonObjLast2 = JSONObject()
                                newjsonObjLast2.put("name", "Anti DDoS")

                                jsonArr.put(newjsonObjLast2)
                            }

                            if (jsonfeatures.optBoolean("standard"))
                            {
                                val newjsonObjLast2 = JSONObject()
                                newjsonObjLast2.put("name", "Standard VPN servers")

                                jsonArr.put(newjsonObjLast2)
                            }

                            if (jsonfeatures.optBoolean("p2p"))
                            {
                                val newjsonObjLast2 = JSONObject()
                                newjsonObjLast2.put("name", "P2P")

                                jsonArr.put(newjsonObjLast2)
                            }

                            if (jsonfeatures.optBoolean("dedicated"))
                            {
                                val newjsonObjLast2 = JSONObject()
                                newjsonObjLast2.put("name", "Dedicated IP servers")

                                jsonArr.put(newjsonObjLast2)
                            }

                            newjsonObjLast.put("categories", jsonArr)

                            jsonObjLast.put(newjsonObjLast)

                        }
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                    val file = File(this.getExternalFilesDir(null),"outputtest.json")
                    file.writeText(jsonObjLast.toString())
                    Log.d(TAG, file.toString())
                }
            }
        }
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        //Log.e(TAG, p0?.tag.toString())
       return true
    }

    override fun onCameraIdle() {
        //Log.e(TAG, "i am called")
        if(mMap != null && items.count() != 0) {
            val bounds = mMap.projection.visibleRegion.latLngBounds
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
}
