package io.github.sdsstudios.nvidiagpumonitor

import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.sonelli.juicessh.pluginlibrary.PluginClient
import com.sonelli.juicessh.pluginlibrary.listeners.OnClientStartedListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity() {

    companion object {
        const val READ_CONNECTIONS = "com.sonelli.juicessh.api.v1.permission.READ_CONNECTIONS"
        const val OPEN_SESSIONS = "com.sonelli.juicessh.api.v1.permission.OPEN_SESSIONS"
        const val PERMISSION_REQUEST_CODE = 23
    }

    private var mReadConnectionsPerm = false
    private var mOpenSessionsPerm = false

    private val mClient = PluginClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        requestPermissions()
    }

    override fun onStart() {
        super.onStart()

        mClient.start(this, object : OnClientStartedListener {
            override fun onClientStarted() {
                buttonConnect.isEnabled = true
            }

            override fun onClientStopped() {
                buttonConnect.isEnabled = false
            }
        })
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
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

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
}
