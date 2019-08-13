package io.github.sdsstudios.nvidiagpumonitor

import android.content.Context
import android.content.Intent
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
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
import org.jetbrains.anko.longToast
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

    companion object {
        const val JUICESSH_REQUEST_CODE: Int = 345
    }

    private val openpyn = MutableLiveData<Int>()
    private var mSessionKey = ""
    private var mSessionId = 0
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

        mActivitySessionStartedListener.onSessionStarted(sessionId, sessionKey)

        mClient.addSessionFinishedListener(sessionId, sessionKey, this)

        mControllers.forEach { it.start(mClient, mSessionId, mSessionKey) }
    }

    override fun onSessionFinished() {
        mSessionId = 0
        mSessionKey = ""

        mActivitySessionFinishedListener.onSessionFinished()

        mControllers.forEach { it.stop() }
    }

    override fun onSessionCancelled() {
        mActivitySessionStartedListener.onSessionCancelled()
    }

    fun isConnected(): Boolean {
        return mSessionId > 0
    }

    fun toggleConnection(uuid: UUID, activity: AppCompatActivity) {
        if (isConnected()) disconnect() else connect(uuid, activity)
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
                mCtx.longToast(R.string.error_couldnt_connect_to_service)
            }
        }).start()
    }

    private fun connect(uuid: UUID, activity: AppCompatActivity) {
        Thread(Runnable {
            try {
                mClient.connect(activity, uuid, this, JUICESSH_REQUEST_CODE)
            } catch (e: ServiceNotConnectedException) {
                mCtx.longToast(R.string.error_couldnt_connect_to_service)
            }
        }).start()
    }
}
