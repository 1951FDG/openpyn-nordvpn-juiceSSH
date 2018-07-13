/*
 * NetworkInfo
 * Utility class that provide information about the device's network connectivity
 * MIT License
 * Copyright (c) 2018 Evert Arias
 *
 */

package io.github.sdsstudios.nvidiagpumonitor

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import de.jupf.staticlog.Log
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket


class NetworkInfo(context: Context) : BroadcastReceiver(), AnkoLogger {

    private var network: Network

    // collection of listeners
    private val listeners = mutableSetOf<NetworkInfoListener>()

    // constructor
    init {
        context.registerReceiver(this, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        network = Network()
    }

    // receive network changes
    override fun onReceive(context: Context, intent: Intent) = runBlocking {
        debug("onReceive")
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        val job = launch {
            // verify network availability
            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting) {
                debug("Network available")
                // verify internet access
                if(hostAvailable("google.com", 80)){
                    // internet access
                    debug("Internet Access Detected")
                    network.status = NetworkStatus.INTERNET
                } else {
                    // no internet access
                    debug("Unable to access Internet")
                    network.status = NetworkStatus.OFFLINE
                }
                // get network type
                when (activeNetwork.type) {
                    ConnectivityManager.TYPE_MOBILE -> {
                        // mobile network
                        debug("Connectivity: MOBILE")
                        network.type = NetworkType.MOBILE
                    }
                    ConnectivityManager.TYPE_WIFI -> {
                        // wifi network
                        debug("Connectivity: WIFI")
                        network.type = NetworkType.WIFI
                    }
                    else -> {
                        // no network available
                        debug("Network not available")
                        network.type = NetworkType.NONE
                    }
                }
            } else {
                // no network available
                debug("Network not available")
                network.type = NetworkType.NONE
                network.status = NetworkStatus.OFFLINE
            }
        }
        job.join()
        notifyNetworkChangeToAll()
    }

    // verify host availability
    private fun hostAvailable(host: String, port: Int): Boolean {
        debug("Verifying host availability: $host:$port")
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 2000)
                socket.close()
                // host available
                debug("Host: $host:$port is available")
                return true
            }
        } catch (e: IOException) {
            // host unreachable or timeout
            debug("Host: $host:$port is not available")
            return false
        }
    }

    // notify network change to all listeners
    private fun notifyNetworkChangeToAll() {
        debug("notifyStateToAll")
        for (listener in listeners) {
            notifyNetworkChange(listener)
        }
    }

    // notify network change
    private fun notifyNetworkChange(listener: NetworkInfoListener) {
        debug("notifyState")
        listener.networkStatusChange(network)
    }

    // add a listener
    fun addListener(listener: NetworkInfoListener) {
        debug("addListener")
        listeners.add(listener)
        notifyNetworkChange(listener)
    }

    // remove a listener
    fun removeListener(listener: NetworkInfoListener) {
        debug("removeListener")
        listeners.remove(listener)
    }

    // get current network information
    fun getNetwork(): Network {
        return network
    }

    // static content
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var ns: NetworkInfo? = null

        // get a singleton
        fun getInstance(ctx: Context): NetworkInfo {
            if (ns == null) {
                ns = NetworkInfo(ctx.applicationContext)
            }
            return ns as NetworkInfo
        }

        // get current network connectivity
        fun getConnectivity(ctx: Context): Network {
            val network = Network()
            // application context is recommended here
            val manager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = manager.activeNetworkInfo

            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting) {
                Log.debug("Network available")
                // internet access
                Log.debug("Internet Access Detected")
                network.status = NetworkStatus.INTERNET
                // get network type
                when (activeNetwork.type) {
                    ConnectivityManager.TYPE_MOBILE -> {
                        // mobile network
                        Log.debug("Connectivity: MOBILE")
                        network.type = NetworkType.MOBILE
                    }
                    ConnectivityManager.TYPE_WIFI -> {
                        // wifi network
                        Log.debug("Connectivity: WIFI")
                        network.type = NetworkType.WIFI
                    }
                    else -> {
                        // no network available
                        Log.debug("Network not available")
                        network.type = NetworkType.NONE
                    }
                }
            } else {
                // no network available
                Log.debug("Network not available")
                network.type = NetworkType.NONE
                network.status = NetworkStatus.OFFLINE
            }

            return network
        }
    }

    // interface that represent the [NetworkStatusListener]
    interface NetworkInfoListener {
        fun networkStatusChange(network: Network)
    }

    data class Network(
            var type: NetworkType = NetworkType.NONE,
            var status: NetworkStatus = NetworkStatus.OFFLINE
    )

    enum class NetworkType { NONE, WIFI, MOBILE }

    enum class NetworkStatus { OFFLINE, INTERNET }
}