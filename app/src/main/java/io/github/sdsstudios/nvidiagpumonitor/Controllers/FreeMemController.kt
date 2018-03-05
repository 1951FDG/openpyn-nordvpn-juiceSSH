package io.github.sdsstudios.nvidiagpumonitor.Controllers

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import kotlin.reflect.KProperty0

/**
 * Created by Seth on 05/03/18.
 */

class FreeMemController(
        ctx: Context,
        liveData: KProperty0<MutableLiveData<Int>>
) : BaseController(ctx, liveData) {

    override val regex = Regex("""\d+""")

    override val command = "nvidia-smi --query-gpu=memory.free --format=csv"

    override fun convertDataToInt(data: String): Int {
        return data.toInt()
    }
}