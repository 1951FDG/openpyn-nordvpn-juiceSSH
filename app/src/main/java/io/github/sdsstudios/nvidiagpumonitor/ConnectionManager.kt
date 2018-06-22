package io.github.sdsstudios.nvidiagpumonitor

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import com.sonelli.juicessh.pluginlibrary.PluginClient
import com.sonelli.juicessh.pluginlibrary.exceptions.ServiceNotConnectedException
import com.sonelli.juicessh.pluginlibrary.listeners.OnClientStartedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import io.github.sdsstudios.nvidiagpumonitor.controllers.*
import org.jetbrains.anko.longToast
import java.util.*

/**
 * Created by Seth on 04/03/18.
 */

class ConnectionManager(ctx: Context,
                        private val mActivitySessionStartedListener: OnSessionStartedListener,
                        private val mActivitySessionFinishedListener: OnSessionFinishedListener

) : OnSessionStartedListener, OnSessionFinishedListener {

    companion object {
        const val JUICESSH_REQUEST_CODE = 345
    }

//    val powerUsage = MutableLiveData<Int>()
//    val temperature = MutableLiveData<Int>()
//    val fanSpeed = MutableLiveData<Int>()
//    val freeMemory = MutableLiveData<Int>()
//    val usedMemory = MutableLiveData<Int>()
//    val graphicsClock = MutableLiveData<Int>()
//    val videoClock = MutableLiveData<Int>()
//    val memoryClock = MutableLiveData<Int>()

    private val openpyn = MutableLiveData<Int>()

    private var connected = false

    private var mSessionKey = ""
    private var mSessionId = 0

    private val mClient = PluginClient()
    private val mCtx: Context = ctx.applicationContext

//    private val mPowerController = PowerController(mCtx, powerUsage)
//    private val mTempController = TempController(mCtx, temperature)
//    private val mFanSpeedController = FanSpeedController(mCtx, fanSpeed)
//    private val mFreeMemController = FreeMemController(mCtx, freeMemory)
//    private val mUsedMemController = UsedMemController(mCtx, usedMemory)
//    private val mGraphicsClockController = GraphicsClockController(mCtx, graphicsClock)
//    private val mVideoClockController = VideoClockController(mCtx, videoClock)
//    private val mMemoryClockController = MemoryClockController(mCtx, memoryClock)
    private val mOpenpynController = OpenpynController(ctx, openpyn)

    private val mControllers = listOf(
//            mPowerController,
//            mTempController,
//            mFanSpeedController,
//            mFreeMemController,
//            mUsedMemController,
//            mGraphicsClockController,
//            mVideoClockController,
//            mMemoryClockController,
            mOpenpynController
    )

    override fun onSessionStarted(sessionId: Int, sessionKey: String?) {
        mSessionId = sessionId
        mSessionKey = sessionKey!!

        mActivitySessionStartedListener.onSessionStarted(sessionId, sessionKey)

        mClient.addSessionFinishedListener(sessionId, sessionKey, this)

        connected = true

        mControllers.forEach { it.start(mClient, mSessionId, mSessionKey) }
    }

    override fun onSessionFinished() {
        mActivitySessionFinishedListener.onSessionFinished()

        connected = false

        mControllers.forEach { it.stop() }
    }

    override fun onSessionCancelled() {
        /*
        onSessionCancelled is still called even during a successful connection.
        Only execute onSessionCancelled when its a failed connection
        */

        if (!connected) {
            mActivitySessionStartedListener.onSessionCancelled()
        }
    }

    fun toggleConnection(uuid: UUID, activity: AppCompatActivity) {
        if (!connected) {
            connect(uuid, activity)
        } else {
            disconnect()
        }
    }

    fun startClient(onClientStartedListener: OnClientStartedListener) {
        mClient.start(mCtx, onClientStartedListener)
    }

    fun gotActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mClient.gotActivityResult(requestCode, resultCode, data)
    }

    fun onDestroy() {
        if (connected) disconnect()

        mClient.stop(mCtx)
    }

    private fun disconnect() {
        Thread(Runnable {
            try {
                mControllers.forEach { it.kill(mClient, mSessionId, mSessionKey) }
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