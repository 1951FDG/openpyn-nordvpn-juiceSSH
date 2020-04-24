package io.github.sdsstudios.nvidiagpumonitor.controllers

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import com.sonelli.juicessh.pluginlibrary.PluginClient
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener
import io.github.getsixtyfour.openpyn.R
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnCommandExecuteListener
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnOutputLineListener
import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

class OpenpynController(
    ctx: Context,
    liveData: MutableLiveData<Int>,
    private var mSessionExecuteListener: OnSessionExecuteListener?,
    private var mCommandExecuteListener: OnCommandExecuteListener?,
    private var mOnOutputLineListener: OnOutputLineListener?
) : BaseController(ctx, liveData), AnkoLogger {

    override val regex: Regex = Regex("""\d+""")
    private var test = false
    private var nvram = false
    @Suppress("MagicNumber")
    private var buffer = StringBuilder(256)

    @Suppress("MagicNumber")
    override fun onCompleted(exitCode: Int) {
        super.onCompleted(exitCode)

        info("$exitCode")

        when (exitCode) {
            143 -> {
                info("Terminated")
                return
            }
            -1 -> {
                info("Terminated \"abnormal\"")
                info(startCommand)
            }
        }

        mSessionExecuteListener?.onCompleted(exitCode)
    }

    override fun onOutputLine(line: String) {
        info(line)

        when {
            line.startsWith("Killing the running openvpn", true) -> mCommandExecuteListener?.onDisconnect()
            line.startsWith("CONNECTING TO SERVER", true) -> mCommandExecuteListener?.onConnect()
        }

        mSessionExecuteListener?.onOutputLine(line)

        buffer.append(line)
        val logging = listOf(
            SPAM, DEBUG, VERBOSE, INFO, NOTICE, WARNING, SUCCESS, ERROR, CRITICAL
        )
        val preferences = PreferenceManager.getDefaultSharedPreferences(mCtx)
        val level = checkNotNull(preferences.getString("pref_log_level", "25")).toInt()

        @Suppress("MagicNumber") logging.forEach {
            val str = ":$it"
            val end = buffer.indexOf(str)
            if (end > -1) {
                val message = buffer.subSequence(0, end)
                buffer.delete(0, end + str.length)

                mOnOutputLineListener?.let { listener: OnOutputLineListener ->
                    when {
                        0 == level -> return@let
                        it == SPAM && 5 >= level -> listener.spam(message)
                        it == DEBUG && 10 >= level -> listener.debug(message)
                        it == VERBOSE && 15 >= level -> listener.verbose(message)
                        it == INFO && 20 >= level -> listener.info(message)
                        it == NOTICE && 25 >= level -> listener.notice(message)
                        it == WARNING && 30 >= level -> listener.warning(message)
                        it == SUCCESS && 35 >= level -> listener.success(message)
                        it == ERROR && 40 >= level -> listener.error(message)
                        it == CRITICAL && 50 >= level -> listener.critical(message)
                    }
                }

                return@forEach
            }
        }
    }

    override fun onError(error: Int, reason: String) {
        super.onError(error, reason)

        mSessionExecuteListener?.onError(error, reason)
    }

    @Suppress("ComplexMethod", "LongMethod")
    override fun start(pluginClient: PluginClient, sessionId: Int, sessionKey: String): Boolean {
        fun code(iso: String): String = when (iso) {
            "gb" -> "uk" // "domain":"uk1000.nordvpn.com", res["domain"][:2]
            else -> iso
        }

        var pair: Pair<Coordinate?, String> = Pair(null, "")
        mCommandExecuteListener?.let { pair = it.positionAndFlagForSelectedMarker() }

        val (location, flag) = pair
        val preferences = PreferenceManager.getDefaultSharedPreferences(mCtx)
        val server = preferences.getString("pref_server", "")!!
        val country = preferences.getString("pref_country", "")!!
        val tcp = preferences.getBoolean("pref_tcp", false)
        val load = preferences.getString("pref_max_load", "")!!
        val top = preferences.getString("pref_top_servers", "")!!
        /*val pings = preferences.getString("pref_pings", "")!!*/
        val rules = preferences.getBoolean("pref_force_fw", false)
        val p2p = preferences.getBoolean("pref_p2p", false)
        val dedicated = preferences.getBoolean("pref_dedicated", false)
        val double = preferences.getBoolean("pref_double", false)
        val onion = preferences.getBoolean("pref_tor", false)
        val obfuscated = preferences.getBoolean("pref_anti_ddos", false)
        val netflix = preferences.getBoolean("pref_netflix", false)
        test = preferences.getBoolean("pref_test", false)
        val patch = preferences.getBoolean("pref_skip_dns_patch", false)
        var silent = preferences.getBoolean("pref_silent", false)
        nvram = preferences.getBoolean("pref_nvram", false)
        var openvpn = ""
        val options = StringBuilder("openpyn")
        val openvpnmgmt = preferences.getBoolean(mCtx.getString(R.string.pref_openvpnmgmt_key), false)
        val host = preferences.getString(
            mCtx.getString(R.string.pref_openvpnmgmt_host_key), mCtx.getString(R.string.pref_openvpnmgmt_host_default)
        )!!
        val port = preferences.getString(
            mCtx.getString(R.string.pref_openvpnmgmt_port_key), mCtx.getString(R.string.pref_openvpnmgmt_port_default)
        )!!
        val password = preferences.getString(mCtx.getString(R.string.pref_openvpnmgmt_password_file_key), "")!!
        val username = preferences.getString(mCtx.getString(R.string.pref_openvpnmgmt_username_key), "")!!
        val userpass = preferences.getString(mCtx.getString(R.string.pref_openvpnmgmt_userpass_key), "")!!

        if (openvpnmgmt) {
            silent = true

            openvpn = "--management $host ${port.toInt()}"

            if (password.isNotEmpty()) {
                openvpn = "$openvpn $password"
            }

            if (username.isNotEmpty() && userpass.isNotEmpty()) {
                openvpn = "$openvpn --auth-nocache --auth-retry interact --management-hold --management-query-passwords"
                options.append(" --application")
            }

            openvpn = "$openvpn --management-up-down"
        }

        when {
            flag.isNotEmpty() -> options.append(" ${code(flag)}")
            server.isNotEmpty() -> options.append(" --server $server")
            country.isNotEmpty() -> options.append(" ${code(country)}")
        }
        /*if area:
            openpyn_options += " --area " + area*/
        if (tcp) options.append(" --tcp")
        if (load.isNotEmpty()) options.append(" --max-load $load")
        if (top.isNotEmpty()) options.append(" --top-servers $top")
        /*if (pings.isNotEmpty())
            options.append(" --pings $pings")*/
        if (rules) options.append(" --force-fw-rules")
        if (p2p) options.append(" --p2p")
        if (dedicated) options.append(" --dedicated")
        if (double) options.append(" --double")
        if (onion) options.append(" --tor")
        if (obfuscated) options.append(" --anti-ddos")
        if (netflix) options.append(" --netflix")
        if (test) options.append(" --test")
        /*if internally_allowed
        open_ports = ""
        for port_number in internally_allowed:
        open_ports += " " + port_number
        openpyn_options += " --allow" + open_ports*/
        if (patch) options.append(" --skip-dns-patch")
        if (silent) options.append(" --silent")
        if (nvram) options.append(" --nvram " + preferences.getString("pref_nvram_client", "5"))
        if (openvpn.isNotEmpty()) options.append(" --openvpn-options '$openvpn'")
        if (location != null) options.append(" --location ${location.latitude} ${location.longitude}")
        val openpyn = "$options"
        info(openpyn)
        // The file /etc/profile is only loaded for a login shell, this is a non-interactive shell
        startCommand = "[ -f /opt/etc/profile ] && . /opt/etc/profile ; $openpyn"
        /*startCommand = "echo \$PATH ; echo \$-"*/
        info(startCommand)

        if (super.start(pluginClient, sessionId, sessionKey)) {
            return true
        }
        return false
    }

    override fun kill(pluginClient: PluginClient, sessionId: Int, sessionKey: String): Boolean {
        stopCommand = when {
            test || nvram -> ""
            else -> "sudo openpyn --kill"
        }
        info(stopCommand)

        if (super.kill(pluginClient, sessionId, sessionKey)) {
            return true
        }
        return false
    }

    override fun convertDataToInt(data: String): Int {
        return data.toInt()
    }

    override fun onDestroy() {
        mSessionExecuteListener = null
        mCommandExecuteListener = null
        mOnOutputLineListener = null
    }

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
}
