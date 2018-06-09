package io.github.sdsstudios.nvidiagpumonitor.controllers

import android.arch.lifecycle.MutableLiveData
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Handler
import android.util.Log
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.sonelli.juicessh.pluginlibrary.PluginClient
import com.sonelli.juicessh.pluginlibrary.exceptions.ServiceNotConnectedException
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener
import io.github.sdsstudios.nvidiagpumonitor.MainActivity

/**
 * Created by Seth on 05/03/18.
 */

abstract class BaseController(
        ctx: Context,
        private val mLiveData: MutableLiveData<Int>) : OnSessionExecuteListener {

    companion object {
        private const val INTERVAL_SECS = 2
    }

    val mCtx = ctx.applicationContext
    val mainActivity = ctx as MainActivity

    var isRunning = false

    abstract val regex: Regex
    var command = ""
    var stopcommand = ""

    override fun onCompleted(exitCode: Int) {
        when (exitCode) {
            127 -> {
                mLiveData.value = null
                Log.d(TAG, "Tried to run a command but the command was not found on the server")
            }
        }
    }

    override fun onOutputLine(line: String) {
        val matchResult = regex.find(line)

        if (matchResult != null) {
            mLiveData.value = convertDataToInt(matchResult.value)
        }
    }

    override fun onError(error: Int, reason: String) {
        Toast.makeText(mCtx, reason, LENGTH_SHORT).show()
    }

    open fun start(pluginClient: PluginClient,
              sessionId: Int,
              sessionKey: String) {

        isRunning = true

//        val handler = Handler()
//
//        handler.post(object : Runnable {
//            override fun run() {
                try {
                    pluginClient.executeCommandOnSession(
                            sessionId,
                            sessionKey,
                            command,
                            this@BaseController
                    )

                } catch (e: ServiceNotConnectedException) {
                    Log.d(TAG, "Tried to execute a command but could not connect to JuiceSSH plugin service")
                }

//                if (isRunning) {
//                    handler.postDelayed(this, INTERVAL_SECS * 1000L)
//                }
//            }
//        })
    }

    fun stop() {
        isRunning = false
    }

    open fun kill(pluginClient: PluginClient,
             sessionId: Int,
             sessionKey: String) {

        try {
            pluginClient.executeCommandOnSession(
                    sessionId,
                    sessionKey,
                    stopcommand,
                    this@BaseController
            )

        } catch (e: ServiceNotConnectedException) {
            Log.d(TAG, "Tried to execute a command but could not connect to JuiceSSH plugin service")
        }
    }

    abstract fun convertDataToInt(data: String): Int
}