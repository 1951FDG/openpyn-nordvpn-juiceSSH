package io.github.sdsstudios.nvidiagpumonitor.listeners

interface OnOutputLineListener {
    fun spam(message: CharSequence)
    fun debug(message: CharSequence)
    fun verbose(message: CharSequence)
    fun info(message: CharSequence)
    fun notice(message: CharSequence)
    fun warning(message: CharSequence)
    fun success(message: CharSequence)
    fun error(message: CharSequence)
    fun critical(message: CharSequence)
}
