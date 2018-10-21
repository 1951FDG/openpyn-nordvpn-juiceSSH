package io.github.sdsstudios.nvidiagpumonitor.controllers

import androidx.lifecycle.MutableLiveData
import android.content.Context
import android.os.Handler
import androidx.preference.PreferenceManager
import com.sonelli.juicessh.pluginlibrary.PluginClient
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.info
import org.jetbrains.anko.longToast

class OpenpynController(
        ctx: Context,
        liveData: MutableLiveData<Int>
) : BaseController(ctx, liveData), AnkoLogger {

    override val regex = Regex("""\d+""")

    override fun start(pluginClient: PluginClient,
            sessionId: Int,
            sessionKey: String) {

        val preferences = PreferenceManager.getDefaultSharedPreferences(mCtx)

        val server = preferences.getString("pref_server", null)
        //val country = args.country
        //val area = args.area
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
        val test = preferences.getBoolean("pref_test", false)
        //val internally_allowed = args.internally_allowed
        val patch = preferences.getBoolean("pref_skip_dns_patch", false)
        val silent = preferences.getBoolean("pref_silent", false)
        val nvram = preferences.getBoolean("pref_nvram", false)
        //val openvpn_options = args.openvpn_options
        val openvpn = "--syslog openpyn"
        val (location, flag) = mainActivity.positionAndFlagForSelectedMarker()

        val options = StringBuilder()

        if (server != null && !server.isEmpty())
            options.append(" --server $server")
        else if (flag != null)
            options.append(flag)
        else if (country != null)
            options.append(country)

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
        command = "[ -f /opt/etc/profile ] && . /opt/etc/profile ; openpyn $openpyn"

        super.start(pluginClient, sessionId, sessionKey)
    }

    override fun kill(pluginClient: PluginClient,
                       sessionId: Int,
                       sessionKey: String) {
        stopcommand = "sudo openpyn --kill"

        super.kill(pluginClient, sessionId, sessionKey)
    }

    override fun convertDataToInt(data: String): Int {
        return data.toInt()
    }

    @Suppress("MagicNumber")
    override fun onOutputLine(line: String) {
        info(line)
        mCtx.longToast(line)
        if (line.startsWith("CONNECTING TO SERVER", true)) {
            Handler().postDelayed({
                mainActivity.updateMasterMarker(true)
            }, 10000)
        }
    }
}
