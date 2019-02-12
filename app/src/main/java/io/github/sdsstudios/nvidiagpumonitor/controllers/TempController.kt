package io.github.sdsstudios.nvidiagpumonitor.controllers

import android.content.Context
import androidx.lifecycle.MutableLiveData

/**
 * Created by Seth on 05/03/18.
 */
class TempController(
    ctx: Context,
    liveData: MutableLiveData<Int>
) : BaseController(ctx, liveData) {
    override val regex = Regex("""\d+""")
    //override val command = "nvidia-smi --query-gpu=temperature.gpu --format=csv"
    override fun convertDataToInt(data: String): Int {
        return data.toInt()
    }
}