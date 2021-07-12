package io.github.getsixtyfour.openpyn

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ArrayRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.abdeveloper.library.MultiSelectDialog.SubmitCallbackListener
import com.google.android.material.transition.platform.MaterialFadeThrough
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.sonelli.juicessh.pluginlibrary.PluginContract.Connections.PERMISSION_READ
import com.sonelli.juicessh.pluginlibrary.PluginContract.PERMISSION_OPEN_SESSIONS
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import io.github.getsixtyfour.functions.getCurrentNavigationFragment
import io.github.getsixtyfour.functions.onGenerateItemSelected
import io.github.getsixtyfour.functions.onLoggingItemSelected
import io.github.getsixtyfour.functions.onSettingsItemSelected
import io.github.getsixtyfour.functions.showJuiceAlertDialog
import io.github.getsixtyfour.functions.showOpenpynAlertDialog
import io.github.getsixtyfour.ktextension.apkSignatures
import io.github.getsixtyfour.ktextension.handleUpdate
import io.github.getsixtyfour.ktextension.isJuiceSSHInstalled
import io.github.getsixtyfour.ktextension.isPlayStoreCertificate
import io.github.getsixtyfour.ktextension.isPlayStorePackage
import io.github.getsixtyfour.ktextension.juiceSSHInstall
import io.github.getsixtyfour.ktextension.startUpdate
import io.github.getsixtyfour.openpyn.core.MapFragmentDirections
import io.github.getsixtyfour.openpyn.dialog.PreferenceDialog.NoticeDialogListener
import io.github.getsixtyfour.openpyn.utils.ManagementService
import io.github.getsixtyfour.openpyn.utils.Toaster
import io.github.sdsstudios.nvidiagpumonitor.ConnectionManager
import io.github.sdsstudios.nvidiagpumonitor.adapters.ConnectionListAdapter
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnCommandExecuteListener
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnLoaderChangedListener
import io.github.sdsstudios.nvidiagpumonitor.loaders.ConnectionListLoader
import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate
import kotlinx.android.synthetic.main.activity_main.container
import kotlinx.android.synthetic.main.activity_main.spinner
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.overlay_juicessh.layout_overlay
import kotlinx.android.synthetic.main.overlay_juicessh.material_text_button
import kotlinx.android.synthetic.main.overlay_juicessh.material_text_view
import mu.KotlinLogging
import pub.devrel.easypermissions.AppSettingsDialog
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity(R.layout.activity_main), View.OnClickListener, NoticeDialogListener, OnLoaderChangedListener,
    OnCommandExecuteListener, OnSessionExecuteListener, OnSessionStartedListener, OnSessionFinishedListener, SubmitCallbackListener {

    private val logger = KotlinLogging.logger {}

    private val mAppUpdateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(applicationContext) }

    private val mGooglePlayStoreCertificate: Boolean by lazy { isPlayStoreCertificate() }
    private val mGooglePlayStorePackage: Boolean by lazy { isPlayStorePackage() }

    private val mConnectionAdapter: ConnectionListAdapter by lazy { ConnectionListAdapter(this) }
    private val mConnectionId: UUID?
        get() = mConnectionAdapter.getConnectionId(spinner.selectedItemPosition)
    private var mConnectionManager: ConnectionManager? = null

    private var mAppSettingsDialogShown: Boolean = false

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
                logger.warn { "Update flow failed! Result code: $resultCode" }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.exitTransition = MaterialFadeThrough()
        window.reenterTransition = MaterialFadeThrough()

        // Hide both the navigation bar and the status bar
        /*hideSystemUI(window, window.decorView)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navigationBarColor)*/

        // This app draws behind the system bars, so we want to handle fitting system windows
        WindowCompat.setDecorFitsSystemWindows(window, false)

        /*setProgressToolBar(this, toolbar)*/

        ManagementService.start(this)

        // TODO: remove after beta release test, add delay?
        logger.error(Exception()) { "$apkSignatures" }

        // Add overlayLayout as background
        addOverlayLayout(container)
    }

    override fun onResume() {
        super.onResume()

        if (mConnectionManager != null) {
            if (mGooglePlayStorePackage && mGooglePlayStoreCertificate) handleUpdate(mAppUpdateManager, UPDATE_REQUEST_CODE)
            return
        }

        if (isJuiceSSHInstalled()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || hasPermissions(this, PERMISSION_READ, PERMISSION_OPEN_SESSIONS)) {
                onPermissionsGranted(PERMISSION_REQUEST_CODE)
            } else {
                if (!mAppSettingsDialogShown) {
                    showOverlayLayout(R.string.error_juicessh_permissions)
                }
            }
        } else {
            showOverlayLayout(R.string.error_juicessh_app)
        }
    }

    private fun hideOverlayLayout() {
        layout_overlay.visibility = View.GONE
    }

    private fun showOverlayLayout(@StringRes resId: Int) {
        material_text_view.text = getString(resId)
        material_text_button.tag = resId
        layout_overlay.visibility = View.VISIBLE
    }

    private fun addOverlayLayout(parent: ViewGroup) {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.overlay_juicessh, parent, false)
        parent.addView(view)

        material_text_button.setOnClickListener {
            (it.parent as? View)?.visibility = View.INVISIBLE

            when (it.tag) {
                R.string.error_juicessh_permissions -> ActivityCompat.requestPermissions(
                    this, arrayOf(PERMISSION_READ, PERMISSION_OPEN_SESSIONS), PERMISSION_REQUEST_CODE
                )
                R.string.error_juicessh_app -> this.juiceSSHInstall()
            }
        }
    }

    private fun removeOverlayLayout(parent: ViewGroup) {
        material_text_button.setOnClickListener(null)
        parent.removeView(layout_overlay)
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
        return when (item.title) {
            getString(R.string.title_settings) -> {
                onSettingsItemSelected(this)
                true
            }
            getString(R.string.menu_generate) -> {
                lifecycleScope.onGenerateItemSelected(this)
                true
            }
            getString(R.string.menu_logfile) -> {
                onLoggingItemSelected(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onClick(v: View) {
        fun getEntryForValue(@ArrayRes entryArrayId: Int, @ArrayRes valueArrayId: Int, value: String): String {
            return resources.getStringArray(entryArrayId)[resources.getStringArray(valueArrayId).indexOf(value)]
        }

        fun element(location: Coordinate?, flag: String, server: String, country: String): String = when {
            flag.isNotEmpty() -> {
                if (BuildConfig.DEBUG && location != null) {
                    // Enforce Locale to English for double to string conversion
                    val latitude = "%.7f".format(Locale.ENGLISH, location.latitude)
                    val longitude = "%.7f".format(Locale.ENGLISH, location.longitude)
                    logger.info { "https://www.google.com/maps?q=$latitude,$longitude" }
                    getString(R.string.vpn_name_location, latitude, longitude)
                } else {
                    getEntryForValue(R.array.pref_country_entries, R.array.pref_country_values, flag)
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

        if (v.id != R.id.fab0) return

        if (mConnectionAdapter.count > 0) {
            mConnectionManager?.let {
                if (!it.isConnected()) {
                    val action = MapFragmentDirections.actionMapFragmentToPreferenceDialogFragment(message())
                    Navigation.findNavController(v).navigate(action)
                } else {
                    it.toggleConnection(this, mConnectionId, JUICESSH_REQUEST_CODE)
                }
            }
        } else {
            showJuiceAlertDialog(this)
        }
    }

    override fun onDialogPositiveClick(dialog: DialogFragment) {
        mConnectionManager?.toggleConnection(this, mConnectionId, JUICESSH_REQUEST_CODE)
    }

    override fun onDialogNegativeClick(dialog: DialogFragment) {
    }

    override fun onLoaderChanged(cursor: Cursor?) {
        mConnectionAdapter.swapCursor(cursor)
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

    @Suppress("MagicNumber")
    override fun onCompleted(exitCode: Int) {
        toolbar.hideProgress(true)

        when (exitCode) {
            // command terminated successfully (0)
            0 -> {
                ManagementService.start(this)
            }
            // command not found (127)
            127 -> {
                logger.debug("Tried to run a command but the command was not found on the server")
                showOpenpynAlertDialog(this)
            }
        }

        mConnectionManager?.let { if (it.isConnected()) it.disconnect() }
    }

    override fun onOutputLine(line: String) {
    }

    override fun onError(reason: Int, message: String) {
        toolbar.hideProgress(true)
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

    override fun onSelected(selectedIds: ArrayList<Int>, selectedNames: ArrayList<String>, dataString: String) {
        (getCurrentNavigationFragment(this) as? SubmitCallbackListener)?.onSelected(selectedIds, selectedNames, dataString)
    }

    override fun onCancel() {
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onPermissionsGranted(requestCode: Int, vararg perms: String) {
        if (requestCode != PERMISSION_REQUEST_CODE) return

        if (mConnectionManager != null) return

        removeOverlayLayout(container)

        spinner.adapter = mConnectionAdapter

        LoaderManager.getInstance(this).initLoader(0, null, ConnectionListLoader(this, this)).forceLoad()

        mConnectionManager = ConnectionManager(
            context = this,
            lifecycleOwner = this,
            mSessionStartedListener = this,
            mSessionFinishedListener = this,
            sessionExecuteListener = this,
            commandExecuteListener = this,
            onOutputLineListener = Toaster(this)
        )

        if (mGooglePlayStorePackage && mGooglePlayStoreCertificate) startUpdate(mAppUpdateManager, UPDATE_REQUEST_CODE)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onPermissionsDenied(requestCode: Int, vararg perms: String) {
        if (perms.any { !ActivityCompat.shouldShowRequestPermissionRationale(this, it) }) {
            mAppSettingsDialogShown = true
            AppSettingsDialog.Builder(this).build().show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun hasPermissions(context: Context, vararg perms: String): Boolean {
        return perms.none { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
    }

    companion object {

        const val UPDATE_REQUEST_CODE: Int = 1
        const val PERMISSION_REQUEST_CODE: Int = 2
        const val JUICESSH_REQUEST_CODE: Int = 3
    }
}
