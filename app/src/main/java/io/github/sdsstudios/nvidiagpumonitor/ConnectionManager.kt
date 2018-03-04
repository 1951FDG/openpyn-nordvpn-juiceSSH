package io.github.sdsstudios.nvidiagpumonitor

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.sonelli.juicessh.pluginlibrary.PluginClient
import com.sonelli.juicessh.pluginlibrary.exceptions.ServiceNotConnectedException
import com.sonelli.juicessh.pluginlibrary.listeners.OnClientStartedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import java.util.*

/**
 * Created by Seth on 04/03/18.
 */

class ConnectionManager(private val mCtx: Context,
                        private val mActivitySessionStartedListener: OnSessionStartedListener,
                        private val mActivitySessionFinishedListener: OnSessionFinishedListener

) : OnSessionStartedListener, OnSessionFinishedListener {

    companion object {
        const val JUICESSH_REQUEST_CODE = 345
    }

    var connected = false

    private val mClient = PluginClient()

    override fun onSessionStarted(sessionId: Int, sessionKey: String?) {
        mActivitySessionStartedListener.onSessionStarted(sessionId, sessionKey)

        mClient.addSessionFinishedListener(sessionId, sessionKey, this)

        connected = true
    }

    override fun onSessionFinished() {
        mActivitySessionFinishedListener.onSessionFinished()

        connected = false
    }

    override fun onSessionCancelled() {}

    fun toggleConnection(uuid: UUID, activity: AppCompatActivity) {
        if (!connected) {
            connect(uuid, activity)
        }
    }

    fun startClient(onClientStartedListener: OnClientStartedListener) {
        mClient.start(mCtx, onClientStartedListener)
    }

    fun gotActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        mClient.gotActivityResult(requestCode, resultCode, data)
    }

    private fun connect(uuid: UUID, activity: AppCompatActivity) {
        Thread(Runnable {
            try {
                mClient.connect(activity, uuid, this, JUICESSH_REQUEST_CODE)

            } catch (e: ServiceNotConnectedException) {

                Toast.makeText(
                        mCtx,
                        R.string.error_couldnt_connect_to_service,
                        Toast.LENGTH_SHORT
                ).show()
            }
        }).start()
    }
}