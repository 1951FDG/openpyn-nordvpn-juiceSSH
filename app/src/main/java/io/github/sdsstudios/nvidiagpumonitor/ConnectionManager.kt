package io.github.sdsstudios.nvidiagpumonitor

import android.annotation.SuppressLint
import android.content.Context
import com.sonelli.juicessh.pluginlibrary.PluginClient
import com.sonelli.juicessh.pluginlibrary.listeners.OnClientStartedListener

/**
 * Created by Seth on 04/03/18.
 */

class ConnectionManager(private val mCtx: Context) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: ConnectionManager? = null

        fun getInstance(ctx: Context): ConnectionManager {
            if (INSTANCE == null) {
                //Always pass application context
                INSTANCE = ConnectionManager(ctx.applicationContext)
            }

            return INSTANCE!!
        }
    }

    val connected = false

    private val mClient = PluginClient()

    fun startClient(onClientStartedListener: OnClientStartedListener) {
        mClient.start(mCtx, onClientStartedListener)
    }
}