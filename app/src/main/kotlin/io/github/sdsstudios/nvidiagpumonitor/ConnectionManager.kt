package io.github.sdsstudios.nvidiagpumonitor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import com.sonelli.juicessh.pluginlibrary.PluginClient
import com.sonelli.juicessh.pluginlibrary.exceptions.ServiceNotConnectedException
import com.sonelli.juicessh.pluginlibrary.listeners.OnClientStartedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import io.github.getsixtyfour.openpyn.R
import io.github.sdsstudios.nvidiagpumonitor.controllers.OpenpynController
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnCommandExecuteListener
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnOutputLineListener
import java.util.UUID

/**
 * Created by Seth on 04/03/18.
 */
@MainThread
class ConnectionManager(
    ctx: Context,
    private val onClientStartedListener: OnClientStartedListener,
    private val mActivitySessionStartedListener: OnSessionStartedListener,
    private val mActivitySessionFinishedListener: OnSessionFinishedListener,
    mActivitySessionExecuteListener: OnSessionExecuteListener?,
    mActivityCommandExecuteListener: OnCommandExecuteListener?,
    mActivityOnOutputLineListener: OnOutputLineListener?
) : OnSessionStartedListener, OnSessionFinishedListener {

    private val openpyn = MutableLiveData<Int>()
    private var mSessionKey = ""
    private var mSessionId = 0
    private var mSessionRunning = false
    private val mClient = PluginClient()
    private val mCtx: Context = ctx.applicationContext
    private val mOpenpynController = OpenpynController(
        mCtx, openpyn, mActivitySessionExecuteListener, mActivityCommandExecuteListener, mActivityOnOutputLineListener
    )
    private val mControllers = listOf(
        mOpenpynController
    )

    override fun onSessionStarted(sessionId: Int, sessionKey: String) {
        mSessionId = sessionId
        mSessionKey = sessionKey
        mSessionRunning = false

        mActivitySessionStartedListener.onSessionStarted(sessionId, sessionKey)

        mClient.addSessionFinishedListener(sessionId, sessionKey, this)

        mControllers.forEach { it.start(mClient, mSessionId, mSessionKey) }
    }

    override fun onSessionFinished() {
        mSessionId = 0
        mSessionKey = ""
        mSessionRunning = false

        mActivitySessionFinishedListener.onSessionFinished()

        mControllers.forEach { it.stop() }
    }

    override fun onSessionCancelled() {
        mSessionRunning = false

        mActivitySessionStartedListener.onSessionCancelled()
    }

    fun isConnected(): Boolean {
        return mSessionId > 0
    }

    fun isConnectingOrDisconnecting(): Boolean {
        return mSessionRunning
    }

    fun toggleConnection(activity: Activity, id: UUID, requestCode: Int) {
        if (isConnectingOrDisconnecting()) return

        mSessionRunning = true

        if (isConnected()) disconnect() else connect(activity, id, requestCode)
    }

    fun startClient() {
        mClient.start(mCtx, onClientStartedListener)
    }

    fun gotActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mClient.gotActivityResult(requestCode, resultCode, data)
    }

    fun onDestroy() {
        if (isConnected()) disconnect()

        mClient.stop(mCtx)
    }

    @Suppress("MagicNumber")
    private fun disconnect() {
        mControllers.forEach { it.kill(mClient, mSessionId, mSessionKey) }

        Thread(Runnable {
            try {
                Thread.sleep(5000)
                mClient.disconnect(mSessionId, mSessionKey)
            } catch (e: ServiceNotConnectedException) {
                Toast.makeText(mCtx, R.string.error_could_not_connect_to_service, Toast.LENGTH_LONG).show()
            }
        }).start()
    }

    private fun connect(activity: Activity, id: UUID, requestCode: Int) {
        Thread(Runnable {
            try {
                mClient.connect(activity, id, this, requestCode)
            } catch (e: ServiceNotConnectedException) {
                Toast.makeText(mCtx, R.string.error_could_not_connect_to_service, Toast.LENGTH_LONG).show()
            }
        }).start()
    }
}
