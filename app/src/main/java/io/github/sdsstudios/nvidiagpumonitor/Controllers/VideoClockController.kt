package io.github.sdsstudios.nvidiagpumonitor.controllers

import androidx.lifecycle.MutableLiveData
import android.content.Context
import com.sonelli.juicessh.pluginlibrary.PluginClient

/**
 * Created by Seth on 05/03/18.
 */

class VideoClockController(
        ctx: Context,
        liveData: MutableLiveData<Int>
) : BaseController(ctx, liveData) {

    override val regex = Regex("""\d+""")

    //override val command = "nvidia-smi --query-gpu=clocks.video --format=csv"

    override fun convertDataToInt(data: String): Int {
        return data.toInt()
    }
}
