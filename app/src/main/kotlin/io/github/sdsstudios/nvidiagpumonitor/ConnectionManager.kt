package io.github.sdsstudios.nvidiagpumonitor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import com.sonelli.juicessh.pluginlibrary.PluginClient
import com.sonelli.juicessh.pluginlibrary.exceptions.ServiceNotConnectedException
import com.sonelli.juicessh.pluginlibrary.listeners.OnClientStartedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import io.github.getsixtyfour.openpyn.R
import io.github.sdsstudios.nvidiagpumonitor.controllers.BaseController
import io.github.sdsstudios.nvidiagpumonitor.controllers.OpenpynController
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnCommandExecuteListener
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnOutputLineListener
import java.util.UUID

@MainThread
class ConnectionManager(
    ctx: Context,
    lifecycleOwner: LifecycleOwner?,
    private var mSessionStartedListener: OnSessionStartedListener?,
    private var mSessionFinishedListener: OnSessionFinishedListener?,
    sessionExecuteListener: OnSessionExecuteListener?,
    commandExecuteListener: OnCommandExecuteListener?,
    onOutputLineListener: OnOutputLineListener?
) : LifecycleObserver, OnClientStartedListener, OnSessionStartedListener, OnSessionFinishedListener {

    init {
        lifecycleOwner?.lifecycle?.addObserver(this)
    }

    private val openpyn = MutableLiveData<Int>()
    private var mSessionKey = ""
    private var mSessionId = 0
    private var mSessionRunning = false
    private val mClient = PluginClient()
    private val mCtx = ctx.applicationContext
    private val mOpenpynController = OpenpynController(
        mCtx, openpyn, sessionExecuteListener, commandExecuteListener, onOutputLineListener
    )
    private val mControllers: List<BaseController> = listOf(
        mOpenpynController
    )

    override fun onClientStarted() {
    }

    override fun onClientStopped() {
    }

    override fun onSessionCancelled() {
        mSessionRunning = false

        mSessionStartedListener?.onSessionCancelled()
    }

    override fun onSessionStarted(sessionId: Int, sessionKey: String) {
        mSessionId = sessionId
        mSessionKey = sessionKey
        mSessionRunning = false

        mSessionStartedListener?.onSessionStarted(sessionId, sessionKey)

        mClient.addSessionFinishedListener(sessionId, sessionKey, this)

        mControllers.forEach { it.start(mClient, mSessionId, mSessionKey) }
    }

    override fun onSessionFinished() {
        mSessionId = 0
        mSessionKey = ""
        mSessionRunning = false

        mSessionFinishedListener?.onSessionFinished()

        mControllers.forEach { it.stop() }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mClient.gotActivityResult(requestCode, resultCode, data)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        mSessionStartedListener = null
        mSessionFinishedListener = null

        mControllers.forEach { it.onDestroy() }

        if (isConnected()) disconnect()

        stopClient()
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
        mClient.start(mCtx, this)
    }

    fun stopClient() {
        mClient.stop(mCtx)
    }

    private fun connect(activity: Activity, id: UUID, requestCode: Int) {
        Thread(Runnable {
            try {
                mClient.connect(activity, id, this, requestCode)
            } catch (e: ServiceNotConnectedException) {
                Toast.makeText(mCtx, R.string.error_juicessh_service, Toast.LENGTH_LONG).show()
            }
        }).start()
    }

    @Suppress("MagicNumber")
    private fun disconnect() {
        mControllers.forEach { it.kill(mClient, mSessionId, mSessionKey) }

        Thread(Runnable {
            try {
                Thread.sleep(5000)
                mClient.disconnect(mSessionId, mSessionKey)
            } catch (e: ServiceNotConnectedException) {
                Toast.makeText(mCtx, R.string.error_juicessh_service, Toast.LENGTH_LONG).show()
            }
        }).start()
    }
}
