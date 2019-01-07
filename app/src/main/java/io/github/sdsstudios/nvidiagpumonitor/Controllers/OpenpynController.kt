package io.github.sdsstudios.nvidiagpumonitor.controllers

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.sonelli.juicessh.pluginlibrary.PluginClient
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnCommandExecuteListener
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnOutputLineListener
import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class OpenpynController(
    ctx: Context,
    liveData: MutableLiveData<Int>,
    private val mActivitySessionExecuteListener: OnSessionExecuteListener?,
    private val mActivityExecuteCommandListener: OnCommandExecuteListener?,
    private val mActivityOnOutputLineListener: OnOutputLineListener?
) : BaseController(ctx, liveData), AnkoLogger {
    companion object {
        private const val SPAM = "SPAM"
        private const val DEBUG = "DEBUG"
        private const val VERBOSE = "VERBOSE"
        private const val INFO = "INFO"
        private const val NOTICE = "NOTICE"
        private const val WARNING = "WARNING"
        private const val SUCCESS = "SUCCESS"
        private const val ERROR = "ERROR"
        private const val CRITICAL = "CRITICAL"
    }

    override val regex: Regex = Regex("""\d+""")
    private var test = false
    private var nvram = false
    private var buffer = StringBuilder(256)

    override fun onCompleted(exitCode: Int) {
        super.onCompleted(exitCode)

        info(exitCode.toString())

        when (exitCode) {
            143 -> {
                info("Terminated")
                return
            }
        }

        mActivitySessionExecuteListener?.onCompleted(exitCode)
    }

    override fun onOutputLine(line: String) {
        info(line)

        mActivitySessionExecuteListener?.onOutputLine(line)

        buffer.append(line)

        val logging = listOf(
                SPAM,
                DEBUG,
                VERBOSE,
                INFO,
                NOTICE,
                WARNING,
                SUCCESS,
                ERROR,
                CRITICAL
        )

        logging.forEach {
            val str = ":$it"
            val end = buffer.indexOf(str)
            if (end > -1) {
                val message = buffer.subSequence(0, end)
                buffer.delete(0, end + str.length)
                val preferences = PreferenceManager.getDefaultSharedPreferences(mCtx)
                val level = checkNotNull(preferences.getString("pref_log_level", "25")).toInt()

                if (it == SPAM && 5 >= level) mActivityOnOutputLineListener?.spam(message)
                else if (it == DEBUG && 10 >= level) mActivityOnOutputLineListener?.debug(message)
                else if (it == VERBOSE && 15 >= level) mActivityOnOutputLineListener?.verbose(message)
                else if (it == INFO && 20 >= level) mActivityOnOutputLineListener?.info(message)
                else if (it == NOTICE && 25 >= level) mActivityOnOutputLineListener?.notice(message)
                else if (it == WARNING && 30 >= level) mActivityOnOutputLineListener?.warning(message)
                else if (it == SUCCESS && 35 >= level) mActivityOnOutputLineListener?.success(message)
                else if (it == ERROR && 40 >= level) mActivityOnOutputLineListener?.error(message)
                else if (it == CRITICAL && 50 >= level) mActivityOnOutputLineListener?.critical(message)

                return@forEach
            }
        }
    }

    override fun onError(error: Int, reason: String) {
        super.onError(error, reason)

        mActivitySessionExecuteListener?.onError(error, reason)
    }

    override fun start(pluginClient: PluginClient, sessionId: Int, sessionKey: String): Boolean {
        var pair: Pair<Coordinate?, String?> = Pair(null, null)
        if (mActivityExecuteCommandListener != null) {
            pair = mActivityExecuteCommandListener.positionAndFlagForSelectedMarker()
        }
        val (location, flag) = pair
        val preferences = PreferenceManager.getDefaultSharedPreferences(mCtx)
        val server = preferences.getString("pref_server", null)
        val country = preferences.getString("pref_country", "gb")
        val tcp = preferences.getBoolean("pref_tcp", false)
        val load = preferences.getString("pref_max_load", "70")
        val top = preferences.getString("pref_top_servers", "10")
        val pings = preferences.getString("pref_pings", "3")
        val rules = preferences.getBoolean("pref_force_fw", false)
        val p2p = preferences.getBoolean("pref_p2p", false)
        val dedicated = preferences.getBoolean("pref_dedicated", false)
        val double = preferences.getBoolean("pref_double", false)
        val onion = preferences.getBoolean("pref_tor", false)
        val obfuscated = preferences.getBoolean("pref_anti_ddos", false)
        val netflix = preferences.getBoolean("pref_netflix", false)
        test = preferences.getBoolean("pref_test", false)
        //val internally_allowed = args.internally_allowed
        val patch = preferences.getBoolean("pref_skip_dns_patch", false)
        val silent = preferences.getBoolean("pref_silent", false)
        nvram = preferences.getBoolean("pref_nvram", false)
        //val openvpn_options = args.openvpn_options
        val openvpn = "--syslog openpyn"
        val options = StringBuilder("openpyn")

        if (server != null && !server.isEmpty())
            options.append(" --server $server")
        else if (flag != null)
            if (flag == "gb") options.append(" uk") else options.append(" $flag")
        else if (country != null)
            if (country == "gb") options.append(" uk") else options.append(" $country")
        //if area:
        //openpyn_options += " --area " + area
        if (tcp)
            options.append(" --tcp")
        if (load != null)
            options.append(" --max-load $load")
        if (top != null)
            options.append(" --top-servers $top")
        if (pings != null)
            options.append(" --pings $pings")
        if (rules)
            options.append(" --force-fw-rules")
        if (p2p)
            options.append(" --p2p")
        if (dedicated)
            options.append(" --dedicated")
        if (double)
            options.append(" --double")
        if (onion)
            options.append(" --tor")
        if (obfuscated)
            options.append(" --anti-ddos")
        if (netflix)
            options.append(" --netflix")
        if (test)
            options.append(" --test")
        //if internally_allowed
        //open_ports = ""
        //for port_number in internally_allowed:
        //open_ports += " " + port_number
        //openpyn_options += " --allow" + open_ports
        if (patch)
            options.append(" --skip-dns-patch")
        if (silent)
            options.append(" --silent")
        if (nvram)
            options.append(" --nvram " + preferences.getString("pref_nvram_client", "5"))
        if (!openvpn.isEmpty())
            options.append(" --openvpn-options '$openvpn'")
        if (location != null)
            options.append(" --location " + location.latitude.toString() + " " + location.longitude.toString())
        val openpyn = options.toString()
        info(openpyn)
        // the file /etc/profile is only loaded for a login shell, this is a non-interactive shell
        // command = "echo \$PATH ; echo \$-"
        command = "[ -f /opt/etc/profile ] && . /opt/etc/profile ; $openpyn"
        info(command)

        if (super.start(pluginClient, sessionId, sessionKey)) {
            mActivityExecuteCommandListener?.onConnect()
            return true
        }
        return false
    }

    override fun kill(pluginClient: PluginClient, sessionId: Int, sessionKey: String): Boolean {
        stopcommand = when {
            test || nvram -> ""
            else -> "sudo openpyn --kill"
        }
        info(stopcommand)

        if (super.kill(pluginClient, sessionId, sessionKey)) {
            mActivityExecuteCommandListener?.onDisconnect()
            return true
        }
        return false
    }

    override fun convertDataToInt(data: String): Int {
        return data.toInt()
    }
}
