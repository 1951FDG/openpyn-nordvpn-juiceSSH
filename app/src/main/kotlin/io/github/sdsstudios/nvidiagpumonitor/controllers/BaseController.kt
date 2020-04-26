package io.github.sdsstudios.nvidiagpumonitor.controllers

import android.content.Context
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import com.sonelli.juicessh.pluginlibrary.PluginClient
import com.sonelli.juicessh.pluginlibrary.exceptions.ServiceNotConnectedException
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener
import io.github.getsixtyfour.openpyn.R
import mu.KLoggable

@MainThread
abstract class BaseController(
    protected val mCtx: Context, private val mLiveData: MutableLiveData<Int>
) : OnSessionExecuteListener, KLoggable {

    private var isRunning = false
    abstract val regex: Regex
    var startCommand: String = ""
    var stopCommand: String = ""

    @Suppress("MagicNumber")
    @CallSuper
    override fun onCompleted(exitCode: Int) {
        when (exitCode) {
            127 -> {
                mLiveData.value = null
                logger.error("Tried to run a command but the command was not found on the server")
            }
        }
    }

    override fun onOutputLine(line: String) {
        regex.find(line)?.let { mLiveData.value = convertDataToInt(it.value) }
    }

    @CallSuper
    override fun onError(error: Int, reason: String) {
        logger.error(reason)
    }

    @CallSuper
    open fun start(pluginClient: PluginClient, sessionId: Int, sessionKey: String): Boolean {
        isRunning = true

        try {
            if (startCommand.isNotEmpty()) {
                pluginClient.executeCommandOnSession(
                    sessionId, sessionKey, startCommand, this@BaseController
                )
                return true
            }
        } catch (e: ServiceNotConnectedException) {
            Toast.makeText(mCtx, R.string.error_juicessh_service, Toast.LENGTH_LONG).show()
        }
        return false
    }

    fun stop() {
        isRunning = false
    }

    @CallSuper
    open fun kill(pluginClient: PluginClient, sessionId: Int, sessionKey: String): Boolean {
        isRunning = true

        try {
            if (stopCommand.isNotEmpty()) {
                pluginClient.executeCommandOnSession(
                    sessionId, sessionKey, stopCommand, this@BaseController
                )
                return true
            }
        } catch (e: ServiceNotConnectedException) {
            Toast.makeText(mCtx, R.string.error_juicessh_service, Toast.LENGTH_LONG).show()
        }
        return false
    }

    abstract fun convertDataToInt(data: String): Int

    abstract fun onDestroy()
}
