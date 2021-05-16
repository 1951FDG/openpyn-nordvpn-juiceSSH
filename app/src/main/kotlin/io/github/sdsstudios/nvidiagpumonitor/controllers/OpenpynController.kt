package io.github.sdsstudios.nvidiagpumonitor.controllers

import android.content.Context
import androidx.preference.PreferenceManager
import com.sonelli.juicessh.pluginlibrary.PluginClient
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionExecuteListener
import io.github.getsixtyfour.openpyn.R
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnCommandExecuteListener
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnOutputLineListener
import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate
import mu.KotlinLogging

class OpenpynController(
    context: Context,
    private var mSessionExecuteListener: OnSessionExecuteListener?,
    private var mCommandExecuteListener: OnCommandExecuteListener?,
    private var mOnOutputLineListener: OnOutputLineListener?
) : BaseController(context) {

    private val logger = KotlinLogging.logger {}

    @Suppress("MagicNumber")
    private var buffer = StringBuilder(256)

    override fun onDestroy() {
        mSessionExecuteListener = null
        mCommandExecuteListener = null
        mOnOutputLineListener = null
    }

    @Suppress("ComplexMethod", "HardCodedStringLiteral")
    override fun connect(pluginClient: PluginClient, sessionId: Int, sessionKey: String): Boolean {
        fun code(iso: String): String = when (iso) {
            "gb" -> "uk"
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
        val test = preferences.getBoolean("pref_test", false)
        val patch = preferences.getBoolean("pref_skip_dns_patch", false)
        var silent = preferences.getBoolean("pref_silent", false)
        val nvram = preferences.getBoolean("pref_nvram", false)
        var openvpn = ""
        val options = StringBuilder()
        val openvpnmgmt = preferences.getBoolean(mCtx.getString(R.string.pref_openvpnmgmt_key), false)
        val host = preferences.getString(
            mCtx.getString(R.string.pref_openvpnmgmt_host_key), mCtx.getString(R.string.pref_openvpnmgmt_host_default)
        )!!
        val port = preferences.getString(
            mCtx.getString(R.string.pref_openvpnmgmt_port_key), mCtx.getString(R.string.pref_openvpnmgmt_port_default)
        )!!
        val str = preferences.getString(mCtx.getString(R.string.pref_openvpnmgmt_password_key), "")!!
        val password = preferences.getString(mCtx.getString(R.string.pref_openvpnmgmt_password_file_key), "")!!
        val username = preferences.getString(mCtx.getString(R.string.pref_openvpnmgmt_username_key), "")!!
        val userpass = preferences.getString(mCtx.getString(R.string.pref_openvpnmgmt_userpass_key), "")!!

        if (openvpnmgmt) {
            silent = true

            openvpn = "--management $host ${port.toInt()}"

            if (password.isNotEmpty() && str.isNotEmpty()) {
                openvpn = "$openvpn '$password'"
            }

            if (username.isNotEmpty() && userpass.isNotEmpty() && !nvram) {
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

        if (tcp) options.append(" --tcp")
        if (load.isNotEmpty()) options.append(" --max-load $load")
        if (top.isNotEmpty()) options.append(" --top-servers $top")
        if (rules) options.append(" --force-fw-rules")
        if (p2p) options.append(" --p2p")
        if (dedicated) options.append(" --dedicated")
        if (double) options.append(" --double")
        if (onion) options.append(" --tor")
        if (obfuscated) options.append(" --anti-ddos")
        if (netflix) options.append(" --netflix")
        if (test) options.append(" --test")
        if (patch) options.append(" --skip-dns-patch")
        if (silent) options.append(" --silent")
        if (nvram) options.append(" --nvram " + preferences.getString("pref_nvram_client", "5"))
        if (openvpn.isNotEmpty()) options.append(" --openvpn-options '$openvpn'")
        if (location != null) options.append(" --location ${location.latitude} ${location.longitude}")
        // The file /etc/profile is only loaded for a login shell, this is a non-interactive shell
        command = "[ -f /etc/profile ] && . /etc/profile ; [ -f /opt/etc/profile ] && . /opt/etc/profile ; openpyn$options"
        /*command = "echo \$PATH ; echo \$-"*/
        logger.info(command)

        return super.connect(pluginClient, sessionId, sessionKey)
    }

    @Suppress("HardCodedStringLiteral")
    override fun disconnect(pluginClient: PluginClient, sessionId: Int, sessionKey: String): Boolean {
        command = "openpyn --kill"
        logger.info(command)

        return super.disconnect(pluginClient, sessionId, sessionKey)
    }

    @Suppress("ComplexMethod", "HardCodedStringLiteral", "MagicNumber", "SpellCheckingInspection")
    override fun onCompleted(exitCode: Int) {
        super.onCompleted(exitCode)

        val map by lazy {
            mapOf(
                1 to "HUP",
                2 to "INT",
                3 to "QUIT",
                4 to "ILL",
                5 to "TRAP",
                6 to "ABRT",
                8 to "FPE",
                9 to "KILL",
                11 to "SEGV",
                14 to "ALRM",
                15 to "TERM"
            )
        }

        when (exitCode) {
            // command terminated successfully (0)
            0 -> {
                logger.info { "Openpyn finished with exit code $exitCode" }
            }
            // command terminated unsuccessfully (1)
            // command not executable (126)
            // command not found (127)
            1, 126, 127, 128 -> {
                val message = "Openpyn finished with non-zero exit code $exitCode"
                mOnOutputLineListener?.error(message)
                logger.error(message)
            }
            // command terminated with signal
            129, 130, 131, 132, 133, 134, 136, 137, 139, 142 -> {
                val message = "Openpyn terminated with signal SIG${map[exitCode - 128]}"
                mOnOutputLineListener?.error(message)
                logger.error(message)
            }
            // command terminated with signal SIGTERM (143 - 128 = 15)
            143 -> {
                val message = "Openpyn terminated with signal SIGTERM"
                mOnOutputLineListener?.warning(message)
                logger.warn(message)
            }
            else -> {
                val message = "Openpyn finished with non-zero exit code $exitCode"
                mOnOutputLineListener?.error(message)
                logger.error(Exception()) { message }
            }
        }

        mSessionExecuteListener?.onCompleted(exitCode)
    }

    @Suppress("HardCodedStringLiteral", "MagicNumber")
    override fun onOutputLine(line: String) {
        logger.info(line)

        when {
            line.startsWith("Shutting down safely, please wait until process exits", true) -> {
                mCommandExecuteListener?.onDisconnect()
            }
            line.startsWith("Killing the running openvpn process", true) -> {
                mCommandExecuteListener?.onDisconnect()
            }
            line.startsWith("CONNECTING TO SERVER", true) -> {
                mCommandExecuteListener?.onConnect()
            }
        }

        buffer.append(line)
        val logging = listOf(SPAM, DEBUG, VERBOSE, INFO, NOTICE, WARNING, SUCCESS, ERROR, CRITICAL)
        val preferences = PreferenceManager.getDefaultSharedPreferences(mCtx)
        val level = checkNotNull(preferences.getString("pref_log_level", "25")).toInt()

        logging.forEach {
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

        mSessionExecuteListener?.onOutputLine(line)
    }

    override fun onError(reason: Int, message: String) {
        mOnOutputLineListener?.error(message)
        logger.error(message)

        mSessionExecuteListener?.onError(reason, message)
    }

    @Suppress("HardCodedStringLiteral")
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
