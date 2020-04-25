package io.github.getsixtyfour.openpyn

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
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.loader.app.LoaderManager
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.michaelflisar.gdprdialog.GDPR
import com.michaelflisar.gdprdialog.GDPRConsentState
import com.michaelflisar.gdprdialog.helper.GDPRPreperationData
import com.sonelli.juicessh.pluginlibrary.PluginContract.Connections.PERMISSION_READ
import com.sonelli.juicessh.pluginlibrary.PluginContract.PERMISSION_OPEN_SESSIONS
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import com.tingyik90.snackprogressbar.SnackProgressBarManager
import es.dmoral.toasty.Toasty
import io.github.getsixtyfour.ktextension.apkSignatures
import io.github.getsixtyfour.ktextension.handleUpdate
import io.github.getsixtyfour.ktextension.isJuiceSSHInstalled
import io.github.getsixtyfour.ktextension.startUpdate
import io.github.getsixtyfour.ktextension.verifyInstallerId
import io.github.getsixtyfour.ktextension.verifySigningCertificate
import io.github.getsixtyfour.openpyn.dialog.PreferenceDialog.NoticeDialogListener
import io.github.getsixtyfour.openpyn.map.MapFragmentDirections
import io.github.getsixtyfour.openpyn.utils.Toaster
import io.github.sdsstudios.nvidiagpumonitor.ConnectionListAdapter
import io.github.sdsstudios.nvidiagpumonitor.ConnectionListLoader
import io.github.sdsstudios.nvidiagpumonitor.ConnectionManager
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnCommandExecuteListener
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnLoaderChangedListener
import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate
import kotlinx.android.synthetic.main.activity_main.container
import kotlinx.android.synthetic.main.activity_main.spinner
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error
import org.jetbrains.anko.info
import pub.devrel.easypermissions.AppSettingsDialog
import java.util.Locale

class MainActivity : AppCompatActivity(R.layout.activity_main), AnkoLogger, GDPR.IGDPRCallback, OnClickListener, NoticeDialogListener,
    OnLoaderChangedListener, OnCommandExecuteListener, OnSessionExecuteListener, OnSessionStartedListener, OnSessionFinishedListener,
    CoroutineScope by MainScope() {

    private var mConnectionListAdapter: ConnectionListAdapter? = null
    private var mConnectionManager: ConnectionManager? = null
    private var mAppSettingsDialogShown: Boolean = false
    // TODO: remove container reference
    val mSnackProgressBarManager: SnackProgressBarManager by lazy { SnackProgressBarManager(container, this) }
    private val mSetup by lazy { getGDPR(this, R.style.ThemeOverlay_MaterialComponents_Dialog_Alert_Custom) }
    private val mAppUpdateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(applicationContext) }
    private val mGooglePlayStorePackage: Boolean by lazy { verifyInstallerId(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE) }
    private val mGooglePlayStoreCertificate: Boolean by lazy { verifySigningCertificate(listOf(getString(R.string.app_signature))) }
    /*private var dialog: MorphDialog? = null*/

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)

        setProgressToolBar(this, toolbar)

        setSnackBarManager(this, mSnackProgressBarManager)

        showGDPRIfNecessary(this, mSetup)

        startVpnService(this)
        // TODO: remove after beta release test
        error("$apkSignatures")

        /*val api = GoogleApiAvailability.getInstance()
        when (val errorCode = api.isGooglePlayServicesAvailable(applicationContext)) {
            ConnectionResult.SUCCESS -> onActivityResult(GOOGLE_REQUEST_CODE, RESULT_OK, null)
            //api.isUserResolvableError(errorCode) -> api.showErrorDialogFragment(this, errorCode, GOOGLE_REQUEST_CODE)
            else -> error(api.getErrorString(errorCode))
        }*/
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == JUICESSH_REQUEST_CODE) {
            mConnectionManager?.onActivityResult(requestCode, resultCode, data)
        }

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            mAppSettingsDialogShown = false
        }

        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                // If the update is cancelled or fails, you can request to start the update again
                error("Update flow failed! Result code: $resultCode")
            }
        }

        /*if (requestCode == GOOGLE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
            }
        }*/

        /*MorphDialog.registerOnActivityResult(requestCode, resultCode, data).forDialogs(dialog)*/
    }

    override fun onResume() {
        super.onResume()

        if (mConnectionManager != null) {
            if (mGooglePlayStorePackage && mGooglePlayStoreCertificate) handleUpdate(mAppUpdateManager, UPDATE_REQUEST_CODE)
            return
        }

        if (isJuiceSSHInstalled()) {
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)

        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                onSettingsItemSelected(this, item)
                true
            }
            R.id.menu_github -> {
                onGitHubItemSelected(this, item)
                true
            }
            /*
            R.id.menu_generate -> {
                generateXML()
                true
            }
            */
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onConsentInfoUpdate(consentState: GDPRConsentState, isNewState: Boolean) {
        // Consent is known, handle this
        info("ConsentState: ${consentState.logString()}")

        if (consentState.consent.isPersonalConsent) {
            initCrashlytics(this)
        }
    }

    override fun onConsentNeedsToBeRequested(data: GDPRPreperationData?) {
        // Forward the result and show the dialog
        GDPR.getInstance().showDialog(this, mSetup, data?.location)
    }

    @Suppress("ComplexMethod")
    override fun onClick(v: View?) {
        val id = checkNotNull(v).id

        fun getEntryForValue(@ArrayRes entryArrayId: Int, @ArrayRes valueArrayId: Int, value: String): String {
            return resources.getStringArray(entryArrayId)[resources.getStringArray(valueArrayId).indexOf(value)]
        }

        fun element(location: Coordinate?, flag: String, server: String, country: String): String = when {
            flag.isNotEmpty() -> {
                val name = getEntryForValue(R.array.pref_country_entries, R.array.pref_country_values, flag)
                if (location != null) {
                    // Enforce Locale to English for double to string conversion
                    val latitude = "%.7f".format(Locale.ENGLISH, location.latitude)
                    val longitude = "%.7f".format(Locale.ENGLISH, location.longitude)
                    val string = getString(R.string.at_preposition)
                    getString(R.string.vpn_name_location, name, string, latitude, longitude)
                } else {
                    name
                }
            }
            server.isNotEmpty() -> {
                getString(R.string.vpn_name_server, server)
            }
            country.isNotEmpty() -> {
                getString(R.string.vpn_name_country, getEntryForValue(R.array.pref_country_entries, R.array.pref_country_values, country))
            }
            else -> {
                getString(R.string.empty)
            }
        }

        fun message(): String {
            val (location, flag) = positionAndFlagForSelectedMarker()
            val preferences = PreferenceManager.getDefaultSharedPreferences(this)
            val server = preferences.getString("pref_server", "")!!
            val country = preferences.getString("pref_country", "")!!
            return getString(R.string.vpn_msg_connect, element(location, flag, server, country))
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
            if (mConnectionListAdapter?.count ?: 0 > 0) {
                if (!it.isConnected()) {
                    /*dialog = showMessageDialog(v)*/
                    val action = MapFragmentDirections.actionMapFragmentToPreferenceDialogFragment(message())
                    Navigation.findNavController(v).navigate(action)
                } else {
                    toggleConnection()
                }
            } else {
                /*showWarningDialog(v)*/
                AlertDialog.Builder(this).apply {
                    setTitle(R.string.title_error)
                    setMessage(R.string.error_juicessh_server)
                    setPositiveButton(android.R.string.ok, null)
                    show()
                }
            }
        }
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        toggleConnection()
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
    }

    override fun onLoaderChanged(newCursor: Cursor?) {
        mConnectionListAdapter?.swapCursor(newCursor)
    }

    override fun positionAndFlagForSelectedMarker(): Pair<Coordinate?, String> {
        return (getCurrentNavigationFragment(this) as? OnCommandExecuteListener)?.positionAndFlagForSelectedMarker() ?: Pair(null, "")
    }

    override fun onConnect() {
        toolbar.hideProgress(true)

        (getCurrentNavigationFragment(this) as? OnCommandExecuteListener)?.onConnect()
    }

    override fun onDisconnect() {
        toolbar.hideProgress(true)

        (getCurrentNavigationFragment(this) as? OnCommandExecuteListener)?.onDisconnect()
    }

    override fun onError(error: Int, reason: String) {
        toolbar.hideProgress(true)

        Toasty.error(this, reason, Toasty.LENGTH_LONG, false).show()
        error(String)
    }

    override fun onCompleted(exitCode: Int) {
        toolbar.hideProgress(true)

        when (exitCode) {
            0 -> {
                Toasty.success(this, "$exitCode", Toasty.LENGTH_LONG, false).show()
                info("Success")
            }
            -1, 1 -> {
                Toasty.error(this, "$exitCode", Toasty.LENGTH_LONG, false).show()
                error("Failure")
            }
            else -> {
                Toasty.error(this, "$exitCode", Toasty.LENGTH_LONG, false).show()
                error("Unknown failure")
            }
        }
    }

    override fun onOutputLine(line: String) {
    }

    override fun onSessionCancelled() {
        (getCurrentNavigationFragment(this) as? OnSessionStartedListener)?.onSessionCancelled()
    }

    override fun onSessionStarted(sessionId: Int, sessionKey: String) {
        toolbar.showProgress(true)

        (getCurrentNavigationFragment(this) as? OnSessionStartedListener)?.onSessionStarted(sessionId, sessionKey)

        spinner.isEnabled = false
    }

    override fun onSessionFinished() {
        toolbar.hideProgress(true)

        (getCurrentNavigationFragment(this) as? OnSessionFinishedListener)?.onSessionFinished()

        spinner.isEnabled = true
    }

    private fun onPermissionsGranted(requestCode: Int, vararg perms: String) {
        if (requestCode != PERMISSION_REQUEST_CODE) return

        mConnectionListAdapter = ConnectionListAdapter(this)
        spinner.adapter = mConnectionListAdapter
        LoaderManager.getInstance(this).initLoader(0, null, ConnectionListLoader(this, this)).forceLoad()

        mConnectionManager = ConnectionManager(
            ctx = this,
            lifecycleOwner = this,
            mSessionStartedListener = this,
            mSessionFinishedListener = this,
            sessionExecuteListener = this,
            commandExecuteListener = this,
            onOutputLineListener = Toaster(this)
        )
        mConnectionManager?.startClient()

        if (mGooglePlayStorePackage && mGooglePlayStoreCertificate) startUpdate(mAppUpdateManager, UPDATE_REQUEST_CODE)
    }

    private fun onPermissionsDenied(requestCode: Int, vararg perms: String) {
        if (perms.any { !ActivityCompat.shouldShowRequestPermissionRationale(this, it) }) {
            mAppSettingsDialogShown = true
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    private fun hasPermissions(context: Context, vararg perms: String): Boolean {
        return perms.none { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
    }

    private fun toggleConnection() {
        mConnectionListAdapter?.getConnectionId(spinner.selectedItemPosition)?.let {
            mConnectionManager?.toggleConnection(this, it, JUICESSH_REQUEST_CODE)
        }
    }

    companion object {
        const val UPDATE_REQUEST_CODE: Int = 1
        const val PERMISSION_REQUEST_CODE: Int = 2
        const val JUICESSH_REQUEST_CODE: Int = 3
        /*private const val GOOGLE_REQUEST_CODE = 4*/
    }
}
