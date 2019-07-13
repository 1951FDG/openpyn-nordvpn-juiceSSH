package io.github.getsixtyfour.openpyn

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.PreferenceManager
import com.adityaanand.morphdialog.MorphDialog
import com.adityaanand.morphdialog.utils.MorphDialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.ariascode.networkutility.NetworkInfo
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.michaelflisar.gdprdialog.GDPR
import com.michaelflisar.gdprdialog.GDPRConsentState
import com.michaelflisar.gdprdialog.GDPRDefinitions
import com.michaelflisar.gdprdialog.GDPRSetup
import com.michaelflisar.gdprdialog.helper.GDPRPreperationData
import com.sonelli.juicessh.pluginlibrary.PluginContract.Connections.PERMISSION_READ
import com.sonelli.juicessh.pluginlibrary.PluginContract.PERMISSION_OPEN_SESSIONS
import com.sonelli.juicessh.pluginlibrary.listeners.OnClientStartedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBar.OnActionClickListener
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import io.fabric.sdk.android.Fabric
import io.github.getsixtyfour.openpyn.map.MapFragment
import io.github.getsixtyfour.openpyn.utilities.Toaster
import io.github.getsixtyfour.openpyn.utilities.createJson
import io.github.getsixtyfour.openpyn.utilities.isJuiceSSHInstalled
import io.github.getsixtyfour.openpyn.utilities.juiceSSHInstall
import io.github.getsixtyfour.openpyn.utilities.logException
import io.github.getsixtyfour.openpyn.utilities.stringifyJsonArray
import io.github.sdsstudios.nvidiagpumonitor.ConnectionListAdapter
import io.github.sdsstudios.nvidiagpumonitor.ConnectionListLoader
import io.github.sdsstudios.nvidiagpumonitor.ConnectionListLoaderFinishedCallback
import io.github.sdsstudios.nvidiagpumonitor.ConnectionManager
import io.github.sdsstudios.nvidiagpumonitor.ConnectionManager.Companion.JUICESSH_REQUEST_CODE
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnCommandExecuteListener
import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate
import kotlinx.android.synthetic.main.activity_main.mainlayout
import kotlinx.android.synthetic.main.activity_main.spinnerConnectionList
import kotlinx.android.synthetic.main.activity_main.toolbar
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.error
import org.jetbrains.anko.info
import org.jetbrains.anko.longToast
import org.jetbrains.anko.onComplete
import org.jetbrains.anko.uiThread
import org.json.JSONArray
import tk.wasdennnoch.progresstoolbar.ProgressToolbar
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class MainActivity : AppCompatActivity(),
    AnkoLogger,
    ConnectionListLoaderFinishedCallback,
    GDPR.IGDPRCallback,
    OnClickListener,
    OnClientStartedListener,
    OnCommandExecuteListener,
    OnSessionExecuteListener,
    OnSessionFinishedListener,
    OnSessionStartedListener {
    private var dialog: MorphDialog? = null

    private val mConnectionListAdapter by lazy {
        ConnectionListAdapter(if (supportActionBar == null) this else supportActionBar!!.themedContext)
    }
    private var mConnectionManager: ConnectionManager? = null
    private val mSetup by lazy {
        GDPRSetup(GDPRDefinitions.FABRIC_CRASHLYTICS, GDPRDefinitions.FIREBASE_CRASH, GDPRDefinitions.FIREBASE_ANALYTICS)
            .withExplicitAgeConfirmation(true)
            .withForceSelection(true)
            .withShowPaidOrFreeInfoText(false)
    }
    private var snackProgressBarManager: SnackProgressBarManager? = null
    private val handler = Handler()
    private val runnable = Runnable {
        val fragment = getCurrentNavigationFragment() as? MapFragment
        fragment?.controlTower?.updateMasterMarker(true)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        showGDPRIfNecessary()

        setContentView(R.layout.activity_main)

        setProgressToolBar(toolbar)

        setSnackBarManager()

        setDefaultPreferences()
        val api = GoogleApiAvailability.getInstance()

        when (val errorCode = api.isGooglePlayServicesAvailable(this)) {
            ConnectionResult.SUCCESS -> onActivityResult(REQUEST_GOOGLE_PLAY_SERVICES, RESULT_OK, null)
            //api.isUserResolvableError(errorCode) -> api.showErrorDialogFragment(this, errorCode, REQUEST_GOOGLE_PLAY_SERVICES)
            else -> error(api.getErrorString(errorCode))
        }

        if (isJuiceSSHInstalled(this)) {
            if (hasPermission(PERMISSION_READ) && hasPermission(PERMISSION_OPEN_SESSIONS)) onPermissionsGranted()
        }
    }

    override fun onResume() {
        super.onResume()

        val snackProgressBar = snackProgressBarManager?.getLastShown()

        if (isJuiceSSHInstalled(this)) {
            if (hasPermission(PERMISSION_READ) && hasPermission(PERMISSION_OPEN_SESSIONS)) return

            when (snackProgressBar) {
                null -> snackProgressBarManager?.show(SNACK_BAR_PERMISSIONS, SnackProgressBarManager.LENGTH_INDEFINITE)
                else -> snackProgressBarManager?.getSnackProgressBar(SNACK_BAR_PERMISSIONS)?.let { snackProgressBarManager?.updateTo(it) }
            }
        } else {
            when (snackProgressBar) {
                null -> snackProgressBarManager?.show(SNACK_BAR_JUICESSH, SnackProgressBarManager.LENGTH_INDEFINITE)
                else -> snackProgressBarManager?.getSnackProgressBar(SNACK_BAR_JUICESSH)?.let { snackProgressBarManager?.updateTo(it) }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        mConnectionManager?.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == JUICESSH_REQUEST_CODE) {
            mConnectionManager?.gotActivityResult(requestCode, resultCode, data)
        }

        if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES) {
            if (resultCode == RESULT_OK) {
                // TODO
            }
        }

        MorphDialog.registerOnActivityResult(requestCode, resultCode, data).forDialogs(dialog)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        fun isGranted(index: Int): Boolean {
            return (index >= 0 && index <= grantResults.lastIndex) && (grantResults[index] == PackageManager.PERMISSION_GRANTED)
        }

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (isGranted(1) && isGranted(0)) {
                onPermissionsGranted()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                onSettingsItemSelected(item)
                true
            }
            R.id.action_refresh -> {
                onRefreshItemSelected(item)
                true
            }
            R.id.action_about -> {
                onAboutItemSelected(item)
                true
            }
            R.id.action_github -> {
                onGitHubItemSelected(item)
                true
            }
            /*
            R.id.action_generate -> {
                generateXML()
                true
            }
            */
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onLoaderFinished(newCursor: Cursor?) {
        mConnectionListAdapter.swapCursor(newCursor)
    }

    override fun onConsentInfoUpdate(consentState: GDPRConsentState, isNewState: Boolean) {
        // consent is known, handle this
        info("ConsentState: ${consentState.logString()}")

        if (consentState.consent.isPersonalConsent) {
            val debug = BuildConfig.DEBUG
            if (!debug) {
                val core = CrashlyticsCore.Builder().disabled(debug).build()
                Fabric.with(this, Crashlytics.Builder().core(core).build())

                FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)
            }
        }
    }

    override fun onConsentNeedsToBeRequested(data: GDPRPreperationData?) {
        // forward the result and show the dialog
        GDPR.getInstance().showDialog(this, mSetup, data?.location)
    }

    @Suppress("ComplexMethod")
    override fun onClick(v: View?) {
        val id = checkNotNull(v).id

        fun getEntryForValue(value: String, vararg ids: Int): String {
            return resources.getStringArray(ids[0])[resources.getStringArray(ids[1]).indexOf(value)]
        }

        fun element(location: Coordinate?, flag: String, server: String, country: String): String = when {
            flag.isNotEmpty() -> {
                val name = getEntryForValue(flag, R.array.pref_country_entries, R.array.pref_country_values)
                if (location != null) "$name at ${location.latitude}, ${location.longitude}" else name
            }
            server.isNotEmpty() -> {
                "server $server.nordvpn.com"
            }
            country.isNotEmpty() -> {
                getEntryForValue(country, R.array.pref_country_entries, R.array.pref_country_values)
            }
            else -> ""
        }

        fun toggleConnection(v: FloatingActionButton) {
            v.isClickable = false
            val uuid = mConnectionListAdapter.getConnectionId(spinnerConnectionList.selectedItemPosition)
            mConnectionManager?.toggleConnection(uuid!!, this)
        }

        fun morph(v: FloatingActionButton) {
            val (location, flag) = this.positionAndFlagForSelectedMarker()
            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            val server: String = preferences.getString("pref_server", "")!!
            val country: String = preferences.getString("pref_country", "")!!
            val content = "Are you sure you want to connect to ${element(location, flag, server, country)}"

            val builder = MorphDialog.Builder(this, v).apply {
                title("VPN Connection")
                content(content)
                positiveText(android.R.string.ok)
                negativeText(android.R.string.cancel)
                onPositive { _: MorphDialog, _: MorphDialogAction -> toggleConnection(v) }
            }

            dialog = builder.show()
        }

        if (id == R.id.fab0 && v is FloatingActionButton) {
            if (mConnectionListAdapter.count > 0) {
                if (spinnerConnectionList.isEnabled) {
                    morph(v)
                } else {
                    toggleConnection(v)
                }
            } else {
                MorphDialog.Builder(this, v).apply {
                    title("Error")
                    content(R.string.error_must_have_atleast_one_server)
                    positiveText(android.R.string.ok)
                    show()
                }
            }
        }
    }

    override fun onClientStarted() {
    }

    override fun onClientStopped() {
    }

    override fun onConnect() {
        toolbar.showProgress(true)
    }

    @Suppress("MagicNumber")
    override fun onDisconnect() {
        toolbar.showProgress(true)

        handler.postDelayed(runnable, 10000)
    }

    @MainThread
    override fun positionAndFlagForSelectedMarker(): Pair<Coordinate?, String> {
        val fragment = getCurrentNavigationFragment() as? MapFragment
        return fragment?.controlTower?.positionAndFlagForSelectedMarker() ?: Pair(null, "")
    }

    override fun onError(error: Int, reason: String) {
        toolbar.hideProgress(true)

        longToast(reason)
    }

    override fun onCompleted(exitCode: Int) {
        toolbar.hideProgress(true)

        longToast(exitCode.toString())
        when (exitCode) {
            0 -> {
                info("Success")
            }
            -1, 1 -> {
                info("Failure")
            }
        }
    }

    @Suppress("MagicNumber")
    override fun onOutputLine(line: String) {
        if (line.startsWith("CONNECTING TO SERVER", true)) {
            toolbar.hideProgress(true)

            handler.postDelayed(runnable, 10000)
        }
    }

    override fun onSessionFinished() {
        info("onSessionFinished")
        val fragment = getCurrentNavigationFragment() as? OnSessionFinishedListener

        fragment?.onSessionFinished()

        spinnerConnectionList.isEnabled = true
        //cardViewLayout.visibility = View.GONE
    }

    override fun onSessionStarted(sessionId: Int, sessionKey: String) {
        info("onSessionStarted")
        val fragment = getCurrentNavigationFragment() as? OnSessionStartedListener

        fragment?.onSessionStarted(sessionId, sessionKey)

        spinnerConnectionList.isEnabled = false
        //cardViewLayout.visibility = View.VISIBLE
    }

    override fun onSessionCancelled() {
        info("onSessionCancelled")
        val fragment = getCurrentNavigationFragment() as? OnSessionStartedListener

        fragment?.onSessionCancelled()
    }

    private fun getCurrentNavigationFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.primaryNavigationFragment as? NavHostFragment
        val host = navHostFragment?.host
        if (host == null) {
            logException(IllegalStateException("Fragment $navHostFragment has not been attached yet."))
        }

        return when (host) {
            null -> null
            else -> navHostFragment.childFragmentManager.primaryNavigationFragment
        }
    }

    fun getSnackProgressBarManager(): SnackProgressBarManager? {
        return snackProgressBarManager
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun onPermissionsGranted() {
        spinnerConnectionList.adapter = mConnectionListAdapter
        LoaderManager.getInstance(this).initLoader(0, null, ConnectionListLoader(this, this)).forceLoad()

        mConnectionManager = ConnectionManager(
            ctx = this,
            onClientStartedListener = this,
            mActivitySessionStartedListener = this,
            mActivitySessionFinishedListener = this,
            mActivitySessionExecuteListener = this,
            mActivityCommandExecuteListener = this,
            mActivityOnOutputLineListener = Toaster(this)
        )
        /*
        mConnectionManager.powerUsage.observe(this, Observer {
            textViewPower.setData(it, "W")
        })

        mConnectionManager.temperature.observe(this, Observer {
            textViewTemp.setData(it, "C")
        })

        mConnectionManager.fanSpeed.observe(this, Observer {
            textViewFanSpeed.setData(it, "%")
        })

        mConnectionManager.freeMemory.observe(this, Observer {
            textViewFreeMemory.setData(it, "MB")
        })

        mConnectionManager.usedMemory.observe(this, Observer {
            textViewUsedMemory.setData(it, "MB")
        })

        mConnectionManager.graphicsClock.observe(this, Observer {
            textViewClockGraphics.setData(it, "MHz")
        })

        mConnectionManager.videoClock.observe(this, Observer {
            textViewClockVideo.setData(it, "MHz")
        })

        mConnectionManager.memoryClock.observe(this, Observer {
            textViewClockMemory.setData(it, "MHz")
        })
        */

        mConnectionManager?.startClient()
    }

    fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(PERMISSION_READ, PERMISSION_OPEN_SESSIONS), PERMISSION_REQUEST_CODE)
    }

    fun showGDPRIfNecessary() {
        val debug = BuildConfig.DEBUG
        if (!debug) {
            GDPR.getInstance().checkIfNeedsToBeShown(this, mSetup)
        }
    }

    private fun onAboutItemSelected(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        SettingsActivity.startAboutFragment(this)
    }

    private fun onGitHubItemSelected(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        val uriString = "https://github.com/1951FDG/openpyn-nordvpn-juiceSSH"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
        ContextCompat.startActivity(this, intent, null)
    }

    private fun onRefreshItemSelected(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        //val drawable = item.icon as? Animatable
        //drawable?.start()
        toolbar.showProgress(true)

        doAsync {
            val debug = BuildConfig.DEBUG
            var jsonArray: JSONArray? = null
            var json: String? = null
            var thrown = true
            if (NetworkInfo.getInstance().isOnline()) {
                jsonArray = createJson()
            }

            if (jsonArray != null) {
                json = when {
                    debug -> stringifyJsonArray(jsonArray)
                    else -> jsonArray.toString()
                }
            }

            if (json != null) {
                try {
                    val child = resources.getResourceEntryName(R.raw.nordvpn) + ".json"
                    val file = File(getExternalFilesDir(null), child)
                    file.writeText(json)
                    thrown = false
                } catch (e: Resources.NotFoundException) {
                    logException(e)
                } catch (e: FileNotFoundException) {
                    logException(e)
                } catch (e: IOException) {
                    logException(e)
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
    }

    private fun onSettingsItemSelected(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        SettingsActivity.startSettingsFragment(this)
    }

    private fun setProgressToolBar(toolbar: ProgressToolbar) {
        toolbar.hideProgress()
        toolbar.isIndeterminate = true

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setSnackBarManager() {
        fun snackProgressBar(
            type: Int,
            message: String,
            action: String,
            onActionClickListener: OnActionClickListener
        ): SnackProgressBar {
            return SnackProgressBar(type, message).setAction(action, onActionClickListener)
        }

        snackProgressBarManager = SnackProgressBarManager(mainlayout)
        val type = SnackProgressBar.TYPE_NORMAL
        val action = getString(android.R.string.ok)

        snackProgressBarManager?.put(
            snackProgressBar(
                type,
                getString(R.string.error_must_enable_permissions),
                action,
                object : OnActionClickListener {
                    override fun onActionClick() {
                        requestPermissions()
                    }
                }),
            SNACK_BAR_PERMISSIONS
        )

        snackProgressBarManager?.put(
            snackProgressBar(
                type,
                getString(R.string.error_must_install_juicessh),
                action,
                object : OnActionClickListener {
                    override fun onActionClick() {
                        juiceSSHInstall(this@MainActivity)
                    }
                }),
            SNACK_BAR_JUICESSH
        )
    }

    private fun setDefaultPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false)
        PreferenceManager.setDefaultValues(this, R.xml.pref_api, true)
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 23
        private const val REQUEST_GOOGLE_PLAY_SERVICES = 1972
        private const val SNACK_BAR_JUICESSH = 1
        private const val SNACK_BAR_PERMISSIONS = 0
    }
}
