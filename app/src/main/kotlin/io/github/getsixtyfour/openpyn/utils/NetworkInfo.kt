package io.github.getsixtyfour.openpyn.utils

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_ETHERNET
import android.net.NetworkCapabilities.TRANSPORT_LOWPAN
import android.net.NetworkCapabilities.TRANSPORT_VPN
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.net.NetworkCapabilities.TRANSPORT_WIFI_AWARE
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

class NetworkInfo internal constructor(private val connectivityManager: ConnectivityManager) : LiveData<Boolean>(), AnkoLogger {
    private val mLock = Any()
    @Volatile
    private var mMainHandler: Handler? = null

    private fun postToMainThread(runnable: Runnable) {
        if (mMainHandler == null) {
            synchronized(mLock) {
                if (mMainHandler == null) {
                    mMainHandler = Handler(Looper.getMainLooper())
                }
            }
        }

        mMainHandler!!.post(runnable)
    }

    private fun executeOnMainThread(runnable: Runnable) {
        if (isMainThread()) {
            runnable.run()
        } else {
            postToMainThread(runnable)
        }
    }

    private fun isMainThread(): Boolean {
        return Looper.getMainLooper().thread == Thread.currentThread()
    }

    // constructor
    init {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            // receive network changes
            @Suppress("MagicNumber")
            override fun onAvailable(network: Network?) {
                // network available
                debug("Network available")
                // get network type
                val netCap: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(network)
                netCap?.let {
                    when {
                        it.hasTransport(TRANSPORT_CELLULAR) -> {
                            debug("Connectivity: CELLULAR")
                        }
                        it.hasTransport(TRANSPORT_WIFI) -> {
                            debug("Connectivity: WIFI")
                        }
                        it.hasTransport(TRANSPORT_BLUETOOTH) -> {
                            debug("Connectivity: BLUETOOTH")
                        }
                        it.hasTransport(TRANSPORT_ETHERNET) -> {
                            debug("Connectivity: ETHERNET")
                        }
                        it.hasTransport(TRANSPORT_VPN) -> {
                            debug("Connectivity: VPN")
                        }
                        it.hasTransport(TRANSPORT_WIFI_AWARE) -> {
                            debug("Connectivity: WIFI_AWARE")
                        }
                        it.hasTransport(TRANSPORT_LOWPAN) -> {
                            debug("Connectivity: LOWPAN")
                        }
                    }
                }
                // verify internet access
                if (hostAvailable("google.com", 80)) {
                    // internet access
                    debug("Internet Access Detected")
                    postValue(true)
                } else {
                    // no internet access
                    debug("Unable to access Internet")
                    postValue(false)
                }

                notifyNetworkChangeToAll(network)
            }

            override fun onLost(network: Network?) {
                // no network available
                debug("Network not available")
                postValue(false)

                notifyNetworkChangeToAll(network)
            }
        }
        val builder = NetworkRequest.Builder()
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
    }

    // collection of listeners
    private val listeners by lazy { LinkedHashSet<NetworkInfoListener>() }

    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    constructor(application: Application) : this(application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)

    // verify host availability
    @Suppress("MagicNumber")
    fun hostAvailable(host: String, port: Int): Boolean {
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
    fun notifyNetworkChangeToAll(network: Network?) {
        debug("notifyStateToAll")
        for (listener in listeners) {
            notifyNetworkChange(listener, network)
        }
    }

    // notify network change
    private fun notifyNetworkChange(listener: NetworkInfoListener, network: Network?) {
        debug("notifyState")
        executeOnMainThread(Runnable { listener.networkStatusChange(network) })
    }

    // add a listener
    fun addListener(listener: NetworkInfoListener) {
        debug("addListener")
        listeners.add(listener)
    }

    // remove a listener
    fun removeListener(listener: NetworkInfoListener) {
        debug("removeListener")
        listeners.remove(listener)
    }

    // get current network status
    fun isOnline(): Boolean {
        var ns = this.value
        if (ns == null) {
            ns = false
        }
        return ns
    }

    // static content
    companion object {

        var ns: NetworkInfo? = null

        // get a singleton
        @MainThread
        fun getInstance(application: Application? = null): NetworkInfo {
            if (ns == null) {
                ns = NetworkInfo(requireNotNull(application))
            }
            return ns as NetworkInfo
        }
    }

    // interface that represent the [NetworkStatusListener]
    interface NetworkInfoListener {

        fun networkStatusChange(network: Network?)
    }
}
