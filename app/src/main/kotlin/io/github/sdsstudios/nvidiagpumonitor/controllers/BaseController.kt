package io.github.sdsstudios.nvidiagpumonitor.controllers

import android.content.Context
import androidx.annotation.CallSuper
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import com.sonelli.juicessh.pluginlibrary.PluginClient
import com.sonelli.juicessh.pluginlibrary.exceptions.ServiceNotConnectedException
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener
import io.github.getsixtyfour.openpyn.R
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error
import org.jetbrains.anko.longToast

/**
 * Created by Seth on 05/03/18.
 */
@MainThread
abstract class BaseController(
    protected val mCtx: Context,
    private val mLiveData: MutableLiveData<Int>
) : OnSessionExecuteListener, AnkoLogger {

    private var isRunning = false
    abstract val regex: Regex
    var command: String = ""
    var stopcommand: String = ""

    @Suppress("MagicNumber")
    @CallSuper
    override fun onCompleted(exitCode: Int) {
        when (exitCode) {
            127 -> {
                mLiveData.value = null
                error("Tried to run a command but the command was not found on the server")
            }
        }
    }

    override fun onOutputLine(line: String) {
        val matchResult = regex.find(line)

        if (matchResult != null) {
            mLiveData.value = convertDataToInt(matchResult.value)
        }
    }

    @CallSuper
    override fun onError(error: Int, reason: String) {
        error(reason)
    }

    @CallSuper
    open fun start(pluginClient: PluginClient, sessionId: Int, sessionKey: String): Boolean {
        isRunning = true

        try {
            if (command.isNotEmpty()) {
                pluginClient.executeCommandOnSession(
                    sessionId,
                    sessionKey,
                    command,
                    this@BaseController
                )
                return true
            }
        } catch (e: ServiceNotConnectedException) {
            mCtx.longToast(R.string.error_could_not_connect_to_service)
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
            if (stopcommand.isNotEmpty()) {
                pluginClient.executeCommandOnSession(
                    sessionId,
                    sessionKey,
                    stopcommand,
                    this@BaseController
                )
                return true
            }
        } catch (e: ServiceNotConnectedException) {
            mCtx.longToast(R.string.error_could_not_connect_to_service)
        }
        return false
    }

    abstract fun convertDataToInt(data: String): Int
}
