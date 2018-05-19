package io.github.sdsstudios.nvidiagpumonitor.Controllers

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import com.sonelli.juicessh.pluginlibrary.PluginClient
import kotlin.math.roundToInt

/**
 * Created by Seth on 05/03/18.
 */

class PowerController(
        ctx: Context,
        liveData: MutableLiveData<Int>
) : BaseController(ctx, liveData) {

    override val regex = Regex("""\d+((.)\d+)?""")

    //override val command = "nvidia-smi --query-gpu=power.draw --format=csv"

    override fun start(pluginClient: PluginClient,
                       sessionId: Int,
                       sessionKey: String) {
    }

    override fun convertDataToInt(data: String): Int {
        return data.toFloat().roundToInt()
    }
}