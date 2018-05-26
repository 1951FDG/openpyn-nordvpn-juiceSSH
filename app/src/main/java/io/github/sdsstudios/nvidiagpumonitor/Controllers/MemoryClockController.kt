package io.github.sdsstudios.nvidiagpumonitor.controllers

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import com.sonelli.juicessh.pluginlibrary.PluginClient

/**
 * Created by Seth on 05/03/18.
 */

class MemoryClockController(
        ctx: Context,
        liveData: MutableLiveData<Int>
) : BaseController(ctx, liveData) {

    override val regex = Regex("""\d+""")

    //override val command = "nvidia-smi --query-gpu=clocks.mem --format=csv"

    override fun convertDataToInt(data: String): Int {
        return data.toInt()
    }
}