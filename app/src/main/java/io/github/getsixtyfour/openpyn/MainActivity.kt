package io.github.getsixtyfour.openpyn

import android.Manifest.permission.ACCESS_COARSE_LOCATION
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
import androidx.fragment.app.FragmentActivity
import androidx.loader.app.LoaderManager
import androidx.preference.PreferenceManager
import com.adityaanand.morphdialog.MorphDialog
import com.afollestad.materialdialogs.MaterialDialog
import com.ariascode.networkutility.NetworkInfo
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.michaelflisar.gdprdialog.GDPR
import com.michaelflisar.gdprdialog.GDPRConsentState
import com.michaelflisar.gdprdialog.GDPRDefinitions
import com.michaelflisar.gdprdialog.GDPRSetup
import com.michaelflisar.gdprdialog.helper.GDPRPreperationData
import com.sonelli.juicessh.pluginlibrary.listeners.OnClientStartedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import com.tingyik90.snackprogressbar.SnackProgressBar
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import io.fabric.sdk.android.Fabric
import io.github.getsixtyfour.openpyn.utilities.Toaster
import io.github.getsixtyfour.openpyn.utilities.createJson
import io.github.getsixtyfour.openpyn.utilities.isJuiceSSHInstalled
import io.github.getsixtyfour.openpyn.utilities.juiceSSHInstall
import io.github.getsixtyfour.openpyn.utilities.logException
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
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class MainActivity : AppCompatActivity(),
    OnSessionStartedListener,
    OnSessionFinishedListener,
    OnClientStartedListener,
    ConnectionListLoaderFinishedCallback,
    AnkoLogger,
    OnSessionExecuteListener,
    OnCommandExecuteListener,
    OnClickListener,
    GDPR.IGDPRCallback {

    companion object {
        private const val READ_CONNECTIONS = "com.sonelli.juicessh.api.v1.permission.READ_CONNECTIONS"
        private const val OPEN_SESSIONS = "com.sonelli.juicessh.api.v1.permission.OPEN_SESSIONS"
        private const val PERMISSION_REQUEST_CODE = 23
        private const val REQUEST_GOOGLE_PLAY_SERVICES = 1972
    }

    private val mConnectionListAdapter by lazy {
        ConnectionListAdapter(if (supportActionBar == null) this else supportActionBar!!.themedContext)
    }
    private var mConnectionManager: ConnectionManager? = null
    private var networkInfo: NetworkInfo? = null
    private var snackProgressBarManager: SnackProgressBarManager? = null
    private val mSetup by lazy {
        GDPRSetup(GDPRDefinitions.FABRIC_CRASHLYTICS)
            .withExplicitAgeConfirmation(true)
            .withForceSelection(true)
            .withShowPaidOrFreeInfoText(false)
    }

    fun getSnackProgressBarManager(): SnackProgressBarManager? {
        return snackProgressBarManager
    }

    fun showGDPRIfNecessary() {
        GDPR.getInstance().checkIfNeedsToBeShown(this, mSetup)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        showGDPRIfNecessary()

        setContentView(R.layout.activity_main)

        toolbar.hideProgress()
        toolbar.isIndeterminate = true

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        snackProgressBarManager = SnackProgressBarManager(mainlayout)

        PreferenceManager.setDefaultValues(this, R.xml.pref_settings, false)

        networkInfo = NetworkInfo.getInstance(application)
        val api = GoogleApiAvailability.getInstance()
        val errorCode = api.isGooglePlayServicesAvailable(this)

        when (errorCode) {
            ConnectionResult.SUCCESS -> onActivityResult(REQUEST_GOOGLE_PLAY_SERVICES, AppCompatActivity.RESULT_OK, null)
            //api.isUserResolvableError(errorCode) -> api.showErrorDialogFragment(this, errorCode, REQUEST_GOOGLE_PLAY_SERVICES)
            else -> error(api.getErrorString(errorCode))
        }

        if (isJuiceSSHInstalled(this)) {
            mConnectionManager = ConnectionManager(
                ctx = this,
                mActivitySessionStartedListener = this,
                mActivitySessionFinishedListener = this,
                mActivitySessionExecuteListener = this,
                mActivityCommandExecuteListener = this,
                mActivityOnOutputLineListener = Toaster(this)
            )
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
            val permissions = arrayOf(READ_CONNECTIONS, OPEN_SESSIONS, ACCESS_COARSE_LOCATION)
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
        }
    }

    override fun onResume() {
        super.onResume()

        if (!isJuiceSSHInstalled(this)) {
            val snackProgressBar = SnackProgressBar(SnackProgressBar.TYPE_NORMAL, getString(R.string.error_must_install_juicessh))
            snackProgressBar.setAction(
                getString(android.R.string.ok),
                object : SnackProgressBar.OnActionClickListener {
                    override fun onActionClick() {
                        juiceSSHInstall(this@MainActivity)
                    }
                }
            )

            snackProgressBarManager?.show(snackProgressBar, SnackProgressBarManager.LENGTH_INDEFINITE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mConnectionManager?.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        fun isGranted(index: Int): Boolean {
            return (index >= 0 && index <= grantResults.lastIndex) && (grantResults[index] == PackageManager.PERMISSION_GRANTED)
        }

        fun hasPermission(permission: String): Boolean {
            return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        fun requestPermissions() {
            if (!hasPermission(READ_CONNECTIONS) || !hasPermission(OPEN_SESSIONS)) {
                ActivityCompat.requestPermissions(this, arrayOf(READ_CONNECTIONS, OPEN_SESSIONS), PERMISSION_REQUEST_CODE)
            }
        }

        fun onPermissionsGranted() {
            mConnectionManager?.startClient(this)

            spinnerConnectionList.adapter = mConnectionListAdapter
            LoaderManager.getInstance<FragmentActivity>(this).initLoader(0, null, ConnectionListLoader(this, this))
        }

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (isGranted(1) && isGranted(0)) {
                onPermissionsGranted()
            } else {
                val snackProgressBar = SnackProgressBar(SnackProgressBar.TYPE_NORMAL, getString(R.string.error_must_enable_permissions))
                snackProgressBar.setAction(
                    getString(android.R.string.ok),
                    object : SnackProgressBar.OnActionClickListener {
                        override fun onActionClick() {
                            requestPermissions()
                        }
                    }
                )

                snackProgressBarManager?.show(snackProgressBar, SnackProgressBarManager.LENGTH_INDEFINITE)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return when (id) {
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

    private fun onGitHubItemSelected(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        val uriString = "https://github.com/1951FDG/openpyn-nordvpn-juiceSSH"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
        ActivityCompat.startActivity(this, intent, null)
    }

    private fun onAboutItemSelected(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        AboutActivity.launch(this)
    }

    private fun onRefreshItemSelected(@Suppress("UNUSED_PARAMETER") item: MenuItem) {
        //val drawable = item.icon as? Animatable
        //drawable?.start()
        toolbar.showProgress(true)

        doAsync {
            var jsonArray: JSONArray? = null

            if (networkInfo!!.isOnline()) {
                jsonArray = createJson()
            }
            var thrown = true

            if (jsonArray != null) {
                val json = jsonArray.toString()

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
        /*
        startActivity<SettingsActivity>(
                EXTRA_SHOW_FRAGMENT to SettingsActivity.SettingsSyncPreferenceFragment::class.java.name,
                EXTRA_NO_HEADERS to true
        )
        */

        SettingsActivity.launch(this)
    }

    @Suppress("MagicNumber")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == JUICESSH_REQUEST_CODE) {
            mConnectionManager?.gotActivityResult(requestCode, resultCode, data)
        }

        if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                // TODO
            }
        }
    }

    override fun onSessionStarted(sessionId: Int, sessionKey: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? OnSessionStartedListener

        fragment?.onSessionStarted(sessionId, sessionKey)

        spinnerConnectionList.isEnabled = false
        //cardViewLayout.visibility = View.VISIBLE
    }

    override fun onSessionCancelled() {
        val fragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? OnSessionStartedListener

        fragment?.onSessionCancelled()
    }

    @Suppress("MagicNumber")
    override fun onSessionFinished() {
        toolbar.hideProgress(true)
        val fragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? OnSessionFinishedListener

        fragment?.onSessionFinished()

        spinnerConnectionList.isEnabled = true
        //cardViewLayout.visibility = View.GONE
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
        if (line.startsWith("CONNECTING TO SERVER", true)) {
            toolbar.hideProgress(true)

            Handler().postDelayed({
                val fragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? MapFragment
                fragment?.updateMasterMarker(true)
            }, 10000)
        }
    }

    override fun onError(error: Int, reason: String) {
        longToast(reason)
    }

    override fun onCompleted(exitCode: Int) {
        toolbar.hideProgress(true)

        longToast(exitCode.toString())
        when (exitCode) {
            0 -> {
                info("Success")
            }
            1 -> {
                info("Failure")
            }
        }
    }

    @MainThread
    override fun positionAndFlagForSelectedMarker(): Pair<Coordinate?, String?> {
        val fragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? MapFragment

        return fragment?.positionAndFlagForSelectedMarker() ?: Pair(null, null)
    }

    override fun onConnect() {
        toolbar.showProgress(true)
    }

    override fun onDisconnect() {
        toolbar.showProgress(true)

        Handler().postDelayed({
            val fragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as? MapFragment
            fragment?.updateMasterMarker(true)
        }, 10000)
    }

    override fun onClick(v: View?) {
        val id = checkNotNull(v).id

        if (id == R.id.fab0) {
            if (mConnectionListAdapter.count == 0 && v is FloatingActionButton) {
                MorphDialog.Builder(this, v)
                    .title("Error")
                    .content(R.string.error_must_have_atleast_one_server)
                    .positiveText(android.R.string.ok)
                    .show()
                return
            }

            v.isClickable = false
            val uuid = mConnectionListAdapter.getConnectionId(spinnerConnectionList.selectedItemPosition)
            mConnectionManager?.toggleConnection(uuid!!, this)
        }
    }

    override fun onConsentInfoUpdate(consentState: GDPRConsentState, isNewState: Boolean) {
        // consent is known, handle this
        info("ConsentState: ${consentState.logString()}")

        if (consentState.consent.isPersonalConsent) {
            val debug = BuildConfig.DEBUG
            if (!debug) {
                val core = CrashlyticsCore.Builder().disabled(debug).build()
                Fabric.with(this, Crashlytics.Builder().core(core).build())
            }
        }
    }

    override fun onConsentNeedsToBeRequested(data: GDPRPreperationData?) {
        // forward the result and show the dialog
        GDPR.getInstance().showDialog(this, mSetup, data?.location)
    }
}
