package io.github.sdsstudios.nvidiagpumonitor.controllers

import androidx.lifecycle.MutableLiveData
import android.content.Context
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
        val country_code = preferences.getString("pref_country", "uk")
        //val country = args.country
        //val area = args.area
        val tcp = preferences.getBoolean("pref_tcp", false)
        val max_load = preferences.getString("pref_max_load", "70")
        val top_servers = preferences.getString("pref_top_servers", "10")
        val pings = preferences.getString("pref_pings", "3")
        val force_fw_rules = preferences.getBoolean("pref_force_fw", false)
        val p2p = preferences.getBoolean("pref_p2p", false)
        val dedicated = preferences.getBoolean("pref_dedicated", false)
        val double_vpn = preferences.getBoolean("pref_double", false)
        val tor_over_vpn = preferences.getBoolean("pref_tor", false)
        val anti_ddos = preferences.getBoolean("pref_anti_ddos", false)
        val netflix = preferences.getBoolean("pref_netflix", false)
        val test = preferences.getBoolean("pref_test", false)
        //val internally_allowed = args.internally_allowed
        val skip_dns_patch = preferences.getBoolean("pref_skip_dns_patch", false)
        val silent = preferences.getBoolean("pref_silent", false)
        val nvram = preferences.getBoolean("pref_nvram", false)
        //val openvpn_options = args.openvpn_options
        val openvpn_options = "--syslog openpyn"
        val (location, flag) = mainActivity.positionAndFlagForSelectedMarker()

        val openpyn_options = StringBuilder()

        if (server != null && !server.isEmpty())
            openpyn_options.append(" --server $server")
        else if (flag != null)
            openpyn_options.append(flag)
        else if (country_code != null)
            openpyn_options.append(country_code)

        //if area:
        //openpyn_options += " --area " + area
        if (tcp)
            openpyn_options.append(" --tcp")
        if (max_load != null)
            openpyn_options.append(" --max-load $max_load")
        if (top_servers != null)
            openpyn_options.append(" --top-servers $top_servers")
        if (pings != null)
            openpyn_options.append(" --pings $pings")
        if (force_fw_rules)
            openpyn_options.append(" --force-fw-rules")
        if (p2p)
            openpyn_options.append(" --p2p")
        if (dedicated)
            openpyn_options.append(" --dedicated")
        if (double_vpn)
            openpyn_options.append(" --double")
        if (tor_over_vpn)
            openpyn_options.append(" --tor")
        if (anti_ddos)
            openpyn_options.append(" --anti-ddos")
        if (netflix)
            openpyn_options.append(" --netflix")
        if (test)
            openpyn_options.append(" --test")
        //if internally_allowed
        //open_ports = ""
        //for port_number in internally_allowed:
        //open_ports += " " + port_number
        //openpyn_options += " --allow" + open_ports
        if (skip_dns_patch)
            openpyn_options.append(" --skip-dns-patch")
        if (silent)
            openpyn_options.append(" --silent")
        if (nvram)
            openpyn_options.append(" --nvram " + preferences.getString("pref_nvram_client", "5"))
        if (!openvpn_options.isEmpty())
            openpyn_options.append(" --openvpn-options '$openvpn_options'")
        if (location != null)
            openpyn_options.append(" --location " + location.latitude.toString() + " " + location.longitude.toString())

        val openpyn = openpyn_options.toString()
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

    override fun onOutputLine(line: String) {
        debug(line)

        mCtx.longToast(line)
        if (line.startsWith("CONNECTING TO SERVER", true)) {
            android.os.Handler().postDelayed({
                mainActivity.updateMasterMarker()
            }, 10000)
        }
    }
}