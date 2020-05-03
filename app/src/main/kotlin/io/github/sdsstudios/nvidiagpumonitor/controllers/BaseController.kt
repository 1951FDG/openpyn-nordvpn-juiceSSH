package io.github.sdsstudios.nvidiagpumonitor.controllers

import android.content.Context
import android.widget.Toast
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import com.sonelli.juicessh.pluginlibrary.PluginClient
import com.sonelli.juicessh.pluginlibrary.exceptions.ServiceNotConnectedException
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener
import io.github.getsixtyfour.openpyn.R

@MainThread
abstract class BaseController(protected val mCtx: Context) : OnSessionExecuteListener {

    protected var command: String = ""
    var isRunning: Boolean = false

    @CallSuper
    override fun onCompleted(exitCode: Int) {
        isRunning = false
    }

    @CallSuper
    open fun connect(pluginClient: PluginClient, sessionId: Int, sessionKey: String): Boolean =
        run(pluginClient, sessionId, sessionKey).also { isRunning = it }

    @CallSuper
    open fun disconnect(pluginClient: PluginClient, sessionId: Int, sessionKey: String): Boolean =
        run(pluginClient, sessionId, sessionKey).also { isRunning = it }

    private fun run(pluginClient: PluginClient, sessionId: Int, sessionKey: String): Boolean {
        try {
            if (command.isNotEmpty()) {
                pluginClient.executeCommandOnSession(sessionId, sessionKey, command, this)
                return true
            }
        } catch (e: ServiceNotConnectedException) {
            Toast.makeText(mCtx, R.string.error_juicessh_service, Toast.LENGTH_LONG).show()
        }
        return false
    }

    abstract fun onDestroy()
}
