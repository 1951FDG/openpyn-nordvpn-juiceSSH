package io.github.getsixtyfour.openpyn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.loader.app.LoaderManager
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
// import com.getsixtyfour.openvpnmgmt.android.VPNAuthenticationHandler
// import com.getsixtyfour.openvpnmgmt.android.VPNLaunchHelper.startOpenVPNService
// import com.getsixtyfour.openvpnmgmt.android.constant.IntentConstants
// import com.getsixtyfour.openvpnmgmt.net.ManagementConnection
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
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import io.fabric.sdk.android.Fabric
import io.github.getsixtyfour.openpyn.dialog.PreferenceDialog
import io.github.getsixtyfour.openpyn.map.MapFragment
import io.github.getsixtyfour.openpyn.map.MapFragmentDirections
import io.github.getsixtyfour.openpyn.utils.Toaster
import io.github.sdsstudios.nvidiagpumonitor.ConnectionListAdapter
import io.github.sdsstudios.nvidiagpumonitor.ConnectionListLoader
import io.github.sdsstudios.nvidiagpumonitor.ConnectionListLoaderFinishedCallback
import io.github.sdsstudios.nvidiagpumonitor.ConnectionManager
import io.github.sdsstudios.nvidiagpumonitor.ConnectionManager.Companion.JUICESSH_REQUEST_CODE
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnCommandExecuteListener
import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate
import kotlinx.android.synthetic.main.activity_main.container
import kotlinx.android.synthetic.main.activity_main.spinner
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import pub.devrel.easypermissions.AppSettingsDialog
import java.util.Locale

const val DELAY_MILLIS: Long = 10000

class MainActivity : AppCompatActivity(R.layout.activity_main), AnkoLogger, ConnectionListLoaderFinishedCallback, GDPR.IGDPRCallback,
    OnClickListener, OnClientStartedListener, OnCommandExecuteListener, OnSessionExecuteListener, OnSessionFinishedListener,
    OnSessionStartedListener, PreferenceDialog.NoticeDialogListener, CoroutineScope by MainScope() {

    // private var dialog: MorphDialog? = null
    // todo check value
    private val mConnectionListAdapter by lazy {
        ConnectionListAdapter(if (supportActionBar == null) this else supportActionBar!!.themedContext)
    }
    private var mConnectionManager: ConnectionManager? = null
    private val mSetup by lazy {
        with(
            GDPRSetup(
                GDPRDefinitions.FABRIC_CRASHLYTICS, GDPRDefinitions.FIREBASE_CRASH, GDPRDefinitions.FIREBASE_ANALYTICS
            )
        ) {
            withCustomDialogTheme(R.style.ThemeOverlay_MaterialComponents_Dialog_Alert)
            withForceSelection(true)
            withShowPaidOrFreeInfoText(false)
        }
    }
    val mSnackProgressBarManager: SnackProgressBarManager by lazy { SnackProgressBarManager(container, this) }
    private var mAppSettingsDialogShown: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        showGDPRIfNecessary(this, mSetup)

        setProgressToolBar(this, toolbar)

        setSnackBarManager(this, mSnackProgressBarManager)

        // val api = GoogleApiAvailability.getInstance()
        // when (val errorCode = api.isGooglePlayServicesAvailable(applicationContext)) {
        //     ConnectionResult.SUCCESS -> onActivityResult(REQUEST_GOOGLE_PLAY_SERVICES, RESULT_OK, null)
        //     //api.isUserResolvableError(errorCode) -> api.showErrorDialogFragment(this, errorCode, REQUEST_GOOGLE_PLAY_SERVICES)
        //     else -> error(api.getErrorString(errorCode))
        // }

        // if (isJuiceSSHInstalled(this)) {
        //     if (hasPermissions(this, PERMISSION_READ, PERMISSION_OPEN_SESSIONS)) onPermissionsGranted(PERMISSION_REQUEST_CODE)
        // }

        // run {
        //     PreferenceManager.getDefaultSharedPreferences(this).edit().apply {
        //         // Android Emulator - Special alias to your host loopback interface (i.e., 127.0.0.1 on your development machine)
        //         putString(getString(R.string.pref_openvpnmgmt_host_key), "10.0.2.2")
        //         // Default management port used by Openpyn
        //         putString(getString(R.string.pref_openvpnmgmt_port_key), "7015")
        //         apply()
        //     }
        //
        //     val connection = ManagementConnection.getInstance()
        //     val handler = VPNAuthenticationHandler(this)
        //     val host = VPNAuthenticationHandler.getHost(this)
        //     val port = VPNAuthenticationHandler.getPort(this)
        //     val bundle = Bundle().apply {
        //         putBoolean(IntentConstants.EXTRA_SHOW_NOTIFICATION, true)
        //         putString(IntentConstants.EXTRA_HOST, host)
        //         putInt(IntentConstants.EXTRA_PORT, port)
        //     }
        //
        //     connection.setUsernamePasswordHandler(handler)
        //
        //     startOpenVPNService(this, bundle)
        // }
    }

    override fun onResume() {
        super.onResume()
        if (isJuiceSSHInstalled(this)) {
            if (hasPermissions(this, PERMISSION_READ, PERMISSION_OPEN_SESSIONS)) {
                mSnackProgressBarManager.dismiss()
                onPermissionsGranted(PERMISSION_REQUEST_CODE)
            } else {
                if (!mAppSettingsDialogShown) {
                    showSnackProgressBar(mSnackProgressBarManager, SNACK_BAR_PERMISSIONS)
                }
            }
        } else {
            showSnackProgressBar(mSnackProgressBarManager, SNACK_BAR_JUICESSH)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mConnectionManager?.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == JUICESSH_REQUEST_CODE) {
            mConnectionManager?.gotActivityResult(requestCode, resultCode, data)
        }

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            mAppSettingsDialogShown = false
        }

        // if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES) {
        //     if (resultCode == RESULT_OK) {
        //         // TODO
        //     }
        // }

        // MorphDialog.registerOnActivityResult(requestCode, resultCode, data).forDialogs(dialog)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        var granted: Array<String> = emptyArray()
        var denied: Array<String> = emptyArray()
        permissions.withIndex().forEach {
            if (grantResults[it.index] == PackageManager.PERMISSION_GRANTED) granted = granted.plus(it.value)
            else denied = denied.plus(it.value)
        }

        if (denied.isNotEmpty()) onPermissionsDenied(requestCode, *denied)
        else if (granted.isNotEmpty()) onPermissionsGranted(requestCode, *granted)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                onSettingsItemSelected(this, item)
                true
            }
            R.id.action_github -> {
                onGitHubItemSelected(this, item)
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
                if (location != null) {
                    // Enforce Locale to English for double to string conversion
                    val latitude = "%.7f".format(Locale.ENGLISH, location.latitude)
                    val longitude = "%.7f".format(Locale.ENGLISH, location.longitude)
                    val string = getString(R.string.at_preposition)
                    getString(R.string.vpn_location_name, name, string, latitude, longitude)
                } else {
                    name
                }
            }
            server.isNotEmpty() -> {
                getString(R.string.vpn_server_name, server)
            }
            country.isNotEmpty() -> {
                getString(R.string.vpn_country_name, getEntryForValue(country, R.array.pref_country_entries, R.array.pref_country_values))
            }
            else -> {
                getString(R.string.empty)
            }
        }

        fun message(): String {
            val (location, flag) = this.positionAndFlagForSelectedMarker()
            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            val server = preferences.getString("pref_server", "")!!
            val country = preferences.getString("pref_country", "")!!
            return getString(R.string.vpn_connect_message, element(location, flag, server, country))
        }

        /*fun showMessageDialog(v: FloatingActionButton): MorphDialog = MorphDialog.Builder(this, v).run {
            title(R.string.title_dialog_connect)
            content(message())
            positiveText(android.R.string.ok)
            negativeText(android.R.string.cancel)
            onPositive { _: MorphDialog, _: MorphDialogAction -> toggleConnection() }
            show()
        }*/

        /*fun showWarningDialog(v: FloatingActionButton): MorphDialog = MorphDialog.Builder(this, v).run {
            title(R.string.title_dialog_error)
            content(R.string.error_must_have_at_least_one_server)
            positiveText(android.R.string.ok)
            show()
        }*/

        if (id != R.id.fab0 || v !is FloatingActionButton) return

        mConnectionManager?.let {
            if (mConnectionListAdapter.count > 0) {
                if (!it.isConnected()) {
                    // dialog = showMessageDialog(v)
                    val action = MapFragmentDirections.actionMapFragmentToPreferenceDialogFragment(message())
                    Navigation.findNavController(v).navigate(action)
                } else {
                    toggleConnection()
                }
            } else {
                // showWarningDialog(v)
                AlertDialog.Builder(this).apply {
                    setTitle(R.string.title_dialog_error)
                    setMessage(R.string.error_must_have_at_least_one_server)
                    setPositiveButton(android.R.string.ok, null)
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
        toolbar.hideProgress(true)
        val fragment = getCurrentNavigationFragment(this) as? MapFragment
        fragment?.controlTower?.updateMasterMarkerWithDelay(true, DELAY_MILLIS)
    }

    @Suppress("MagicNumber")
    override fun onDisconnect() {
        toolbar.hideProgress(true)
        val fragment = getCurrentNavigationFragment(this) as? MapFragment
        fragment?.controlTower?.updateMasterMarkerWithDelay(true, DELAY_MILLIS)
    }

    override fun positionAndFlagForSelectedMarker(): Pair<Coordinate?, String> {
        val fragment = getCurrentNavigationFragment(this) as? OnCommandExecuteListener
        return fragment?.positionAndFlagForSelectedMarker() ?: Pair(null, "")
    }

    override fun onError(error: Int, reason: String) {
        toolbar.hideProgress(true)

        Toast.makeText(this, reason, Toast.LENGTH_LONG).show()
    }

    override fun onCompleted(exitCode: Int) {
        toolbar.hideProgress(true)

        Toast.makeText(this, exitCode.toString(), Toast.LENGTH_LONG).show()

        when (exitCode) {
            0 -> {
                info("Success")
            }
            -1, 1 -> {
                info("Failure")
            }
        }
    }

    override fun onOutputLine(line: String) {
    }

    override fun onSessionFinished() {
        toolbar.hideProgress(true)

        info("onSessionFinished")
        val fragment = getCurrentNavigationFragment(this) as? OnSessionFinishedListener

        fragment?.onSessionFinished()

        spinner.isEnabled = true
    }

    override fun onSessionStarted(sessionId: Int, sessionKey: String) {
        toolbar.showProgress(true)

        info("onSessionStarted")
        val fragment = getCurrentNavigationFragment(this) as? OnSessionStartedListener

        fragment?.onSessionStarted(sessionId, sessionKey)

        spinner.isEnabled = false
    }

    override fun onSessionCancelled() {
        info("onSessionCancelled")
        val fragment = getCurrentNavigationFragment(this) as? OnSessionStartedListener

        fragment?.onSessionCancelled()
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        toggleConnection()
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
    }

    private fun hasPermissions(context: Context, vararg perms: String): Boolean {
        return perms.none { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
    }

    private fun onPermissionsGranted(requestCode: Int, vararg perms: String) {
        if (requestCode != PERMISSION_REQUEST_CODE) return
        if (mConnectionManager != null) return

        spinner.adapter = mConnectionListAdapter
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

        mConnectionManager?.startClient()
    }

    private fun onPermissionsDenied(requestCode: Int, vararg perms: String) {
        if (somePermissionPermanentlyDenied(this, *perms)) {
            mAppSettingsDialogShown = true
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    private fun somePermissionPermanentlyDenied(activity: Activity, vararg perms: String): Boolean {
        return perms.any { !ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
    }

    private fun toggleConnection() {
        val uuid = mConnectionListAdapter.getConnectionId(spinner.selectedItemPosition)
        mConnectionManager?.toggleConnection(this, uuid!!)
    }

    companion object {
        const val PERMISSION_REQUEST_CODE: Int = 23
        // private const val REQUEST_GOOGLE_PLAY_SERVICES = 1972
    }
}
