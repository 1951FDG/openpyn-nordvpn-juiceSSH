package io.github.getsixtyfour.openpyn.utils

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LiveData
import io.github.getsixtyfour.openpyn.AppConfig
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

        (mMainHandler ?: return).post(runnable)
    }

    private fun executeOnMainThread(runnable: Runnable) {
        if (isMainThread()) {
            runnable.run()
        } else {
            postToMainThread(runnable)
        }
    }

    private fun isMainThread(): Boolean = Looper.getMainLooper().thread == Thread.currentThread()

    // Constructor
    init {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            // Receive network changes
            @Suppress("MagicNumber")
            override fun onAvailable(network: Network?) {
                // Network available
                debug("Network available")
                // Get network type
                val netCap: NetworkCapabilities? = connectivityManager.getNetworkCapabilities(network)
                netCap?.let {
                    when {
                        it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                            debug("Connectivity: CELLULAR")
                        }
                        it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            debug("Connectivity: WIFI")
                        }
                        it.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> {
                            debug("Connectivity: BLUETOOTH")
                        }
                        it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                            debug("Connectivity: ETHERNET")
                        }
                        it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> {
                            debug("Connectivity: VPN")
                        }
                        it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI_AWARE) -> {
                            debug("Connectivity: WIFI_AWARE")
                        }
                        it.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN) -> {
                            debug("Connectivity: LOWPAN")
                        }
                    }
                }
                // Verify internet access
                if (hostAvailable("google.com", 80)) {
                    // Internet access
                    debug("Internet Access Detected")
                    postValue(true)
                } else {
                    // No internet access
                    debug("Unable to access Internet")
                    postValue(false)
                }

                notifyNetworkChangeToAll(network)
            }

            override fun onLost(network: Network?) {
                // No network available
                debug("Network not available")
                postValue(false)

                notifyNetworkChangeToAll(network)
            }
        }
        val builder = NetworkRequest.Builder()
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
    }

    // Collection of listeners
    private val listeners by lazy { LinkedHashSet<NetworkInfoListener>() }

    @RequiresPermission(android.Manifest.permission.ACCESS_NETWORK_STATE)
    constructor(application: Application) : this(application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)

    // Verify host availability
    @Suppress("MagicNumber")
    fun hostAvailable(host: String, port: Int): Boolean {
        debug("Verifying host availability: $host:$port")
        if (!AppConfig.ONLINE) {
            return false
        }

        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 2000)
                socket.close()
                // Host available
                debug("Host: $host:$port is available")
                return@hostAvailable true
            }
        } catch (e: IOException) {
            // Host unreachable or timeout
            debug("Host: $host:$port is not available")
            return false
        }
    }

    // Notify network change to all listeners
    fun notifyNetworkChangeToAll(network: Network?) {
        debug("notifyStateToAll")
        for (listener in listeners) {
            notifyNetworkChange(listener, network)
        }
    }

    // Notify network change
    private fun notifyNetworkChange(listener: NetworkInfoListener, network: Network?) {
        debug("notifyState")
        executeOnMainThread(Runnable { listener.networkStatusChange(network) })
    }

    // Add a listener
    @Suppress("unused")
    fun addListener(listener: NetworkInfoListener) {
        debug("addListener")
        listeners.add(listener)
    }

    // Remove a listener
    @Suppress("unused")
    fun removeListener(listener: NetworkInfoListener) {
        debug("removeListener")
        listeners.remove(listener)
    }

    // Get current network status
    fun isOnline(): Boolean {
        var ns = this.value
        if (ns == null) {
            ns = false
        }
        return ns
    }

    // Static content
    companion object {

        var ns: NetworkInfo? = null

        // Get a singleton
        @MainThread
        fun getInstance(application: Application? = null): NetworkInfo {
            if (ns == null) {
                ns = NetworkInfo(requireNotNull(application))
            }
            return ns as NetworkInfo
        }
    }

    // Interface that represent the [NetworkStatusListener]
    interface NetworkInfoListener {

        fun networkStatusChange(network: Network?)
    }
}
