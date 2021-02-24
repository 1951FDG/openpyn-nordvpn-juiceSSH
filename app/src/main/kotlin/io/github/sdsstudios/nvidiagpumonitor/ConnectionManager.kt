package io.github.sdsstudios.nvidiagpumonitor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@MainThread
class ConnectionManager(
    ctx: Context,
    lifecycleOwner: LifecycleOwner?,
    private var mSessionStartedListener: OnSessionStartedListener?,
    private var mSessionFinishedListener: OnSessionFinishedListener?,
    sessionExecuteListener: OnSessionExecuteListener?,
    commandExecuteListener: OnCommandExecuteListener?,
    onOutputLineListener: OnOutputLineListener?
) : LifecycleObserver, OnClientStartedListener, OnSessionStartedListener, OnSessionFinishedListener,
    CoroutineScope by CoroutineScope(Job() + Dispatchers.IO) {

    private var mSessionKey = ""
    private var mSessionId = 0
    private var mSessionRunning = false
    private val mClient = PluginClient()
    private val mCtx = ctx.applicationContext
    private val mOpenpynController = OpenpynController(
        mCtx, sessionExecuteListener, commandExecuteListener, onOutputLineListener
    )
    private val mControllers: List<BaseController> = listOf(
        mOpenpynController
    )

    init {
        lifecycleOwner?.lifecycle?.addObserver(this)
        startClient()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        cancel()

        mSessionStartedListener = null
        mSessionFinishedListener = null

        mControllers.forEach(BaseController::onDestroy)

        if (isConnected()) mClient.disconnect(mSessionId, mSessionKey)

        stopClient()
    }

    override fun onClientStarted() {
    }

    override fun onClientStopped() {
    }

    override fun onSessionStarted(sessionId: Int, sessionKey: String) {
        mSessionId = sessionId
        mSessionKey = sessionKey
        mSessionRunning = false

        mSessionStartedListener?.onSessionStarted(mSessionId, mSessionKey)

        mClient.addSessionFinishedListener(mSessionId, mSessionKey, this)

        mControllers.forEach { it.connect(mClient, mSessionId, mSessionKey) }
    }

    override fun onSessionCancelled() {
        mSessionId = 0
        mSessionKey = ""
        mSessionRunning = false

        mSessionStartedListener?.onSessionCancelled()
    }

    override fun onSessionFinished() {
        mSessionId = 0
        mSessionKey = ""
        mSessionRunning = false

        mSessionFinishedListener?.onSessionFinished()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mClient.gotActivityResult(requestCode, resultCode, data)
    }

    fun isConnected(): Boolean = mSessionId > 0

    fun isConnectingOrDisconnecting(): Boolean = mSessionRunning

    fun toggleConnection(activity: Activity, id: UUID?, requestCode: Int) {
        when {
            id == null -> return
            isConnected() -> disconnect()
            else -> connect(activity, id, requestCode)
        }
    }

    private fun startClient() {
        mClient.start(mCtx, this)
    }

    private fun stopClient() {
        mClient.stop(mCtx)
    }

    fun connect(activity: Activity, id: UUID, requestCode: Int) {
        if (isConnectingOrDisconnecting()) return

        mSessionRunning = true

        launch {
            try {
                mClient.connect(activity, id, this@ConnectionManager, requestCode)
            } catch (e: ServiceNotConnectedException) {
                Toast.makeText(mCtx, R.string.error_juicessh_service, Toast.LENGTH_LONG).show()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    @Suppress("MagicNumber")
    fun disconnect() {
        if (isConnectingOrDisconnecting()) return

        mSessionRunning = true

        mControllers.filter(BaseController::isRunning).forEach { it.disconnect(mClient, mSessionId, mSessionKey) }

        launch {
            try {
                // Delay is required to receive method calls in OnSessionExecuteListener
                delay(5.seconds.toLongMilliseconds())
                mClient.disconnect(mSessionId, mSessionKey)
            } catch (e: ServiceNotConnectedException) {
                Toast.makeText(mCtx, R.string.error_juicessh_service, Toast.LENGTH_LONG).show()
            }
        }
    }
}
